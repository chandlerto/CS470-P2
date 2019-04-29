import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;


/**
 * Constantly runs and listens for client requests.
 * When a client request is received, a new thread is constructed
 * to deal with multiple requests in parallel.
 */

public class ThreadDriver {
	
	public static void main(String[] args) throws SocketException 
	{
		ServerSocket serverSocket = null;
		try
		{
			serverSocket = new ServerSocket( 80 );
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		while (true)
		{
			try 
			{
				Socket newSocket = serverSocket.accept();
				System.out.println(" accepted client request \n" );
				new ConnectionHandlerThread( newSocket ).start();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}
}