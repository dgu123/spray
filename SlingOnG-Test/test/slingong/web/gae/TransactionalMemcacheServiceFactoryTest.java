package slingong.web.gae;

import java.io.Serializable;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

public class TransactionalMemcacheServiceFactoryTest {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig = new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig = new LocalMemcacheServiceTestConfig();
	protected final LocalServiceTestHelper gaeTestHelper = 
		new LocalServiceTestHelper( gaeDatastoreTestConfig, gaeMemcacheTestConfig);
	// --------------------------------------------------------------------------------------------

	protected TransactionalMemcacheServiceFactory transactionalMemcacheServiceFactory; 
	
	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
		
		TransactionalMemcacheServiceFactory.instanceMap.clear();
		transactionalMemcacheServiceFactory  = new TransactionalMemcacheServiceFactory();
	}
	@After
	public void tearDown() throws Throwable {
		gaeTestHelper.tearDown();
	}

	protected String getMethodName() {
		return Thread.currentThread().getStackTrace()[ 2].getMethodName();
	}
	
	@Test
	public void testGetInstance1() {
		String memcacheNamespaceStr = getMethodName();
		
		String memcacheNamespaceStr1 = memcacheNamespaceStr + "a";
		MemcacheService memcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespaceStr1);
			Assert.assertTrue( memcacheService1 instanceof TransactionalMemcacheServiceImpl);
			Assert.assertEquals( 
					memcacheNamespaceStr1,
					memcacheService1.getNamespace()
					);
			Assert.assertEquals( 
					1,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
			Assert.assertEquals(
					TransactionalMemcacheServiceImpl.DefaultKeySetKey, 
					((TransactionalMemcacheServiceImpl)memcacheService1).getKeySetKey()
					);
			
		Assert.assertEquals( 
				memcacheService1, 
				transactionalMemcacheServiceFactory.getInstance( memcacheNamespaceStr1)
				);
			Assert.assertEquals( 
					1,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
			
		String memcacheNamespaceStr2 = memcacheNamespaceStr + "b";
		MemcacheService memcacheService2 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespaceStr2);
			Assert.assertEquals( 
					memcacheNamespaceStr2,
					memcacheService2.getNamespace()
					);
			Assert.assertEquals( 
					2,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
		Assert.assertEquals( 
				memcacheService2, 
				transactionalMemcacheServiceFactory.getInstance( memcacheNamespaceStr2)
				);
			Assert.assertEquals( 
					2,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
		
		String keySetKey = "testGetInstance_keySetKey";
		MemcacheService memcacheService2a
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespaceStr2, keySetKey, null, null);
			Assert.assertEquals( 
					2,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
			Assert.assertEquals(
					keySetKey, 
					((TransactionalMemcacheServiceImpl)memcacheService2a).getKeySetKey()
					);
		
		TransactionalMemcacheServiceTransactionHelper originalTransactionHelper 
		= TransactionalMemcacheServiceImpl.getTransactionalMemcacheServiceTransactionHelper();
		
		TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper
		= new TransactionalMemcacheServiceTransactionHelperImpl();
		memcacheService2a
		= transactionalMemcacheServiceFactory.getInstance( 
				memcacheNamespaceStr2, null, transactionalMemcacheServiceTransactionHelper, null);
			Assert.assertEquals( 
					2,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
			Assert.assertEquals(
					keySetKey, 
					((TransactionalMemcacheServiceImpl)memcacheService2a).getKeySetKey()
					);
			Assert.assertEquals(
					transactionalMemcacheServiceTransactionHelper, 
					TransactionalMemcacheServiceImpl.getTransactionalMemcacheServiceTransactionHelper()
					);
		
		TransactionalMemcacheServiceSyncHelper originalSyncHelper 
		= TransactionalMemcacheServiceImpl.getTransactionalMemcacheServiceSyncHelper();
			
		TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
		= new TransactionalMemcacheServiceSyncHelperImpl();
		memcacheService2a
		= transactionalMemcacheServiceFactory.getInstance( 
				memcacheNamespaceStr2, 
				null, 
				transactionalMemcacheServiceTransactionHelper, 
				transactionalMemcacheServiceSyncHelper
				);
			Assert.assertEquals( 
					2,
					TransactionalMemcacheServiceFactory.instanceMap.size()
					);
			Assert.assertEquals(
					keySetKey, 
					((TransactionalMemcacheServiceImpl)memcacheService2a).getKeySetKey()
					);
			Assert.assertEquals(
					transactionalMemcacheServiceTransactionHelper, 
					TransactionalMemcacheServiceImpl.getTransactionalMemcacheServiceTransactionHelper()
					);
			Assert.assertEquals(
					transactionalMemcacheServiceSyncHelper, 
					TransactionalMemcacheServiceImpl.getTransactionalMemcacheServiceSyncHelper()
					);
			
		TransactionalMemcacheServiceImpl
		.setTransactionalMemcacheServiceHelpers(
				originalTransactionHelper, originalSyncHelper);		
	}
	
	@Test
	public void testGetInstance2() {
		String memcacheNamespaceStr1 = getMethodName();
		MemcacheService memcacheService1a = MemcacheServiceFactory.getMemcacheService( memcacheNamespaceStr1);
			String key1 = memcacheNamespaceStr1 + "_Key";
			String value1 = memcacheNamespaceStr1 + "_Value";
			memcacheService1a.put( key1, value1);
			
		MemcacheService memcacheService1b 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespaceStr1);
			Assert.assertEquals( 
					memcacheNamespaceStr1,
					memcacheService1b.getNamespace()
					);
			
			String keySetKey = ((TransactionalMemcacheServiceImpl)memcacheService1b).getKeySetKey();
			Set<Serializable> keySet = (Set<Serializable>)memcacheService1a.get( keySetKey);
				keySet.add( key1);
				memcacheService1a.put( keySetKey, keySet);
			Set<Serializable> syncHelperKeySet 
			= ((TransactionalMemcacheServiceImpl)memcacheService1b)
				.getTransactionalMemcacheServiceSyncHelper().getKeySet( memcacheNamespaceStr1);
				syncHelperKeySet.add( key1);
				
			Assert.assertEquals(
					value1,
					memcacheService1b.get( key1)
					);
	}
	
	@Test
	public void testAroundAdvicedTransactionalMemcacheServiceImplInitialization() {
		String memcacheNamespaceStr = getMethodName();
		
		String memcacheNamespaceStr1 = memcacheNamespaceStr + "1";
		TransactionalMemcacheServiceImpl transactionalMemcacheServiceImpl1; 
		try {
			transactionalMemcacheServiceImpl1
			= new TransactionalMemcacheServiceImpl( memcacheNamespaceStr1, null, null, null);
			
			Assert.fail( "Expected TransactionalMemcacheServiceException exception was thrown.");
		}
		catch( TransactionalMemcacheServiceException exception) { // expected exception
			Assert.assertTrue( exception.getCause() instanceof UnsupportedOperationException);
		}
	}
	
}
