package slingong.web.gae;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.jsr107cache.Cache;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

public class JCacheServiceTest {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig = new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig = new LocalMemcacheServiceTestConfig();
	// LocalTaskQueueTestConfig is for Slim3 use
	protected LocalTaskQueueTestConfig gaeTaskQueueTestConfig = new LocalTaskQueueTestConfig();
	{
		gaeTaskQueueTestConfig = 
			gaeTaskQueueTestConfig.setQueueXmlPath( "war\\WEB-INF\\queue.xml");
	};
	protected final LocalServiceTestHelper gaeTestHelper = 
		new LocalServiceTestHelper( gaeDatastoreTestConfig, gaeMemcacheTestConfig, gaeTaskQueueTestConfig);
	// --------------------------------------------------------------------------------------------
	
	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
	}
	@After
	public void tearDown() throws Throwable {
		gaeTestHelper.tearDown();
	}
	
	@Test
	public void test1GetCache() throws Throwable {
		String cache1Name = "TestCache1";
		Cache cache1 = JCacheService.getCache( cache1Name);
		Assert.assertTrue( cache1 instanceof Cache);
		Assert.assertTrue( cache1.isEmpty());
		Cache cache1Copy = JCacheService.getCache( cache1Name);
		Assert.assertEquals( cache1, cache1Copy);
		
		String cache2Name = "TestCache2";
		Cache cache2 = JCacheService.getCache( cache2Name);
		Assert.assertFalse( cache1.equals( cache2));
		
		Cache cache3 = JCacheService.getCache( null);
		Assert.assertTrue( cache3 instanceof Cache);
	}
	
	/**
	 * Confirmative test on memcache with regular data in usages of DatabaseMessageSource
	 */
	@Test
	public void test1Memcache() throws Throwable {
		String cache1Name = "TestCache1";
		Cache cache1 = JCacheService.getCache( cache1Name);
		
		// Confirmative test that memcache can save Map<Locale, String> object as value
		String messageCode1 = "messageCode1";
		Map<Locale, String> messagesMap = new LinkedHashMap<Locale, String>();
		cache1.put( messageCode1, messagesMap);
		Assert.assertEquals( messagesMap, cache1.get( messageCode1));
		
		Locale enUslocale = new Locale( "en", "US");
			System.out.println( 
					String.format( "enUslocale.getDisplayName() = \"%1$s\"", enUslocale.getDisplayName()));
			System.out.println( 
					String.format( "enUslocale.getLanguage() = \"%1$s\"", enUslocale.getLanguage()));
			System.out.println( 
					String.format( "enUslocale.getDisplayLanguage() = \"%1$s\"", enUslocale.getDisplayLanguage()));
			System.out.println( 
					String.format( "enUslocale.getISO3Language() = \"%1$s\"", enUslocale.getISO3Language()));
			System.out.println( 
					String.format( "enUslocale.getCountry() = \"%1$s\"", enUslocale.getCountry()));
			System.out.println( 
					String.format( "enUslocale.getDisplayCountry() = \"%1$s\"", enUslocale.getDisplayCountry()));
			System.out.println( 
					String.format( "enUslocale.getISO3Country() = \"%1$s\"", enUslocale.getISO3Country()));
			System.out.println( 
					String.format( "enUslocale.getVariant() = \"%1$s\"", enUslocale.getVariant()));
			System.out.println( 
					String.format( "enUslocale.getDisplayVariant() = \"%1$s\"", enUslocale.getDisplayVariant()));
			
		// Confirmative test for that memcache is not holding the reference to object rather holding object self 
		String enUsMessage1 = "enUsMessage1 is for English speaker in the United States";
		messagesMap.put( enUslocale, enUsMessage1);
		Assert.assertFalse( messagesMap.equals( cache1.get( messageCode1)));
			cache1.put( messageCode1, messagesMap);
			Assert.assertEquals( enUsMessage1, ((Map<Locale, String>)cache1.get( messageCode1)).get( enUslocale));
		
		// Confirmative test of usage of common Map<Locale, String> object as value to save for different key
		String enUsMessage2 = "enUsMessage2 is for English speaker in the United States";
		messagesMap.put( enUslocale, enUsMessage2);
		String messageCode2 = "messageCode2";
		cache1.put( messageCode2, messagesMap);
		Assert.assertFalse( 
				cache1.get( messageCode1).equals( cache1.get( messageCode2)));
			Assert.assertEquals( 
					enUsMessage1, ((Map<Locale, String>)cache1.get( messageCode1)).get( enUslocale));
			Assert.assertEquals( 
					enUsMessage2, ((Map<Locale, String>)cache1.get( messageCode2)).get( enUslocale));
			
		Cache cache1Copy = JCacheService.getCache( cache1Name);
		Assert.assertEquals( cache1, cache1Copy);
	}
	
	/**
	 * Test on memcache with irregular data in usages of DatabaseMessageSource
	 */
	@Test
	public void test2Memcache() throws Throwable {
		String cache1Name = "TestCache1";
		Cache cache1 = JCacheService.getCache( cache1Name);
		
		// null message code
		String nullMessageCode = null;
		Map<Locale, String> messagesMap = new LinkedHashMap<Locale, String>();
		cache1.put( nullMessageCode, messagesMap);
		Assert.assertEquals( messagesMap, cache1.get( nullMessageCode));
		
		// null locale
		String messageCode1 = "messageCode1";
		String nullLocaleMessage1 = "null locale should be able to be used as ultimate fallback locale";
		messagesMap.put( null, nullLocaleMessage1);
		cache1.put( messageCode1, messagesMap);
		Assert.assertEquals( messagesMap, cache1.get( messageCode1));
		Assert.assertEquals( 
				nullLocaleMessage1, ((Map<Locale, String>)cache1.get( messageCode1)).get( null));
		
		// null message
		messagesMap.put( null, null);
		cache1.put( messageCode1, messagesMap);
		Assert.assertNull( ((Map<Locale, String>)cache1.get( messageCode1)).get( null));
	}
}
