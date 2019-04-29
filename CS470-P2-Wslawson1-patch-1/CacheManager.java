import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles writing and reading the proxy server's cache.
 */
public class CacheManager {
	
	private static Map<String, Byte[]> cacheHash = Collections.synchronizedMap(new HashMap<String, Byte[]>());
	
	/**
	 * Returns the corresponding file in bytes for the given url.
	 */
	static byte[] getResponse( String url )
	{
		byte[] response = byteArrayToByteArray( cacheHash.get( url ) );
		return response;
	}
	
	/**
	 * Saves the given url and bytes into the HashMap.
	 */
	static void storeResponse( String url, byte[] responseBytes )
	{
		Byte[] byteObjects = byteArrayToByteArray( responseBytes );
		cacheHash.put( url, byteObjects );
	}
	
	/**
	 * Returns the urls currently saved in the cache.
	 */
	static Set getUrls()
	{
		return cacheHash.keySet();
	}
	
	/**
	 * Converts primitive byte[] to wrapper object version.
	 */
	static Byte[] byteArrayToByteArray( byte[] bytes )
	{
		Byte[] bytesObject = new Byte[bytes.length];
		for ( int i = 0; i < bytes.length; i++)
		{
			bytesObject[i] = bytes[i];
		}
		
		return bytesObject;
	}
	
	/**
	 * Converts Byte[] wrapper object to primitive byte[].
	 */
	static byte[] byteArrayToByteArray( Byte[] bytesObject )
	{
		byte[] bytes = new byte[bytesObject.length];
		for ( int i = 0; i < bytesObject.length; i++)
		{
			bytes[i] = bytesObject[i];
		}
		
		return bytes;
	}

	static boolean inCache( String key )
	{
		return cacheHash.containsKey( key );
	}
}
