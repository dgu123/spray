package slingong.web.gae;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.ConstructorSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newmainsoftech.aspectjutil.eventmanager.label.EventTrigger;

@Aspect
public class TransactionalMemcacheServiceFactory {
	protected static Logger logger = LoggerFactory.getLogger( TransactionalMemcacheServiceFactory.class);
	
	protected static TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper 
	= new TransactionalMemcacheServiceTransactionHelperImpl( 
			TransactionalMemcacheServiceCommonConstant.copier);
	
	protected static Map<String, TransactionalMemcacheServiceImpl> instanceMap 
	= Collections.synchronizedMap( new WeakHashMap<String, TransactionalMemcacheServiceImpl>());
	
		public static long maxLockAcquisitionDuration 
		= TransactionalMemcacheServiceCommonConstant.DefaultMaxLockAcquisitionDuration;
			public static long getMaxLockAcquisitionDuration() {
				return maxLockAcquisitionDuration;
			}
			public static void setMaxLockAcquisitionDuration(long maxLockAcquisitionDuration) {
				
				TransactionalMemcacheServiceFactory.maxLockAcquisitionDuration = maxLockAcquisitionDuration;
			}
			
		protected static ReentrantLock instanceMapLock = new ReentrantLock();
		protected static void acquireInstanceMapLock( String memcacheNamespace) {
			long lockAcquisitionPeriod 
			= TransactionalMemcacheServiceFactory.getMaxLockAcquisitionDuration();
			
			try {
				boolean tryLockResult 
				= TransactionalMemcacheServiceFactory
					.instanceMapLock.tryLock( getMaxLockAcquisitionDuration(), TimeUnit.MILLISECONDS);
				
					if ( !tryLockResult) {
						int queueLength 
						= TransactionalMemcacheServiceFactory.instanceMapLock.getQueueLength();
						
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Thread-%1$d could not acquire lock on instanceMapLock member field " 
										+ "within the duration of %2$d [msec] in %3$s mode in order to " 
										+ "construct TransactionalMemcacheServiceImpl instance for memcache " 
										+ "of what namespace is %4$s. The number of other threads waiting " 
										+ "the same lock is about %5$d.",
										Thread.currentThread().getId(), 
										lockAcquisitionPeriod,
										(transactionalMemcacheServiceTransactionHelper.isTransactionModeThread() 
												? "transction" : "non-transaction"),
										memcacheNamespace,
										queueLength
										)
								);
					}
					
					if ( TransactionalMemcacheServiceFactory.logger.isDebugEnabled()) {
						TransactionalMemcacheServiceFactory.logger.debug(
								String.format(
										"Acquired lock on instanceMapLock member field (for %1$s namespace) " 
										+ "in thread-%2$d.",
										memcacheNamespace, 
										Thread.currentThread().getId()
										)
								);
					}
			}
			catch( InterruptedException exception) {
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Thread-%1$d was interrupted before acquiring lock on instanceMapLock " 
								+ "ReentrantLock member field in %2$s mode in order to construct " 
								+ "TransactionalMemcacheServiceImpl instance for memcache of what " 
								+ "namespace is %3$s namespace.",
								Thread.currentThread().getId(), 
								(transactionalMemcacheServiceTransactionHelper.isTransactionModeThread() 
										? "transction" : "non-transaction"),
								memcacheNamespace
								),
						exception
						);
			}
			
		}
		
		protected static void releaseInstanceMapLock( String memcacheNamespace) {
			TransactionalMemcacheServiceFactory.instanceMapLock.unlock();
			
				if ( TransactionalMemcacheServiceFactory.logger.isDebugEnabled()) {
					TransactionalMemcacheServiceFactory.logger.debug(
							String.format(
									"Thread-%1$d released lock on instanceMapLock member field (for " 
									+ "%2$s namespace).",
									Thread.currentThread().getId(), 
									memcacheNamespace
									)
							);
				}
		}

	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	public TransactionalMemcacheService getInstance( 
			final String memcacheNamespace, 
			final String keySetKey, 
			final TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper, 
			final TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
			) 
	{
		TransactionalMemcacheServiceImpl instance = null;
		
		TransactionalMemcacheServiceFactory.acquireInstanceMapLock( memcacheNamespace);
		try {
			instance = TransactionalMemcacheServiceFactory.instanceMap.get( memcacheNamespace);
			if ( instance == null) {
				instance 
				= new TransactionalMemcacheServiceImpl( 
						memcacheNamespace, 
						keySetKey, 
						transactionalMemcacheServiceTransactionHelper, 
						transactionalMemcacheServiceSyncHelper
						);
				
					if ( TransactionalMemcacheServiceFactory.logger.isDebugEnabled()) {
						TransactionalMemcacheServiceFactory.logger.debug(
								String.format(
										"Created new TransactionalMemcacheServiceImpl instance (%1$s) " 
										+ "for %2$s namespace",
										instance.toString(),
										memcacheNamespace
										)
								);
					}
			}
			else {
				if ( TransactionalMemcacheServiceFactory.logger.isDebugEnabled()) {
					TransactionalMemcacheServiceFactory.logger.debug(
							String.format(
									"Found pre-existing TransactionalMemcacheServiceImpl instance (%1$s) " 
									+ "for %2$s namespace in TransactionalMemcacheServiceFactory.instanceMap " 
									+ "member field.",
									instance.toString(),
									memcacheNamespace
									)
							);
				}
				
				
				if ( 
						( transactionalMemcacheServiceTransactionHelper == null) 
						&& ( transactionalMemcacheServiceSyncHelper == null)
						) 
				{
					if (
							( keySetKey != null)
							&& ( !instance.getKeySetKey().equals( keySetKey)) 
							) 
					{
						instance.setKeySetKey( keySetKey);
					}
				}
				else {
					instance.setMembers(
							keySetKey, 
							transactionalMemcacheServiceTransactionHelper, 
							transactionalMemcacheServiceSyncHelper
							);
				}
			}
		}
		finally {
			TransactionalMemcacheServiceFactory.releaseInstanceMapLock( memcacheNamespace);
		}
		
		return instance;
	}
	
	public TransactionalMemcacheService getInstance( final String memcacheNamespace) {
		return getInstance( 
				memcacheNamespace, TransactionalMemcacheServiceImpl.DefaultKeySetKey, null, null);
	} 
	
	@Pointcut( 
			value="execution( slingong.web.gae.TransactionalMemcacheServiceImpl.new( java.lang.String, ..))")
	protected static void pointcutAtTransactionalMemcacheServiceImplConstructors() {}
	
	@Before( value="pointcutAtTransactionalMemcacheServiceImplConstructors()")
	public void beforeTransactionalMemcacheServiceImplInitialization( JoinPoint joinPoint) {
		// Validate caller of TransactionalMemcacheServiceImpl constructor ------------------------
		StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[3];
			/* element 0: StackTraceElement for calling Thread.currentThread().getStackTrace() method
			 * element 1: StackTraceElement for this aroundAdvicedTransactionalMemcacheServiceImplInitialization around-advise
			 * element 2: StackTraceElement for TransactionalMemcacheServiceImpl.<init> constructor
			 * element 3: StackTraceElement for calling getInstance method
			 */
			if ( 
					!TransactionalMemcacheServiceFactory.class.getName().equals( 
							stackTraceElement.getClassName()
							)
					|| !"getInstance".equals( stackTraceElement.getMethodName())
					)
			{
				throw new TransactionalMemcacheServiceException( 
						new UnsupportedOperationException(
								"TransactionalMemcacheServiceImpl instance needs to be obtained via " 
								+ "TransactionalMemcacheServiceFactory.getInstance factory method. " 
								+ "Otherwise, you may develop your own implementation by subclassing " 
								+ "TransactionalMemcacheServiceBase class. " 
								)
						);
			}
		// ----------------------------------------------------------------------------------------
	}
	
	/**
	 * AspectJ's pointcut for the one of TransactionalMemcacheServiceImpl constructors what takes 
	 * 4 arguments. Other constructors are ignored since those constructors will end up calling 
	 * the one targeted.
	 */
	@Pointcut( 
			value="execution( slingong.web.gae.TransactionalMemcacheServiceImpl" 
					+ ".new( " 
						+ "java.lang.String, "
						+ "java.lang.String, " 
						+ "slingong.web.gae.TransactionalMemcacheServiceTransactionHelper, " 
						+ "slingong.web.gae.TransactionalMemcacheServiceSyncHelper))"

			) 
	protected static void pointcutAtTransactionalMemcacheServiceImplConstructorWithMostArgs() {}
	
	@Around( value="pointcutAtTransactionalMemcacheServiceImplConstructorWithMostArgs()") 
	public Object aroundAdvicedTransactionalMemcacheServiceImplInitialization( 
			ProceedingJoinPoint proceedingJoinPoint) {
		
		// Obtain memcacheNamespace argument value ------------------------------------------------
		ConstructorSignature constructorSignature = (ConstructorSignature)proceedingJoinPoint.getSignature();
		int memcacheNamespaceArgIndex = 0;
		if ( 
				!"memcacheNamespace".equals( constructorSignature.getParameterNames()[ memcacheNamespaceArgIndex])
				|| !String.class.equals( constructorSignature.getParameterTypes()[ memcacheNamespaceArgIndex])
				) 
		{
			throw new TransactionalMemcacheServiceException( 
					new IllegalMonitorStateException(
							String.format(
									"aroundAdvicedTransactionalMemcacheServiceImplInitialization " 
									+ "around-advise method needs to be updated regarding arguments of " 
									+ "TransactionalMemcacheServiceImpl constructor: %1$s. " 
									+ "%nIt expected that the first argument is the String type argument " 
									+ "named as memcacheNamespace, however, the actual first argument is " 
									+ "the %2$s type argument named as %3$s.", 
									constructorSignature.getConstructor().toString(),
									constructorSignature.getParameterNames()[ memcacheNamespaceArgIndex], 
									constructorSignature.getParameterTypes()[ memcacheNamespaceArgIndex].toString()
									)
							)
					);
		}
		
		String memcacheNamespace = (String)proceedingJoinPoint.getArgs()[ memcacheNamespaceArgIndex];
		// ----------------------------------------------------------------------------------------
		
		Object instance = null;
		
		TransactionalMemcacheServiceFactory.acquireInstanceMapLock( memcacheNamespace);
		try {
			instance = proceedingJoinPoint.proceed( proceedingJoinPoint.getArgs());
			TransactionalMemcacheServiceFactory.instanceMap.put( 
					memcacheNamespace, (TransactionalMemcacheServiceImpl)proceedingJoinPoint.getThis());
			
				if ( TransactionalMemcacheServiceFactory.logger.isDebugEnabled()) {
					TransactionalMemcacheServiceFactory.logger.debug(
							String.format(
									"Instantiated %1$s for %2$s namespace, and saved it to " 
									+ "TransactionalMemcacheServiceFactory.instanceMap member field.",
									proceedingJoinPoint.getThis().toString(),
									memcacheNamespace
									)
							);
				}
			return instance;
		}
		catch( Throwable throwable) {
			if ( TransactionalMemcacheServiceException.class.equals( throwable.getClass())) {
				throw (TransactionalMemcacheServiceException)throwable;
			}
			else {
				throw new TransactionalMemcacheServiceException( throwable);
			}
		}
		finally {
			TransactionalMemcacheServiceFactory.releaseInstanceMapLock( memcacheNamespace);
		}
	}

}
