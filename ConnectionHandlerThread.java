import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;

public class ConnectionHandlerThread extends Thread
{
    //client and server refer to what the socket is connecting to, not what it is serving as.
    private Socket clientSocket = null;
    private Socket serverSocket = null;
    InetAddress clientInet = null;
    BufferedReader clientReader;
    BufferedWriter clientWriter;

    ConnectionHandlerThread( Socket clientSocket )
    {
        this.clientSocket = clientSocket;
        InetAddress clientInet = clientSocket.getInetAddress();
        try
        {
            this.clientSocket.setSoTimeout( 2000 );
            clientReader = new BufferedReader( new InputStreamReader( clientSocket.getInputStream() ) );
            clientWriter = new BufferedWriter( new OutputStreamWriter( clientSocket.getOutputStream() ) ); 
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        System.out.println( " initialized thread \n" );
    }

    public void run( )
    {
        System.out.println(" running thread \n" );
        String contents = getBufferContents();
        String method = getMethod( contents );
        System.out.println( contents );
        System.out.println();
        System.out.println("METHOD: " + method );

        if ( method.equals("GET") )
        {
            String address = getAddress( contents );
            System.out.println( "ADDRESS: " + address );
    
            //ADD SUPPORT FOR GET IF MODIFIED SINCE
            if ( hasConditionalGet( contents ) )
            {
                String getIfModifiedSince = getConditionalTime( contents );
                System.out.println("If-Modified-Since: " + getIfModifiedSince );
                try
                {
                    sendResponse( address, getIfModifiedSince );
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else if ( !CacheManager.inCache( address ) )
            {
                System.out.println( "not in cache. Sending response " );
                try
                {
                    sendResponse( address );
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            else
            {
                System.out.println( "in cache. sending response");
                try
                {
                    sendCachedResponse( address );
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getConditionalTime( String content )
    {
        int index = content.indexOf("If-Modified-Since:");
        index = index + 19;
        String date = content.substring( index, index + 29);
        return date;
        
    }

    public void sendResponse( String address, String conditionalGetTime ) throws IOException
    {
        URL serverAddress = new URL( address );
        HttpURLConnection serverConnection = (HttpURLConnection) serverAddress.openConnection();
        serverConnection.setRequestMethod("GET");
        serverConnection.setRequestProperty("If-Modified-Since", conditionalGetTime );
        serverConnection.setReadTimeout(5000);
        serverConnection.connect();

        BufferedReader in = new BufferedReader( new InputStreamReader( serverConnection.getInputStream() ) );
        String inputString;
        StringBuffer content = new StringBuffer();
        while ( ( inputString = in.readLine() ) != null )
        {
            content.append( inputString );
            content.append("\n");
        }
        in.close();

        String contentString = content.toString();
        clientWriter.write( contentString );
        clientWriter.flush();
        clientWriter.close();
        clientReader.close();
        clientSocket.close();

        System.out.println( " CONTENT: " + contentString );
        byte[] contentBytes = contentString.getBytes();
        CacheManager.storeResponse( address, contentBytes );
    }

    public void sendResponse( String address ) throws IOException
    {
        System.out.print( address );
        URL serverAddress = new URL( address );
        HttpURLConnection serverConnection = (HttpURLConnection) serverAddress.openConnection();
        serverConnection.setRequestMethod("GET");
        serverConnection.setReadTimeout(5000);
        serverConnection.connect();

        BufferedReader in = new BufferedReader( new InputStreamReader( serverConnection.getInputStream() ) );
        String inputString;
        StringBuffer content = new StringBuffer();
        while ( ( inputString = in.readLine() ) != null )
        {
            content.append( inputString );
            content.append("\n");
        }
        in.close();

        String contentString = content.toString();
        clientWriter.write( contentString );
        clientWriter.flush();
        clientWriter.close();
        clientReader.close();
        clientSocket.close();

        byte[] contentBytes = contentString.getBytes();
        CacheManager.storeResponse( address, contentBytes );
    }

    public void sendCachedResponse( String address ) throws IOException
    {
        byte[] response = CacheManager.getResponse( address );
        String responseString = new String( response );
        clientWriter.write( responseString );
        clientWriter.flush();
        clientWriter.close();
        clientReader.close();
        clientSocket.close();
    }

    public void sendErrorResponse()
    {
        //TODO
        System.out.println( "error" );
    }

    public boolean hasConditionalGet( String contents )
    {
        return contents.contains("If-Modified-Since:");
    }

    public String getMethod( String contents )
    {
        String method = contents.substring(0, 3);
        return method;
    }

    public String getAddress( String contents )
    {
        String address = contents.substring(4);
        address = address.substring(0, address.indexOf(' ') );
        return address;
    }

    public String getBufferContents()
    {
        String line;
        String content = "";

        try
        {
            while (( line = clientReader.readLine() ) != null )
            {
                if ( line.length() == 0 )
                {
                    break;
                }
            content = content + line;
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }
        return content;
    }
}