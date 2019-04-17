import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.GregorianCalendar;
import java.util.Locale;

//TODO Get out whiteboard or something and look at logic structure for reading client message
//Might need to completely restructure it to make it flow better

public class ProxyServer extends Thread {
	
	final int SERVER_PORT = 8716;
	final int CLIENT_PORT = 8717;
	InetAddress clientIp;
	String urlString;
	DatagramSocket socket;
	DatagramPacket packet;
	
	HashMap<String, String> htmlFiles;
	HashMap<String, GregorianCalendar> lastGetTimes;
	
	public ProxyServer() throws SocketException
	{
		super();
		htmlFiles = new HashMap<String, String>();
		lastGetTimes = new HashMap<String, GregorianCalendar>();
		socket = new DatagramSocket(SERVER_PORT);
	}
	
	/**
	 * Receives packets from clients and returns corresponding file.
	 */
	public void run()
	{
		while (true)
		{
			try 
			{
				socket.receive(packet);
				System.out.println("Got a request from a client");
				sendFileToClient();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Parses the client's packet and sends the corresponding file.
	 * @throws IOException
	 */
	private void sendFileToClient() throws IOException 
	{
		urlString = packetUrl();
		int statusCode = HttpURLConnection.HTTP_NOT_MODIFIED;
		
		//If we have a version of the file, need to check if it is up to date
		if (htmlFiles.containsKey(urlString))
		{
			System.out.println("I have this file in my cache.");
			System.out.println("Using GET If-Modified-Since");
			
			//Send HTTP IFModSince request
			URL url = new URL(urlString);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			//If this is a normal GET request, check if current copy is up to date
			if (isInitialRequest())
			{
				connection.setRequestProperty("If-Modified-Since", httpFormatTime(lastGetTimes.get(urlString)));
			}
			
			//If GET IfModSince, use the older time between the client's IfModSince time
			//and the time this cached file was last saved by the server
			else
			{
				connection.setRequestProperty("If-Modified-Since", getOlderTime());
			}
			
			connection.setReadTimeout(10000);
			connection.connect();
			
			//For now, status code is always put at start of packet
			statusCode = Integer.parseInt(connection.getHeaderField(null).substring(10, 13));
			System.out.println("This is the status code: " + statusCode);
			
			//If the file hasn't been modified, update lastGetTime
			if (statusCode == HttpURLConnection.HTTP_NOT_MODIFIED)
			{
				lastGetTimes.put(urlString, new GregorianCalendar());
				String message = "304: The file has not been modified since the specified time.";
				byte[] bytes = message.getBytes();
				DatagramPacket outPacket = new DatagramPacket(
						bytes, bytes.length, packet.getAddress(), CLIENT_PORT);
				socket.send(outPacket);
			}
			//If the file has been modified, update lastGetTime, download new file
			else if (statusCode == HttpURLConnection.HTTP_OK)
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader
						(connection.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String input = null;
				
				while ((input = reader.readLine()) != null)
				{
					builder.append(input + "\n");
				}
				
				htmlFiles.put(urlString, builder.toString());
				lastGetTimes.put(urlString, new GregorianCalendar());
			}
			
			//This response means that server could not understand the request due to invalid syntax.
			else if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST)
			{
				String errorMessage = "400: Bad Request. Your request had invalid syntax.";
				System.out.println("Sending to client: " + errorMessage);
				byte[] bytes = errorMessage.getBytes();
				DatagramPacket outPacket = new DatagramPacket(
						bytes, bytes.length, packet.getAddress(), CLIENT_PORT);
				socket.send(outPacket);	
			}
			
			/**
			 * The request method is not supported by the server and cannot be handled. 
			 * The only methods that servers are required to support (and therefore that must not return this code) are GET and HEAD.
			 * 
			 * The website doesn't support GET ifmodsince. In that case, we will download the file to a temporary variable.
			 * If it is the same as our copy, we act like we got a NOT_MODIFIED message,
			 * and if it isn't, we act like we got a OK message.
			 * 
			 */
			
			else if (statusCode == HttpURLConnection.HTTP_NOT_IMPLEMENTED)
			{
				HttpURLConnection newConnection = (HttpURLConnection) url.openConnection();
				newConnection.setRequestMethod("GET");
				newConnection.setReadTimeout(10000);
				newConnection.connect();
				
				statusCode = Integer.parseInt(newConnection.getHeaderField(null).substring(10, 13));
				System.out.println("After making a normal GET request, this is the status code: " + statusCode);
				if (statusCode == HttpURLConnection.HTTP_OK)
				{
					BufferedReader reader = new BufferedReader(new InputStreamReader
							(connection.getInputStream()));
					StringBuilder builder = new StringBuilder();
					String input = null;
					
					while ((input = reader.readLine()) != null)
					{
						builder.append(input + "\n");
					}
					
					String downloadedFile = builder.toString();
					//Our copy is up to date. If our copy's time is older than the client's,
					//then the client's is also up to date, and we just send a 304 message.
					if (downloadedFile.equals(htmlFiles.get(urlString)))
					{
						statusCode = HttpURLConnection.HTTP_NOT_MODIFIED;
					}
					else
					{
						htmlFiles.put(urlString, downloadedFile);
					}
					lastGetTimes.put(urlString, new GregorianCalendar());
				}
			}
		}
		
		//If we don't have a version of the file, just download it 
		else 
		{
			System.out.println("I don't have the file.");
			if (isInitialRequest()) 
			{
				System.out.println("This is a normal GET request");
				System.out.println("I will download the file and send it");
				URL url = new URL(urlString);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("GET");
				connection.setReadTimeout(10000);
				connection.connect();
				
				statusCode = Integer.parseInt(connection.getHeaderField(null).substring(10, 13));
				//TODO Check to make sure have code of 200? Make plans for if not 200
				
				BufferedReader reader = new BufferedReader(new InputStreamReader
						(connection.getInputStream()));
				StringBuilder builder = new StringBuilder();
				String input = null;
				while ((input = reader.readLine()) != null)
				{
					builder.append(input + "\n");
				}
				
				htmlFiles.put(urlString, builder.toString());
				lastGetTimes.put(urlString, new GregorianCalendar());
			}
			else
			{
				System.out.println("This is a GET If-Modified-Since request");
				URL url = new URL(urlString);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setRequestProperty("If-Modified-Since", getClientTime());
				connection.setReadTimeout(10000);
				connection.connect();
				
				//TODO Insert what to do for each status code here
				//Maybe think of a way to not have redundant code? 
				//This code will probably be repeat of code above 
			}
			
		}
		
		//Send the file to the client if we don't have a bad code
		if (statusCode != HttpURLConnection.HTTP_BAD_REQUEST 
			&& (statusCode != HttpURLConnection.HTTP_NOT_MODIFIED && !isInitialRequest()))
		{
			System.out.println("Sending the file with status code: " + statusCode);
			String message = statusCode + htmlFiles.get(urlString);
			byte[] fileBytes = message.getBytes();
			DatagramPacket outPacket = new DatagramPacket(
					fileBytes, fileBytes.length, packet.getAddress(), CLIENT_PORT);
			socket.send(outPacket);
		}
	}

	/**
	 * Parses the client's packet and returns the target URL.
	 * @return the client's desired URL in String format
	 */
	private String packetUrl() 
	{
		String message = new String(packet.getData());
		int end = 0;
		while(end < message.length() && message.charAt(end) != '$') 
		{
			end++;
		}
		return message.substring(2, end);
	}

	/**
	 * @return if the client's packet is a regular GET request
	 */
	private boolean isInitialRequest() 
	{
		String message = new String(packet.getData());
		return message.charAt(0) == 'G';
	}
	
	/**
	 * @param gregorianCalendar the calendar to be converted
	 * @return the given calendar formatted in RFC 2616 (HTTP) as a String
	 */
	private String httpFormatTime(GregorianCalendar gregorianCalendar) 
	{
		SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm::s z", Locale.US);
		return s.format(gregorianCalendar.getTime());
	}
	
	/**
	 * @return String representing the earlier time of the client's time and the server's time for 
	 * when it last got this file.
	 */
	private String getOlderTime() 
	{
		String clientDate = getClientTime();
		GregorianCalendar clientTime = stringToCalendar(clientDate);
		SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm::s z", Locale.US);
		String serverDate = s.format(lastGetTimes.get(urlString).getTime());
		
		if (isBefore(clientTime, lastGetTimes.get(urlString))) 
		{
			return clientDate;
		}
		else
		{
			return serverDate;
		}
	}

	/**
	 * @return the client's time in RFC 2616 (HTTP) as a String
	 */
	private String getClientTime()
	{
		String message = new String(packet.getData());
		int end = 0;
		while(end < message.length() && message.charAt(end) != '$') 
		{
			end++;
		}
		return message.substring(end+1);
	}
	/**
	 * @param firstTime the 1st calendar
	 * @param secondTime the 2nd calendar
	 * @return if the 1st calendar is before the 2nd calendar
	 */
	private boolean isBefore(GregorianCalendar first, GregorianCalendar second) {
		return (first.getTimeInMillis() - second.getTimeInMillis() < 0);
	}

	/**
	 * @param clientDate a date formatted in RFC 2616 style
	 * @return calendar representing the given RFC 2616 date
	 */
	private GregorianCalendar stringToCalendar(String clientDate) {
		int year = Integer.parseInt(clientDate.substring(12, 16));
		int month = getMonth(clientDate.substring(8, 11));
		int dayOfMonth = Integer.parseInt(clientDate.substring(5, 7));
		int hourOfDay = Integer.parseInt(clientDate.substring(17, 19));
		int minute = Integer.parseInt(clientDate.substring(20, 22));
		int second = Integer.parseInt(clientDate.substring(24, 26));
		
		return new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
	}

	//Prob make a less dumb solution later lol
	private int getMonth(String s) {
		if (s.equals("Jan"))
			return 1;
		else if (s.equals("Feb"))
			return 2;
		else if (s.equals("Mar"))
			return 3;
		else if (s.equals("Apr"))
			return 4;
		else if (s.equals("May"))
			return 5;
		else if (s.equals("Jun"))
			return 6;
		else if (s.equals("Jul"))
			return 7;
		else if (s.equals("Aug"))
			return 8;
		else if (s.equals("Sep"))
			return 9;
		else if (s.equals("Oct"))
			return 10;
		else if (s.equals("Nov"))
			return 11;
		return 12;
	}
	
}
