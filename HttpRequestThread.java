import java.io.*;
import java.util.*;
import java.net.*;

public class HttpRequestThread extends Thread
{
	Socket clientSocket;
	BufferedReader clientInputStream;
	BufferedWriter clientOutputStream;
	
	public HttpRequestThread(Socket s)
	{
		clientSocket = s;
	}
	
	public void run()
	{
		String clientRequest;
		
		try
		{
			// Create buffered streams to read from and write to client's socket
			clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			clientOutputStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			clientRequest = clientInputStream.readLine();
		
			// Separate and store request line arguments
			String requestMethod = clientRequest.split(" ")[0];
			String fileRequest = clientRequest.split(" ")[1];
			String httpVersion = clientRequest.split(" ")[2];
			
			// Separate and store header arguments
			String headerLine;
			Map<String, String> headerMap = new HashMap<String, String>();
			while((headerLine = clientInputStream.readLine()) != null)
			{
				if(headerLine.length() > 0)
				{
					headerMap.put(headerLine.split(": ")[0], headerLine.split(": ")[1]);
				}
				else
				{
					break;
				}
			}
			
			// Store additional parameter arguments
			String parameterLine = "";
			if(clientInputStream.ready() && (parameterLine = clientInputStream.readLine()) != null) {}

			// Create full URL from Host header value and file from request line
			//String hostName = headerMap.get("Host"); // Currently unused. Originally used to create full URL if fileRequest did not specify full address
			String fullURL = "";	
			fullURL = fileRequest;
			
			// Append "http://" if it's not present
			if(!fullURL.substring(0, 4).equals("http"))
			{
				fullURL = "http://" + fullURL;
			}

			// Checks if our proxy contains a cached copy and the request is non-conditional
			// Returns the cached copy if so and returns from thread
			if(CacheManager.inCache(fullURL) && !headerMap.containsKey("If-Modified-Since"))
			{
				clientOutputStream.write(httpVersion + " 200 OK\r\n");
				clientOutputStream.write("\r\n");
        		String cachedResponse = new String(CacheManager.getResponse(fullURL));
				clientOutputStream.write(cachedResponse);
				clientOutputStream.flush();
		        clientOutputStream.close();
		        clientInputStream.close();
		        clientSocket.close();
				return;
			}

			// Create HttpURLConnection
			URL connectionURL = new URL(fullURL);
			HttpURLConnection serverConnection = (HttpURLConnection)connectionURL.openConnection();
			serverConnection.setConnectTimeout(10000);
			
			serverConnection.setRequestMethod(requestMethod);
			// Add headers and values to connection
			for(String headerField : headerMap.keySet())
			{
				serverConnection.setRequestProperty(headerField, headerMap.get(headerField));
			}

			// Add parameter line to connection if it exists
			if(parameterLine.length() > 0)
			{
				serverConnection.setDoOutput(true);
				// I think .getOutputStream() will start the connection
				DataOutputStream connectionWriteStream = new DataOutputStream(serverConnection.getOutputStream());
				connectionWriteStream.writeBytes(parameterLine);
				connectionWriteStream.close();
			}
			
			System.out.println(clientRequest);
			
			serverConnection.connect();
			
			// Create initial response line
			int responseCode = serverConnection.getResponseCode();
			String responseLine = httpVersion + " " + responseCode + " " + serverConnection.getResponseMessage() + "\r\n";
			String copyToCache = "";
			
			if(responseCode == 200) // If the request was OK
			{
				BufferedReader connectionReadStream = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
				// Write the initial response line to client
				clientOutputStream.write(responseLine);
				// Write response headers to client
				String responseHeaderLine = "";
				for(int headerIndex = 1; serverConnection.getHeaderField(headerIndex) != null; headerIndex++)
				{
					responseHeaderLine = serverConnection.getHeaderFieldKey(headerIndex) + ": " + serverConnection.getHeaderField(headerIndex) + "\r\n";
					clientOutputStream.write(responseHeaderLine);
				}
				// Response headers are separated by a blank line
				clientOutputStream.write("\r\n");
				// Write response body to client
				String responseBodyLine;
				while ((responseBodyLine = connectionReadStream.readLine()) != null)
				{
					responseBodyLine += "\r\n";
					clientOutputStream.write(responseBodyLine);
					copyToCache += responseBodyLine;
				}
				connectionReadStream.close();
				clientOutputStream.flush();
		        clientOutputStream.close();
		        clientInputStream.close();
		        clientSocket.close();
				// Store the response body copy in the cache
				CacheManager.storeResponse(fullURL, copyToCache.getBytes());
			}
			else // Write the status line to client if error received
			{
				clientOutputStream.write(responseLine);
				clientOutputStream.flush();
		        clientOutputStream.close();
		        clientInputStream.close();
		        clientSocket.close();
			}
		}
		catch (IOException e)
		{
			return;
		}
		
	}
}
