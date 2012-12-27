package slingong.web.gae;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slingong.web.gae.CopierImpl.CopierEngine;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class CopierImplTest {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());

	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig = new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig = new LocalMemcacheServiceTestConfig();
	protected final LocalServiceTestHelper gaeTestHelper = 
		new LocalServiceTestHelper( gaeDatastoreTestConfig, gaeMemcacheTestConfig);
	// --------------------------------------------------------------------------------------------

	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
	}
	@After
	public void tearDown() throws Throwable {
		gaeTestHelper.tearDown();
		
		TransactionalMemcacheServiceCommonConstant.copier.turnOffCopier();
	}
	
	@Test
	public void testGetCopierEngine() throws Throwable {
		Assert.assertFalse( TransactionalMemcacheServiceCommonConstant.copier.isCopierOn());
		
		Assert.assertTrue( 
				((CopierImpl)TransactionalMemcacheServiceCommonConstant.copier)
				.getCopierEngine() instanceof CopierEngine
				);
		
		Assert.assertTrue( TransactionalMemcacheServiceCommonConstant.copier.isCopierOn());
	}
	
	@Test
	public void testTurnOffCopier1() throws Throwable {
		Assert.assertFalse( TransactionalMemcacheServiceCommonConstant.copier.isCopierOn());
		
		String originalStr = "testTurnOffCopier1";
		String copiedStr = TransactionalMemcacheServiceCommonConstant.copier.generateCopy( originalStr);
			Assert.assertEquals( originalStr, copiedStr);
		
		Assert.assertTrue( TransactionalMemcacheServiceCommonConstant.copier.isCopierOn());
		
		TransactionalMemcacheServiceCommonConstant.copier.turnOffCopier();
		
		Assert.assertFalse( TransactionalMemcacheServiceCommonConstant.copier.isCopierOn());
	}
}
