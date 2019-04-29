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
			if((parameterLine = clientInputStream.readLine()) != null) {}

			// Create full URL from Host header value and file from request line
			String fullURL = headerMap.get("Host") + fileRequest;
			// Append "http://" if it's not present
			if(!fullURL.substring(0, 4).equals("http"))
			{
				fullURL = "http://" + fullURL;
			}

			// Checks if proxy contains a cached copy and the request is non-conditional
			// Returns the cached copy if so
			if(CacheManager.inCache(fullURL) && !headerMap.containsKey("If-Modified-Since"))
			{
        		String cachedResponse = new String(CacheManager.getResponse(fullURL));
				clientOutputStream.write(cachedResponse);
				return;
			}

			// Create HttpURLConnection
			URL connectionURL = new URL(fullURL);
			HttpURLConnection serverConnection = (HttpURLConnection)connectionURL.openConnection();
			
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
				DataOutputStream connectionWriteStream = new DataOutputStream(serverConnection.getOutputStream());
				connectionWriteStream.writeBytes(parameterLine);
				connectionWriteStream.close();
			}
			
			serverConnection.connect();
			
			// Create full response line
			int responseCode = serverConnection.getResponseCode();
			String responseLine = httpVersion + responseCode + serverConnection.getResponseMessage();
			String copyToCache = "";
			
			if(responseCode == 200)
			{
				BufferedReader connectionReadStream = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
				String responseBodyLine;
				while ((responseBodyLine = connectionReadStream.readLine()) != null)
				{
					responseBodyLine += "/r/n";
					clientOutputStream.write(responseBodyLine);
					copyToCache += responseBodyLine;
				}
				connectionReadStream.close();
				CacheManager.storeResponse(fullURL, copyToCache.getBytes());
			}
			else
			{
				clientOutputStream.write(responseLine);
			}
		}
		catch (IOException e)
		{
			return;
		}
		
	}
}
