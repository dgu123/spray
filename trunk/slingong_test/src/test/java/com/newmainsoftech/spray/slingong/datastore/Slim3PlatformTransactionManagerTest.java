package com.newmainsoftech.spray.slingong.datastore;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSuspensionNotSupportedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager.GlobalTransactionState;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorBook;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Book;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration( "file:target/test-classes/com/newmainsoftech/spray/slingong/testContext.xml")
@TransactionConfiguration( transactionManager="txManager")
public class Slim3PlatformTransactionManagerTest extends TestBookModelsArranger {
	
	protected boolean debugEnabledInAbstractPlatformTransactionManager = false;
		// This should be the same value as logger.isDebugEnabled() in 
		// AbstractPlatformTransactionManager class returns. 
/* Commented out since changing to use SLF4J from JCL(Apache Jakarta Common Logging) 
	protected void setUpLoggers() {
		java.util.logging.Logger julLogger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME);
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( AbstractPlatformTransactionManager.class.getName());
		julLogger.setLevel( Level.FINEST);
			// Even setting JUL logger level as above additionally to setting in log4j.properties file and 
			// logging.properties file, in org.springframework.transaction.support.AbstractPlatformTransactionManager,  
			// debug level log won't be shown.  
			// However, debugEnabled boolean local member field (of what value is obtained by logger.isDebugEnabled() 
			// method) in AbstractPlatformTransactionManager class was true.
			// Don't know why debug level log won't be shown. 
			// It was confirmed by getTransaction method in AbstractPlatformTransactionManager class.
		julLogger = Logger.getLogger( ExtendedAnnotationTransactionAspect.class.getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( Slim3AnnotationTransactionAspect.class.getName());
		julLogger.setLevel( Level.FINEST);
	}
*/
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
/*	{ // Commented out since changing to use SLF4J from JCL(Apache Jakarta Common Logging)
		setUpLoggers();
		debugEnabledInAbstractPlatformTransactionManager 
		= LogFactory.getLog( Slim3PlatformTransactionManager.class).isDebugEnabled();
	};
*/
	
	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig = new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig = new LocalMemcacheServiceTestConfig();
	// LocalTaskQueueTestConfig is for Slim3 use
	protected LocalTaskQueueTestConfig gaeTaskQueueTestConfig = new LocalTaskQueueTestConfig();
	{
		gaeTaskQueueTestConfig = 
			gaeTaskQueueTestConfig.setQueueXmlPath( "src\\test\\webapp\\WEB-INF\\queue.xml");
	};
	protected final LocalServiceTestHelper gaeTestHelper = 
		new LocalServiceTestHelper( gaeDatastoreTestConfig, gaeMemcacheTestConfig, gaeTaskQueueTestConfig);
	// --------------------------------------------------------------------------------------------
	
	@Autowired
	protected ApplicationContext applicationContext;
	
	@Autowired
	protected Slim3PlatformTransactionManager slim3PlatformTransactionManager;
	protected InOrder mockedSlim3TxMangerInOrder;
	
	@Autowired
	protected AnnotationTransactionAspect annotationTransactionAspect;
/*	@Autowired
	protected Slim3AnnotationTransactionAspect annotationTransactionAspect;
*/
	
	protected void verifyNoActiveGlobalTransaction() {
		Collection<GlobalTransaction> gtxCollection = Datastore.getActiveGlobalTransactions();
		if ( gtxCollection.size() > 0) {
			if ( logger.isDebugEnabled()) {
				String message = "List of active GlobalTransaction instance(s):";
				GlobalTransaction currentGtx = Datastore.getCurrentGlobalTransaction();
				String currentGtxIdStr = ((currentGtx != null) ? currentGtx.getId() : null);
				for( GlobalTransaction gtx : Datastore.getActiveGlobalTransactions()) {
					String gtxIdStr = gtx.getId();
					message = message 
					+ String.format( 
							"%n%1$cGlobalTransaction ID:%2$s", 
							'\t', gtxIdStr
							);
					if ( gtxIdStr.equals( currentGtxIdStr)) {
						message = message + " (current GlobalTransaction)";
					}
				} // for
				logger.debug( message);
			}
			
			Assert.fail( 
					String.format( "Found %1$d active GlobalTransaction instance(s).", gtxCollection.size())
					);
		}
	} // protected void verifyNoActiveGlobalTransaction()
	
	protected void logGlobalTransactionStatus() {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"%1$cCurrent globalTransaction instance (ID:%2$s) is %3$s.",
							'\t',
							(gtx == null ? "null" : gtx.getId()), 
							(gtx == null ? "null" : (gtx.isActive() ? "active" : "inactive"))
							)
					);
		}
		for( GlobalTransaction gtxObj : Datastore.getActiveGlobalTransactions()) {
			if ( gtxObj instanceof GlobalTransaction) {
				if ( gtxObj.getId().equals( gtx.getId())) continue;
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"%1$cGlobalTransaction instance (ID:%2$s) is %3$s.", 
									'\t',
									(gtxObj == null ? "null" : gtxObj.getId()), 
									(gtxObj == null ? "null" : (gtxObj.isActive() ? "active" : "inactive"))
									)
							);
				}
			}
		} // for
	} // protected void logGlobalTransactionStatus()
	
	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
		
		// Confirmations on Spring ApplicationContext status --------------------------------------
		Assert.assertNotNull( applicationContext);
		final String annotationBeanConfigurerAspectBeanId = "annotationBeanConfigurerAspect";
			Assert.assertTrue( applicationContext.containsBean( annotationBeanConfigurerAspectBeanId));
			Assert.assertNotNull( applicationContext.getBean( annotationBeanConfigurerAspectBeanId));
		
		final String slim3PlatformTransactionManagerBeanId = "txManager";
			Assert.assertTrue( applicationContext.containsBean( slim3PlatformTransactionManagerBeanId));
			Assert.assertEquals( 
					applicationContext.getBean( slim3PlatformTransactionManagerBeanId), 
					slim3PlatformTransactionManager
					);
			Assert.assertTrue( slim3PlatformTransactionManager instanceof Slim3PlatformTransactionManager);
			Assert.assertEquals( 
					AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS, 
					slim3PlatformTransactionManager.getTransactionSynchronization()
					);
			Assert.assertFalse( slim3PlatformTransactionManager.isNestedTransactionAllowed());

		final String annotationTransactionAspectBeanId = "annotationTransactionAspect";
			Assert.assertTrue( applicationContext.containsBean( annotationTransactionAspectBeanId));
			Assert.assertEquals( 
					applicationContext.getBean( annotationTransactionAspectBeanId), 
					annotationTransactionAspect);
		final String annotationTransactionOnStaticMethodAspectBeanId 
		= "annotationTransactionOnStaticMethodAspect";
			Assert.assertTrue( 
					applicationContext.containsBean( annotationTransactionOnStaticMethodAspectBeanId));
			Assert.assertNotNull( 
					applicationContext.getBean( annotationTransactionOnStaticMethodAspectBeanId));
		// ----------------------------------------------------------------------------------------
		
		mockedSlim3TxMangerInOrder = Mockito.inOrder( slim3PlatformTransactionManager);

		// Extracting JCL (Apache Jakarta Common Logging) logger debug setting of Slim3PlatformTransactionManager.class
		debugEnabledInAbstractPlatformTransactionManager 
		= LogFactory.getLog( Slim3PlatformTransactionManager.class).isDebugEnabled();
			/* org.springframework.transaction.support.AbstractPlatformTransactionManager (what is super 
			 * class of Slim3PlatformTransactionManager class) uses logger's debug flag as one of arguments 
			 * in some calls as you see in its getTransaction method code and the signatures of the 
			 * following methods of AbstractPlatformTransactionManager:
			 *  	handleExistingTransaction
			 *  	newTransactionStatus
			 *  	prepareTransactionStatus
			 * 
			 * I also confirmed that SLF4J's LogFactory.getLog( Slim3PlatformTransactionManager.class).isDebugEnabled() 
			 * returned the same value.
			 */
		
		logGlobalTransactionStatus();
	} // public void setUp()
	
	@After
	public void tearDown() throws Throwable {
		/* Resetting mocked object slim3PlatformTransactionManager here is necessary, otherwise 
		 * unnecessary tests fails when run multiple tests in single sequence, not individually. 
		 * That symptom seems to happen because either Spring's application context is not initialized 
		 * cleanly after the execution of each test or initialization on Spring's application context 
		 * is not reflecting onto mocked slim3PlatformTransactionManager object. So, mocked 
		 * slim3PlatformTransactionManager object becomes dirty state from previous test.
		 */
		Mockito.reset( slim3PlatformTransactionManager);
		
		gaeTestHelper.tearDown();
	}
	
/* @BeforeTransaction and @AfterTransaction didn't work. I think those are for Spring's AOP proxy.
	@BeforeTransaction
	public void beforeTransaction() {
		if ( logger.isInfoEnabled()) {
			logger.info( "GlobalTransaction instances status BEFORE transactional processing:");
			logGlobalTransactionStatus();
		}
	} // public void beforeTransaction()
	
	@AfterTransaction
	public void afterTransaction() {
		if ( logger.isInfoEnabled()) {
			logger.info( "GlobalTransaction instances status AFTER transactional processing:");
			logGlobalTransactionStatus();
		}
	} // public void afterTransaction()
*/
	
	@Test
	public void testTransactionalAttributesOnStaticMethod1() throws Throwable {
		try {
			Method createNewAuthorStaticMethod 
			= Author.class.getDeclaredMethod( "createNewAuthor", new Class[]{ String.class});
			
			TransactionAttributeSource transactionAttributeSource
//			= new AnnotationTransactionAttributeSource( false);
			= annotationTransactionAspect.getTransactionAttributeSource();
			
			TransactionAttribute transactionAttribute
			= transactionAttributeSource.getTransactionAttribute( createNewAuthorStaticMethod, Author.class);
			
			/* Made addition to ExtendedAnnotationTransactionAspect, so no matter of attribute value of 
			 * @Transactional annotation, noRollBack method instead of rollback method of 
			 * Slim3PlatformTransactionManager class will be called when encountering 
			 * ConcurrentModificationException exception and DeadlineExceededException exception. 
			Assert.assertFalse( 
					transactionAttribute.rollbackOn( new ConcurrentModificationException()));
			Assert.assertFalse( 
					transactionAttribute.rollbackOn( new DeadlineExceededException()));
			*/
			
			String name = transactionAttribute.getName();
				String message
				= String.format(
						"Attribution values of @Transactional annotation on %1$s:" +
						"%n%2$cvalue=\"%3$s\"", 
						createNewAuthorStaticMethod.toString(),
						'\t', 
						(name == null ? "" : name)
						);
			int isolationLevel = transactionAttribute.getIsolationLevel();
			Assert.assertEquals( TransactionAttribute.ISOLATION_DEFAULT, isolationLevel);
				switch( isolationLevel) {
					case TransactionAttribute.ISOLATION_DEFAULT:
						message = message + String.format( "%n%1$cisolation=ISOLATION_DEFAULT", '\t');
						break;
					case TransactionAttribute.ISOLATION_READ_COMMITTED:
						message = message + String.format( "%n%1$cisolation=ISOLATION_READ_COMMITTED", '\t');
						break;
					case TransactionAttribute.ISOLATION_READ_UNCOMMITTED:
						message = message + String.format( "%n%1$cisolation=ISOLATION_READ_UNCOMMITTED", '\t');
						break;
					case TransactionAttribute.ISOLATION_REPEATABLE_READ:
						message = message + String.format( "%n%1$cisolation=ISOLATION_REPEATABLE_READ", '\t');
						break;
					case TransactionAttribute.ISOLATION_SERIALIZABLE:
						message = message + String.format( "%n%1$cisolation=ISOLATION_SERIALIZABLE", '\t');
						break;
					default:
						Assert.fail( "Unrecognizable isolation level.");
				} // switch
			
			int propagationBehavior = transactionAttribute.getPropagationBehavior();
//Default propagation behavior seems to be TransactionAttribute.PROPAGATION_REQUIRED
			Assert.assertEquals( TransactionAttribute.PROPAGATION_REQUIRED, propagationBehavior);
				switch( propagationBehavior) {
					case TransactionAttribute.PROPAGATION_MANDATORY:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_MANDATORY", '\t');
						break;
					case TransactionAttribute.PROPAGATION_NESTED:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_NESTED", '\t');
						break;
					case TransactionAttribute.PROPAGATION_NEVER:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_NEVER", '\t');
						break;
					case TransactionAttribute.PROPAGATION_NOT_SUPPORTED:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_NOT_SUPPORTED", '\t');
						break;
					case TransactionAttribute.PROPAGATION_REQUIRED:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_REQUIRED", '\t');
						break;
					case TransactionAttribute.PROPAGATION_REQUIRES_NEW:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_REQUIRES_NEW", '\t');
						break;
					case TransactionAttribute.PROPAGATION_SUPPORTS:
						message = message + String.format( "%n%1$cpropagation=PROPAGATION_SUPPORTS", '\t');
						break;
					default:
						Assert.fail( "Unrecognizable propagation behavior.");
				} // switch
			
			String qualifier = transactionAttribute.getQualifier();
				message = message + String.format( "%n%1$cqualifier=%2$s", '\t', qualifier);
			
			int timeout = transactionAttribute.getTimeout();
			Assert.assertEquals( TransactionAttribute.TIMEOUT_DEFAULT, timeout);
				message = message + String.format( "%n%1$ctimeout=%2$d", '\t', timeout);
			
			boolean readOnlyFlag = transactionAttribute.isReadOnly();
			Assert.assertFalse( readOnlyFlag);
				message = message + String.format( "%n%1$creadOnly=%2$s", '\t', String.valueOf( readOnlyFlag));
			
			if ( logger.isDebugEnabled()) {
				logger.debug( message);			
			}
		}
		catch( Throwable throwable) {
			throw throwable;
		}
	} // public void testTransactionalAttributesOnStaticMethod1()
	
	/* propagationBehavior
	 * 
	 */
	protected void verifyGetNewTransactionProcess( 
			final int isolationLevel, 
			final int propagationBehavior, 
			final int timeOutSec, 
			final boolean debugEnabled, 
			final boolean isSynchronizationActive 
			) 
	{
		ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor 
		= ArgumentCaptor.forClass( TransactionDefinition.class);
/* Mockito cannot spy on "final" method because it cannot mock "final" method: 
 * @see http://docs.mockito.googlecode.com/hg/org/mockito/Mockito.html#13		
		inOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.getTransaction( transactionDefinitionArgumentCaptor.capture());
		Assert.assertNull( transactionDefinitionArgumentCaptor.getValue());
 */
	
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.doGetTransaction();
			
				ArgumentCaptor<GlobalTransaction> globalTransactionArgumentCaptor 
				= ArgumentCaptor.forClass( GlobalTransaction.class);
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
					Assert.assertNull( globalTransactionArgumentCaptor.getValue());
				
			ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.isExistingTransaction( objectArgumentCaptor.capture());
				Assert.assertNull( objectArgumentCaptor.getValue());
				
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
					GlobalTransaction gtx = globalTransactionArgumentCaptor.getValue();
					Assert.assertNull( gtx);
	
			// protected final SuspendedResourcesHolder suspend(Object transaction) throws TransactionException
				
			/* Mockito cannot spy on "final" method because it cannot mock "final" method: 
			 * @see http://docs.mockito.googlecode.com/hg/org/mockito/Mockito.html#13		
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.getTransactionSynchronization();
			 */
			
			final boolean newSynchronization = true;
				// value true is from result of comparison operation of 
				// getTransactionSynchronization() != SYNCHRONIZATION_NEVER
			ArgumentCaptor<Object> suspendedResourcesHolderArgumentCaptor 
			= ArgumentCaptor.forClass( Object.class);
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.newTransactionStatus( 
					transactionDefinitionArgumentCaptor.capture(), 
					Mockito.eq( gtx), 
					Mockito.eq( true), 
					Mockito.eq( newSynchronization), 
					Mockito.eq( debugEnabled), 
					suspendedResourcesHolderArgumentCaptor.capture()
					);
				TransactionDefinition transactionDefinition 
				= transactionDefinitionArgumentCaptor.getValue();
				Assert.assertEquals( isolationLevel, transactionDefinition.getIsolationLevel());
				Assert.assertEquals( propagationBehavior, transactionDefinition.getPropagationBehavior());
				Assert.assertEquals( timeOutSec, transactionDefinition.getTimeout());
				if ( isSynchronizationActive) {
					Assert.assertNotNull( suspendedResourcesHolderArgumentCaptor.getValue());
				}
				else {
					Assert.assertNull( suspendedResourcesHolderArgumentCaptor.getValue());
				}
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.validateIsolationLevel( Mockito.eq( isolationLevel));
			
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.getGlobalTransactionStates( Mockito.eq( gtx));
		
				ArgumentCaptor<Set> gtxStateSetArgumentCaptor = ArgumentCaptor.forClass( Set.class);
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.beginGlogalTransaction( 
						Mockito.eq( gtx), 
						gtxStateSetArgumentCaptor.capture(), 
						Mockito.eq( transactionDefinition));
					Set<GlobalTransactionState> gtxStateSet = gtxStateSetArgumentCaptor.getValue();
					Assert.assertFalse( 
							gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance));
					Assert.assertFalse( 
							gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction));
					Assert.assertFalse( 
							gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction));
					
					mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
					.setSlim3AsnyncTimeout( Mockito.eq( timeOutSec));
				
					mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
					.getPropagationBehaviorStr( Mockito.eq( propagationBehavior));
			
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doBegin( Mockito.eq( gtx), Mockito.eq( transactionDefinition));
		
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.getGlobalTransactionStates( Mockito.eq( gtx));
			
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
				GlobalTransaction currentGtx = globalTransactionArgumentCaptor.getValue();
				Assert.assertTrue( currentGtx instanceof GlobalTransaction);
			
		// protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition)
		
	} // protected void verifyGetNewTransactionProcess( ....)
	
	/* For the case that no GlobalTransaction is available and not instantiate new GlobalTransaction.
	 * Therefore, propagationBehavior should be one of the followings:
	 * 		TransactionAttribute.PROPAGATION_SUPPORTS
	 * 			when existingTransactionFlag is true means execute by riding on existing transaction
	 * 			when existingTransactionFlag is false means execute non-transactionally 
	 * 		TransactionAttribute.PROPAGATION_NOT_SUPPORTED
	 * 			always execute non-transactionally.
	 * 		TransactionAttribute.PROPAGATION_NEVER
	 * 			throw an exception if a current transaction exists.
	 */
	protected void verifyNotGetNewTransactionProcess( 
			boolean existingTransactionFlag, 
			int isolationLevel, int propagationBehavior, int timeOutSec, boolean debugEnabled
			) 
	{
		// public final TransactionStatus getTransaction(TransactionDefinition definition)
		
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.doGetTransaction();
		
				ArgumentCaptor<GlobalTransaction> globalTransactionArgumentCaptor 
				= ArgumentCaptor.forClass( GlobalTransaction.class);
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
				GlobalTransaction gtx = globalTransactionArgumentCaptor.getValue();
				if ( existingTransactionFlag) {
					Assert.assertTrue( gtx instanceof GlobalTransaction);
				}
				else {
					Assert.assertNull( gtx);
				}
			
			ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.isExistingTransaction( Mockito.eq( gtx));
	
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.getGlobalTransactionStates( Mockito.eq( gtx));
		
		if ( existingTransactionFlag) {
			Assert.fail( 
					"Wrong usage of this test method; should be used for the case that " +
					"no GlobalTransaction is available"
					);
/* The below codes are for the time to extend this test method to handle (active) GlobalTransaction is available. 
			// private TransactionStatus handleExistingTransaction( TransactionDefinition definition, Object transaction, boolean debugEnabled)
			
				switch( propagationBehavior) {
				case TransactionAttribute.PROPAGATION_NEVER:
					// Do nothing since IllegalTransactionStateException should have been thrown at
					// AbstractPlatformTransactionManager.handleExistingTransaction method.
					break;
				case TransactionAttribute.PROPAGATION_NOT_SUPPORTED:
				case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
					// Do nothing since, in this scenario, TransactionSuspensionNotSupportedException
					// should have been thrown at AbstractPlatformTransactionManager.doSuspend method
					// by following logic flow:
						// protected final SuspendedResourcesHolder suspend(Object transaction)
							// - TransactionSynchronizationManager.isSynchronizationActive() should always be false
							// due to Slim3PlatformTransactionManager does not support transaction synchronization currently.
							// - transaction argument to suspend method should not be null due to
							// value of existingTransactionFlag argument to this verifyNotGetNewTransactionProcess method.
					
							// protected Object doSuspend(Object transaction)
								// throws TransactionSuspensionNotSupportedException
					break;
				case TransactionDefinition.PROPAGATION_NESTED:
					// Do nothing since, in this scenario, NestedTransactionNotSupportedException should
					// have been thrown at AbstractPlatformTransactionManager.handleExistingTransaction
					// by following logic below
						// public final boolean isNestedTransactionAllowed()
							// Should return false because the use case of this verifyNotGetNewTransactionProcess
							// method is for not getting new transaction.
					break;
				default:
					// public final boolean isValidateExistingTransaction()
						// IllegalTransactionStateException can be thrown depends on value of isolation
					// public final int getTransactionSynchronization()
					// protected final DefaultTransactionStatus prepareTransactionStatus( TransactionDefinition definition, Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources)
						ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor 
						= ArgumentCaptor.forClass( TransactionDefinition.class);
						ArgumentCaptor<Boolean> booleanArgumentCaptor 
						= ArgumentCaptor.forClass( Boolean.class);
						mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
						.newTransactionStatus( 
								transactionDefinitionArgumentCaptor.capture(), // TransactionDefinition definition
								Mockito.eq( gtx), // Object transaction
								Mockito.eq( false), Mockito.eq( false), // boolean newTransaction, boolean newSynchronization
								Mockito.eq( debugEnabled), Mockito.eq( null) //boolean debug, Object suspendedResources
								);
							TransactionDefinition transactionDefinition 
							= transactionDefinitionArgumentCaptor.getValue();
							Assert.assertEquals( isolationLevel, transactionDefinition.getIsolationLevel());
							Assert.assertEquals( propagationBehavior, transactionDefinition.getPropagationBehavior());
							Assert.assertEquals( timeOutSec, transactionDefinition.getTimeout());
							
						// protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition)
					break;
				} // switch
*/
		}
		else {
			switch( propagationBehavior) {
			case TransactionDefinition.PROPAGATION_REQUIRED:
			case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
			case TransactionDefinition.PROPAGATION_NESTED:
				Assert.fail( 
						"Wrong usage of this test method; value of propagationBehavior argument should be " 
						+ "one among the nexts: PROPAGATION_SUPPORTS, PROPAGATION_NOT_SUPPORTED, " 
						+ "and PROPAGATION_NEVER"
						);
/* The below codes are for the time to extend this test method to handle (active) GlobalTransaction is available. 
				// protected final SuspendedResourcesHolder suspend(Object transaction)
					// suspend method should return null due to the following logic flow:
					// - TransactionSynchronizationManager.isSynchronizationActive() should always be false
					// due to Slim3PlatformTransactionManager does not support transaction synchronization currently.
					// - transaction argument to suspend method should be null due to
					// value of existingTransactionFlag argument to this verifyNotGetNewTransactionProcess method.
				// public final int getTransactionSynchronization()
				ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor 
				= ArgumentCaptor.forClass( TransactionDefinition.class);
				ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass( Boolean.class);
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.newTransactionStatus( 
						transactionDefinitionArgumentCaptor.capture(), // TransactionDefinition definition
						Mockito.eq( null), // Object transaction
						Mockito.eq( true), Mockito.eq( false), // boolean newTransaction, boolean newSynchronization
						Mockito.eq( debugEnabled), Mockito.eq( null) //boolean debug, Object suspendedResources
						);
					TransactionDefinition transactionDefinition 
					= transactionDefinitionArgumentCaptor.getValue();
					Assert.assertEquals( isolationLevel, transactionDefinition.getIsolationLevel());
					Assert.assertEquals( propagationBehavior, transactionDefinition.getPropagationBehavior());
					Assert.assertEquals( timeOutSec, transactionDefinition.getTimeout());
				// protected void doBegin( Object transaction, TransactionDefinition definition)
				// protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition)
*/
				break;
			default: // Case of creating "empty" transaction: no actual transaction, but potentially synchronization.
				// public final int getTransactionSynchronization()
				// protected final DefaultTransactionStatus prepareTransactionStatus( TransactionDefinition definition, Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources)
					ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor 
					= ArgumentCaptor.forClass( TransactionDefinition.class);
					final boolean newSynchronization = true;
						// value true is from result of comparison operation of 
						// getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS
					mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
					.newTransactionStatus( 
							transactionDefinitionArgumentCaptor.capture(), 
							Mockito.eq( null), 
							Mockito.eq( true), 
							Mockito.eq( newSynchronization), 
							Mockito.eq( debugEnabled), 
							Mockito.eq( null)
							);
						TransactionDefinition transactionDefinition 
						= transactionDefinitionArgumentCaptor.getValue();
						Assert.assertEquals( isolationLevel, transactionDefinition.getIsolationLevel());
						Assert.assertEquals( propagationBehavior, transactionDefinition.getPropagationBehavior());
						Assert.assertEquals( timeOutSec, transactionDefinition.getTimeout());
						
						mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
						.validateIsolationLevel( Mockito.eq( isolationLevel));
						
						mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
						.getGlobalTransactionStates( null);
						
					// protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition)
				break;
			} // switch
		}

	} // protected void verifyNotGetNewTransactionProcess( ....)
	
	/* For following propagation cases: PROPAGATION_SUPPORTS or PROPAGATION_REQUIRED
	 */
	protected void verifyGetExistingTransactionProcess(
			int isolationLevel, int propagationBehavior, int timeOutSec, boolean debugEnabled
			) 
	{
		// public final TransactionStatus getTransaction(TransactionDefinition definition)
		
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doGetTransaction();
		
			ArgumentCaptor<GlobalTransaction> globalTransactionArgumentCaptor 
			= ArgumentCaptor.forClass( GlobalTransaction.class);
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
				GlobalTransaction gtx = globalTransactionArgumentCaptor.getValue();
				Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.isExistingTransaction( Mockito.eq( gtx));
		
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.getGlobalTransactionStates( Mockito.eq( gtx));
		
		// private TransactionStatus handleExistingTransaction( TransactionDefinition definition, Object transaction, boolean debugEnabled)
			// public final boolean isValidateExistingTransaction()
			// public final int getTransactionSynchronization()
			// protected final DefaultTransactionStatus prepareTransactionStatus( TransactionDefinition definition, Object transaction, boolean newTransaction, boolean newSynchronization, boolean debug, Object suspendedResources)
		
				ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor 
				= ArgumentCaptor.forClass( TransactionDefinition.class);
				final boolean newSynchronization = true;
					// value true is from result of comparison operation of 
					// getTransactionSynchronization() != SYNCHRONIZATION_NEVER
				mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
				.newTransactionStatus( 
						transactionDefinitionArgumentCaptor.capture(), 
						Mockito.eq( gtx), 
						Mockito.eq( false), 
						Mockito.eq( newSynchronization), 
						Mockito.eq( debugEnabled), 
						Mockito.eq( null)
						);
					TransactionDefinition transactionDefinition 
					= transactionDefinitionArgumentCaptor.getValue();
					Assert.assertEquals( isolationLevel, transactionDefinition.getIsolationLevel());
					Assert.assertEquals( 
							propagationBehavior, transactionDefinition.getPropagationBehavior());
					Assert.assertEquals( timeOutSec, transactionDefinition.getTimeout());

						mockedSlim3TxMangerInOrder.verify( 
								slim3PlatformTransactionManager, Mockito.times( 1))
						.validateIsolationLevel( Mockito.eq( isolationLevel));
	
						mockedSlim3TxMangerInOrder.verify( 
								slim3PlatformTransactionManager, Mockito.times( 1))
						.getGlobalTransactionStates( Mockito.eq( gtx));
			
				// protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition)
	} // protected void verifyGetExistingTransactionProcess( ....)
	
	/**
	 * @param transactionCommited: true when committed. false for not committed, but it's not for verifying 
	 * roll back.
	 */
	protected void verifyCommitProcess( boolean transactionCommited) {
/* Mockito cannot spy on "final" method because it cannot mock "final" method: 
 * @see http://docs.mockito.googlecode.com/hg/org/mockito/Mockito.html#13		
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.commit( transactionStatusArgumentCaptor.capture());
*/
		// protected boolean shouldCommitOnGlobalRollbackOnly()
		// private void processCommit(DefaultTransactionStatus status)
		// protected void prepareForCommit(DefaultTransactionStatus status)
		// protected final void triggerBeforeCommit(DefaultTransactionStatus status)
		// protected final void triggerBeforeCompletion(DefaultTransactionStatus status)
		// If it's not new transaction (means not outer most transaction), then call public final boolean isFailEarlyOnGlobalRollbackOnly()
		
		ArgumentCaptor<DefaultTransactionStatus> defaultTransactionStatusArgumentCaptor 
		= ArgumentCaptor.forClass( DefaultTransactionStatus.class);
		if ( transactionCommited) {
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.doCommit( defaultTransactionStatusArgumentCaptor.capture());
				DefaultTransactionStatus defaultTransactionStatus 
				= (DefaultTransactionStatus)(defaultTransactionStatusArgumentCaptor.getValue());
				Assert.assertTrue( defaultTransactionStatus.isCompleted()); // should be true since actually commit has already finished
				Assert.assertTrue( defaultTransactionStatus.isNewTransaction());
				Assert.assertTrue( defaultTransactionStatus.hasTransaction());
				Assert.assertTrue( defaultTransactionStatus.getTransaction() instanceof GlobalTransaction);
			
			ArgumentCaptor<GlobalTransaction> globalTransactionArgumentCaptor 
			= ArgumentCaptor.forClass( GlobalTransaction.class);
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
			.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
				GlobalTransaction gtx = globalTransactionArgumentCaptor.getValue();
				Assert.assertTrue( gtx instanceof GlobalTransaction);
		}
		else {
			mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.never())
			.doCommit( defaultTransactionStatusArgumentCaptor.capture());
		}
		
		// private void triggerAfterCommit(DefaultTransactionStatus status)
		// private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus)
		// private void cleanupAfterCompletion(DefaultTransactionStatus status)
		// protected void doCleanupAfterCompletion(Object transaction)
	} // protected void verifyCommitProcess( boolean transactionCommited)
	
	// simple test case of static public transactional method -------------------------------------	
	@Test
	public void testTransactionAtStaticMethod() {
		String authorName = "Author1";
		Author.createNewAuthor( authorName);	// Author.createNewAuthor static method got @Transactional annotation
		
		verifyNoActiveGlobalTransaction();
		
		Assert.assertEquals( authorName, Datastore.query( Author.class).asSingle().getName());
		
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	} // public void testTransactionAtStaticMethod()
	// --------------------------------------------------------------------------------------------

	// Simple test case of protected transactional non-static method ------------------------------
	@Transactional
	protected void prepTestEntities() throws Throwable {
		prepTestModels();
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertNotNull( gtx);
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
	}
	
	/**
	 * Test simple case of transactional method additionally to the followings:
	 * 		Confirming default isolation level value (TransactionAttribute.ISOLATION_DEFAULT) 
	 * 		Confirming default propagation behavior value (TransactionAttribute.PROPAGATION_REQUIRED) 
	 * 		Confirm default timeout value (TransactionAttribute.TIMEOUT_DEFAULT)
	 * 		Confirm functionality of prepTestModels method what is going to be used by other tests.
	 * 		Confirm functionality of prepTestEntities method what is going to be used by other tests.
	 */
	
	@Test
	public void prepTestEntitiesTest() throws Throwable {
		prepTestEntities();
		verifyPrepedEntities();
		
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyCommitProcess( true);
	}
	// --------------------------------------------------------------------------------------------
	
	// Test case: Execute commit in another protected transactional method ------------------------
	@Transactional( propagation=Propagation.REQUIRED)
	protected void cascadeCommitCallee1() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
	}
	@Transactional( propagation=Propagation.REQUIRED)
	protected void cascadeCommitCaller1() throws Throwable {
		prepTestModels();
		cascadeCommitCallee1();
	}
	
	@Test
	public void testRequiredPropagationOnExistingTransaction() throws Throwable {
		cascadeCommitCaller1();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRED,	// Default propergation behavior 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	} // public void testRequiredPropagationOnExistingTransaction() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Simple test case of protected transactional method with Propagation.REQUIRES_NEW -----------
	@Transactional( propagation=Propagation.REQUIRES_NEW)
	protected void requiredPropagationCommit() throws Throwable {
		prepTestModels();
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		Assert.assertTrue( gtx.isActive());
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
	}
	
	@Test
	public void testPropagationRequiredNew() throws Throwable {
		requiredPropagationCommit();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationRequiredNew() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	//TODO Exception case (roll back case): TransactionAttribute.PROPAGATION_REQUIRES_NEW when transaction available
	@Transactional( propagation=Propagation.REQUIRES_NEW)
	protected void requiresNewPropagationOnExistingTransaction() throws Throwable {
		prep3rdBookModels(); // Note: Calling non transactional method
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
	} // protected void supportPropagationToExistingTransaction()
	
	@Test
	public void testRequiresNewPropagationOnExistingTransaction() throws Throwable {
		Method requiresNewPropagationOnExistingTransactionMethod 
		= this.getClass().getDeclaredMethod( 
				"requiresNewPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		try {
			prepTransactionForPropergation( requiresNewPropagationOnExistingTransactionMethod);
			Assert.fail( 
					"TransactionSuspensionNotSupportedException exception wasn't thrown by " + 
					requiresNewPropagationOnExistingTransactionMethod.getName() + " method"
					);
		}
		catch( InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			// TransactionSuspensionNotSupportedException is expected 
			if ( !( cause instanceof TransactionSuspensionNotSupportedException)) {
				throw cause;
			}
		}
		
		verifyNoActiveGlobalTransaction();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		List<Author> authorList = Datastore.query( Author.class).asList();
		Assert.assertEquals( 0, authorList.size());
		List<Book> bookList = Datastore.query( Book.class).asList();
		Assert.assertEquals( 0, bookList.size());
		List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
		Assert.assertEquals( 0, chapterList.size());
		
		verifyGetNewTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifySuspendProcess();
		verifyRollbackProcess( false, false, debugEnabledInAbstractPlatformTransactionManager);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testRequiresNewPropagationOnExistingTransaction() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Simple test case of protected transactional method with Propagation.SUPPORTS ---------------
	@Transactional( propagation=Propagation.SUPPORTS)
	protected void supportPropagationCommit() throws Throwable {
		prepTestModels();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		
		Datastore.put( a1); Datastore.put( a2);
		Datastore.put( b1); Datastore.put( b2);
		Datastore.put( a1b2); 
		Datastore.put( a2b1); Datastore.put( a2b2);
		Datastore.put( b1c1); Datastore.put( b1c2);
		Datastore.put( b2c1); Datastore.put( b2c2);
	}
	
	@Test
	public void testPropagationSupports() throws Throwable {
		supportPropagationCommit();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyNotGetNewTransactionProcess(
				false, 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_SUPPORTS, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationSupports() throws Throwable
	// --------------------------------------------------------------------------------------------
	Author author3;
	Book book3;
	AuthorBook a2book3;
	Chapter book3Chapter1;
	AuthorChapter a3b3Chapter1;
	protected void prep3rdBookModels() {
		String author3Name = "Author on propagation";
		author3 = new Author();
		author3.setName( author3Name);
		author3.setKey( Datastore.allocateId( Author.class));
		
		book3 = new Book();
		String book3Title = "Book about propagation";
		book3.setTitle( book3Title);
		book3.setKey( Datastore.allocateId( author3.getKey(), Book.class));
		
		a2book3 = new AuthorBook();
		a2book3.getAuthorRef().setModel( a2);
		a2book3.getBookRef().setModel( book3);
		
		book3Chapter1 = new Chapter();
		String b3c1Title = "Chapter about propagation";
		book3Chapter1.setTitle( b3c1Title);
		int b3c1NumPages = 31;
		book3Chapter1.setNumPages( b3c1NumPages);
		book3Chapter1.setKey( Datastore.allocateId( book3.getKey(), Chapter.class));
		
		a3b3Chapter1 = new AuthorChapter();
		a3b3Chapter1.getAuthorRef().setModel( author3);
		a3b3Chapter1.getChapterRef().setModel( book3Chapter1);
	} // protected void prep3rdBookModels()
	
	protected void verify3rdBookEntities() {
		List<Key> author3GroupKeyList = Datastore.query( author3.getKey()).asKeyList();
		Assert.assertTrue( author3GroupKeyList.contains( author3.getKey()));
		Assert.assertTrue( author3GroupKeyList.contains( book3.getKey()));
		Book book3Copy = Datastore.get( Book.class, book3.getKey());
		List<AuthorBook> authorBookList = book3Copy.getAuthorBookListRef().getModelList();
		Assert.assertEquals( 1, authorBookList.size());
		Assert.assertEquals( a2, authorBookList.get( 0).getAuthorRef().getModel());
		
		List<Chapter> chapterList = book3Copy.getChapterList();
		Assert.assertTrue( chapterList.contains( book3Chapter1));
		
		List<AuthorChapter> authorChapterList 
		= chapterList.get( chapterList.indexOf( book3Chapter1)).getAuthorChapterListRef().getModelList();
		Assert.assertEquals( 1, authorChapterList.size());
		Assert.assertEquals( author3, authorChapterList.get( 0).getAuthorRef().getModel());
	} // protected void verify3rdBookEntities()
	
	
	// Test case of transactional method with Propagation.SUPPORTS for existing transaction -------
	@Transactional( propagation=Propagation.SUPPORTS)
	protected void supportPropagationOnExistingTransaction() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		prep3rdBookModels(); // Note: Calling non transactional method
		
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
	} // protected void supportPropagationToExistingTransaction()
	
	@Transactional( propagation=Propagation.REQUIRES_NEW)
	protected void prepTransactionForPropergation( Method calleeMethod) throws Throwable {
		prepTestModels();
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
		
		calleeMethod.setAccessible( true);
		calleeMethod.invoke( this);
	}
	
	@Test
	public void testSupportPropagationOnExistingTransaction() throws Throwable {
		Method supportPropagationOnExistingTransactionMethod 
		= this.getClass().getDeclaredMethod( 
				"supportPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		prepTransactionForPropergation( supportPropagationOnExistingTransactionMethod);
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verify3rdBookEntities();
		
		verifyGetNewTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyGetExistingTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_SUPPORTS, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	} // public void testSupportPropagationOnExistingTransaction() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Test case of transactional method with Propagation.MANDATORY for existing transaction -------
	@Transactional( propagation=Propagation.MANDATORY)
	protected void mandatoryPropagationOnExistingTransaction() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		prep3rdBookModels(); // Note: Calling non transactional method
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
	} // protected void supportPropagationToExistingTransaction()
	
	@Test
	public void testMandatoryPropagationOnExistingTransaction() throws Throwable {
		Method mandatoryPropagationOnExistingTransactionMethod 
		= this.getClass().getDeclaredMethod( 
				"mandatoryPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		prepTransactionForPropergation( mandatoryPropagationOnExistingTransactionMethod);
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verify3rdBookEntities();
		
		verifyGetNewTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyGetExistingTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_MANDATORY, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	}
	// --------------------------------------------------------------------------------------------
	
	//TODO Exception case (roll back case): TransactionAttribute.PROPAGATION_MANDATORY when no transaction available
	@Transactional( propagation=Propagation.MANDATORY)
	protected void mandatoryPropagationTransaction() throws Throwable {
		prepTestModels();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		
		Datastore.put( a1); Datastore.put( a2);
		Datastore.put( b1); Datastore.put( b2);
		Datastore.put( a1b2); 
		Datastore.put( a2b1); Datastore.put( a2b2);
		Datastore.put( b1c1); Datastore.put( b1c2);
		Datastore.put( b2c1); Datastore.put( b2c2);
	}
	
	@Test
	public void testPropagationMandatory() throws Throwable {
		try {
			mandatoryPropagationTransaction();
			Assert.fail( 
					"IllegalTransactionStateException exception wasn't thrown by " +
					"mandatoryPropagationTransaction method"
					);
		}
		catch( IllegalTransactionStateException exception) {
			// Expected exception; do nothing.
		}
		
		verifyNoActiveGlobalTransaction();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		List<Author> authorList = Datastore.query( Author.class).asList();
		Assert.assertEquals( 0, authorList.size());
		List<Book> bookList = Datastore.query( Book.class).asList();
		Assert.assertEquals( 0, bookList.size());
		List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
		Assert.assertEquals( 0, chapterList.size());

		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doGetTransaction();
		
		ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.isExistingTransaction( objectArgumentCaptor.capture());
			Assert.assertNull( objectArgumentCaptor.getValue());
		
		ArgumentCaptor<TransactionDefinition> transactionDefinitionArgumentCaptor 
		= ArgumentCaptor.forClass( TransactionDefinition.class);
		ArgumentCaptor<Boolean> dummyArgumentCaptor1 = ArgumentCaptor.forClass( Boolean.class);
		ArgumentCaptor<Boolean> dummyArgumentCaptor2 = ArgumentCaptor.forClass( Boolean.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.never())
		.newTransactionStatus( 
				transactionDefinitionArgumentCaptor.capture(), 
				objectArgumentCaptor.capture(), 
				dummyArgumentCaptor1.capture(), 
				Mockito.eq( false), 
				dummyArgumentCaptor2.capture(), 
				Mockito.eq( null)
				);
			
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationMandatory() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	//Test case of transactional method with TransactionAttribute.PROPAGATION_NOT_SUPPORTED -------
	@Transactional( propagation=Propagation.NOT_SUPPORTED)
	protected void notSupportedPropagationCommit() throws Throwable {
		prepTestModels();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		
		Datastore.put( a1); Datastore.put( a2);
		Datastore.put( b1); Datastore.put( b2);
		Datastore.put( a1b2); 
		Datastore.put( a2b1); Datastore.put( a2b2);
		Datastore.put( b1c1); Datastore.put( b1c2);
		Datastore.put( b2c1); Datastore.put( b2c2);
	}
	
	@Test
	public void testPropagationNotSupported() throws Throwable {
		notSupportedPropagationCommit();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyNotGetNewTransactionProcess(
				false, 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_NOT_SUPPORTED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationSupports() throws Throwable
	// --------------------------------------------------------------------------------------------

	//Exception case (roll back case): TransactionAttribute.PROPAGATION_NOT_SUPPORTED when transaction available
	/* TransactionSuspensionNotSupportedException exception will be thrown by 
	 * org.springframework.transaction.support.AbstractPlatformTransactionManager.handleExistingTransaction method. 
	 */
	@Transactional( propagation=Propagation.NOT_SUPPORTED)
	protected void notSupportedPropagationOnExistingTransaction() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		prep3rdBookModels(); // Note: Calling non transactional method
		Datastore.put( author3);
		Datastore.put( book3);
		Datastore.put( a2book3);
		Datastore.put( book3Chapter1);
		Datastore.put( a3b3Chapter1);
	} // protected void supportPropagationToExistingTransaction()
	
	/* For the case of suspending existing transaction, 
	 * means that Propagation.NOT_SUPPORTED is specified, and TransactionSuspensionNotSupportedException 
	 * will be thrown.
	 */
	protected void verifySuspendProcess() {
		// public final TransactionStatus getTransaction(TransactionDefinition definition)
		
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doGetTransaction();
		
		ArgumentCaptor<GlobalTransaction> globalTransactionArgumentCaptor 
		= ArgumentCaptor.forClass( GlobalTransaction.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.getGlobalTransactionStates( globalTransactionArgumentCaptor.capture());
			GlobalTransaction gtx = globalTransactionArgumentCaptor.getValue();
			Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.isExistingTransaction( Mockito.eq( gtx));

		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.getGlobalTransactionStates( Mockito.eq( gtx));
		
		// private TransactionStatus handleExistingTransaction( TransactionDefinition definition, Object transaction, boolean debugEnabled)
		// protected final SuspendedResourcesHolder suspend(Object transaction)
		// protected Object doSuspend(Object transaction)
		
	} // protected void verifySuspendProcess()
	
	protected void verifyRollbackProcess( 
			boolean readOnlyEnabled, boolean roolBackOnlyEnabled, boolean debugEnabled
			) 
	{
		// public final void rollback(TransactionStatus status)
		// private void processRollback(DefaultTransactionStatus status)
		// protected final void triggerBeforeCompletion(DefaultTransactionStatus status)
		
		ArgumentCaptor<DefaultTransactionStatus> defaultTransactionStatusArgumentCaptor 
		= ArgumentCaptor.forClass( DefaultTransactionStatus.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doRollback( defaultTransactionStatusArgumentCaptor.capture());
			DefaultTransactionStatus defaultTransactionStatus 
			= defaultTransactionStatusArgumentCaptor.getValue();
			Assert.assertTrue( defaultTransactionStatus.getTransaction() instanceof GlobalTransaction);
			GlobalTransaction gtx = (GlobalTransaction)defaultTransactionStatus.getTransaction();
			Assert.assertTrue( defaultTransactionStatus.isNewTransaction());
			Assert.assertTrue( defaultTransactionStatus.isCompleted()); // Should be true since actually transaction has already been finished
			Assert.assertEquals( readOnlyEnabled, defaultTransactionStatus.isReadOnly());
			Assert.assertEquals( roolBackOnlyEnabled, defaultTransactionStatus.isRollbackOnly());
				/* If defaultTransactionStatus.isRollbackOnly() returns true, then either 
				 * its isGlobalRollbackOnly() or its isLocalRollbackOnly() returns true.
				 */
				if( defaultTransactionStatus.isRollbackOnly() && logger.isDebugEnabled()) {
					logger.debug( 
							"defaultTransactionStatus.isGlobalRollbackOnly() = " + 
							defaultTransactionStatus.isGlobalRollbackOnly()
							);
					logger.debug( 
							"defaultTransactionStatus.isLocalRollbackOnly() = " + 
							defaultTransactionStatus.isLocalRollbackOnly()
							);
				}
			Assert.assertEquals( debugEnabled, defaultTransactionStatus.isDebug());
			Assert.assertFalse( defaultTransactionStatus.isTransactionSavepointManager());
			Assert.assertTrue( defaultTransactionStatus.isNewSynchronization());
			
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.getGlobalTransactionStates( Mockito.eq( gtx));
			
		// private void triggerAfterCompletion(DefaultTransactionStatus status, int completionStatus)
		// private void cleanupAfterCompletion(DefaultTransactionStatus status)
		// protected void doCleanupAfterCompletion(Object transaction)
			
	} // protected void verifyRollbackProcess( boolean readOnlyEnabled, boolean debugEnabled)
	
	@Test
	public void testNotSupportedPropagationOnExistingTransaction() throws Throwable {
		Method notSupportedPropagationOnExistingTransactionMethod 
		= this.getClass().getDeclaredMethod( 
				"notSupportedPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		try {
			prepTransactionForPropergation( notSupportedPropagationOnExistingTransactionMethod);
			Assert.fail( 
					"TransactionSuspensionNotSupportedException exception wasn't thrown by " +
					"notSupportedPropagationOnExistingTransaction method"
					);
		}
		catch( InvocationTargetException exception) {
			Throwable cause = exception.getCause();
			// TransactionSuspensionNotSupportedException is expected 
			if ( !( cause instanceof TransactionSuspensionNotSupportedException)) {
				throw cause;
			}
		}
		
		verifyNoActiveGlobalTransaction();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		List<Author> authorList = Datastore.query( Author.class).asList();
		Assert.assertEquals( 0, authorList.size());
		List<Book> bookList = Datastore.query( Book.class).asList();
		Assert.assertEquals( 0, bookList.size());
		List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
		Assert.assertEquals( 0, chapterList.size());
		
		verifyGetNewTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifySuspendProcess();
		verifyRollbackProcess( false, false, debugEnabledInAbstractPlatformTransactionManager);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testNotSupportedPropagationOnExistingTransaction() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	//Start by TransactionAttribute.PROPAGATION_NOT_SUPPORTED and go to TransactionAttribute.PROPAGATION_REQUIRED - 
	// --------------------------------------------------------------------------------------------
	@Transactional( propagation=Propagation.NOT_SUPPORTED)
	protected void notSupportedToRequiredPropagation() throws Throwable {
		prepTestModels();
		
		cascadeCommitCallee1();	//  cascadeCommitCallee1 method is annotated with Propagation.REQUIRED
	} // protected void neverPropagationCommit() throws Throwable
	
	@Test
	public void testPropagationNotSupportedToRequired() throws Throwable {
		notSupportedToRequiredPropagation();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		
		verifyNotGetNewTransactionProcess(
				false, 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_NOT_SUPPORTED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
			// Created "empty" transaction: no actual transaction, but potentially synchronization.		
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				true
				);
		verifyCommitProcess( true);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationNever() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Simple test case of protected transactional method with Propagation.NEVER ------------------
	@Transactional( propagation=Propagation.NEVER)
	protected void neverPropagationCommit() throws Throwable {
		prepTestModels();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		
		Datastore.put( a1); Datastore.put( a2);
		Datastore.put( b1); Datastore.put( b2);
		Datastore.put( a1b2); 
		Datastore.put( a2b1); Datastore.put( a2b2);
		Datastore.put( b1c1); Datastore.put( b1c2);
		Datastore.put( b2c1); Datastore.put( b2c2);
	} // protected void neverPropagationCommit() throws Throwable
	
	@Test
	public void testPropagationNever() throws Throwable {
		neverPropagationCommit();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyNotGetNewTransactionProcess(
				false, 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_NEVER, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationNever() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	//Exception case (roll back case): TransactionAttribute.PROPAGATION_NEVER when transaction available -
	// --------------------------------------------------------------------------------------------
	@Transactional( propagation=Propagation.NEVER)
	protected void neverPropagationOnExistingTransaction() throws Throwable {
		prep3rdBookModels(); // Note: Calling non transactional method
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
	} // protected void neverPropagationOnExistingTransaction()
	
	@Test
	public void testNeverPropagationOnExistingTransaction() throws Throwable {
		Method neverPropagationOnExistingTransactionMethod
		= this.getClass().getDeclaredMethod( 
				"neverPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		try {
			prepTransactionForPropergation( neverPropagationOnExistingTransactionMethod);
			Assert.fail( 
					"IllegalTransactionStateException exception wasn't thrown by " + 
					neverPropagationOnExistingTransactionMethod.getName() + " method"
					);
		}
		catch( InvocationTargetException exception) {
			// Expected NestedTransactionNotSupportedException exception
			Throwable cause = exception.getCause();
			if ( !( cause instanceof IllegalTransactionStateException)) {
				throw cause;
			}
		}
		
		verifyNoActiveGlobalTransaction();
		
		Assert.assertNull( Datastore.getCurrentGlobalTransaction());
		List<Author> authorList = Datastore.query( Author.class).asList();
		Assert.assertEquals( 0, authorList.size());
		List<Book> bookList = Datastore.query( Book.class).asList();
		Assert.assertEquals( 0, bookList.size());
		List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
		Assert.assertEquals( 0, chapterList.size());
		
		verifyGetNewTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		
		// public final TransactionStatus getTransaction(TransactionDefinition definition)
		
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doGetTransaction();
		
		ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.isExistingTransaction( objectArgumentCaptor.capture());
			Assert.assertTrue( objectArgumentCaptor.getValue() instanceof GlobalTransaction);
		
		// private TransactionStatus handleExistingTransaction( TransactionDefinition definition, Object transaction, boolean debugEnabled)
		
		verifyRollbackProcess( false, false, debugEnabledInAbstractPlatformTransactionManager);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testNeverPropagationOnExistingTransaction() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Start by TransactionAttribute.PROPAGATION_NEVER and go to TransactionAttribute.PROPAGATION_REQUIRED -	
	// --------------------------------------------------------------------------------------------
	/* Found that, with current Slim3TrnasactionManager design, this case does not roll back triggered by 
	 * exception since this does not throw the exception, rather it finishes the transaction with committing 
	 * to persist models to datastore. 
	 * Even neverToRequiredPropagation method is marked its propagation attribute as Propagation.NEVER, 
	 * new transaction will be obtained when cascadeCommitCallee1 method (what is marked its propagation 
	 * attribute as Propagation.NEW) is called from it.
	 * 
	 * By checking source code of Spring's JdoTransactionManager class, JdoTransactionManager class also 
	 * seem not to roll back triggered by exception. 
	 * 		When transaction begin with Propagation.NEVER setting:
	 * 			AbstractPlatformTransactionManager.getTransaction method calls 
	 * 			AbstractPlatformTransactionManager.prepareTransactionStatus method.
	 * 			prepareTransactionStatus method calls newTransactionStatus method to create empty 
	 * 			transaction for synchronization sake.
	 * 		Then method with Propagation.REQUIRED is called:
	 * 			AbstractPlatformTransactionManager.getTransaction method calls 
	 * 			org.springframework.orm.jdo.JdoTransactionManager.isExistingTransaction method.
	 * 			isExistingTransaction method checks the value returned by persistenceManagerHolder.isTransactionActive method in JdoTransactionObject object.
	 * 			Since the created transaction was empty one, persistenceManagerHolder.isTransactionActive method returns false.
	 * 			Next, it will create new transaction state by calling AbstractPlatformTransactionManager.newTransactionStatus method what instantiates DefaultTransactionStatus object.
	 * 			
	 */
	@Transactional( propagation=Propagation.NEVER)
	protected void neverToRequiredPropagation() throws Throwable {
		prepTestModels();
		
		cascadeCommitCallee1();	//  cascadeCommitCallee1 method is annotated with Propagation.REQUIRED
	} // protected void neverPropagationCommit() throws Throwable
	
	@Test
	public void testPropagationNeverToRequired() throws Throwable {
		neverToRequiredPropagation();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		
		verifyNotGetNewTransactionProcess(
				false, 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_NEVER, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager
				);
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				true
				);
		verifyCommitProcess( true);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationNever() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	//Test case of transactional method with TransactionAttribute.PROPAGATION_NESTED --------------
	@Transactional( propagation=Propagation.NESTED)
	protected void nestedPropagationCommit() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		prepTestModels();
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
	} // protected void nestedPropagationOnExistingTransaction() throws Throwable
	
	@Test
	public void testPropagationNested() throws Throwable {
		nestedPropagationCommit();

		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_NESTED, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager, 
				false
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	} // public void testPropagationNested() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Exception case (roll back case): TransactionAttribute.PROPAGATION_NESTED when transaction available -
	// --------------------------------------------------------------------------------------------
	@Transactional( propagation=Propagation.NESTED)
	protected void nestedPropagationOnExistingTransaction() throws Throwable {
		prep3rdBookModels(); // Note: Calling non transactional method
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		Assert.assertTrue( gtx.isActive());
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
	} // protected void supportPropagationToExistingTransaction()
	
	@Test
	public void testNestedPropagationOnExistingTransaction1() throws Throwable {
		Method nestedPropagationOnExistingTransactionMethod
		= this.getClass().getDeclaredMethod( 
				"nestedPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		try {
			prepTransactionForPropergation( nestedPropagationOnExistingTransactionMethod);
			Assert.fail( 
					"NestedTransactionNotSupportedException exception wasn't thrown by " + 
					nestedPropagationOnExistingTransactionMethod.getName() + " method"
					);
		}
		catch( InvocationTargetException exception) {
			// Expected NestedTransactionNotSupportedException exception
			Throwable cause = exception.getCause();
			if ( !( cause instanceof NestedTransactionNotSupportedException)) {
				throw cause;
			}
		}
		
		verifyNoActiveGlobalTransaction();
		
		List<Author> authorList = Datastore.query( Author.class).asList();
		Assert.assertEquals( 0, authorList.size());
		List<Book> bookList = Datastore.query( Book.class).asList();
		Assert.assertEquals( 0, bookList.size());
		List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
		Assert.assertEquals( 0, chapterList.size());
		
		verifyGetNewTransactionProcess(
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager,
				false
				);
		
		// public final TransactionStatus getTransaction(TransactionDefinition definition)
		
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.doGetTransaction();
		
		ArgumentCaptor<Object> objectArgumentCaptor = ArgumentCaptor.forClass( Object.class);
		mockedSlim3TxMangerInOrder.verify( slim3PlatformTransactionManager, Mockito.times( 1))
		.isExistingTransaction( objectArgumentCaptor.capture());
			Assert.assertTrue( objectArgumentCaptor.getValue() instanceof GlobalTransaction);
		
		// private TransactionStatus handleExistingTransaction( TransactionDefinition definition, Object transaction, boolean debugEnabled)
		// public final boolean isNestedTransactionAllowed()
		
		verifyRollbackProcess( false, false, debugEnabledInAbstractPlatformTransactionManager);
		verifyCommitProcess( false);
		
		verifyNoActiveGlobalTransaction();
	} // public void testNestedPropagationOnExistingTransaction1() throws Throwable
	
	
	@Test
	@DirtiesContext
	public void testNestedPropagationOnExistingTransaction2() throws Throwable {
		slim3PlatformTransactionManager.setNestedTransactionAllowed( true); 
		
		Method nestedPropagationOnExistingTransactionMethod
		= this.getClass().getDeclaredMethod( 
				"nestedPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		prepTransactionForPropergation( nestedPropagationOnExistingTransactionMethod);
		
		verifyNoActiveGlobalTransaction();

		verifyPrepedEntities();
		verify3rdBookEntities();
		
		verifyNoActiveGlobalTransaction();
	} // public void testNestedPropagationOnExistingTransaction2() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Test case: Execute commit in non transactional method called from protected transactional method -
	// --------------------------------------------------------------------------------------------
	protected void cascadeCommitCallee2() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
	}
	
	@Transactional
	protected void cascadeCommitCaller2() throws Throwable {
		prepTestModels();
		cascadeCommitCallee2();
	}
	
	@Test
	public void testCommitInNonTransactionalMethod() throws Throwable {
		cascadeCommitCaller2();
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		verifyGetNewTransactionProcess( 
				TransactionAttribute.ISOLATION_DEFAULT, 
				TransactionAttribute.PROPAGATION_REQUIRED, // Default propergation behavior 
				TransactionAttribute.TIMEOUT_DEFAULT, 
				debugEnabledInAbstractPlatformTransactionManager,
				false
				);
		verifyCommitProcess( true);
		
		verifyNoActiveGlobalTransaction();
	} // public void testCommitInNonTransactionalMethod() throws Throwable
	// --------------------------------------------------------------------------------------------

	// Slim3's auto-roll-back exception cases by ConcurrentModificationException without 
	// noRollbackFor attribute of @Transactional annotation ---------------------------------------
	String b1c1TitleInOuterGtx;
	String b1c2TitleInOuterGtx;
	
	@ExpectedException( ConcurrentModificationException.class)
	protected void throwConcurrentModification() throws Throwable {
		GlobalTransaction gtx1 = Datastore.getCurrentGlobalTransaction(); 
		
		// Give new titles to c1 and c2 within outer global transaction
		String outerGtxSuffix = " outer GTX version";
		b1c1TitleInOuterGtx = b1c1OriginalTitle + outerGtxSuffix;
		b1c2TitleInOuterGtx = b1c2OriginalTitle + outerGtxSuffix;
		
		com.newmainsoftech.spray.slingong.datastore.testmodel.book.ChapterMeta chapterMeta
		= com.newmainsoftech.spray.slingong.datastore.testmodel.book.ChapterMeta.get();
		List<Chapter> chapterList 
		= gtx1.query( Chapter.class, b1.getKey())
			.filter( chapterMeta.key.in( b1c1.getKey(), b1c2.getKey()))
			.asList();
		for( Chapter chapter : chapterList) {
			if ( chapter.getKey().equals( b1c1.getKey())) {
				chapter.setTitle( b1c1TitleInOuterGtx);
			} else if ( chapter.getKey().equals( b1c2.getKey())) {
				chapter.setTitle( b1c2TitleInOuterGtx);
			}
		} // for
		gtx1.put( chapterList);	// Save c1 and c2 with new titles
		
		List<Book> bookList = gtx1.get( Book.class, b1.getKey(), b2.getKey());
		
		// Start inner global transaction context
		GlobalTransaction gtx2 = Datastore.beginGlobalTransaction();
		
		// Give new titles to b and b2 within inner global transaction
		String innerGtxSuffix = " inner GTX version";
		for ( Book book : bookList) {
			if ( book.getKey().equals( b1.getKey())) {
				book.setTitle( b1OriginalTitle + innerGtxSuffix);
			} else if (book.getKey().equals(b2.getKey())) {
				book.setTitle( b2OriginalTitle + innerGtxSuffix);
			}
		} // for
		try {
			gtx2.put( bookList);	// This should cause throwing ConcurrentModificationException
			gtx2.commit();
			
			Assert.fail();
		}
		catch( Throwable throwable) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						"Logged the exception for review purpose: " 
						+ "expecting ConcurrentModificationException exception here.", 
						throwable
						);
			}
			throw throwable;
		}
		finally {
			if ( gtx2.isActive()) {
				if ( logger.isInfoEnabled()) {
					logger.info( 
							String.format( 
									"GlobalTransaction instance (ID:%1$s) remains active.", 
									gtx2.getId()
									)
							);
				}
			}
		}
	} // protected void throwConcurrentModification() throws Throwable
	
	@Transactional
	protected void rollbackByConcurrentModificationException() throws Throwable {
		throwConcurrentModification();
	} // protected void rollbackByConcurrentModificationException() throws Throwable
	
	@Test
	public void testRollbackByConcurrentModificationException() throws Throwable {
		prepTestEntities();
		verifyPrepedEntities();
		
		try {
			rollbackByConcurrentModificationException();
			Assert.fail( "ConcurrentModificationException exception wasn't thrown.");
		}
		catch( ConcurrentModificationException exception) {
			// Do nothing since ConcurrentModificationException is expected. 
		}
		
		verifyNoActiveGlobalTransaction();
		
		verifyPrepedEntities();
		
		verifyNoActiveGlobalTransaction();
	} // public void testRollbackByConcurrentModificationException() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Slim3's auto-roll-back exception cases by ConcurrentModificationException with  
	// noRollbackFor attribute of @Transactional annotation ---------------------------------------
	@Transactional( noRollbackFor=ConcurrentModificationException.class)
	protected void noRollbackByException() throws Throwable {
		throwConcurrentModification();
	} // protected void rollbackByConcurrentModificationException2() throws Throwable
	
	@Test
	public void testNoRollbackByException() throws Throwable {
		prepTestEntities();
		verifyPrepedEntities();
		
		try {
			noRollbackByException();
			Assert.fail( "ConcurrentModificationException exception wasn't thrown.");
		}
		catch( ConcurrentModificationException exception) {
			// Do nothing since ConcurrentModificationException is expected. 
		}
		
		verifyNoActiveGlobalTransaction();
		
		com.newmainsoftech.spray.slingong.datastore.testmodel.book.ChapterMeta chapterMeta 
		= com.newmainsoftech.spray.slingong.datastore.testmodel.book.ChapterMeta.get();
		List<Chapter> chapterList 
		= Datastore.query( Chapter.class, b1.getKey())
			.filter( chapterMeta.key.in( b1c1.getKey(), b1c2.getKey()))
			.asList();
		for( Chapter chapter : chapterList) {
			if ( chapter.getKey().equals( b1c1.getKey())) {
				Assert.assertEquals( b1c1TitleInOuterGtx, chapter.getTitle());
			} else if ( chapter.getKey().equals( b1c2.getKey())) {
				Assert.assertEquals( b1c2TitleInOuterGtx, chapter.getTitle());
			}
		} // for
		
		com.newmainsoftech.spray.slingong.datastore.testmodel.book.BookMeta bookMeta 
		= com.newmainsoftech.spray.slingong.datastore.testmodel.book.BookMeta.get();
		List<Book> bookList 
		= Datastore.query( Book.class)
			.filter( bookMeta.key.in( b1.getKey(), b2.getKey()))
			.asList();
		for ( Book book : bookList) {
			if ( book.getKey().equals( b1.getKey())) {
				Assert.assertEquals( b1OriginalTitle, book.getTitle());
			} else if (book.getKey().equals(b2.getKey())) {
				Assert.assertEquals( b2OriginalTitle, book.getTitle());
			}
		} // for
		
	} // public void testNoRollbackByException() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// One shot test (not necessary to repeat) to confirm casting null to GlobalTransaction: happening in doRollback method and doCommit method.
	@Ignore
	@Test
	public void castNullToGlobalTransaction() {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertNull( gtx);
		Object gtxObj = gtx;
		GlobalTransaction gtxCopy = (GlobalTransaction)gtxObj;
		Assert.assertNull( gtxCopy);
		Assert.assertFalse( gtxCopy instanceof GlobalTransaction);
	} // public void castNullToGlobalTransaction()
	// --------------------------------------------------------------------------------------------

	
	
//TODO private method
//TODO static private method
//TODO create test case to confirm doRollbackOnCommitException will not be called after throwing UnexpectedRollbackException
	/* doCommit method throws UnexpectedRollbackException.
	 */
//TODO In cascading existing transaction case, catching exception thrown by callee in caller transactional method.
	/* Expectation about the above case:
	 * Exception case will be considerably classified as 3 cases:
	 * 		Exception thrown during getting transaction
	 * 		Exception thrown during target method invocation
	 * 		Exception thrown during commit phase (such as the case of cascading from Propagation.REQUIRES_NEW to Propagation.REQUIRES_NEW)
	 * And commit phase case becomes out of consideration for this situation.
	 * For the rest of cases, if caller has proper catch block, then the expectation should not cause roll back
	 */
//TODO Slim3's auto-roll-back exception cases: DeadlineExceededException
//TODO LocalRollbackOnly case
//TODO GlobalRollbackOnly case
//TODO cascade commit by static method	
//TODO Don't forget the cases of transactional class type
//TODO setFailEarlyOnGlobalRollbackOnly affect
	/* isFailEarlyOnGlobalRollbackOnly method is called at the followings:
	 * 		public final void commit(TransactionStatus status)
	 * 		private void processCommit(DefaultTransactionStatus status)
	 */
}
