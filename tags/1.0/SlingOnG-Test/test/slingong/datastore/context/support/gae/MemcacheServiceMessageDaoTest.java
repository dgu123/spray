package slingong.datastore.context.support.gae;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration( "file:test/slingong/datastore/testContext.xml")
@TransactionConfiguration( transactionManager="txManager")
public class MemcacheServiceMessageDaoTest {
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
	
	// member fields to back up initial value of each static member field of MemcacheServiceMessageDao 
	// --------------------------------------------------------------------------------------------
	protected static String messageMemcacheName = MemcacheServiceMessageDao.messageMemcacheName;
	protected static MemcacheService messageMemcache = MemcacheServiceMessageDao.messageMemcache;
	protected static MemcacheService cacheNameMessageCodesHolderMemcache 
	= MemcacheServiceMessageDao.cacheNameMessageCodesHolderMemcache;
	// --------------------------------------------------------------------------------------------
	
	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
	}
	@After
	public void tearDown() throws Throwable {
		gaeTestHelper.tearDown();
		
		// Restore initial value of each static member field of MemcacheServiceMessageDao ---------
		MemcacheServiceMessageDao.messageMemcacheName = messageMemcacheName;
		MemcacheServiceMessageDao.messageMemcache = messageMemcache;
		MemcacheServiceMessageDao.cacheNameMessageCodesHolderMemcache = cacheNameMessageCodesHolderMemcache;
		// ----------------------------------------------------------------------------------------
	}
	
	@Test
	public void test1PutCacheNameMessageCodesHolder() {
		String cacheName1 = "CacheNameOne";
		String messageCodesHolderName1 = "MessageCodesHolder";
		MemcacheServiceMessageDao.putCacheNameMessageCodesHolder( cacheName1, messageCodesHolderName1);
		
		CacheNameMessageCodesHolderModel cacheNameMessageCodesHolderModel 
		= MemcacheServiceMessageDao.readCacheNameMessageCodesHolderModel();
		
		Assert.assertEquals( 1, cacheNameMessageCodesHolderModel.getCacheNameList().size());
			Assert.assertEquals( cacheName1, cacheNameMessageCodesHolderModel.getCacheNameList().get( 0));
		Assert.assertEquals( 1, cacheNameMessageCodesHolderModel.getMessageCodesHolderList().size());
			Assert.assertEquals( 
					messageCodesHolderName1, 
					cacheNameMessageCodesHolderModel.getMessageCodesHolderList().get( 0)
					);
			
			Assert.assertEquals( 
					messageCodesHolderName1, 
					cacheNameMessageCodesHolderModel.getMessageCodesHolder( cacheName1)
					);
	} // public void test1PutCacheNameMessageCodesHolder()
	
	@Test
	public void test1InitCacheNameMessageCodesHolderMemcache() {
		MemcacheServiceMessageDao.initCacheNameMessageCodesHolderMemcache();
			Assert.assertEquals( 
					MemcacheServiceMessageDao.CacheNameMessageCodesHolderMemcacheName,
					MemcacheServiceMessageDao.cacheNameMessageCodesHolderMemcache.getNamespace()
					);
			LinkedHashMap<String, String> cacheNameMessageCodesHolderMap 
			= (LinkedHashMap<String, String>)MemcacheServiceMessageDao
				.cacheNameMessageCodesHolderMemcache.get( 
					MemcacheServiceMessageDao.KeyNameForCacheNameMessageCodesHolderMap);
			Assert.assertEquals( 0, cacheNameMessageCodesHolderMap.size());
			
		String cacheName1 = "CacheNameOne";
		String messageCodesHolderName1 = "MessageCodesHolder";
		MemcacheServiceMessageDao.putCacheNameMessageCodesHolder( cacheName1, messageCodesHolderName1);
		MemcacheServiceMessageDao.initCacheNameMessageCodesHolderMemcache();
			cacheNameMessageCodesHolderMap 
			= (LinkedHashMap<String, String>)MemcacheServiceMessageDao
				.cacheNameMessageCodesHolderMemcache.get( 
					MemcacheServiceMessageDao.KeyNameForCacheNameMessageCodesHolderMap);
			Assert.assertEquals( 1, cacheNameMessageCodesHolderMap.size());
			Assert.assertEquals( 
					messageCodesHolderName1, 
					cacheNameMessageCodesHolderMap.get( cacheName1)
					);
	} // public void test1InitCacheNameMessageCodesHolderMemcache()
	
	@Test
	public void test1GetKeyNameOfCacheNameMessageCodesHolder() {
		String memcacheService1Name = "MemcacheServiceOne";
		Assert.assertNull( 
				MemcacheServiceMessageDao.getKeyNameOfCacheNameMessageCodesHolder( memcacheService1Name)
				);
	} // public void test1GetKeyNameOfCacheNameMessageCodesHolder()
	
	@Test
	public void test2GetKeyNameOfCacheNameMessageCodesHolder() {
		String memcacheService1Name = "MemcacheServiceOne";
		String messageCodesHolder1Name = "MessageCodesHolderOne";
		MemcacheServiceMessageDao.putCacheNameMessageCodesHolder( 
				memcacheService1Name, messageCodesHolder1Name);
		MemcacheServiceMessageDao.initCacheNameMessageCodesHolderMemcache();
		
		Assert.assertEquals( 
				messageCodesHolder1Name, 
				MemcacheServiceMessageDao.getKeyNameOfCacheNameMessageCodesHolder( memcacheService1Name)
				);
		
	} // public void test1GetKeyNameOfCacheNameMessageCodesHolder()
	
	@Test
	public void test1GetMemcacheService() {
		String memcacheService1Name = "MemcacheServiceOne";
		MemcacheService memcacheService1 
		= MemcacheServiceMessageDao.getMemcacheService( memcacheService1Name);
			Assert.assertEquals(
					memcacheService1Name, 
					memcacheService1.getNamespace()
					);
			Assert.assertEquals(
					0, 
					((List<String>)memcacheService1.get( 
							MemcacheServiceMessageDao.DefaultKeyNameForKeysListName)).size()
					);
			
			LinkedHashMap<String, String> cacheNameMessageCodesHolderMap 
			= (LinkedHashMap<String, String>)MemcacheServiceMessageDao
				.cacheNameMessageCodesHolderMemcache.get( 
					MemcacheServiceMessageDao.KeyNameForCacheNameMessageCodesHolderMap);
			Assert.assertEquals( 1, cacheNameMessageCodesHolderMap.size());
				Assert.assertEquals( 
						MemcacheServiceMessageDao.DefaultKeyNameForKeysListName, 
						cacheNameMessageCodesHolderMap.get( memcacheService1Name)
						);
			
			
		String memcacheService2Name = "MemcacheServiceTwo";
		String messageCodesHolder2Name = "MessageCodesHolderTwo";
		MemcacheServiceMessageDao.putCacheNameMessageCodesHolder( 
				memcacheService2Name, messageCodesHolder2Name);
		String memcacheService3Name = "MemcacheServiceThree";
		String messageCodesHolder3Name = "MessageCodesHolderThree";
		MemcacheServiceMessageDao.putCacheNameMessageCodesHolder( 
				memcacheService3Name, messageCodesHolder3Name);
		
		MemcacheServiceMessageDao.initCacheNameMessageCodesHolderMemcache();
		
		MemcacheService memcacheService2 
		= MemcacheServiceMessageDao.getMemcacheService( memcacheService2Name);
			Assert.assertEquals(
					memcacheService2Name, 
					memcacheService2.getNamespace()
					);
			Assert.assertEquals(
					0, 
					((List<String>)memcacheService2.get( messageCodesHolder2Name)).size()
					);
		
		MemcacheService memcacheService3 
		= MemcacheServiceMessageDao.getMemcacheService( memcacheService3Name);
			Assert.assertEquals(
					memcacheService3Name, 
					memcacheService3.getNamespace()
					);
			Assert.assertEquals(
					0, 
					((List<String>)memcacheService3.get( messageCodesHolder3Name)).size()
					);
			
		cacheNameMessageCodesHolderMap 
		= (LinkedHashMap<String, String>)MemcacheServiceMessageDao
			.cacheNameMessageCodesHolderMemcache.get( 
				MemcacheServiceMessageDao.KeyNameForCacheNameMessageCodesHolderMap);
		Assert.assertEquals( 3, cacheNameMessageCodesHolderMap.size());
			Assert.assertEquals( 
					MemcacheServiceMessageDao.DefaultKeyNameForKeysListName, 
					cacheNameMessageCodesHolderMap.get( memcacheService1Name)
					);
			Assert.assertEquals( 
					messageCodesHolder2Name, 
					cacheNameMessageCodesHolderMap.get( memcacheService2Name)
					);
			Assert.assertEquals( 
					messageCodesHolder3Name, 
					cacheNameMessageCodesHolderMap.get( memcacheService3Name)
					);
	} // public void test1GetMemcacheService()
	
}
