import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CacheManager {
	
	private static Map<String, Byte[]> cacheHash = Collections.synchronizedMap(new HashMap<String, Byte[]>());
	
	static byte[] getResponse( String url )
	{
		byte[] response = byteArrayToByteArray( cacheHash.get( url ) );
		return response;
	}
	
	static void storeResponse( String url, byte[] responseBytes )
	{
		Byte[] byteObjects = byteArrayToByteArray( responseBytes );
		cacheHash.put( url, byteObjects );
	}
	
	static Set getUrls()
	{
		return cacheHash.keySet();
	}
	
	static Byte[] byteArrayToByteArray( byte[] bytes )
	{
		Byte[] bytesObject = new Byte[bytes.length];
		for ( int i = 0; i < bytes.length; i++)
		{
			bytesObject[i] = bytes[i];
		}
		
		return bytesObject;
	}
	
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
