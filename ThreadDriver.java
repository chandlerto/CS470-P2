import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;


/*
 * Constantly runs and listens for client requests.
 * When a client request is received, a new thread is constructed
 * to deal with multiple requests in parallel.
 */

public class ThreadDriver {

	//TODO Copy the number other classes are using here
	//final static int DRIVER_PORT = ;
	
	public static void main(String[] args) throws SocketException 
	{
		cacheUpdateThread updator = new cacheUpdateThread();
		updator.start();
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
				//ServerSocket serverSocket = new ServerSocket( 80 );
				Socket newSocket = serverSocket.accept();
				System.out.println(" accepted client request \n" );
				new HttpRequestThread( newSocket ).start();
				
				//TODO What parameter to pass to thread?
				//Currently passing in the client's socket
				//Thread will then use socket.getInputStream() to get info from client
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
	}

}

// my ip: 150.243.201.182