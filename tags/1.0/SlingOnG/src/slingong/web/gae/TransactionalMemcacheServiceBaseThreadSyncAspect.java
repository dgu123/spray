package slingong.web.gae;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclareMixin;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class TransactionalMemcacheServiceBaseThreadSyncAspect {
	public interface KeySetKeyLockHandler {
		public int getQueueLengthOnKeySetKeyLock();
		public void lockKeySetKeyLock( final long lockAcquisitionPeriod);
		public void unlockKeySetKeyLock();
		public int getQueueLengthOnKeySetKeyLockingCounterLock();
		public void lockKeySetKeyLockingCounterLock( final long lockAcquisitionPeriod);
		public void unlockKeySetKeyLockingCounterLock();
		public int incrementKeySetKeyLockingCounter( final long lockAcquisitionPeriod);
		public void decrementKeySetKeyLockingCounter( final long lockAcquisitionPeriod);
		public void awaitZeroOfKeySetKeyLockingCounter( 
				final long maxWaitPeriod, final int maxNotificationTimes);
	}
	
	public static class KeySetKeyLockHandlerImpl implements KeySetKeyLockHandler {
		protected Logger logger = LoggerFactory.getLogger( this.getClass());
		
		TransactionalMemcacheServiceImpl transactionalMemcacheServiceImpl;
		
		public KeySetKeyLockHandlerImpl( TransactionalMemcacheServiceImpl transactionalMemcacheServiceImpl) {
			this.transactionalMemcacheServiceImpl = transactionalMemcacheServiceImpl;
		}
		
		// About transactionalMemcacheServiceImpl.keySetKeyLock -----------------------------------
		@Override
		public int getQueueLengthOnKeySetKeyLock() {
			return transactionalMemcacheServiceImpl.keySetKeyLock.getQueueLength();
		}

		@Override
		public void lockKeySetKeyLock( long lockAcquisitionPeriod) {
			try {
				boolean tryLockResult 
				= transactionalMemcacheServiceImpl
					.keySetKeyLock.tryLock( lockAcquisitionPeriod, TimeUnit.MILLISECONDS);
					if ( !tryLockResult) {
						int queueLength = getQueueLengthOnKeySetKeyLock();
						
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Thread (id: %1$d) could not acquire lock on keySetKeyLock member " 
										+ "field of %2$s instance (of what the target namespace is %3$s) " 
										+ "within the duration of %4$d [msec] in %5$s mode. The number of " 
										+ "other threads waiting the same lock is about %6$d.",
										Thread.currentThread().getId(),
										transactionalMemcacheServiceImpl.toString(),
										transactionalMemcacheServiceImpl.getNamespace(),
										lockAcquisitionPeriod, 
										(TransactionalMemcacheServiceBase
												.getTransactionalMemcacheServiceTransactionHelper()
												.isTransactionModeThread() 
													? "transaction" : "non-transaction"),
										queueLength
										)
								);
					}
					
					if ( logger.isDebugEnabled()) {
						logger.debug( 
								String.format(
										"Thread-%1$d acquired lock on keySetKeyLock member field " 
										+ "of %2$s instance.",
										Thread.currentThread().getId(),
										transactionalMemcacheServiceImpl.toString()
										)
								);
					}
			}
			catch( InterruptedException exception) {
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Thread (id: %1$d) is interrupted by other thread before obtaining lock on " 
								+ "keySetKeyLock member field of %2$s instance (of what the target namespace " 
								+ "is %3$s) in %4$s mode.",
								Thread.currentThread().getId(), 
								transactionalMemcacheServiceImpl.getClass().getName(), 
								transactionalMemcacheServiceImpl.getNamespace(), 
								(TransactionalMemcacheServiceBase
										.getTransactionalMemcacheServiceTransactionHelper()
										.isTransactionModeThread() ? "transaction" : "non-transaction")
								),
						exception
						);
			}
		}

		@Override
		public void unlockKeySetKeyLock() {
			transactionalMemcacheServiceImpl.keySetKeyLock.unlock();
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"Thread-%1$d released lock on keySetKeyLock member field " 
									+ "of %2$s instance.",
									Thread.currentThread().getId(),
									transactionalMemcacheServiceImpl.toString()
									)
							);
				}
		}
		// ----------------------------------------------------------------------------------------

		// About transactionalMemcacheServiceImpl.keySetKeyLockingCounterLock ---------------------
		@Override
		public int getQueueLengthOnKeySetKeyLockingCounterLock() {
			return transactionalMemcacheServiceImpl.keySetKeyLockingCounterLock.getQueueLength();
		}
		@Override
		public void lockKeySetKeyLockingCounterLock( long lockAcquisitionPeriod) {
			try {
				boolean tryLockResult 
				= transactionalMemcacheServiceImpl
					.keySetKeyLockingCounterLock.tryLock( lockAcquisitionPeriod, TimeUnit.MILLISECONDS);
					if ( !tryLockResult) {
						int queueLength = getQueueLengthOnKeySetKeyLockingCounterLock();
						
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Thread (id: %1$d) could not acquire lock on keySetKeyLockingCounterLock member " 
										+ "field of %2$s instance (of what the target namespace is %3$s) " 
										+ "within the duration of %4$d [msec] in %5$s mode. The number of " 
										+ "other threads waiting the same lock is about %6$d.",
										Thread.currentThread().getId(),
										transactionalMemcacheServiceImpl.getClass().getName(),
										transactionalMemcacheServiceImpl.getNamespace(),
										lockAcquisitionPeriod, 
										(TransactionalMemcacheServiceBase
												.getTransactionalMemcacheServiceTransactionHelper()
												.isTransactionModeThread() 
													? "transaction" : "non-transaction"),
										queueLength
										)
								);
					}
					
					if ( logger.isDebugEnabled()) {
						logger.debug( 
								String.format(
										"Thread-%1$d acquired lock on keySetKeyLockingCounterLock member " 
										+ "field of %2$s instance.",
										Thread.currentThread().getId(),
										transactionalMemcacheServiceImpl.toString()
										)
								);
					}
			}
			catch( InterruptedException exception) {
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Thread (id: %1$d) is interrupted by other thread before obtaining lock on " 
								+ "keySetKeyLockingCounterLock member field of %2$s instance (of what " 
								+ "the target namespace is %3$s) in %4$s mode.",
								Thread.currentThread().getId(), 
								transactionalMemcacheServiceImpl.getClass().getName(), 
								transactionalMemcacheServiceImpl.getNamespace(), 
								(TransactionalMemcacheServiceBase
										.getTransactionalMemcacheServiceTransactionHelper()
										.isTransactionModeThread() ? "transaction" : "non-transaction")
								),
						exception
						);
			}
		}
		
		@Override
		public void unlockKeySetKeyLockingCounterLock() {
			transactionalMemcacheServiceImpl.keySetKeyLockingCounterLock.unlock();
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"Thread-%1$d released lock on keySetKeyLockingCounterLock member " 
									+ "field of %2$s instance.",
									Thread.currentThread().getId(),
									transactionalMemcacheServiceImpl.toString()
									)
							);
				}
		}
		// ----------------------------------------------------------------------------------------
		
		@Override
		public int incrementKeySetKeyLockingCounter( long lockAcquisitionPeriod) {
			lockKeySetKeyLock( lockAcquisitionPeriod);
			try {
				return ++transactionalMemcacheServiceImpl.keySetKeyLockingCounter;
			}
			finally {
				unlockKeySetKeyLock();
			}
		}
		@Override
		public void decrementKeySetKeyLockingCounter( long lockAcquisitionPeriod) {
			int keySetKeyLockingCounter = -1;
			try {
				lockKeySetKeyLockingCounterLock( lockAcquisitionPeriod);
			}
			finally {
//TODO create aspect to notify change of value of keySetKeyLockingCounter
				keySetKeyLockingCounter = --transactionalMemcacheServiceImpl.keySetKeyLockingCounter;
			}
			
			try {
				if ( keySetKeyLockingCounter == 0) {
					transactionalMemcacheServiceImpl.keySetKeyLockingCounterLockCondition.signalAll();
						if ( logger.isDebugEnabled()) {
							logger.debug( 
									String.format(
											"Thread-%1$d signaled keySetKeyLockingCounterLockCondition " 
											+ "condition on %2$s instance.",
											Thread.currentThread().getId(),
											transactionalMemcacheServiceImpl.toString()
											)
									);
						}
				}
				else if ( keySetKeyLockingCounter < 0) {
					transactionalMemcacheServiceImpl.keySetKeyLockingCounter = 0;
					
					String messageOnKeySetKeyLockConditionCondition = null;
						try {
							int waitQueueLength 
							= transactionalMemcacheServiceImpl.keySetKeyLockingCounterLock.getWaitQueueLength(
									transactionalMemcacheServiceImpl.keySetKeyLockingCounterLockCondition
									);
							
								if ( waitQueueLength > 0) {
									messageOnKeySetKeyLockConditionCondition 
									= String.format(
											"Other %1$d %2$s are waiting keySetKeyLockingCounterLockCondition condition.", 
											waitQueueLength, 
											(( waitQueueLength == 1) ? "thread" : "threads")
											);
								}
						}
						catch( Throwable throwable) {
							// Do nothing since this try block is just for constructing additional informative message
						}
						
					throw new TransactionalMemcacheServiceException( 
							new IllegalMonitorStateException(
									String.format(
											"keySetKeyLockingCounter member field, what keeps track of " 
											+ "calls to methods of TransactionalMemcacheService interface " 
											+ "(except clearAll method, switchToTransactionMode method, " 
											+ "switchToNonTransactionMode method) on the %1$s instance (of " 
											+ "what the target namespace is %2$s) became less than zero. " 
											+ "(Thread of what id is %3$d encoutnered this in %4$s mode.) " 
											+ "This must never happen. Since having resetted it to zero, " 
											+ "this same warning may appear repeatedly for awhile as " 
											+ "recovery process until it becomes actual intial state. %5$s",
											transactionalMemcacheServiceImpl.toString(),
											transactionalMemcacheServiceImpl.getNamespace(),
											Thread.currentThread().getId(),
											(TransactionalMemcacheServiceBase
													.getTransactionalMemcacheServiceTransactionHelper()
													.isTransactionModeThread() ? "transaction" : "non-transaction"),
											((messageOnKeySetKeyLockConditionCondition == null) 
													? "" : messageOnKeySetKeyLockConditionCondition)
											)
									)
							);
				}
			}
			finally {
				unlockKeySetKeyLockingCounterLock();
			}
		}
		
		@Override
		public void awaitZeroOfKeySetKeyLockingCounter( long maxWaitPeriod, int maxNotificationTimes) {
			
			int receivedNotificationCount = 0;
			
			while( transactionalMemcacheServiceImpl.keySetKeyLockingCounter > 0) {
				
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"Thread-%1$d is waiting keySetKeyLockingCounterLockCondition " 
									+ "condition on %2$s instance.",
									Thread.currentThread().getId(),
									transactionalMemcacheServiceImpl.toString()
									)
							);
				}
				
				try {
					boolean awaitResult 
					= transactionalMemcacheServiceImpl
						.keySetKeyLockingCounterLockCondition.await(
								maxWaitPeriod, TimeUnit.MILLISECONDS);
						if ( !awaitResult) {
							int queueLength = getQueueLengthOnKeySetKeyLock();
							String additionalMessage 
							= String.format(
									"%n%1$cThe number of other threads waiting for this thread to release " 
									+ "lock on keySetKeyLock member field of %2$s instance: %3$d",
									'\t',
									transactionalMemcacheServiceImpl.toString(),
									queueLength
									);
									
							queueLength = getQueueLengthOnKeySetKeyLockingCounterLock();
							additionalMessage 
							= additionalMessage + String.format(
									"%n%1$cThe number of other threads waiting for this thread to release " 
									+ "lock on keySetKeyLockingCounterLock member field of %2$s instance: %3$d",
									'\t',
									transactionalMemcacheServiceImpl.toString(),
									queueLength
									);
							
							try {
								int waitQueueLength 
								= transactionalMemcacheServiceImpl
									.keySetKeyLockingCounterLock.getWaitQueueLength( 
											transactionalMemcacheServiceImpl.keySetKeyLockingCounterLockCondition
											);
								additionalMessage 
								= additionalMessage + String.format(
										"%n%1$cThe number of other threads waiting for the same " 
										+ "notifcation of keySetKeyLockingCounter member " 
										+ "field becoming zero on %2$s instance: %3$d", 
										'\t',
										transactionalMemcacheServiceImpl.toString(),
										waitQueueLength
										);
							}
							catch( Throwable throwable) { // Do nothing since it's just for additional info 
							}
							throw new TransactionalMemcacheServiceException(
									String.format(
											"Thread (id: %1$d) did not meet the condtion of " 
											+ "keySetKeyLockingCounter member field becoming zero " 
											+ "on %2$s instance (of what the targeted namespace is %3$s) " 
											+ "for %4$d [msec] in %5$s mode. %nAdditional info: " 
											+ "%n%6$cThe current value of keySetKeyLockingCounter member " 
											+ "field: %7$d. %8$s",
											Thread.currentThread().getId(),
											transactionalMemcacheServiceImpl.toString(),
											transactionalMemcacheServiceImpl.getNamespace(),
											maxWaitPeriod,
											(TransactionalMemcacheServiceBase
													.getTransactionalMemcacheServiceTransactionHelper()
													.isTransactionModeThread() ? "transaction" : "non-transaction"),
											'\t',
											transactionalMemcacheServiceImpl.keySetKeyLockingCounter,
											additionalMessage
											)
									);
						}
						
						if ( logger.isDebugEnabled()) {
							logger.debug( 
									String.format(
											"Thread-%1$d woke from waiting keySetKeyLockingCounterLockCondition " 
											+ "condition on %2$s instance.",
											Thread.currentThread().getId(),
											transactionalMemcacheServiceImpl.toString()
											)
									);
						}
				}
				catch( InterruptedException exception) {
					throw new TransactionalMemcacheServiceException(
							String.format(
									"Thread (id: %1$d) was interrupted in %2$s mode before confirming " 
									+ "the termination of already-running methods of " 
									+ "TransactionalMemcacheService interface on %3$s instance (of what " 
									+ "the target namespace is %4$s) in other threads.", 
									Thread.currentThread().getId(), 
									(TransactionalMemcacheServiceBase
											.getTransactionalMemcacheServiceTransactionHelper()
											.isTransactionModeThread() ? "transaction" : "non-transaction"),
									transactionalMemcacheServiceImpl.toString(),
									transactionalMemcacheServiceImpl.getNamespace()									
									),
							exception
							);
				}
				
				if ( ++receivedNotificationCount > maxNotificationTimes) {
					transactionalMemcacheServiceImpl.keySetKeyLockingCounter = 0;
					
					throw new TransactionalMemcacheServiceException(
							String.format(
									"In %1$s mode thread of what ID is %2$d, encountered the situation " 
									+ "that the lock on keySetKeyLockingCounterLock member field has been released " 
									+ "the specified %3$d times before meeting the condition of " 
									+ "keySetKeyLockingCounter member field becoming zero on %4$s instance " 
									+ "(of what the target namespace is %5$s.) Thereby initialized " 
									+ "keySetKeyLockingCounter member field to zero. As recovery process " 
									+ "from now on awhile because of that, it is expected that some other " 
									+ "threads may hit TransactionalMemcacheServiceException exception " 
									+ "claiming that value of keySetKeyLockingCounter member field became " 
									+ "less than zero.",
									(TransactionalMemcacheServiceBase
											.getTransactionalMemcacheServiceTransactionHelper()
											.isTransactionModeThread() ? "transaction" : "non-transaction"),
									Thread.currentThread().getId(), 
									maxNotificationTimes,
									transactionalMemcacheServiceImpl.toString(),
									transactionalMemcacheServiceImpl.getNamespace()									
									)
							);
				}
			} // while
		}
	}
	
	@DeclareMixin( value="slingong.web.gae.TransactionalMemcacheServiceImpl")
	public static KeySetKeyLockHandler addKeySetKeyLockHandlerInterface( Object instance) {
		return new KeySetKeyLockHandlerImpl( (TransactionalMemcacheServiceImpl)instance);
	}
	
	@Pointcut( 
			value="execution( * slingong.web.gae.TransactionalMemcacheServiceBase+.getNamespace()) " 
					+ "|| execution( * slingong.web.gae.TransactionalMemcacheServiceBase+.getErrorHandler()) " 
					+ "|| execution( * slingong.web.gae.TransactionalMemcacheServiceBase+.setErrorHandler(..))"
					+ "|| execution( * slingong.web.gae.TransactionalMemcacheServiceBase+.getStatistics()) " 
			)
	protected static void pointcutAtConcurrencyAllowedMethodsExecution() {}
	
	@Before( 
			value="slingong.web.gae.TransactionalMemcacheServiceAspect" 
					+ ".pointcutAtTransactionalMemcacheServicePublicMethodExecution() " 
					+ "&& !pointcutAtConcurrencyAllowedMethodsExecution() " 
					+ "&& this( keySetKeyLockHandler)"
			)
	public void beforeAdvisedTransactionalMemcacheServicePublicMethodExecution(
			JoinPoint joinPoint, KeySetKeyLockHandler keySetKeyLockHandler) {
		
		long maxLockAcquisitionPeriod 
		= ((TransactionalMemcacheServiceImpl)joinPoint.getTarget())
			.getMaxLockAcquisitionDuration();
		
		
		keySetKeyLockHandler.incrementKeySetKeyLockingCounter( maxLockAcquisitionPeriod);
	}
	
	@After(
			value="slingong.web.gae.TransactionalMemcacheServiceAspect" 
					+ ".pointcutAtTransactionalMemcacheServicePublicMethodExecution() " 
					+ "&& !pointcutAtConcurrencyAllowedMethodsExecution() " 
					+ "&& this( keySetKeyLockHandler)"
			)
	public void afterAdvisedTransactionalMemcacheServicePublicMethodExecution(
			JoinPoint joinPoint, KeySetKeyLockHandler keySetKeyLockHandler) {
		
		long maxLockAcquisitionPeriod 
		= ((TransactionalMemcacheServiceImpl)joinPoint.getTarget())
			.getMaxLockAcquisitionDuration();
		
		keySetKeyLockHandler.decrementKeySetKeyLockingCounter( maxLockAcquisitionPeriod);
	}
	
/*	@Around( 
			value="slingong.web.gae.TransactionalMemcacheServiceAspect" 
					+ ".pointcutAtTransactionalMemcacheServicePublicMethodExecution() " 
					+ "&& !pointcutAtConcurrencyAllowedMethodsExecution() " 
					+ "&& this( keySetKeyLockHandler)"
			)
	public Object aroundAdvisedTransactionalMemcacheServicePublicMethodExecution( 
			ProceedingJoinPoint proceedingJoinPoint, KeySetKeyLockHandler keySetKeyLockHandler) {
		
		long maxLockAcquisitionPeriod 
		= ((TransactionalMemcacheServiceImpl)proceedingJoinPoint.getTarget())
			.getMaxLockAcquisitionDuration();
		
		
		keySetKeyLockHandler.incrementKeySetKeyLockingCounter( maxLockAcquisitionPeriod);
		try {
			if ( proceedingJoinPoint.getArgs().length < 1) {
				return proceedingJoinPoint.proceed();
			}
			else {
				return proceedingJoinPoint.proceed( proceedingJoinPoint.getArgs());
			}
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
			keySetKeyLockHandler.decrementKeySetKeyLockingCounter( maxLockAcquisitionPeriod);
		}
	}
*/
	
	@Before( value="slingong.web.gae.TransactionalMemcacheServiceAspect" 
					+ ".pointcutAtSetKeySetKeyMethodExecution() " 
					+ "&& this( keySetKeyLockHandler)"
			)
	public void beforeAdvisedSetKeySetKeyMethodExecution( 
			JoinPoint joinPoint, KeySetKeyLockHandler keySetKeyLockHandler) {
		
		long maxLockAcquisitionPeriod 
		= ((TransactionalMemcacheServiceImpl)joinPoint.getTarget())
			.getMaxLockAcquisitionDuration();
		long maxWaitPeriod
		= ((TransactionalMemcacheServiceImpl)joinPoint.getTarget())
			.getMaxWaitDuration();
		int maxNotificationTimes 
		= ((TransactionalMemcacheServiceImpl)joinPoint.getTarget())
			.getMaxNotificationCount();
		
		keySetKeyLockHandler.lockKeySetKeyLock( maxLockAcquisitionPeriod);
			/* Locked out further execution of methods of TransactionalMemcacheService interface on  
			 * particular TransactionalMemcacheServiceImpl instance (what is pointed with 
			 * proceedingJoinPoint argument) by locking its keySetKeyLock member field.
			 */
		
		try {
			// Waits for termination of execution of TransactionalMemcacheService interface methods 
			// having already been running on particular TransactionalMemcacheServiceImpl instance 
			// (what is pointed with proceedingJoinPoint argument) before locking keySetKeyLock member 
			// field ------------------------------------------------------------------------------
			keySetKeyLockHandler.lockKeySetKeyLockingCounterLock( maxLockAcquisitionPeriod);
			try {
				
				keySetKeyLockHandler.awaitZeroOfKeySetKeyLockingCounter( 
						maxWaitPeriod, maxNotificationTimes);
			}
			finally {
				keySetKeyLockHandler.unlockKeySetKeyLockingCounterLock();
			}
			// ------------------------------------------------------------------------------------
		}
		catch( Throwable throwable) {
			keySetKeyLockHandler.unlockKeySetKeyLock();
			
			if ( TransactionalMemcacheServiceException.class.equals( throwable.getClass())) {
				throw (TransactionalMemcacheServiceException)throwable;
			}
			else {
				throw new TransactionalMemcacheServiceException( throwable);
			}
		}
	}
	
	@After( value="slingong.web.gae.TransactionalMemcacheServiceAspect" 
					+ ".pointcutAtSetKeySetKeyMethodExecution() " 
					+ "&& this( keySetKeyLockHandler)"
	)
	public void afterAdvisedSetKeySetKeyMethodExecution( 
			JoinPoint joinPoint, KeySetKeyLockHandler keySetKeyLockHandler) {

//TODO Confirm when it will be executed even exception occurs during setKeySetKey method execution.
		keySetKeyLockHandler.unlockKeySetKeyLock();
	}
	
	
/*	
	@Around( value="slingong.web.gae.TransactionalMemcacheServiceAspect" 
					+ ".pointcutAtSetKeySetKeyMethodExecution() " 
					+ "&& this( keySetKeyLockHandler)"
			) 
	public Object aroundAdvisedSetKeySetKeyMethodExecution( 
			ProceedingJoinPoint proceedingJoinPoint, KeySetKeyLockHandler keySetKeyLockHandler) {
		
		long maxLockAcquisitionPeriod 
		= ((TransactionalMemcacheServiceImpl)proceedingJoinPoint.getTarget())
			.getMaxLockAcquisitionDuration();
		long maxWaitPeriod
		= ((TransactionalMemcacheServiceImpl)proceedingJoinPoint.getTarget())
			.getMaxWaitDuration();
		int maxNotificationTimes 
		= ((TransactionalMemcacheServiceImpl)proceedingJoinPoint.getTarget())
			.getMaxNotificationCount();
		
		
		keySetKeyLockHandler.lockKeySetKeyLock( maxLockAcquisitionPeriod);
			// Locked out further execution of methods of TransactionalMemcacheService interface on  
			// particular TransactionalMemcacheServiceImpl instance (what is pointed with 
			// proceedingJoinPoint argument) by locking its keySetKeyLock member field.
		try {
			// Waits for termination of execution of TransactionalMemcacheService interface methods 
			// having already been running on particular TransactionalMemcacheServiceImpl instance 
			// (what is pointed with proceedingJoinPoint argument) before locking keySetKeyLock member 
			// field ------------------------------------------------------------------------------
			keySetKeyLockHandler.lockKeySetKeyLockingCounterLock( maxLockAcquisitionPeriod);
			try {
				
				keySetKeyLockHandler.awaitZeroOfKeySetKeyLockingCounter( 
						maxWaitPeriod, maxNotificationTimes);
			}
			finally {
				keySetKeyLockHandler.unlockKeySetKeyLockingCounterLock();
			}
			// ------------------------------------------------------------------------------------
			
			// Invocation of actual ---------------------------------------------------------------
			try {
				if ( proceedingJoinPoint.getArgs().length < 1) {
					return proceedingJoinPoint.proceed();
				}
				else {
					return proceedingJoinPoint.proceed( proceedingJoinPoint.getArgs());
				}
			}
			catch( Throwable throwable) {
				if ( TransactionalMemcacheServiceException.class.equals( throwable.getClass())) {
					throw (TransactionalMemcacheServiceException)throwable;
				}
				else {
					throw new TransactionalMemcacheServiceException( throwable);
				}
			}
			// ------------------------------------------------------------------------------------
		}
		finally {
			keySetKeyLockHandler.unlockKeySetKeyLock();
		}
	}
*/	
}
