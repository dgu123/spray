package slingong.datastore.context.support.gae;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

public class JCacheMessageDaoTest {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig 
	= new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig 
	= new LocalMemcacheServiceTestConfig();
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
	
	protected String messageCode1 = "messageCode1";
	protected Locale enUsLocale = new Locale( "en", "US");
	protected String enUsMessage1 = "enUsMessage1 is for English speaker in the United States";

	@Test
	public void test1GetMessagesMap() throws Throwable {
		Assert.assertNull( JCacheMessageDao.getMessagesMap( messageCode1));
	}
	
	@Test
	public void test1SetMessagesMap() throws Throwable {
		Map<Locale, String> messagesMap = new LinkedHashMap<Locale, String>();
			messagesMap.put( enUsLocale, enUsMessage1);
		JCacheMessageDao.setMessagesMap( messageCode1, messagesMap);
			Assert.assertEquals( messagesMap, JCacheMessageDao.getMessagesMap( messageCode1));
	}
	
	@Test
	public void test1SetMessageToMemcache() throws Throwable {
		JCacheMessageDao.setMessageToMemcache( messageCode1, enUsLocale, enUsMessage1);
			Assert.assertEquals( 
					enUsMessage1, 
					((Map<Locale, String>)JCacheMessageDao.getMessagesMap( messageCode1)).get(enUsLocale)
					);
	}
	
	@Test
	public void test1RemoveMessagesMap() throws Throwable {
		test1SetMessagesMap();
		JCacheMessageDao.removeMessagesMap( messageCode1);
		Assert.assertNull( JCacheMessageDao.getMessagesMap( messageCode1));
	}
	
	@Test
	public void test1RemoveMessageFromMemcache() throws Throwable {
		test1SetMessageToMemcache();
		JCacheMessageDao.removeMessageFromMemcache( messageCode1, enUsLocale);
		Assert.assertNull( 
				((Map<Locale, String>)JCacheMessageDao.getMessagesMap( messageCode1)).get( enUsLocale)
				);
	}
	
	@Test
	public void testClearMessageMemcache() throws Throwable {
		test1SetMessagesMap();
		Assert.assertFalse( JCacheMessageDao.messageMemcache.isEmpty());
		JCacheMessageDao.clearMessageMemcache();
		Assert.assertTrue( JCacheMessageDao.messageMemcache.isEmpty());
	}
	
	/**
	 * This will fail until the next issue is fixed: http://code.google.com/p/googleappengine/issues/detail?id=5935
	 * @throws Throwable
	 */
	@Ignore
	@Test
	public void testSetMessageMemcacheName() throws Throwable {
		JCacheMessageDao.setMessageToMemcache( messageCode1, enUsLocale, enUsMessage1);
			Assert.assertEquals( 
					enUsMessage1, 
					((Map<Locale, String>)JCacheMessageDao.getMessagesMap( messageCode1)).get(enUsLocale)
					);
		String messageMemcacheName1 = this.getClass().getSimpleName();
		JCacheMessageDao.setMessageMemcacheName( messageMemcacheName1);
		Assert.assertNull( 
				String.format(
						"Expected null but %1$s", 
						((Map<Locale, String>)JCacheMessageDao.getMessagesMap( messageCode1)).get(enUsLocale)
						), 
				((Map<Locale, String>)JCacheMessageDao.getMessagesMap( messageCode1)).get(enUsLocale)
				);
	}
	
		// ----------------------------------------------------------------------------------------
		// Due to failure of testSetMessageMemcacheName test method, just purely checking functionality of 
		// CacheManager's registerCache method. And found that it's not working appropriately.
		// Filed the issue at http://code.google.com/p/googleappengine/issues/detail?id=5935
		@Ignore
		@Test
		public void testRegisterCache() throws Throwable {
			// By using JCache ------------------------------------------------------------------------
			CacheManager cacheManager = CacheManager.getInstance();
			CacheFactory cacheFactory = cacheManager.getCacheFactory();
			
			String cache1Name = "CacheOne";
			Assert.assertNull( cacheManager.getCache( cache1Name));
			Cache cache1 = cacheFactory.createCache( Collections.emptyMap());
				String key1 = "DummyKey1";
				String value1 = "DummyValue1";
				cache1.put( key1, value1);
			cacheManager.registerCache( cache1Name, cache1);
			
			String cache2Name = "CacheTwo";
			Assert.assertNull( cacheManager.getCache( cache2Name));
			Cache cache2 = cacheFactory.createCache( Collections.emptyMap());
				Assert.assertFalse( cache1.equals( cache2));
				Assert.assertNull( 
						String.format( "Expected null but %1$s", cache2.get( key1)), 
						cache2.get( key1)
						);												// This failed
			cacheManager.registerCache( cache2Name, cache2);
			
			Cache cache2Copy = cacheManager.getCache( cache2Name);
				Assert.assertTrue( cache2.equals( cache2Copy));
				Assert.assertNull( 
						String.format( "Expected null but %1$s", cache2Copy.get( key1)), 
						cache2Copy.get( key1)
						);												// This failed
				String value2 = "DummyValue2";
				cache2Copy.put( key1, value2);
			
			Cache cache1Copy = cacheManager.getCache( cache1Name);
				Assert.assertTrue( cache1.equals( cache1Copy));
				Assert.assertEquals( value1, cache1Copy.get( key1));	// This failed
			// ----------------------------------------------------------------------------------------
				
			// By using MemcacheService ---------------------------------------------------------------
			String memcacheService1Name = "MemcacheServiceOne";
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheService1Name);
				Assert.assertNull( memcacheService1.get( key1));
				String key3 = "DummyKey3";
				String value3 = "DummyValue3";
				memcacheService1.put( key3, value3);
			String memcacheService2Name = "MemcacheServiceTwo";
			MemcacheService memcacheService2 = MemcacheServiceFactory.getMemcacheService( memcacheService2Name);
				Assert.assertNull( memcacheService2.get( key3));
			// ----------------------------------------------------------------------------------------
		}
		
		@Ignore
		@Test
		public void test2RegisterCache() throws Throwable {
			CacheManager cacheManager = CacheManager.getInstance();
			CacheFactory cacheFactory = cacheManager.getCacheFactory();
			
			String cache1Name = "CacheOne";
			Assert.assertNull( cacheManager.getCache( cache1Name));
			Cache cache1 = cacheFactory.createCache( Collections.emptyMap());
			cacheManager.registerCache( cache1Name, cache1);
				String key1 = "DummyKey1";
				String value1 = "DummyValue1";
				cache1.put( key1, value1);
			
			String cache2Name = "CacheTwo";
			Assert.assertNull( cacheManager.getCache( cache2Name));
			Cache cache2 = cacheFactory.createCache( Collections.emptyMap());
			cacheManager.registerCache( cache2Name, cache2);
				Assert.assertFalse( cache1.equals( cache2));
				Assert.assertNull( 
						String.format( "Expected null but %1$s", cache2.get( key1)), 
						cache2.get( key1)
						);
		}
		// ----------------------------------------------------------------------------------------
	
	// Tests via methods of CachedMessageDao interface --------------------------------------------
	
	@Test
	public void testGetCachedMessages() throws Throwable {
		JCacheMessageDao jcacheMessageDao = new JCacheMessageDao();
		Assert.assertNull( jcacheMessageDao.getCachedMessages( messageCode1));
	}
	// --------------------------------------------------------------------------------------------
}
