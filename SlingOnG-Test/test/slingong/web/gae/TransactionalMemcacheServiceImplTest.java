package slingong.web.gae;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slingong.web.gae.CopierImpl.CopierEngine;
import slingong.web.gae.TransactionalMemcacheServiceBase.NoObject;
import slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl.NonTransactionModeEvent;
import slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl.TransactionStash;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.newmainsoftech.aspectjutil.eventmanager.EventInfo;

@Aspect
public class TransactionalMemcacheServiceImplTest {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());

	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig = new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig = new LocalMemcacheServiceTestConfig();
	protected final LocalServiceTestHelper gaeTestHelper = 
		new LocalServiceTestHelper( gaeDatastoreTestConfig, gaeMemcacheTestConfig);
	// --------------------------------------------------------------------------------------------

	protected TransactionalMemcacheServiceFactory transactionalMemcacheServiceFactory; 
	protected TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper 
	= TransactionalMemcacheServiceCommonConstant.transactionalMemcacheServiceSyncHelper;
	protected TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper 
	= TransactionalMemcacheServiceCommonConstant.transactionalMemcacheServiceTransactionHelper;

	@BeforeClass
	public static void beforeClass() throws Throwable {
		arraysToStringMethod = Arrays.class.getMethod( "toString", (new Object[]{}).getClass()); 
			arraysToStringMethod.setAccessible( true);
		
		arraysEqualsMethod 
		= Arrays.class.getMethod( "equals", (new Object[]{}).getClass(), (new Object[]{}).getClass());
			arraysEqualsMethod.setAccessible( true);
	}
	
	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
		
		TransactionalMemcacheServiceFactory.instanceMap.clear();
		transactionalMemcacheServiceFactory  = new TransactionalMemcacheServiceFactory();
		
		TransactionalMemcacheServiceImplTest.executionRecordArrayList.clear();
	}
	@After
	public void tearDown() throws Throwable {
		gaeTestHelper.tearDown();
	}
	
	protected String getMethodName() {
		return Thread.currentThread().getStackTrace()[ 2].getMethodName();
	}

	protected Random random = new Random();
	
	protected static ArrayList<JoinPoint> executionRecordArrayList = new ArrayList<JoinPoint>();
	
	@Pointcut( 
			value="execution( * slingong.web.gae.TransactionalMemcacheServiceBase+" 
					+ ".removeTransactionHandlerThreadLocal( aspectjutil.eventmanager.EventInfo)) " 
//					+ ".removeTransactionHandlerThreadLocalWoker()) " 
					+ "&& target( slingong.web.gae.TransactionalMemcacheServiceImpl)"
			)
	public static void pointcutAtRemoveTransactionHandlerThreadLocalExecution() {
	}
	
	@org.aspectj.lang.annotation.Before( 
			value="pointcutAtRemoveTransactionHandlerThreadLocalExecution()"
			)
	public void beforeAdvisedRemoveTransactionHandlerThreadLocalMethodExecution( JoinPoint joinPoint) {
		TransactionalMemcacheServiceImplTest.executionRecordArrayList.add( joinPoint);
	}
	
	protected void throwExceptionInTransactionMode( 
			TransactionalMemcacheService transactionalMemcacheService) throws Throwable {
		// Throw exception in transaction mode ----------------------------------------------------
		try {
			transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
				Assert.assertTrue( 
						transactionalMemcacheServiceTransactionHelper
						.getNonTransactionModeEvent().isTransactionChangeToBeSaved()
						); 
					// This initializes NonTransactionModeEvent.booleanThreadLocal member field. 
					// Means ThreadLocal.initialValue method is called for that field.
int elementCounter = TransactionalMemcacheServiceImplTest.executionRecordArrayList.size();
				String key1 = "throwExceptionInTransactionMode_key1";
				String value1 = "throwExceptionInTransactionMode_value1";	
				transactionalMemcacheService.put( key1, value1);
Assert.assertTrue( 
		String.format(
				"The number of elements of executionRecordArrayList %1$s after calling " 
				+ "TransactionalMemcacheService.put method. " 
				+ "%n The contents of executionRecordArrayList after calling TransactionalMemcacheService" 
				+ ".put method: %2$s",
				((elementCounter == TransactionalMemcacheServiceImplTest.executionRecordArrayList.size()) 
						? "has not been changed" : "has been reduced"),
				TransactionalMemcacheServiceImplTest.executionRecordArrayList.toString()
				),
		elementCounter < TransactionalMemcacheServiceImplTest.executionRecordArrayList.size()
		);
System.out.format( 
		"executionRecordArrayList = %1$s%n", 
		TransactionalMemcacheServiceImplTest.executionRecordArrayList.toString()
		);
					Assert.assertTrue( 
							TransactionalMemcacheServiceImplTest.executionRecordArrayList.size() > 0);
					
					// Pre-confirmation of instantiations of following ThreadLocal objects:
					//	- TransactionStash object
					//	- Boolean object for NonTransactionModeEvent.booleanThreadLocal member field
					//	- CopierEngine object
				
					int executionRecordIndex = 0;
					boolean isTransactionStashThreadLocalInitialized = false;
					boolean isNonTransactionModeEventBooleanThreadLocalInitialized = false;
					boolean isCopierEngineThreadLocalInitialized = false;
					while( 
							TransactionalMemcacheServiceImplTest.executionRecordArrayList.size() 
							> executionRecordIndex
							) 
					{
						JoinPoint joinPoint 
						= TransactionalMemcacheServiceImplTest
						.executionRecordArrayList.get( executionRecordIndex++);
						
						Method targetMethod = ((MethodSignature)joinPoint.getSignature()).getMethod();
						if ( "initialValue".equals( targetMethod.getName())) {
							Assert.assertEquals( 0, targetMethod.getParameterTypes().length);
							Assert.assertTrue( targetMethod.getDeclaringClass().isAnonymousClass());
							
							// About call to ThreadLocal.initialValue anonymous method for 
							// TransactionalMemcacheServiceTransactionHelperImpl.TransactionStash object in  
							// following locations: 
							// 		- isTransactionModeThread method from TransactionalMemcacheServiceAspect
							// 			.aroundAdvisedSwitchThreadToTransactionModeMethodExecution around-advice 
							// 		- getTransactionStash method in TransactionalMemcacheServiceTransactionHelperImpl
							// 			.switchThreadToTransactionMode method 
							if ( 
									TransactionStash.class.equals( targetMethod.getReturnType()) 
									&& !isTransactionStashThreadLocalInitialized
									) 
							{
								Assert.assertEquals( 
										TransactionalMemcacheServiceTransactionHelperImpl.class, 
										targetMethod.getDeclaringClass().getEnclosingClass()
										);
								
								isTransactionStashThreadLocalInitialized = true;
							}
							// About ThreadLocal.initialValue anonymous method for TransactionalMemcacheServiceTransactionHelperImpl
							// .NonTransactionModeEvent.booleanThreadLocal member field
							else if (
									Boolean.class.equals( targetMethod.getReturnType()) 
									&& !isNonTransactionModeEventBooleanThreadLocalInitialized
									) 
							{
								Assert.assertEquals( 
										NonTransactionModeEvent.class, 
										targetMethod.getDeclaringClass().getEnclosingClass()
										);
								
								isNonTransactionModeEventBooleanThreadLocalInitialized = true;
							}
							// About ThreadLocal.initialValue anonymous method for CopierImpl.copierEngineThreadLocal 
							// member field
							else if ( 
									CopierEngine.class.equals( targetMethod.getReturnType())
									&& !isCopierEngineThreadLocalInitialized
									) 
							{
								Assert.assertEquals( 
										CopierImpl.class, 
										targetMethod.getDeclaringClass().getEnclosingClass()
										);
								
								isCopierEngineThreadLocalInitialized = true;
							}
						}
						
						if ( 
								isTransactionStashThreadLocalInitialized 
								&& isNonTransactionModeEventBooleanThreadLocalInitialized 
								&& isCopierEngineThreadLocalInitialized
								) 
						{
							break;
						}
					} // while
					
					Assert.assertTrue(
							String.format( 
									"%1$s%2$s%3$s", 
									(isTransactionStashThreadLocalInitialized 
											? "" : "TransactionalMemcacheServiceTransactionHelperImpl" 
											+ ".transactionStashThreadLocal member field wasn't initialized.%n"
											),
									(isNonTransactionModeEventBooleanThreadLocalInitialized
											? "" : "TransactionalMemcacheServiceTransactionHelperImpl" 
											+ ".NonTransactionModeEvent.booleanThreadLocal member field " 
											+ "wasn't initialized.%n"
											),
									(isCopierEngineThreadLocalInitialized 
											? "" : "CopierImpl.copierEngineThreadLocal member field wasn't " 
											+ "initialized."
											)
									),
							isTransactionStashThreadLocalInitialized 
							&& isNonTransactionModeEventBooleanThreadLocalInitialized 
							&& isCopierEngineThreadLocalInitialized
							);
				
				TransactionalMemcacheServiceImplTest.executionRecordArrayList.clear();
					
				String key2 = "throwExceptionInTransactionMode_key2";
				Object value2 = new Object();	// Not serializable; will cause exception
				transactionalMemcacheService.put( key2, value2);
					
					Assert.fail( "Expected that TransactionalMemcacheServiceException exception was thrown.");
		}
		catch( TransactionalMemcacheServiceException exception) { // expected exception; do nothing
		}
		// ----------------------------------------------------------------------------------------
		
		// About TransactionalMemcacheServiceBase.removeTransactionHandlerThreadLocal(EventInfo)
		Assert.assertTrue( TransactionalMemcacheServiceImplTest.executionRecordArrayList.size() > 0);
		int executionRecordIndex = 0;
		boolean transactionalMemcacheServiceInstanceCheck = false;
		int transactionalMemcacheServiceInstanceCounter = 0;
		while( TransactionalMemcacheServiceImplTest.executionRecordArrayList.size() > executionRecordIndex) {
			JoinPoint joinPoint 
			= TransactionalMemcacheServiceImplTest.executionRecordArrayList.get( executionRecordIndex++);
			
			if ( joinPoint.getTarget() instanceof TransactionalMemcacheServiceImpl) {
				transactionalMemcacheServiceInstanceCounter++;
				
				if ( transactionalMemcacheService.equals( joinPoint.getTarget())) {
					transactionalMemcacheServiceInstanceCheck = true;
				}
				
				Assert.assertEquals( 
						TransactionalMemcacheServiceBase.class
						.getDeclaredMethod( 
								"removeTransactionHandlerThreadLocal", 
								new Class[]{ EventInfo.class}
								),
						((MethodSignature)joinPoint.getSignature()).getMethod()
						);
				EventInfo eventInfo = (EventInfo)joinPoint.getArgs()[ 0];
				Assert.assertEquals( 
						TransactionalMemcacheServiceException.class, eventInfo.getTriggeredEvent());
			}
		} // while
		Assert.assertTrue( 
				String.format(
						"No records of invocation of removeTransactionHandlerThreadLocal method " 
						+ "on %1$s instance. %nexecutionRecordArrayList = %2$s",
						transactionalMemcacheService.toString(),
						TransactionalMemcacheServiceImplTest.executionRecordArrayList.toString()
						),
				transactionalMemcacheServiceInstanceCheck
				);
		Assert.assertTrue( 
				"No records of invocation of removeTransactionHandlerThreadLocal method of " 
				+ "TransactionalMemcacheServiceImpl instances.",
				(transactionalMemcacheServiceInstanceCounter > 0)
				);
		
		TransactionalMemcacheServiceImplTest.executionRecordArrayList.clear();
	}

	@org.aspectj.lang.annotation.Before(
			value="within( slingong.web.gae.CopierImpl) " 
					+ "&& ( execution( java.lang.Boolean java.lang.ThreadLocal.initialValue())" 
						+ "|| execution( slingong.web.gae.CopierImpl.CopierEngine java.lang.ThreadLocal.initialValue()))"
			)
	public void beforeAdvisedCopierInitialization( JoinPoint joinPoint) {
		executionRecordArrayList.add( joinPoint);
	}
	
	/**
	 * Test to confirm removal of ThreadLocal member fields in CopierImpl class by throwing 
	 * TransactionalMemcacheServiceException exception
	 * @throws Throwable
	 */
	@Test
	public void testThrowingExceptionInTransactionMode3() throws Throwable {
		String memcacheNamespace1 = "testThrowingExceptionInTransactionMode3";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		throwExceptionInTransactionMode( transactionalMemcacheService1);
		
		((CopierImpl)TransactionalMemcacheServiceCommonConstant.copier).isCopierOn();
			int executionRecordIndex = 0;
			JoinPoint joinPoint 
			= TransactionalMemcacheServiceImplTest.executionRecordArrayList.get( executionRecordIndex++);
			Method targetMethod = ((MethodSignature)joinPoint.getSignature()).getMethod();
			
			Assert.assertEquals( 
					"initialValue",
					targetMethod.getName()
					);
			Assert.assertTrue( targetMethod.getDeclaringClass().isAnonymousClass());
			Assert.assertEquals( 
					CopierImpl.class, 
					targetMethod.getDeclaringClass().getEnclosingClass()
					);
			Assert.assertEquals( Boolean.class, targetMethod.getReturnType());
			
			TransactionalMemcacheServiceImplTest.executionRecordArrayList.clear();
			
		((CopierImpl)TransactionalMemcacheServiceCommonConstant.copier).getCopierEngine();
			executionRecordIndex = 0;
			joinPoint 
			= TransactionalMemcacheServiceImplTest.executionRecordArrayList.get( executionRecordIndex++);
			targetMethod = ((MethodSignature)joinPoint.getSignature()).getMethod();
			
			Assert.assertEquals( 
					"initialValue",
					targetMethod.getName()
					);
			Assert.assertTrue( targetMethod.getDeclaringClass().isAnonymousClass());
			Assert.assertEquals( 
					CopierImpl.class, 
					targetMethod.getDeclaringClass().getEnclosingClass()
					);
			Assert.assertEquals( 0, targetMethod.getParameterTypes().length);
			Assert.assertEquals( CopierEngine.class, targetMethod.getReturnType());
	}

	@org.aspectj.lang.annotation.Before(
			value="within( slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl" +
					".NonTransactionModeEvent) " 
					+ "&& execution( java.lang.Boolean java.lang.ThreadLocal.initialValue())"
			)
	public void beforeAdvisedNonTransactionModeEventThreadLocalInitialization( JoinPoint joinPoint) {
		executionRecordArrayList.add( joinPoint);
	}
	
	/**
	 * Test to confirm removal of booleanThreadLocal (Boolean ThreadLocal) member fields in 
	 * TransactionalMemcacheServiceTransactionHelperImpl.NonTransactionModeEvent class by throwing 
	 * TransactionalMemcacheServiceException exception
	 * @throws Throwable
	 */
	@Test
	public void testThrowingExceptionInTransactionMode2() throws Throwable {
		String memcacheNamespace1 = "testThrowingExceptionInTransactionMode2";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		throwExceptionInTransactionMode( transactionalMemcacheService1);
		
		Assert.assertTrue( 
				transactionalMemcacheServiceTransactionHelper
				.getNonTransactionModeEvent().isTransactionChangeToBeSaved()
				); 
		Assert.assertEquals( 1, TransactionalMemcacheServiceImplTest.executionRecordArrayList.size());
			// About ThreadLocal.initialValue anonymous method for 
			// TransactionalMemcacheServiceTransactionHelperImpl.NonTransactionModeEvent.booleanThreadLocal member field
			int executionRecordIndex = 0;
			JoinPoint joinPoint 
			= TransactionalMemcacheServiceImplTest.executionRecordArrayList.get( executionRecordIndex++);
			Method targetMethod = ((MethodSignature)joinPoint.getSignature()).getMethod();
			
			Assert.assertEquals( 
					"initialValue",
					targetMethod.getName()
					);
			Assert.assertTrue( targetMethod.getDeclaringClass().isAnonymousClass());
			Assert.assertEquals( 
					NonTransactionModeEvent.class, 
					targetMethod.getDeclaringClass().getEnclosingClass()
					);
			Assert.assertEquals( 0, targetMethod.getParameterTypes().length);
			Assert.assertEquals( Boolean.class, targetMethod.getReturnType());
	}
	
	@org.aspectj.lang.annotation.Before(
			value="within( slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl) " 
					+ "&& execution( slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl" 
						+ ".TransactionStash java.lang.ThreadLocal.initialValue())"
			)
	public void beforeAdvisedTransactionStashThreadLocalInitialization( JoinPoint joinPoint) {
		executionRecordArrayList.add( joinPoint);
	}
	
	/**
	 * Test to confirm removal of transactionStashThreadLocal (TransactionStash ThreadLocal) 
	 * member fields in TransactionalMemcacheServiceTransactionHelperImpl class by throwing 
	 * TransactionalMemcacheServiceException exception
	 * @throws Throwable
	 */
	@Test
	public void testThrowingExceptionInTransactionMode1() throws Throwable {
		String memcacheNamespace1 = "testThrowingExceptionInTransactionMode1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		throwExceptionInTransactionMode( transactionalMemcacheService1);
		
		Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		Assert.assertEquals( 1, TransactionalMemcacheServiceImplTest.executionRecordArrayList.size());
			// About ThreadLocal.initialValue anonymous method for 
			// TransactionalMemcacheServiceTransactionHelperImpl.TransactionStash object
			int executionRecordIndex = 0;
			JoinPoint joinPoint 
			= TransactionalMemcacheServiceImplTest.executionRecordArrayList.get( executionRecordIndex++);
			Method targetMethod = ((MethodSignature)joinPoint.getSignature()).getMethod();
			Assert.assertEquals( 
					"initialValue",
					targetMethod.getName()
					);
			Assert.assertTrue( targetMethod.getDeclaringClass().isAnonymousClass());
			Assert.assertEquals( 
					TransactionalMemcacheServiceTransactionHelperImpl.class, 
					targetMethod.getDeclaringClass().getEnclosingClass()
					);
			Assert.assertEquals( 0, targetMethod.getParameterTypes().length);
			Assert.assertEquals( TransactionStash.class, targetMethod.getReturnType());
	}

	@Test
	public void testClearInTransactionMode2() throws Throwable {
		String methodName = getMethodName();
		String memcacheNamespace1 = methodName + "1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
			Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
		
			Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() > 0);
		
		String memcacheNamespace2 = methodName + "2";
		TransactionalMemcacheService transactionalMemcacheService2 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2);
			MemcacheService memcacheService2 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2);
		
			Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
				String key2 = Long.toHexString( random.nextLong());
				entryMap2.put( key2, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
					transactionalMemcacheService2.put( entry.getKey(), entry.getValue());
				} // for
		
				Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() > 0);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
		
			String memcacheNamespace3 = methodName + "3";
			TransactionalMemcacheService transactionalMemcacheService3 
			= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace3);
				MemcacheService memcacheService3 
				= MemcacheServiceFactory.getMemcacheService( memcacheNamespace3);
			
				Map<Serializable, Serializable> entryMap3 = generateTestMapObj();
					String key3 = Long.toHexString( random.nextLong());
					entryMap3.put( key3, generateTestMapObj());
					for( Entry<Serializable, Serializable> entry : entryMap3.entrySet()) {
						transactionalMemcacheService3.put( entry.getKey(), entry.getValue());
					} // for
			
					Assert.assertTrue( 
							transactionalMemcacheService3.getAll( entryMap3.keySet()).size() > 0);
			
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).clear();
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService3).clear();
			
			Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
				Map<Serializable, Object> cachedEntryMap1 = memcacheService1.getAll( entryMap1.keySet());
				compareMaps( entryMap1, cachedEntryMap1);
				
			Map<Serializable, Object> cachedEntryMap2 
			= transactionalMemcacheService2.getAll( entryMap2.keySet());
			compareMaps( entryMap2, cachedEntryMap2);
			
			Assert.assertEquals( 0, transactionalMemcacheService3.getAll( entryMap3.keySet()).size());
				Assert.assertEquals( 0, memcacheService3.getAll( entryMap3.keySet()).size());
					
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( false);
		
		cachedEntryMap1 = transactionalMemcacheService1.getAll( entryMap1.keySet());
		compareMaps( entryMap1, cachedEntryMap1);
		
		cachedEntryMap2 = transactionalMemcacheService2.getAll( entryMap2.keySet());
		compareMaps( entryMap2, cachedEntryMap2);
		
		Assert.assertEquals( 0, transactionalMemcacheService3.getAll( entryMap3.keySet()).size());
			for( Serializable keyObj : entryMap3.keySet()) {
				Assert.assertFalse( 
						checkOnKey( 
								(TransactionalMemcacheServiceImpl)transactionalMemcacheService3, 
								keyObj
								)
						);
			} // for
	}
	@Test
	public void testClearInTransactionMode1() throws Throwable {
		String methodName = getMethodName();
		String memcacheNamespace1 = methodName + "1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
			Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
		
			Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() > 0);
			
		String memcacheNamespace2 = methodName + "2";
		TransactionalMemcacheService transactionalMemcacheService2 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2);
			MemcacheService memcacheService2 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2);
		
			Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
				String key2 = Long.toHexString( random.nextLong());
				entryMap2.put( key2, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
					transactionalMemcacheService2.put( entry.getKey(), entry.getValue());
				} // for
		
				Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() > 0);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
		
			String memcacheNamespace3 = methodName + "3";
			TransactionalMemcacheService transactionalMemcacheService3 
			= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace3);
				MemcacheService memcacheService3 
				= MemcacheServiceFactory.getMemcacheService( memcacheNamespace3);
			
				Map<Serializable, Serializable> entryMap3 = generateTestMapObj();
					String key3 = Long.toHexString( random.nextLong());
					entryMap3.put( key3, generateTestMapObj());
					for( Entry<Serializable, Serializable> entry : entryMap3.entrySet()) {
						transactionalMemcacheService3.put( entry.getKey(), entry.getValue());
					} // for
			
					Assert.assertTrue( 
							transactionalMemcacheService3.getAll( entryMap3.keySet()).size() > 0);
			
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).clear();
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService3).clear();
			
			Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
				Map<Serializable, Object> cachedEntryMap1 = memcacheService1.getAll( entryMap1.keySet());
				compareMaps( entryMap1, cachedEntryMap1);
				
			Map<Serializable, Object> cachedEntryMap2 
			= transactionalMemcacheService2.getAll( entryMap2.keySet());
			compareMaps( entryMap2, cachedEntryMap2);
			
			Assert.assertEquals( 0, transactionalMemcacheService3.getAll( entryMap3.keySet()).size());
				Assert.assertEquals( 0, memcacheService3.getAll( entryMap3.keySet()).size());
					
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		
		Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
			for( Serializable keyObj : entryMap1.keySet()) {
				Assert.assertFalse( 
						checkOnKey( 
								(TransactionalMemcacheServiceImpl)transactionalMemcacheService1, 
								keyObj
								)
						);
			} // for
		
		cachedEntryMap2 = transactionalMemcacheService2.getAll( entryMap2.keySet());
		compareMaps( entryMap2, cachedEntryMap2);
		
		Assert.assertEquals( 0, transactionalMemcacheService3.getAll( entryMap3.keySet()).size());
			for( Serializable keyObj : entryMap3.keySet()) {
				Assert.assertFalse( 
						checkOnKey( 
								(TransactionalMemcacheServiceImpl)transactionalMemcacheService3, 
								keyObj
								)
						);
			} // for
	}
	
	@Test
	public void testClearInNonTransactionMode() throws Throwable {
		String methodName = getMethodName();
		String memcacheNamespace1 = methodName + "1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
			Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
			
			Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() > 0);
			
		String memcacheNamespace2 = methodName + "2";
		TransactionalMemcacheService transactionalMemcacheService2 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2);
			MemcacheService memcacheService2 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2);
		
			Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
				String key2 = Long.toHexString( random.nextLong());
				entryMap2.put( key2, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
					transactionalMemcacheService2.put( entry.getKey(), entry.getValue());
				} // for
		
				Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() > 0);
				
		((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).clear();
		for( Serializable keyObj : entryMap1.keySet()) {
			Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() < 1);
					Assert.assertFalse( 
							checkOnKey( 
									(TransactionalMemcacheServiceImpl)transactionalMemcacheService1, 
									keyObj
									)
							);
				} //for
			Map<Serializable, Object> cachedEntryMap2 
			= transactionalMemcacheService2.getAll( entryMap2.keySet());
				compareMaps( entryMap2, cachedEntryMap2);
	}
	
	@Test
	public void testClearAllInTransactionMode2() throws Throwable {
		String methodName = getMethodName();
		String memcacheNamespace1 = methodName + "1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
			
			Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
				String key1 = Long.toHexString( random.nextLong());
				entryMap1.put( key1, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
					transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
				} // for
				
				Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() > 0);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			String memcacheNamespace2 = methodName + "2";
			TransactionalMemcacheService transactionalMemcacheService2 
			= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2);
				MemcacheService memcacheService2 
				= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2);
			
				Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
					String key2 = Long.toHexString( random.nextLong());
					entryMap2.put( key2, generateTestMapObj());
					for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
						transactionalMemcacheService2.put( entry.getKey(), entry.getValue());
					} // for
			
					Assert.assertTrue( 
							transactionalMemcacheService2.getAll( entryMap2.keySet()).size() > 0);
					
			transactionalMemcacheService2.clearAll();
				Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() < 1);
					Map<Serializable, Object> cachedEntryMap1 
					= memcacheService1.getAll( entryMap1.keySet());
					compareMaps( entryMap1, cachedEntryMap1);
				Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() < 1);
					Map<Serializable, Object> cachedEntryMap2 
					= memcacheService2.getAll( entryMap2.keySet());
					Assert.assertTrue( cachedEntryMap2.size() < 1);
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( false);
		
		cachedEntryMap1 = transactionalMemcacheService1.getAll( entryMap1.keySet());
			compareMaps( entryMap1, cachedEntryMap1);
		Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() < 1);
			cachedEntryMap2 = memcacheService2.getAll( entryMap2.keySet());
			Assert.assertTrue( cachedEntryMap2.size() < 1);
	}
	@Test
	public void testClearAllInTransactionMode1() throws Throwable {
		String methodName = getMethodName();
		String memcacheNamespace1 = methodName + "1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
			
			Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
				String key1 = Long.toHexString( random.nextLong());
				entryMap1.put( key1, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
					transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
				} // for
				
				Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() > 0);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			String memcacheNamespace2 = methodName + "2";
			TransactionalMemcacheService transactionalMemcacheService2 
			= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2);
				MemcacheService memcacheService2 
				= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2);
			
				Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
					String key2 = Long.toHexString( random.nextLong());
					entryMap2.put( key2, generateTestMapObj());
					for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
						transactionalMemcacheService2.put( entry.getKey(), entry.getValue());
					} // for
			
					Assert.assertTrue( 
							transactionalMemcacheService2.getAll( entryMap2.keySet()).size() > 0);
					
			transactionalMemcacheService2.clearAll();
				Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() < 1);
					Map<Serializable, Object> cachedEntryMap1 
					= memcacheService1.getAll( entryMap1.keySet());
					compareMaps( entryMap1, cachedEntryMap1);
				Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() < 1);
					Map<Serializable, Object> cachedEntryMap2 
					= memcacheService2.getAll( entryMap2.keySet());
					Assert.assertTrue( cachedEntryMap2.size() < 1);
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		
		Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() < 1);
			cachedEntryMap1 = memcacheService1.getAll( entryMap1.keySet());
			Assert.assertTrue( cachedEntryMap1.size() < 1);
		Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() < 1);
			cachedEntryMap2 = memcacheService2.getAll( entryMap2.keySet());
			Assert.assertTrue( cachedEntryMap2.size() < 1);
	}
	
	@Test
	public void testClearAllInNonTransactionMode() {
		String methodName = getMethodName();
		String memcacheNamespace1 = methodName + "1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
			Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
				String key1 = Long.toHexString( random.nextLong());
				entryMap1.put( key1, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
					transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
				} // for
		
		String memcacheNamespace2 = methodName + "2";
		TransactionalMemcacheService transactionalMemcacheService2 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2);
		
			Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
				String key2 = Long.toHexString( random.nextLong());
				entryMap2.put( key2, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
					transactionalMemcacheService2.put( entry.getKey(), entry.getValue());
				} // for
		
		Assert.assertTrue( transactionalMemcacheService1.getAll( entryMap1.keySet()).size() > 0);
		Assert.assertTrue( transactionalMemcacheService2.getAll( entryMap2.keySet()).size() > 0);
		
		transactionalMemcacheService1.clearAll();
			Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
				for( Serializable keyObj : entryMap1.keySet()) {
					Assert.assertFalse( 
							checkOnKey( 
									(TransactionalMemcacheServiceImpl)transactionalMemcacheService1, 
									keyObj
									)
							);
				} //for
			Assert.assertEquals( 0, transactionalMemcacheService2.getAll( entryMap2.keySet()).size());
				for( Serializable keyObj : entryMap2.keySet()) {
					Assert.assertFalse( 
							checkOnKey( 
									(TransactionalMemcacheServiceImpl)transactionalMemcacheService2, 
									keyObj
									)
							);
				} //for
	}
	
	@Test
	public void testSetKeySetKey() {
		String memcacheNamespace1 = "testSetKeySetKey";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		// Set keySetKey in non-Transaction mode --------------------------------------------------
		String keySetKey = Long.valueOf( System.currentTimeMillis()).toString();
		((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).setKeySetKey( keySetKey);
			Assert.assertEquals( 
					keySetKey, 
					((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey()
					);
		// ----------------------------------------------------------------------------------------
		
		// Set keySetKey in Transaction mode ------------------------------------------------------
		keySetKey = Long.valueOf( System.currentTimeMillis()).toString();
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).setKeySetKey( keySetKey);
			Assert.assertEquals( 
					keySetKey, 
					((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey()
					);
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		Assert.assertEquals( 
				keySetKey, 
				((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey()
				);
		// Do not commit change during transaction ------------------------------------------------
		keySetKey = Long.valueOf( System.currentTimeMillis()).toString();
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).setKeySetKey( keySetKey);
				Assert.assertEquals( 
						keySetKey, 
						((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey()
						);
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( false);
		Assert.assertEquals( 
				keySetKey, 
				((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey()
				);
		// ----------------------------------------------------------------------------------------
		// Do not commit change by throwing exception ---------------------------------------------
		keySetKey = Long.valueOf( System.currentTimeMillis()).toString();
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).setKeySetKey( keySetKey);
			
			String key2 = "throwExceptionInTransactionMode_key2";
			Object value2 = new Object();	// Not serializable; will cause exception
			try {
				transactionalMemcacheService1.put( key2, value2);
					Assert.fail( "Expected that TransactionalMemcacheServiceException exception was thrown.");
			}
			catch( TransactionalMemcacheServiceException exception) { // Do nothing
			}
			Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
			
			Assert.assertEquals( 
					keySetKey, 
					((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey()
					);
		// ----------------------------------------------------------------------------------------
		// ----------------------------------------------------------------------------------------
	}
	
	protected long generatePositiveLongValue() {
		long longValue = random.nextLong();
		while( longValue < 0) {
			longValue = random.nextLong();
		}
		return longValue;
	}
	
	@Test
	public void testDeleteAllWithMillisNoReAddInTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			try {
				transactionalMemcacheService1.deleteAll( entryMap1.keySet(), generatePositiveLongValue());
			}
			catch( TransactionalMemcacheServiceException exception) {
				Assert.assertTrue( exception.getCause() instanceof UnsupportedOperationException);
			}
			
		Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		
		Map<Serializable, Object> chachedEntryMap 
		= transactionalMemcacheService1.getAll( entryMap1.keySet());
		compareMaps( entryMap1, chachedEntryMap);
	}
	@Test
	public void testDeleteAllWithMillisNoReAddInNonTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			Random random = new Random();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
		
		try {
			Set<Serializable> deletedKeySet 
			= transactionalMemcacheService1.deleteAll( entryMap1.keySet(), generatePositiveLongValue());
			Assert.fail();
		}
		catch( TransactionalMemcacheServiceException exception) {
			Assert.assertTrue( exception.getCause() instanceof UnsupportedOperationException);
		}
		
		Map<Serializable, Object> chachedEntryMap 
		= transactionalMemcacheService1.getAll( entryMap1.keySet());
		compareMaps( entryMap1, chachedEntryMap);
	}
	
	@Test
	public void testDeleteAllInTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			Random random = new Random();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
			
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Set<Serializable> deletedKeySet1 = transactionalMemcacheService1.deleteAll( entryMap1.keySet());
			Assert.assertEquals( entryMap1.keySet(), deletedKeySet1);
			Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
		
			deletedKeySet1 = transactionalMemcacheService1.deleteAll( entryMap1.keySet());
				Assert.assertEquals( 0, deletedKeySet1.size());
			
			Map<Serializable, Serializable> entryMap2 = generateTestMapObj();
				String key2 = Long.toHexString( random.nextLong());
				entryMap2.put( key2, generateTestMapObj());
				for( Entry<Serializable, Serializable> entry : entryMap2.entrySet()) {
					transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
				} // for
			Set<Serializable> deletedKeySet2 = transactionalMemcacheService1.deleteAll( entryMap2.keySet());
				Assert.assertEquals( entryMap2.keySet(), deletedKeySet2);
				Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap2.keySet()).size());
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		
		Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
		Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap2.keySet()).size());
	}
	/**
	 * Check whether key exists in the following container object:<br />
	 * <ul>
	 * 	<li>mainCache MemcacheService object</li>
	 * 	<li>Keys set in mainCache MemcacheService object</li>
	 * 	<li>Keys set in keysMap static member field of TransactionalMemcacheServiceSyncHelperImpl</li>
	 * </ul>
	 * Check it by using underneath MemcacheService object but transactionalMemcacheServiceImpl.
	 * @param transactionalMemcacheServiceImpl
	 * @param key
	 * @return
	 */
	protected boolean checkOnKey( 
			TransactionalMemcacheServiceImpl transactionalMemcacheServiceImpl, Serializable key) {
		
		String memcacheNamespace = transactionalMemcacheServiceImpl.getNamespace();
		MemcacheService memcacheService 
		= MemcacheServiceFactory.getMemcacheService( memcacheNamespace);
		
		String keySetKey = transactionalMemcacheServiceImpl.getKeySetKey();
		Set<Serializable> keySet = (Set<Serializable>)memcacheService.get( keySetKey);
		
		boolean mainCacheContains = memcacheService.contains( key);
		boolean mainCacheKeySetContains = keySet.contains( key);
		boolean syncHelperKeySetContains 
		= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace).contains( key);
			Assert.assertTrue(
					String.format(
							"Detected inconsistency in holding %1$s as key:%n%2$c%3$s%n%2$c%4$s%n%2$c%5$s",
							key.toString(),
							'\t',
							(mainCacheContains ? 
									"mainCache have an entry for it" 
									: "mainCache does not have an entry for it"),
							(mainCacheKeySetContains ? 
									"Keys set in mainCache contains it" 
									: "Keys set in mainCache does not contains it"),
							(syncHelperKeySetContains ? 
									"Keys set in sync helper contains it" 
									: "Keys set in sync helper does not contains it")
							),
					(mainCacheContains && mainCacheKeySetContains && syncHelperKeySetContains) 
					== (mainCacheContains || mainCacheKeySetContains || syncHelperKeySetContains) 
					);
		return (mainCacheContains && mainCacheKeySetContains && syncHelperKeySetContains);
	}

	@Test
	public void testDeleteAllInNonTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Map<Serializable, Serializable> entryMap1 = generateTestMapObj();
			Random random = new Random();
			String key1 = Long.toHexString( random.nextLong());
			entryMap1.put( key1, generateTestMapObj());
			for( Entry<Serializable, Serializable> entry : entryMap1.entrySet()) {
				transactionalMemcacheService1.put( entry.getKey(), entry.getValue());
			} // for
		Set<Serializable> deletedKeySet = transactionalMemcacheService1.deleteAll( entryMap1.keySet());
			Assert.assertEquals( entryMap1.keySet(), deletedKeySet);
			Assert.assertEquals( 0, transactionalMemcacheService1.getAll( entryMap1.keySet()).size());
		
		deletedKeySet = transactionalMemcacheService1.deleteAll( entryMap1.keySet());
			Assert.assertEquals( 0, deletedKeySet.size());
	}
	
	@Test
	public void testDeleteWithMillisNoReAddInTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1 = Long.toHexString( random.nextLong());
		transactionalMemcacheService1.put( key1, value1);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			try {
				transactionalMemcacheService1.delete( key1, generatePositiveLongValue());
			}
			catch( TransactionalMemcacheServiceException exception) { // Expected exception
				Assert.assertTrue( 
						String.format(
								"Expected TransactionalMemcacheServiceException caused by " 
								+ "UnsupportedOperationException, but actual one wasn't :%1$s",
								exception.getCause().toString()
								),
						( exception.getCause() instanceof UnsupportedOperationException)
						);
			}
		Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		
		Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
	}
	@Test
	public void testDeleteWithMillisNoReAddInNonTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1 = Long.toHexString( random.nextLong());
		transactionalMemcacheService1.put( key1, value1);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
		try {
			transactionalMemcacheService1.delete( key1, generatePositiveLongValue());
		}
		catch( TransactionalMemcacheServiceException exception) { // Expected exception
			Assert.assertTrue( 
					String.format(
							"Expected TransactionalMemcacheServiceException caused by " 
							+ "UnsupportedOperationException, but actual one wasn't :%1$s",
							exception.getCause().toString()
							),
					( exception.getCause() instanceof UnsupportedOperationException)
					);
		}
		Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
	}

	@Test
	public void testDeleteInTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Integer.toString( random.nextInt( 100));
		String value1 = Long.toHexString( System.currentTimeMillis());
		transactionalMemcacheService1.put( key1, value1);
			
		Integer key2 = random.nextInt( 100);
		Assert.assertFalse( transactionalMemcacheService1.delete( key2));
		Long value2 = random.nextLong();
		transactionalMemcacheService1.put( key2, value2);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Assert.assertTrue( transactionalMemcacheService1.delete( key1));
				Assert.assertTrue( 
						checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
						);
			Assert.assertTrue( transactionalMemcacheService1.delete( key2));
				Assert.assertTrue( 
						checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key2)
						);

				Date key3 = new Date();
				ArrayList<Object> strArrayList = new ArrayList<Object>();
					strArrayList.add( Long.toHexString( random.nextLong()));
					strArrayList.add( random.nextInt( 100)); 
					strArrayList.add( random.nextLong()); 
					strArrayList.add( new Date()); 
					strArrayList.add( new TestDummy( UUID.randomUUID())); 
				transactionalMemcacheService1.put( key3, strArrayList);
				Assert.assertTrue( transactionalMemcacheService1.delete( key3));
					
				TestDummy key4 = new TestDummy( UUID.randomUUID());
				Object[] value4 = strArrayList.toArray( new Object[]{});
				transactionalMemcacheService1.put( key4, value4);
				Assert.assertTrue( transactionalMemcacheService1.delete( key4));
					
				String key5 = Long.toHexString( System.currentTimeMillis());
				Map<Serializable, Serializable> value5 = generateTestMapObj();
				transactionalMemcacheService1.put( key5, value5);
				Assert.assertTrue( transactionalMemcacheService1.delete( key5));
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		
		Assert.assertTrue( transactionalMemcacheService1.get( key1) instanceof NoObject);
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
		Assert.assertTrue( transactionalMemcacheService1.get( key2) instanceof NoObject);
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key2)
					);
		Assert.assertTrue( transactionalMemcacheService1.get( key3) instanceof NoObject);
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key3)
					);
		Assert.assertTrue( transactionalMemcacheService1.get( key4) instanceof NoObject);
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key4)
					);
		Assert.assertTrue( transactionalMemcacheService1.get( key5) instanceof NoObject);
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key5)
					);
	}
	
	@Test
	public void testDeleteInNonTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Integer.toString( random.nextInt( 100));
		Assert.assertFalse( transactionalMemcacheService1.delete( key1));
		String value1 = Long.toHexString( System.currentTimeMillis());
		transactionalMemcacheService1.put( key1, value1);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
		Assert.assertTrue( transactionalMemcacheService1.delete( key1));
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key1)
					);
			
		Integer key2 = random.nextInt( 100);
		Assert.assertFalse( transactionalMemcacheService1.delete( key2));
		Long value2 = random.nextLong();
		transactionalMemcacheService1.put( key2, value2);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key2)
					);
		Assert.assertTrue( transactionalMemcacheService1.delete( key2));
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key2)
					);
		
		Date key3 = new Date();
		Assert.assertFalse( transactionalMemcacheService1.delete( key3));
		ArrayList<Object> strArrayList = new ArrayList<Object>();
			strArrayList.add( Long.toHexString( random.nextLong()));
			strArrayList.add( random.nextInt( 100)); 
			strArrayList.add( random.nextLong()); 
			strArrayList.add( new Date()); 
			strArrayList.add( new TestDummy( UUID.randomUUID())); 
		transactionalMemcacheService1.put( key3, strArrayList);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key3)
					);
		Assert.assertTrue( transactionalMemcacheService1.delete( key3));
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key3)
					);
			
		TestDummy key4 = new TestDummy( UUID.randomUUID());
		Assert.assertFalse( transactionalMemcacheService1.delete( key4));
		Object[] value4 = strArrayList.toArray( new Object[]{});
		transactionalMemcacheService1.put( key4, value4);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key4)
					);
		Assert.assertTrue( transactionalMemcacheService1.delete( key4));
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key4)
					);
			
		String key5 = Long.toHexString( System.currentTimeMillis());
		Map<Serializable, Serializable> value5 = generateTestMapObj();
		transactionalMemcacheService1.put( key5, value5);
			Assert.assertTrue( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key5)
					);
		Assert.assertTrue( transactionalMemcacheService1.delete( key5));
			Assert.assertFalse( 
					checkOnKey( (TransactionalMemcacheServiceImpl)transactionalMemcacheService1, key5)
					);
	}
	
	protected static Method arraysToStringMethod = null;
	protected static Method arraysEqualsMethod = null;
	
	protected static class TestDummy implements Serializable {
		private static final long serialVersionUID = -20120221L;
		
		protected final UUID uuid;
			public UUID getUuid() {
				return uuid;
			}

		public TestDummy( UUID uuid) {
			this.uuid = uuid;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			
			TestDummy other = (TestDummy) obj;
			if (uuid == null) {
				if (other.uuid != null) return false;
			} 
			else if (!uuid.equals(other.uuid)) return false;
			return true;
		}
	}
	
	protected HashMap<Serializable, Serializable> generateTestMapObj() {
		HashMap<Serializable, Serializable> elementMap = new HashMap<Serializable, Serializable>();
			Random random = new Random();
			
			String key1 = Integer.toString( random.nextInt( 100));
			String value1 = Long.toHexString( System.currentTimeMillis());
				elementMap.put( key1, value1);
			
			Integer key2 = random.nextInt( 100);
			Long value2 = random.nextLong();
				elementMap.put( key2, value2);
			
			Date key3 = new Date();
			ArrayList<Object> strArrayList = new ArrayList<Object>();
				strArrayList.add( Long.toHexString( random.nextLong()));
				strArrayList.add( random.nextInt( 100)); 
				strArrayList.add( random.nextLong()); 
				strArrayList.add( new Date()); 
				strArrayList.add( new TestDummy( UUID.randomUUID())); 
				elementMap.put( key3, strArrayList);
				
			TestDummy key4 = new TestDummy( UUID.randomUUID());
			Object[] value4 = strArrayList.toArray( new Object[]{});
				elementMap.put( key4, value4);
			
		return elementMap;
	}
	
	protected void compareMaps( Map map1, Map map2) throws Throwable {
		Assert.assertEquals( map1.size(), map2.size());
		
		for( Entry<Object, Object> entry : (Set<Entry<Object, Object>>)map1.entrySet()) {
			if ( entry.getValue().getClass().isArray()) {
				Assert.assertTrue(
						Arrays.equals( 
								(Object[])entry.getValue(), 
								(Object[])map2.get( entry.getKey())
								)
						);
			}
			else if ( entry.getValue() instanceof Collection<?>) {
				Assert.assertTrue(
						Arrays.equals( 
								((Collection<Object>)(entry.getValue())).toArray(),
								((Collection<Object>)(map2.get( entry.getKey()))).toArray()
								)
						);
			}
			else if ( entry.getValue() instanceof Map<?, ?>) {
				Map<Serializable, Serializable> subMap 
				= (Map<Serializable, Serializable>)(map2.get( entry.getKey()));
				for( Entry<Serializable, Serializable> subEntry 
						: ((Map<Serializable, Serializable>)(entry.getValue())).entrySet()) {
					if ( subEntry.getValue().getClass().isArray()) {
						Assert.assertTrue( 
								String.format(
										"Expected %1$s but %2$s%n",
										entry.getValue().toString(),
										map2.get( entry.getKey()).toString()
										),
								Arrays.equals(
										(Object[])(subEntry.getValue()), 
										(Object[])(subMap.get( subEntry.getKey()))
										)
								);
					}
					else {
						Assert.assertEquals( 
								String.format(
										"Expected %1$s but %2$s%n",
										entry.getValue().toString(),
										map2.get( entry.getKey()).toString()
										),
								subEntry.getValue(),
								subMap.get( subEntry.getKey())
								);
					}
				} // for
			}
			else {
				Assert.assertEquals( entry.getValue(), map2.get( entry.getKey()));
			}
		} // for
	}
	
	@Test
	public void testPutAllAndGetAllInTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Map<Serializable, Serializable> elementMap1 = generateTestMapObj();
			transactionalMemcacheService1.putAll( elementMap1);
			Set<Serializable> keySet1 = elementMap1.keySet();
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Map<Serializable, Object> cachedElementMap 
			= transactionalMemcacheService1.getAll( keySet1);
				compareMaps( elementMap1, cachedElementMap);
				
			Map<Serializable, Serializable> elementMap2 = generateTestMapObj();
				transactionalMemcacheService1.putAll( elementMap2);
				Set<Serializable> keySet2 = elementMap2.keySet();
				cachedElementMap = transactionalMemcacheService1.getAll( keySet2);
				compareMaps( elementMap2, cachedElementMap);
			
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
			
		cachedElementMap = transactionalMemcacheService1.getAll( keySet1);
		compareMaps( elementMap1, cachedElementMap);
		
		cachedElementMap = transactionalMemcacheService1.getAll( keySet2);
		compareMaps( elementMap2, cachedElementMap);
	}

	@Test
	public void testPutAllAndGetAllInNonTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Map<Serializable, Serializable> elementMap = new HashMap<Serializable, Serializable>();
			String key1 = "key1";
			String value1 = "value1";
				elementMap.put( key1, value1);
			Integer key2 = new Integer( 10);
			Long value2 = new Long( 100);
				elementMap.put( key2, value2);
			Date key3 = new Date();
			ArrayList<String> strArrayList = new ArrayList<String>();
				for( int elementCount = 0; elementCount < 4; elementCount++) {
					strArrayList.add( Long.toHexString( System.currentTimeMillis()));
					Thread.sleep( 5);
				} // for
				elementMap.put( key3, strArrayList);
			String key4 = "key4";
			String[] value4 = strArrayList.toArray( new String[]{});
				elementMap.put( key4, value4);
		transactionalMemcacheService1.putAll( elementMap);
		Set<Serializable> keySet = elementMap.keySet();
		Map<Serializable, Object> cachedElementMap 
		= transactionalMemcacheService1.getAll( keySet);
			for( Serializable keyObj : keySet) {
				Class<?> elementClass = elementMap.get( keyObj).getClass();
				if ( elementClass.isArray()) {
					Assert.assertTrue( 
							cachedElementMap.get( keyObj).getClass().isArray()
							);
					Assert.assertTrue( 
							elementClass.isAssignableFrom( cachedElementMap.get( keyObj).getClass())
							);
					Assert.assertTrue( 
							String.format(
									"elementMap.get( %1$s) = %2$s," 
									+ "%ncachedElementMap.get( %1$s) = %3$s", 
									keyObj.toString(),
									arraysToStringMethod.invoke( null, elementMap.get( keyObj)),
									arraysToStringMethod.invoke( null, cachedElementMap.get( keyObj))
									),
							(Boolean)(arraysEqualsMethod.invoke( 
									null, elementMap.get( keyObj), cachedElementMap.get( keyObj)))
							);
				}
				else {
					Assert.assertEquals( elementMap.get( keyObj), cachedElementMap.get( keyObj));
				}
			} // for
	}
	
	@Test
	public void testGet() {
		String memcacheNamespace1 = "testGetInTransactionMode";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		String keySetKey = Long.valueOf( System.currentTimeMillis()).toString();
		((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).setKeySetKey( keySetKey);	
		
		String key1 = "key1";
		String value1 = "value1";
			memcacheService1.put( key1, value1);
		Set<Serializable> keysSet = (Set<Serializable>)memcacheService1.get( keySetKey);
			keysSet.add( new String( key1));
			memcacheService1.put( keySetKey, keysSet);
		keysSet 
		= ((TransactionalMemcacheServiceSyncHelperImpl)transactionalMemcacheServiceSyncHelper)
		.getKeySet( memcacheNamespace1);
			keysSet.add( new String( key1));
			((TransactionalMemcacheServiceSyncHelperImpl)transactionalMemcacheServiceSyncHelper)
			.keysMap.put( memcacheNamespace1, keysSet);
		
		Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
		
		// Call get method in Transaction mode ----------------------------------------------------
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( false);
		// ----------------------------------------------------------------------------------------
	}
	
	/**
	 * Test not to commit changes made during transaction. 
	 */
	@Test
	public void testSwitchThreadToNonTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		// Do not commit change during transaction ------------------------------------------------
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			String key1 = "key1";
			String value1 = "value1";
			transactionalMemcacheService1.put( key1, value1);
			
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( false);
		// ----------------------------------------------------------------------------------------
		
		Assert.assertTrue( transactionalMemcacheService1.get( key1) instanceof NoObject);
			String keySetKey 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey();
			Set<Serializable> keysSet = (Set<Serializable>)memcacheService1.get( keySetKey);
			Assert.assertFalse( keysSet.contains( key1));
			
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace1);
			Assert.assertFalse( keysSet.contains( key1));
	}
	
	/**
	 * Test entry modification by external code during transaction mode
	 */
	@Test
	public void testPutInTransactionMode4() {
		String memcacheNamespace4a = "testPutInTransactionMode4a";
		TransactionalMemcacheService transactionalMemcacheService4a 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace4a);
			MemcacheService memcacheService4a 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace4a);
		
		String key1 = "testPutInTransactionMode4_Key1";
		String value1a = "testPutInTransactionMode4_Value1a";
		transactionalMemcacheService4a.put( key1, value1a);
			Assert.assertEquals( value1a, transactionalMemcacheService4a.get( key1));
		
		// Overwrites entries in transaction mode -------------------------------------------------
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
		
		String value1b = "updatedValueByExternalCode";
		memcacheService4a.put( key1, value1b);
		
		String value1c = "updatedValueInTransactionMode";
		transactionalMemcacheService4a.put( key1, value1c);

		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		// ----------------------------------------------------------------------------------------
		
		Assert.assertTrue( transactionalMemcacheService4a.get( key1) instanceof NoObject);
			String keySetKey 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService4a).getKeySetKey();
			Set<Serializable> keysSet = (Set<Serializable>)memcacheService4a.get( keySetKey);
			Assert.assertFalse( keysSet.contains( key1));
			
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace4a);
			Assert.assertFalse( keysSet.contains( key1));
	}
	
	/**
	 * Test overwriting entry during transaction mode 
	 */
	@Test
	public void testPutInTransactionMode3() {
		String memcacheNamespace3a = "testPutInTransactionMode3a";
		TransactionalMemcacheService transactionalMemcacheService3a 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace3a);
		
		String key1 = "testPutInTransactionMode3_Key1";
		String value1a = "testPutInTransactionMode3_Value1a";
		transactionalMemcacheService3a.put( key1, value1a);
			Assert.assertEquals( value1a, transactionalMemcacheService3a.get( key1));
		
		// Overwrites entries in transaction mode -------------------------------------------------
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
		
		String value1b = "testPutInTransactionMode3_Value1b";
		transactionalMemcacheService3a.put( key1, value1b);
			Assert.assertEquals( value1b, transactionalMemcacheService3a.get( key1));
		
		int key2 = (new Random()).nextInt( 100);
		Date value2a = new Date();
		transactionalMemcacheService3a.put( key2, value2a);
		
		Date value2b = new Date();
		transactionalMemcacheService3a.put( key2, value2b);
			Assert.assertEquals( value2b, transactionalMemcacheService3a.get( key2));
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		// ----------------------------------------------------------------------------------------
		
		Assert.assertEquals( value1b, transactionalMemcacheService3a.get( key1));
		Assert.assertEquals( value2b, transactionalMemcacheService3a.get( key2));
	}
	
	/**
	 * Test consistency of transaction mode among TransactionalMemcacheServiceImpl instances. <br /> 
	 * During transaction mode, instantiate another TransactionalMemcacheServiceImpl instance and 
	 * use it to create data entry.
	 */
	@Test
	public void testPutInTransactionMode2() {
		String memcacheNamespace2a = "testPutInTransactionMode2a";
		TransactionalMemcacheService transactionalMemcacheService2a 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2a);
			MemcacheService memcacheService2a 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2a);
		
		// Make entries in transaction mode -------------------------------------------------------
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
		
		String key1 = "testPutInTransactionMode2_Key1";
		String value1 = "testPutInTransactionMode2_Value1";
		transactionalMemcacheService2a.put( key1, value1);
		
		String memcacheNamespace2b = "testPutInTransactionMode2b";
		TransactionalMemcacheService transactionalMemcacheService2b 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace2b);
			MemcacheService memcacheService2b 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace2b);
			
		Assert.assertTrue( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		
		long key2 = System.currentTimeMillis();
		String[] value2 
		= new String[]{ "testPutInTransactionMode2_Valu2a", "testPutInTransactionMode2_Value2b"};
		transactionalMemcacheService2b.put( key2, value2);
			Assert.assertTrue( Arrays.equals( value2, (String[])transactionalMemcacheService2b.get( key2)));
			Assert.assertNull( memcacheService2b.get( key2));
			
			String keySetKey2b 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService2b).getKeySetKey();
			Set<Serializable> keysSet = (Set<Serializable>)memcacheService2b.get( keySetKey2b);
			Assert.assertFalse( keysSet.contains( key2));
			
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace2b);
			Assert.assertFalse( keysSet.contains( key2));
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
			Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		// ----------------------------------------------------------------------------------------
		
		// Verify entries added during transaction mode -------------------------------------------
		Assert.assertEquals( value1, memcacheService2a.get( key1));
			String keySetKey2a 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService2a).getKeySetKey();
			keysSet = (Set<Serializable>)memcacheService2a.get( keySetKey2a);
			Assert.assertTrue( keysSet.contains( key1));
			
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace2a);
			Assert.assertTrue( keysSet.contains( key1));
		
		Assert.assertTrue( Arrays.equals( value2, (String[])memcacheService2b.get( key2)));
			keysSet = (Set<Serializable>)memcacheService2b.get( keySetKey2b);
			Assert.assertTrue( keysSet.contains( key2));
			
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace2b);
			Assert.assertTrue( keysSet.contains( key2));
		// ----------------------------------------------------------------------------------------
			
		// Verify if mode is changed to non-transaction mode --------------------------------------
		String key1b = "testPutInTransactionMode2_Key1b";
		String value1b = "testPutInTransactionMode2_Value1b";
		transactionalMemcacheService2a.put( key1b, value1b);
			Assert.assertEquals( value1b, transactionalMemcacheService2a.get( key1b));
		
		String key2b = "testPutInTransactionMode2_Key2b";
		String value2b = "testPutInTransactionMode2_Value2b";
		transactionalMemcacheService2b.put( key2b, value2b);
			Assert.assertEquals( value2b, transactionalMemcacheService2b.get( key2b));
			
			keysSet = (Set<Serializable>)memcacheService2b.get( keySetKey2b);
			Assert.assertTrue( keysSet.contains( key2b));
			
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace2b);
			Assert.assertTrue( keysSet.contains( key2b));
		// ----------------------------------------------------------------------------------------
	}
	
	@Test
	public void testPutInTransactionMode1() {
		String memcacheNamespace1 = "testPutInTransactionMode1";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
			String keySetKey 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey();
		
		// Make entries in transaction mode -------------------------------------------------------
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Assert.assertTrue( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
			
			int key1 = (new Random()).nextInt( 100);
			Map<String, Serializable> value1 = new HashMap<String, Serializable>();
				long entryValue = System.currentTimeMillis();
				value1.put( 
						Long.toHexString( System.currentTimeMillis()), entryValue);
			transactionalMemcacheService1.put( key1, value1);
				Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
				Assert.assertNull( memcacheService1.get( key1));
				
				Set<Serializable> keysSet = (Set<Serializable>)memcacheService1.get( keySetKey);
				Assert.assertFalse( keysSet.contains( key1));
				
				keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace1);
				Assert.assertFalse( keysSet.contains( key1));
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
			Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		// ----------------------------------------------------------------------------------------
		
		// Verify entries added during transaction mode -------------------------------------------
		Assert.assertEquals( value1, memcacheService1.get( key1));
		
		keysSet = (Set<Serializable>)memcacheService1.get( keySetKey);
		Assert.assertTrue( keysSet.contains( key1));
		
		keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace1);
		Assert.assertTrue( keysSet.contains( key1));
		// ----------------------------------------------------------------------------------------
		
		// Verify if mode is really changed to non-transaction mode -------------------------------
		int key2 = (new Random()).nextInt( 100);
		Map<String, Serializable> value2 = new HashMap<String, Serializable>();
			long entryValue2 = System.currentTimeMillis();
			value2.put( 
					Long.toHexString( System.currentTimeMillis()), entryValue2);
		transactionalMemcacheService1.put( key2, value2);
			Assert.assertEquals( value2, memcacheService1.get( key2));
		// ----------------------------------------------------------------------------------------
	}
	
	/**
	 * Test using an array as the key to cache a value in transaction mode.
	 * This test needs to be skipped until the bug is fixed. 
	 */
	@Ignore
	@Test
	public void testArrayTypeKeyInTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		ArrayList<Object> strArrayList1 = new ArrayList<Object>();
			Random random = new Random();
			strArrayList1.add( Long.toHexString( random.nextLong()));
			strArrayList1.add( random.nextInt( 100)); 
			strArrayList1.add( random.nextLong()); 
			strArrayList1.add( new Date()); 
			strArrayList1.add( new TestDummy( UUID.randomUUID())); 
		Object[] key1 = strArrayList1.toArray( new Object[]{});
		transactionalMemcacheService1.put( key1, strArrayList1);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Assert.assertEquals( strArrayList1, transactionalMemcacheService1.get( key1));
			
			ArrayList<Object> strArrayList2 = new ArrayList<Object>();
				strArrayList2.add( new TestDummy( UUID.randomUUID())); 
				strArrayList2.add( new Date()); 
				strArrayList2.add( random.nextLong()); 
				strArrayList2.add( random.nextInt( 100)); 
				strArrayList2.add( Long.toHexString( random.nextLong()));
			Object[] key2 = strArrayList2.toArray( new Object[]{});
			transactionalMemcacheService1.put( key2, strArrayList2);
				Assert.assertEquals( strArrayList2, transactionalMemcacheService1.get( key2));
			
			ArrayList<Object> strArrayList3 = new ArrayList<Object>();
				strArrayList3.add( generateTestMapObj()); 
				strArrayList3.add( new TestDummy( UUID.randomUUID()));
			Object[] key3 = strArrayList3.toArray( new Object[]{});
			transactionalMemcacheService1.put( key3, strArrayList3);
			Assert.assertTrue( transactionalMemcacheService1.delete( key3));
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		
		Assert.assertEquals( strArrayList1, transactionalMemcacheService1.get( key1));
		Assert.assertEquals( strArrayList2, transactionalMemcacheService1.get( key2));
		Assert.assertTrue( transactionalMemcacheService1.get( key3) instanceof NoObject);
	}
	
	/**
	 * Test using an array as the key to cache a value in non-transaction mode.
	 * This test needs to be skipped until the bug is fixed. 
	 */
	@Ignore
	@Test
	public void testArrayTypeKeyInNonTransactionMode() {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 
			= MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		ArrayList<Object> strArrayList = new ArrayList<Object>();
			Random random = new Random();
			strArrayList.add( Long.toHexString( random.nextLong()));
			strArrayList.add( random.nextInt( 100)); 
			strArrayList.add( random.nextLong()); 
			strArrayList.add( new Date()); 
			strArrayList.add( new TestDummy( UUID.randomUUID())); 
		Object[] key1 = strArrayList.toArray( new Object[]{});
		
		transactionalMemcacheService1.put( key1, strArrayList);
			Assert.assertEquals( strArrayList, transactionalMemcacheService1.get( key1));
		Assert.assertTrue( transactionalMemcacheService1.delete( key1));
			String keySetKey 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey();
			Set<Serializable> keySet = (Set<Serializable>)memcacheService1.get( keySetKey);
			Assert.assertEquals( 0, keySet.size());
			Assert.assertEquals( 
					0, transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace1).size());
	}
	
	/**
	 * Test put method with SetPolicy.SET_ALWAYS in transaction mode.
	 * @throws Throwable
	 */
	@Test
	public void testPutWithSetPolicyInTransactionMode3() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1a = "value1a";
		transactionalMemcacheService1.put( key1, value1a);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			String value1b = "value1b";
			transactionalMemcacheService1.put( key1, value1b, null, SetPolicy.SET_ALWAYS);
				Assert.assertEquals( value1b, transactionalMemcacheService1.get( key1));
			
			String key2 = Long.toHexString( random.nextLong());
			String value2 = "value2";
			transactionalMemcacheService1.put( key2, value2, null, SetPolicy.SET_ALWAYS);
				Assert.assertEquals( value2, transactionalMemcacheService1.get( key2));
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		
		Assert.assertEquals( value1b, transactionalMemcacheService1.get( key1));
		Assert.assertEquals( value2, transactionalMemcacheService1.get( key2));
	}
	/**
	 * Test put method with SetPolicy.REPLACE_ONLY_IF_PRESENT in transaction mode. 
	 * SetPolicy.REPLACE_ONLY_IF_PRESENT is not supported for transaction mode.
	 * @throws Throwable
	 */
	@Test
	public void testPutWithSetPolicyInTransactionMode2() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1a = "value1a";
		transactionalMemcacheService1.put( key1, value1a);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			String value1b = "value1b";
			try {
				transactionalMemcacheService1.put( key1, value1b, null, SetPolicy.REPLACE_ONLY_IF_PRESENT);
				Assert.fail();
			}
			catch( TransactionalMemcacheServiceException exception) { // Expected exception
				Assert.assertTrue( exception.getCause() instanceof UnsupportedOperationException);
			}
		Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		Assert.assertEquals( value1a, transactionalMemcacheService1.get( key1));
	}
	/**
	 * Test put method with SetPolicy.ADD_ONLY_IF_NOT_PRESENT in transaction mode.
	 * SetPolicy.ADD_ONLY_IF_NOT_PRESENT is not supported for transaction mode.
	 * @throws Throwable
	 */
	@Test
	public void testPutWithSetPolicyInTransactionMode1() throws Throwable {
		// ADD_ONLY_IF_NOT_PRESENT and REPLACE_ONLY_IF_PRESENT of SetPolicy are not supported for transaction mode
		
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			Random random = new Random();
			String key1 = Long.toHexString( random.nextLong());
			String value1a = Long.toHexString( random.nextLong());
			try {
				transactionalMemcacheService1.put( key1, value1a, null, SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
				Assert.fail();
			}
			catch( TransactionalMemcacheServiceException exception) { // Expected exception
				Assert.assertTrue( exception.getCause() instanceof UnsupportedOperationException);
			}
		Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		Assert.assertTrue( transactionalMemcacheService1.get( key1) instanceof NoObject);
	}
	
	@Test
	public void testPutWithSetPolicyInNonTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1a = "value1a";
		transactionalMemcacheService1.put( key1, value1a, null, SetPolicy.REPLACE_ONLY_IF_PRESENT);
			Assert.assertTrue( transactionalMemcacheService1.get( key1) instanceof NoObject);
		transactionalMemcacheService1.put( key1, value1a, null, SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
			Assert.assertEquals( value1a, transactionalMemcacheService1.get( key1));
		String value1b = "value1b";
		transactionalMemcacheService1.put( key1, value1b, null, SetPolicy.REPLACE_ONLY_IF_PRESENT);
			Assert.assertEquals( value1b, transactionalMemcacheService1.get( key1));
		String value1c = "value1c";
		transactionalMemcacheService1.put( key1, value1c, null, SetPolicy.ADD_ONLY_IF_NOT_PRESENT);
			Assert.assertEquals( value1b, transactionalMemcacheService1.get( key1));
		transactionalMemcacheService1.put( key1, value1c, null, SetPolicy.SET_ALWAYS);
			Assert.assertEquals( value1c, transactionalMemcacheService1.get( key1));
		
		String key2 = Long.toHexString( random.nextLong());
		String value2 = "value2";
		transactionalMemcacheService1.put( key2, value2, null, SetPolicy.SET_ALWAYS);
			Assert.assertEquals( value2, transactionalMemcacheService1.get( key2));
		
	}
	
	@Test
	public void testPutWithExpirationInTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1 = Long.toHexString( random.nextLong());
		int expirationLength = 5000;
		Expiration expiration1 = Expiration.byDeltaMillis( expirationLength);
		transactionalMemcacheService1.put( key1, value1, expiration1);
		
		transactionalMemcacheServiceTransactionHelper.switchThreadToTransactionMode();
			// After the moment switching to transaction mode, Expiration object for each entry 
			// will be ignored during transaction mode
			long startTime = System.currentTimeMillis();
			do {
				try {
					Thread.sleep( expirationLength);
				}
				catch( InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
			} while( (new Long( System.currentTimeMillis() - startTime)).intValue() < expirationLength);
			
			Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
			Assert.assertNull( memcacheService1.get( key1));
			
			String key2 = Long.toHexString( random.nextLong());
			String value2 = Long.toHexString( random.nextLong());
			transactionalMemcacheService1.put( key2, value2, expiration1);
				startTime = System.currentTimeMillis();
				do {
					try {
						Thread.sleep( expirationLength);
					}
					catch( InterruptedException exception) {
						Thread.currentThread().interrupt();
					}
				} while( (new Long( System.currentTimeMillis() - startTime)).intValue() < expirationLength);
				
				Assert.assertEquals( value2, transactionalMemcacheService1.get( key2));
			
			String key3 = Long.toHexString( random.nextLong());
			String value3 = Long.toHexString( random.nextLong());
			transactionalMemcacheService1.put( key3, value3, expiration1);
		transactionalMemcacheServiceTransactionHelper.switchThreadToNonTransactionMode( true);
		// Once back to non-transaction mode, Expiration object takes its effect back.
		Assert.assertEquals( value3, transactionalMemcacheService1.get( key3));
		Assert.assertTrue( transactionalMemcacheService1.get( key1) instanceof NoObject);
		Assert.assertTrue( transactionalMemcacheService1.get( key2) instanceof NoObject);
		
		startTime = System.currentTimeMillis();
		do {
			try {
				Thread.sleep( expirationLength);
			}
			catch( InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		} while( (new Long( System.currentTimeMillis() - startTime)).intValue() < expirationLength);
		
		Assert.assertTrue( transactionalMemcacheService1.get( key3) instanceof NoObject);
	}
	
	@Ignore
	@Test
	public void testMemcacheServiceGetAllWithCopyOnWriteArraySetArgument() {
		String memcacheNamespace1 = getMethodName();
		MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1 = Long.toHexString( random.nextLong());
		Expiration expiration = Expiration.byDeltaMillis( 4000);
		memcacheService1.put( key1, value1, expiration);
			Assert.assertEquals( value1, memcacheService1.get( key1));
			
		CopyOnWriteArraySet<Serializable> copyOnWriteArraySet = new CopyOnWriteArraySet<Serializable>();
		copyOnWriteArraySet.add( key1);
		Map<Serializable, Object> map = memcacheService1.getAll( copyOnWriteArraySet);
			Assert.assertEquals( 1, map.size());
			Assert.assertEquals( value1, map.get( key1));
	}
	
	@Test
	public void testPutWithExpirationInNonTransactionMode() throws Throwable {
		String memcacheNamespace1 = getMethodName();
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Random random = new Random();
		String key1 = Long.toHexString( random.nextLong());
		String value1 = Long.toHexString( random.nextLong());
		int expirationLength = 5000;
		Expiration expiration = Expiration.byDeltaMillis( expirationLength);
		transactionalMemcacheService1.put( key1, value1, expiration);
			Assert.assertEquals( value1, transactionalMemcacheService1.get( key1));
			
			long startTime = System.currentTimeMillis();
			do {
				try {
					Thread.sleep( expirationLength);
				}
				catch( InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
			} while( (new Long( System.currentTimeMillis() - startTime)).intValue() < expirationLength);
		
			Assert.assertTrue( transactionalMemcacheService1.get( key1) instanceof NoObject);
	}
	
	/**
	 * Test overwrite data entry, what has been added by external code, in non-transaction mode.
	 */
	@Test
	public void testPutInNonTransactionMode3() {
		String memcacheNamespace1 = "testPutInNonTransactionMode3";
		MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
		
		String key1 = "key1";
		String value1a = "value1a";
		memcacheService1.put( key1, value1a);
		
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		Boolean value1b = new Boolean( true);
		transactionalMemcacheService1.put( key1, value1b);
			Assert.assertEquals( value1b, transactionalMemcacheService1.get( key1));
	}
	
	/**
	 * Test overwrite data entry in non-transaction mode.
	 */
	@Test
	public void testPutInNonTransactionMode2() {
		String memcacheNamespace1 = "testPutInNonTransactionMode2";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
		
		String key1 = "key1";
		String value1a = "value1a";
		transactionalMemcacheService1.put( key1, value1a);
			Assert.assertEquals( value1a, transactionalMemcacheService1.get( key1));
		
		Set<Serializable> value1b = new HashSet<Serializable>();
			value1b.add( "value1b1");
			value1b.add( "value1b2");
		transactionalMemcacheService1.put( key1, value1b);
			Assert.assertEquals( value1b, transactionalMemcacheService1.get( key1));
	}
	
	@Test
	public void testPutInNonTransactionMode() {
		String memcacheNamespace1 = "testPutInNonTransactionMode";
		TransactionalMemcacheService transactionalMemcacheService1 
		= transactionalMemcacheServiceFactory.getInstance( memcacheNamespace1);
			Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
		
		String key1 = "key1";
		String value1 = "value1";
		transactionalMemcacheService1.put( key1, value1);
			Assert.assertFalse( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread());
			
			MemcacheService memcacheService1 = MemcacheServiceFactory.getMemcacheService( memcacheNamespace1);
			Assert.assertEquals( value1, memcacheService1.get( key1));
			
			String keySetKey 
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService1).getKeySetKey();
			Set<Serializable> keysSet = (Set<Serializable>)memcacheService1.get( keySetKey);
			Assert.assertTrue( keysSet.contains( key1));
			
			TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
			= ((TransactionalMemcacheServiceImpl)transactionalMemcacheService1)
				.getTransactionalMemcacheServiceSyncHelper();
			keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace1);
			Assert.assertTrue( keysSet.contains( key1));
	}
}

// TODO test clearAll method effect on both transaction mode thread and non-transaction mode thread at once
// TODO test clear method invocation in transaction mode does not affect to other threads (no matter whether they are transaction mode or not) until switching to non-transaction mode
