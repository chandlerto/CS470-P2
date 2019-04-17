import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ProxyClient extends Thread {

	final int SERVER_PORT = 8716;
	final int CLIENT_PORT = 8717;
	InetAddress serverIp;
	DatagramSocket socket;
	DatagramPacket packet;
	
	GregorianCalendar lastDownloadTime;
	boolean isInitialRequest = true;
	String urlString = "google.com";
	String htmlFile;
	
	public ProxyClient(String destIp) throws UnknownHostException, SocketException 
	{
		super(destIp);
		serverIp = InetAddress.getByName(getName());
		socket = new DatagramSocket(CLIENT_PORT);
	}
	
	/**
	 * Regularly send messages to server signaling a GET message for urlString.
	 * 1st message sent signals a regular GET message.
	 * Messages after that are IfModifiedSince messages.
	 * Currently Server decides time for IfModifiedSince.
	 */
	public void run() 
	{
		while (true)
		{
			//Send request to server
			try {
				if (isInitialRequest) 
				{
					socket.send(initialRequestPacket());
					System.out.println("Sent initial request to server");
					isInitialRequest = false;
				}
				else 
				{
					socket.send(IfModSincePacket());
					System.out.println("Sent IfModSince request to server");
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			//Receive response and process it
			try {
				socket.receive(packet);
				System.out.println("Got back packet from server");
				
				// TODO Read the first 3 characters of packet for status code
				// Then do action based upon 200, 304, 400, 501
				
				//Save time this file was received to be used for IfModSince times later
				lastDownloadTime = new GregorianCalendar();
				
				//TODO Should we do something with the file?
				//Open it as html file? Doesn't say on Blackboard
				
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			//Wait a bit before resending request
			try {
				System.out.println("Sleeping for 10 seconds before resending");
				TimeUnit.SECONDS.sleep(10);
			}
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Used for 1st message to proxy server.
	 * @return packet sent to server for a regular GET message
	 * @throws IOException
	 */
	private DatagramPacket initialRequestPacket() throws IOException 
	{
		String message = "G ";
		message = message + urlString;
		DatagramPacket outPacket = new DatagramPacket(
				message.getBytes(), message.getBytes().length, serverIp, SERVER_PORT);
		return outPacket;
	}
	
	/**
	 * Used for subsequent messages to proxy server.
	 * @return packet sent to server for a GET IfModifiedSince message
	 * @throws IOException
	 */
	private DatagramPacket IfModSincePacket() throws IOException 
	{
		String message = "I ";
		message = message + urlString;
		
		GregorianCalendar cal = new GregorianCalendar();
		SimpleDateFormat s = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm::s z", Locale.US);
		message = message + '$' + s.format(cal.getTime());
	
		DatagramPacket outPacket = new DatagramPacket(
				message.getBytes(), message.getBytes().length, serverIp, SERVER_PORT);
		return outPacket;
	}
}
