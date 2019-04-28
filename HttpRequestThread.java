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
			clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			clientOutputStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			clientRequest = clientInputStream.readLine();
		
			String requestMethod = clientRequest.split(" ")[0];
			String fileRequest = clientRequest.split(" ")[1];
			String httpVersion = clientRequest.split(" ")[2];
			
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
			
			String parameterLine = "";
			if((parameterLine = clientInputStream.readLine()) != null) {}
			
			String fullURL = headerMap.get("Host") + fileRequest;
			if(!fullURL.substring(0, 4).equals("http"))
			{
				fullURL = "http://" + fullURL;
			}
			URL connectionURL = new URL(fullURL);
			HttpURLConnection serverConnection = (HttpURLConnection)connectionURL.openConnection();
			
			serverConnection.setRequestMethod(requestMethod);
			for(String headerField : headerMap.keySet())
			{
				serverConnection.setRequestProperty(headerField, headerMap.get(headerField));
			}
			
			if(parameterLine.length() > 0)
			{
				serverConnection.setDoOutput(true);
				DataOutputStream connectionWriteStream = new DataOutputStream(serverConnection.getOutputStream());
				connectionWriteStream.writeBytes(parameterLine);
				connectionWriteStream.close();
			}
			
			serverConnection.connect();
			
			int responseCode = serverConnection.getResponseCode();
			String responseLine = httpVersion + responseCode + serverConnection.getResponseMessage();
			
			if(responseCode == 200)
			{
				BufferedReader connectionReadStream = new BufferedReader(new InputStreamReader(serverConnection.getInputStream()));
				String responseBodyLine;
				while ((responseBodyLine = connectionReadStream.readLine()) != null)
				{
					clientOutputStream.write(responseBodyLine);
				}
				connectionReadStream.close();
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
