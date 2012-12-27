package slingong.datastore;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration( "file:test/slingong/datastore/testContext.xml")
@TransactionConfiguration( transactionManager="txManager")
@Aspect
public class Slim3AnnotationTransactionAspectTest extends TestBookModelsArranger {
/*	
	protected void setUpLoggers() {
		java.util.logging.Logger julLogger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME);
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( this.getClass().getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( Slim3PlatformTransactionManager.class.getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( Slim3AnnotationTransactionAspect.class.getName());
		julLogger.setLevel( Level.FINEST);
	}
*/	
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
/*	{
		setUpLoggers();
	};
*/
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
	
	@Autowired
	protected Slim3PlatformTransactionManager slim3PlatformTransactionManager;
	protected InOrder mockedSlim3TxMangerInOrder;
	
	@Autowired
	protected Slim3AnnotationTransactionAspect slim3AnnotationTransactionAspect;
	
	protected static int globalTransactionNotReadOnlyMethodCallCount;
	protected static int readOnlyTransactionalMethodCallCount;
	protected static int inproperPersistenceTransactionalMethodCallCount;
	
	@Before
	public void setUp() {
		gaeTestHelper.setUp();
		
		// Confirmations on Spring ApplicationContext status --------------------------------------
		Assert.assertTrue( slim3PlatformTransactionManager instanceof Slim3PlatformTransactionManager);
		Assert.assertTrue( slim3AnnotationTransactionAspect instanceof Slim3AnnotationTransactionAspect);
		// ----------------------------------------------------------------------------------------
		
		mockedSlim3TxMangerInOrder = Mockito.inOrder( slim3PlatformTransactionManager);
		
		globalTransactionNotReadOnlyMethodCallCount = 0;
		readOnlyTransactionalMethodCallCount = 0;
		inproperPersistenceTransactionalMethodCallCount = 0;
	}
	
	@After
	public void tearDown() {
		Mockito.reset( slim3PlatformTransactionManager);
		
		gaeTestHelper.tearDown();
	}
	
	@Transactional( readOnly=true)
	protected void inproperPersistenceInReadOnlyTransaction() throws Throwable {
		prepTestModels();
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
	}
	
	@org.aspectj.lang.annotation.After( 
		"Slim3AnnotationTransactionAspect.callingGlobalTransactionNotReadOnlyMethods()")
	public void afterCallingGlobalTransactionNotReadOnlyMethods() {
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					"globalTransactionNotReadOnlyMethodCallCount = " + 
					++globalTransactionNotReadOnlyMethodCallCount
					);
		}
	}
	
	@org.aspectj.lang.annotation.After( 
		"Slim3AnnotationTransactionAspect.executingMethodInAtTransactionalReadOnlyType( atTx)")
	public void afterExecutingMethodInAtTransactionalReadOnlyType( Transactional atTx) {
		readOnlyTransactionalMethodCallCount++;
	}
	
	@org.aspectj.lang.annotation.After( 
		"Slim3AnnotationTransactionAspect.executingReadOnlyTransactionalMethod( atTx)")
	public void afterExecutingReadOnlyTransactionalMethod( Transactional atTx) {
		readOnlyTransactionalMethodCallCount++;
	}
	
	@org.aspectj.lang.annotation.After( 
			"Slim3AnnotationTransactionAspect.inproperPersistenceAtTransactionalReadOnlyType( atTx)")
	public void afterInproperPersistenceAtTransactionalReadOnlyType( 
			Transactional atTx, JoinPoint.EnclosingStaticPart enclosingStaticPart) {
		inproperPersistenceTransactionalMethodCallCount++;
	}
	
	@org.aspectj.lang.annotation.After( 
			"Slim3AnnotationTransactionAspect.inproperPersistenceAtReadOnlyTransactionalMethod( atTx)")
	public void afterInproperPersistenceAtReadOnlyTransactionalMethod( 
			Transactional atTx, JoinPoint.EnclosingStaticPart enclosingStaticPart) {
		inproperPersistenceTransactionalMethodCallCount++;
	}
	
	/* Do not export this to JavaDoc since this is AspectJ's advise method.
	 * This will show info level log when non-read-only methods of Slim3's GlobalTransaction class are  
	 * being executed.
	 */
	@org.aspectj.lang.annotation.Before( 
			"Slim3AnnotationTransactionAspect.callingGlobalTransactionNotReadOnlyMethods()")
	public void beforeExecutionOfGlobalTransactionNotReadOnlyMethods( JoinPoint joinPoint) {
		if ( logger.isDebugEnabled()) {
			logger.debug( "Executing non-read-only method of Slim3 GlobalTransaction: " + joinPoint.getSignature().toString());
		}
	}
	
	@Test
	public void testInproperPersistencePointcuts() throws Throwable {
		inproperPersistenceInReadOnlyTransaction();
		Assert.assertEquals( 1, globalTransactionNotReadOnlyMethodCallCount);
		Assert.assertEquals( 1, readOnlyTransactionalMethodCallCount);
		Assert.assertEquals( 1, inproperPersistenceTransactionalMethodCallCount);
	}
}
