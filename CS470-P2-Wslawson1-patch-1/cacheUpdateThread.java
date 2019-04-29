import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * Singleton class used to update each url in the cache.
 * The thread updates each url's corresponding bytes,
 * sleeps for 50 seconds, and loops again.
 */
public class cacheUpdateThread extends Thread
{
	public void run()
	{
		while(true)
		{	
			Iterator urls = CacheManager.getUrls().iterator();
			URL url;
			while ( urls.hasNext() )
			{
				try {
					url = new URL( (String) urls.next());
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setReadTimeout(0);
					connection.connect();
					int status = connection.getResponseCode();
					BufferedReader in = new BufferedReader( new InputStreamReader(connection.getInputStream()));
					String inputString;
					StringBuffer content = new StringBuffer();
					while (( inputString = in.readLine()) != null)
					{
						content.append(inputString);
						content.append("\n");
					}					
					in.close();
					String contentString = content.toString();
					byte[] contentBytes = contentString.getBytes();
					CacheManager.storeResponse(url.toString(), contentBytes );
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try
			{
				Thread.sleep(50000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
}
