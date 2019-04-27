import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CacheManager {
	
	private static Map<String, String> cacheHash = Collections.synchronizedMap(new HashMap<String, String>());
	
	static String getResponse( String url )
	{
			return cacheHash.get( url );
	}
	
	static void storeResponse( String url, String responseBytes )
	{
		cacheHash.put( url, responseBytes );
	}
	
	static Set getUrls()
	{
		return cacheHash.keySet();
	}
}
