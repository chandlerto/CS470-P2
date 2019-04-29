import java.util.Base64;
import java.util.Iterator;

public class cacheTester {

	public static void main(String[] args) {
		String a = "hello world";
		String b = "goodbye world";
		
		CacheManager.storeResponse( "http://borax.truman.edu/370/", a.getBytes() );
		CacheManager.storeResponse( "http://vh216602.truman.edu/agarvey/", b.getBytes() );
		
		Iterator itr = CacheManager.getUrls().iterator();
		cacheUpdateThread updator = new cacheUpdateThread();
		while ( itr.hasNext() )
		{
			String current = (String) itr.next();
			System.out.println( current  );
			System.out.println( new String(CacheManager.getResponse( current )));
		}
		updator.start();
		System.out.println("started updator");
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Iterator itr2 = CacheManager.getUrls().iterator();
		while ( itr2.hasNext() )
		{
			String current = (String) itr2.next();
			byte[] response = CacheManager.getResponse( current );
			String s = new String(response);

			System.out.println( current  );
			System.out.println( s );
		}
		
	}
	

}
