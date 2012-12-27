package slingong.web.gae;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newmainsoftech.aspectjutil.eventmanager.label.EventTrigger;

/**
 * Aspect to take care of synchronization among TransactionalMemcacheServiceImpl instances and 
 * threads over method of TransactionalMemcacheService interface and a few other methods. 
 * <br />
 * Do not lock on this class type or any static member field in this class unless you really know its 
 * consequence. 
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@Aspect
@DeclarePrecedence( 
		value="slingong.web.gae.TransactionalMemcacheServiceAspect," 
				+ "slingong.web.gae.TransactionalMemcacheServiceBaseThreadSyncAspect"
		)
public class TransactionalMemcacheServiceAspect {
	protected static Logger logger = LoggerFactory.getLogger( TransactionalMemcacheServiceAspect.class);
	
	protected static TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper
	= TransactionalMemcacheServiceCommonConstant.transactionalMemcacheServiceTransactionHelper;
		public static TransactionalMemcacheServiceTransactionHelper getTransactionalMemcacheServiceTransactionHelper() {
			return transactionalMemcacheServiceTransactionHelper;
		}
		public static void setTransactionalMemcacheServiceTransactionHelper(
				TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper) {
			TransactionalMemcacheServiceAspect.transactionalMemcacheServiceTransactionHelper 
			= transactionalMemcacheServiceTransactionHelper;
		}


	protected static long maxLockAcquisitionDuration 
	= TransactionalMemcacheServiceCommonConstant.DefaultMaxLockAcquisitionDuration;
		public static long getMaxLockAcquisitionDuration() {
			return TransactionalMemcacheServiceAspect.maxLockAcquisitionDuration;
		}
		public static void setMaxLockAcquisitionDuration( final long maxLockAcquisitionDuration) {
			if ( maxLockAcquisitionDuration < 1L) {
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"maxLockAcquisitionDuration long member field (current value: %1$d [msec]) " 
									+ "will be set to %2$d [msec] what is not recommended value.",
									TransactionalMemcacheServiceAspect.getMaxLockAcquisitionDuration(), 
									maxLockAcquisitionDuration
									)
							);
				}
			}
			TransactionalMemcacheServiceAspect.maxLockAcquisitionDuration = maxLockAcquisitionDuration;
		}
		
	/**
	 * waiting period for notification via memcacheServiceMethodCallCountCondition condition object.
	 */
	protected static long maxWaitDuration 
	= TransactionalMemcacheServiceCommonConstant.DefaultMaxWaitDuration;
		public static long getMaxWaitDuration() {
			return TransactionalMemcacheServiceAspect.maxWaitDuration;
		}
		public static void setMaxWaitDuration( final long maxWaitDuration) {
			if ( maxWaitDuration < 1L) {
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"maxWaitDuration long member field (current value: %1$d [msec]) " 
									+ "will be set to %2$d [msec] what is not recommended value.",
									TransactionalMemcacheServiceAspect.getMaxWaitDuration(), 
									maxWaitDuration
									)
							);
				}
			}
			
			TransactionalMemcacheServiceAspect.maxWaitDuration = maxWaitDuration;
		}
	
	protected static int maxNotificationCount 
	= TransactionalMemcacheServiceCommonConstant.DefaultMaxNotificationCount;
		public static int getMaxNotificationCount() {
			return TransactionalMemcacheServiceAspect.maxNotificationCount;
		}
		public static void setMaxNotificationCount( final int maxNotificationCount) {
			if ( maxNotificationCount < 1L) {
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"maxNotificationCount int member field (current value: %1$d) " 
									+ "will be set to %2$d what is not recommended value.",
									TransactionalMemcacheServiceAspect.getMaxNotificationCount(), 
									maxNotificationCount
									)
							);
				}
			}
			TransactionalMemcacheServiceAspect.maxNotificationCount = maxNotificationCount;
		}

	/**
	 * ReentrantLock object for use of locking out execution of each method of 
	 * TransactionalMemcacheService interface
	 */
	protected static ReentrantLock memcacheServiceMethodLock = new ReentrantLock();
		protected static int getQueueLengthOnMemcacheServiceMethodLock() {
			return TransactionalMemcacheServiceAspect.memcacheServiceMethodLock.getQueueLength();
		}
		protected static void lockMemcacheServiceMethod( final long lockAcquisitionPeriod) 
		throws InterruptedException {
			try {
				boolean lockingResult 
				= TransactionalMemcacheServiceAspect
					.memcacheServiceMethodLock.tryLock( lockAcquisitionPeriod, TimeUnit.MILLISECONDS);
				
				if ( !lockingResult) {
					int queueLength 
					= TransactionalMemcacheServiceAspect.getQueueLengthOnMemcacheServiceMethodLock();
					
					throw new TransactionalMemcacheServiceException(
							String.format(
									"Thread (id: %1$d) could not acquire lock on memcacheServiceMethodLock " 
									+ "member field within the duration of %2$d [msec] in %3$s mode. " 
									+ "The number of other threads waiting the same lock is about %4$d.",
									Thread.currentThread().getId(),
									lockAcquisitionPeriod, 
									(TransactionalMemcacheServiceAspect
											.getTransactionalMemcacheServiceTransactionHelper()
											.isTransactionModeThread() ? "transaction" : "non-transaction"),
									queueLength
									)
							);
				}
			}
			catch( InterruptedException exception) { // Interrupted by other thread
				throw exception;
			}
		} // protected static void lockMemcacheServiceMethod()
		protected static void unlockMemcacheServiceMethod() throws IllegalMonitorStateException {
			TransactionalMemcacheServiceAspect.memcacheServiceMethodLock.unlock();
		}
		
	/**
	 * To keep track of calls to each method of TransactionalMemcacheService interface (except 
	 * clearAll method, switchToTransactionMode method, switchToNonTransactionMode method, 
	 * setTransactionalMemcacheServiceHelpers method, setTransactionalMemcacheServiceTransactionHelper 
	 * method), in order for each method of TransactionalMemcacheService interface to be able to be 
	 * executed exclusively with those excluded methods.
	 */
	protected static int memcacheServiceMethodCallCounter = 0;		
		/**
		 * Incrementing the value of memcacheServiceMethodCallCounter int member field for the purpose  
		 * of tracking each call to method of TransactionalMemcacheService interface except clearAll 
		 * method, switchToTransactionMode method, switchToNonTransactionMode method, 
		 * setTransactionalMemcacheServiceHelpers method, setTransactionalMemcacheServiceTransactionHelper 
		 * method. 
		 * @param lockAcquisitionPeriod : duration in milliseconds to give up locking memcacheServiceMethodLock lock.
		 * @return value of memcacheServiceMethodCallCounter int member field what is the number of calls 
		 * to method of MemcacheService interface (except clearAll method, switchToTransactionMode method, 
		 * switchToNonTransactionMode method, setTransactionalMemcacheServiceHelpers method, 
		 * setTransactionalMemcacheServiceTransactionHelper method) having not completed yet and 
		 * the execution of those excluded methods needs to wait for.
		 * @throws InterruptedException when thread is interrupted while waiting for lock on 
		 * memcacheServiceMethodLock member field acquired. <br />
		 * @throws TransactionalMemcacheServiceException when lock on memcacheServiceMethodLock member 
		 * field is not acquired for period specified by lockAcquisitionPeriod argument.
		 */
		protected static int incrementMemcacheServiceMethodCallCounter( final long lockAcquisitionPeriod) 
		throws InterruptedException {
			// Make this hold for completion of execution of clearAll method, switchToTransactionMode method, 
			// switchToNonTransactionMode method, setTransactionalMemcacheServiceHelpers method, and  
			// setTransactionalMemcacheServiceTransactionHelper method ----------------------------
			try {
				TransactionalMemcacheServiceAspect.lockMemcacheServiceMethod( lockAcquisitionPeriod);
			}
			catch( InterruptedException exception) { // Interrupted by other thread
				throw exception;
			}
			// ------------------------------------------------------------------------------------
			
			try {
				return ++TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter;
			}
			finally {
				TransactionalMemcacheServiceAspect.unlockMemcacheServiceMethod();
			}
		}
		
	/**
	 * ReentrantLock object for detecting the condition of termination of all running methods of 
	 * TransactionalMemcacheService interface (except clearAll method, switchToTransactionMode method, 
	 * switchToNonTransactionMode method, setTransactionalMemcacheServiceHelpers method, 
	 * setTransactionalMemcacheServiceTransactionHelper method) by combination use with 
	 * memcacheServiceMethodLock object.
	 */
	protected static ReentrantLock memcacheServiceMethodCallCounterLock = new ReentrantLock();
		protected static int getQueueLengthOnMemcacheServiceMethodCallCounterLock() {
			return TransactionalMemcacheServiceAspect
					.memcacheServiceMethodCallCounterLock.getQueueLength();
		}
		protected static void lockMemcacheServiceMethodCallCounter( final long lockAcquisitionPeriod) 
		throws InterruptedException {
			try {
				boolean tryLockResult 
				= TransactionalMemcacheServiceAspect
					.memcacheServiceMethodCallCounterLock.tryLock( 
							lockAcquisitionPeriod, TimeUnit.MILLISECONDS);
				
					if ( !tryLockResult) {
						int queueLength 
						= TransactionalMemcacheServiceAspect
							.getQueueLengthOnMemcacheServiceMethodCallCounterLock();
						
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Thread (id: %1$d) could not acquire lock on " 
										+ "memcacheServiceMethodCallCounterLock member field within " 
										+ "the duration of %2$d [msec] in %3$s mode. The number of other " 
										+ "threads waiting the same lock is about %4$d.",
										Thread.currentThread().getId(),
										lockAcquisitionPeriod, 
										(TransactionalMemcacheServiceAspect
												.getTransactionalMemcacheServiceTransactionHelper()
												.isTransactionModeThread() ? "transaction" : "non-transaction"),
										queueLength
										)
								);
					}
			}
			catch( InterruptedException exception) {
				throw exception;
			}
		} // protected void lockMemcacheServiceMethodCallCounter( long lockAcquisitionPeriod)
		
		protected static void unlockMemcacheServiceMethodCallCounter() throws IllegalMonitorStateException {
			TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounterLock.unlock();
		}
		
		protected static Condition memcacheServiceMethodCallCountCondition 
		= TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounterLock.newCondition();
		
		/**
		 * Decrement the value of memcacheServiceMethodCallCounter int member field for the purpose of 
		 * tracking each call to method of TransactionalMemcacheService interface except clearAll method, 
		 * switchToTransactionMode method, switchToNonTransactionMode method, 
		 * setTransactionalMemcacheServiceHelpers method, setTransactionalMemcacheServiceTransactionHelper 
		 * method.
		 * @param lockAcquisitionPeriod : duration in milliseconds to give up locking 
		 * memcacheServiceMethodCallCounterLock lock.
		 * @throws InterruptedException when thread is interrupted while waiting for lock on 
		 * memcacheServiceMethodCallCounterLock member field acquired. 
		 * @throws TransactionalMemcacheServiceException when lock on memcacheServiceMethodCallCounterLock 
		 * member field is not acquired for period specified by maxLockAcquisitionDuration argument.
		 * @throws TransactionalMemcacheServiceException wrapping IllegalMonitorStateException when 
		 * value of memcacheServiceMethodCallCounter member field become less than 0 what implies that 
		 * system is out of sync in tracking calls to method of TransactionalMemcacheService interface.
		 */
		protected static void decrementMemcacheServiceMethodCallCounter( final long lockAcquisitionPeriod) 
		throws InterruptedException {
			
			try {
				TransactionalMemcacheServiceAspect
				.lockMemcacheServiceMethodCallCounter( lockAcquisitionPeriod);
					/* Gained lock on memcacheServiceMethodCallCounterLock member field  
					 * in order to send notification of memcacheServiceMethodCallCounter's becoming zero 
					 * to waiting thread of that lock.
					 */
			}
			catch( InterruptedException exception) {
				throw exception;
			}
			finally {
				TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter--;
			}
			
			try {
				if ( TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter == 0) {
					TransactionalMemcacheServiceAspect
					.memcacheServiceMethodCallCountCondition.signalAll();
				}
				else if ( TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter < 0) {
					TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter = 0;
					
					String messageOnWaitingCallCountCondition = null;
						try {
							int waitQueueLength 
							= TransactionalMemcacheServiceAspect
								.memcacheServiceMethodCallCounterLock.getWaitQueueLength( 
										TransactionalMemcacheServiceAspect
											.memcacheServiceMethodCallCountCondition
										);
							
								if ( waitQueueLength > 0) {
									messageOnWaitingCallCountCondition 
									= String.format(
											"Other %1$d %2$s are waiting " 
											+ "memcacheServiceMethodCallCountCondition condition.", 
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
											"memcacheServiceMethodCallCounter, what keeps track of calls to " 
											+ "public TransactionalMemcacheService interface methods (except " 
											+ "clearAll method, switchToTransactionMode method, " 
											+ "switchToNonTransactionMode method, " 
											+ "setTransactionalMemcacheServiceHelpers method, " 
											+ "setTransactionalMemcacheServiceTransactionHelper method) " 
											+ "became less than zero. (Thread of what id is %1$d encoutnered " 
											+ "this in %2$s mode.) This must never happen. Since " 
											+ "having resetted it to zero, this same warning may appear " 
											+ "repeatedly for awhile as recovery process until it becomes " 
											+ "actual intial state . %3$s",
											Thread.currentThread().getId(),
											(TransactionalMemcacheServiceAspect
													.getTransactionalMemcacheServiceTransactionHelper()
													.isTransactionModeThread() ? "transaction" : "non-transaction"),
											((messageOnWaitingCallCountCondition == null) ? "" : messageOnWaitingCallCountCondition)
											)
									)
							);
				}
			}
			finally {
				TransactionalMemcacheServiceAspect.unlockMemcacheServiceMethodCallCounter();
			}
		}
		
		/**
		 * Make thread kept waiting for notification via memcacheServiceMethodCallCountCondition condition  
		 * object until when memcacheServiceMethodCallCounter becomes zero. <br /> 
		 * Prerequisite : owning lock on memcacheServiceMethodCallCounterLock member field. <br />
		 * Thread safety: thread safe as long as prerequisite is fulfilled.
		 * @param maxWaitPeriod : the number of milliseconds to wait for each notification via 
		 * memcacheServiceMethodCallCountCondition condition object.
		 * @throws TransactionalMemcacheServiceException when thread isn't waken up by notification via 
		 * memcacheServiceMethodCallCountCondition condition object during period of time specified by 
		 * maxWaitPeriod imput. 
		 * @throws TransactionalMemcacheServiceException wrapping InterruptedException when thread is 
		 * interrupted while waiting for notification via memcacheServiceMethodCallCountCondition 
		 * condition object during period of time specified by maxWaitPeriod input.
		 */
		protected static void awaitZeroOfMemcacheServiceMethodCallCounter( 
				final long maxWaitPeriod, final int maxNotificationTimes) {
			
			int receivedNotificationCount = 0;
			
			while( TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter > 0) {
				try {
					boolean awaitResult 
					= TransactionalMemcacheServiceAspect
						.memcacheServiceMethodCallCountCondition.await( 
								maxWaitPeriod, TimeUnit.MILLISECONDS);
					
						if ( !awaitResult) {
							int queueLength 
							= TransactionalMemcacheServiceAspect.getQueueLengthOnMemcacheServiceMethodLock();
							String additionalMessage 
							= String.format(
									"%n%1$cThe number of other threads waiting for this thread to release " 
									+ "lock on memcacheServiceMethodLock member field: %2$d",
									'\t',
									queueLength
									);
							
							queueLength 
							= TransactionalMemcacheServiceAspect
								.getQueueLengthOnMemcacheServiceMethodCallCounterLock();
							additionalMessage 
							= additionalMessage + String.format(
									"%n%1$cThe number of other threads waiting for the lock of " 
									+ "memcacheServiceMethodCallCounterLock member field : %2$d",
									'\t',
									queueLength
									);
							
							try {
								int waitQueueLength 
								= TransactionalMemcacheServiceAspect
									.memcacheServiceMethodCallCounterLock.getWaitQueueLength( 
											TransactionalMemcacheServiceAspect
												.memcacheServiceMethodCallCountCondition
											);
								additionalMessage 
								= additionalMessage + String.format(
										"%n%1$cThe number of other threads waiting for the same " 
										+ "notifcation of memcacheServiceMethodCallCounter member " 
										+ "field becoming zero: %2$d", 
										'\t',
										waitQueueLength
										);
							}
							catch( Throwable throwable) { // Do nothing since it's just for additional info 
							}
							
							throw new TransactionalMemcacheServiceException(
									String.format(
											"Thread (id: %1$d) did not meet the condtion of " 
											+ "memcacheServiceMethodCallCounter member field becoming zero " 
											+ "for %2$d [msec] in %3$s mode. %nAdditional info:" 
											+ "%n%4$cThe current value of memcacheServiceMethodCallCounter " 
											+ "member field: %5$d. %6$s",
											Thread.currentThread().getId(),
											maxWaitPeriod,
											(TransactionalMemcacheServiceAspect
													.getTransactionalMemcacheServiceTransactionHelper()
													.isTransactionModeThread() ? "transaction" : "non-transaction"),
											'\t',
											TransactionalMemcacheServiceAspect
												.memcacheServiceMethodCallCounter,
											additionalMessage
											)
									);
						}
				}
				catch( InterruptedException exception) {
					throw new TransactionalMemcacheServiceException(
							String.format(
									"Thread (id: %1$d) was interrupted in %2$s mode before confirming " 
									+ "the termination of already-running methods of " 
									+ "TransactionalMemcacheService interface in other threads.", 
									Thread.currentThread().getId(), 
									(TransactionalMemcacheServiceAspect
											.getTransactionalMemcacheServiceTransactionHelper()
											.isTransactionModeThread() ? "transaction" : "non-transaction")
									),
							exception
							);
				}
				
				if ( ++receivedNotificationCount > maxNotificationTimes) {
					TransactionalMemcacheServiceAspect.memcacheServiceMethodCallCounter = 0;
					
					throw new TransactionalMemcacheServiceException(
							String.format(
									"The lock on memcacheServiceMethodCallCounterLock member field has " 
									+ "been released the specified %1$d times before meeting the condition " 
									+ "of memcacheServiceMethodCallCounter member field becoming zero. " 
									+ "Thereby initialized memcacheServiceMethodCallCounter member field to " 
									+ "zero. As recovery process from now on awhile because of that, it is " 
									+ "expected that some other threads may hit " 
									+ "TransactionalMemcacheServiceException exception claiming that value " 
									+ "of memcacheServiceMethodCallCounter member field became less than zero.",
									maxNotificationTimes
									)
							);
				}
			} // while
		} // public void awaitZeroOfMemcacheServiceMethodCallCounter()
		
	// Aspects for TransactionalMemcacheService interface methods ---------------------------------
	/**
	 * AspectJ's pointcut to pick up join points at execution of TransactionalMemcacheService interface 
	 * methods on TransactionalMemcacheServiceImplSkelton class. 
	 */
	@Pointcut( value="execution( public * slingong.web.gae.TransactionalMemcacheServiceImplSkelton.*(..))")
	protected static void pointcutAtTransactionalMemcacheServicePublicMethodExecution() {}
	
	@Pointcut( value="execution( public * slingong.web.gae.TransactionalMemcacheServiceBase+.setKeySetKey( java.lang.String))")
	protected static void pointcutAtSetKeySetKeyMethodExecution() {}

	/**
	 * AspectJ's pointcut to pick up join points at execution of clearAll method of 
	 * MemcacheService interface on sub-class of TransactionalMemcacheServiceBase class.
	 */
	@Pointcut( value="execution( public * slingong.web.gae.TransactionalMemcacheServiceBase+.clearAll())")
	protected static void pointcutAtTransactionalMemcacheServiceClearAllMethodExecution() {}

	/**
	 * AspectJ's around advise for each public method of TransactionalMemcacheService interface except 
	 * clearAll method in order to perform the followings for synchronization with execution of 
	 * clearAll method, switchToTransactionMode method, switchToNonTransactionMode method, 
	 * setTransactionalMemcacheServiceHelpers method, setTransactionalMemcacheServiceTransactionHelper 
	 * method: 
	 * <ol>
	 * <li>Wait for completion of clearAll method, switchThreadToNonTransaction method, 
	 * switchThreadToTransaction method, setTransactionalMemcacheServiceHelpers method, and 
	 * setTransactionalMemcacheServiceTransactionHelper method by attempting to lock 
	 * memcacheServiceMethodLock member field at the invocation of each method of TransactionalMemberService 
	 * interface (except those methods).</li>
	 * <li>Increment the value of memcacheServiceMethodCallCounter member field before execution of 
	 * target method (method of TransactionalMemberService interface).</li>
	 * <li>Execute target method.</li>
	 * <li>After execution of target method, decrement the value of memcacheServiceMethodCallCounter 
	 * member field while locking memcacheServiceMethodCallCounterLock member field. </li>
	 * <li>If the value of memcacheServiceMethodCallCounter member field becomes zero, then notify 
	 * memcacheServiceMethodCallCountCondition condition.</li>
	 * </ol>
	 * In sub-class of TransactionalMemcacheServiceImpl class, when needs to overwrite this Around-advice 
	 * or overwrite either of clearAll method, switchThreadToNonTransaction method, 
	 * switchThreadToTransaction method, setTransactionalMemcacheServiceHelpers method, or  
	 * setTransactionalMemcacheServiceTransactionHelper method, the overwritten method needs to have the 
	 * following functionalities in appropriate mode (transaction or non-transaction):
	 * <ul>
	 * <li>Lock memcacheServiceMethodLock member field to hold further execution of any method 
	 * of TransactionalMemcacheService interface until completion of execution of clearAll method, 
	 * switchThreadToNonTransaction method, switchThreadToTransaction method, 
	 * setTransactionalMemcacheServiceHelpers method, or setTransactionalMemcacheServiceTransactionHelper 
	 * method.</li>
	 * <li>By checking if the value of memcacheServiceMethodCallCounter member field is zero or not, 
	 * detect whether there is any method of TransactionalMemcacheService interface what is at 
	 * the middle of execution.</li>
	 * <li>If there is, then wait for memcacheServiceMethodCallCountCondition condition.</li>
	 * <li>If there is not, then perform tasks of clearAll method.</li>
	 * <li>At the end, unlock memcacheServiceMethodLock member field.</li>
	 * </ul>
	 * @param proceedingJoinPoint
	 * @return
	 */
	@Around( 
			value="(pointcutAtTransactionalMemcacheServicePublicMethodExecution() " 
					+ "|| pointcutAtSetKeySetKeyMethodExecution())" 
					+ "&& !pointcutAtTransactionalMemcacheServiceClearAllMethodExecution() " 
					+ "&& this( slingong.web.gae.TransactionalMemcacheServiceImpl)"
			)
	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	public Object aroundAdvisedMemcacheServicePublicMethodExecution( 
			final ProceedingJoinPoint proceedingJoinPoint) {
		
		boolean transactionMode = 
				TransactionalMemcacheServiceAspect
				.getTransactionalMemcacheServiceTransactionHelper().isTransactionModeThread();
		
		long maxLockAcquisitionPeriod = TransactionalMemcacheServiceAspect.getMaxLockAcquisitionDuration();
		
		if ( !transactionMode) {
			try { 
				// increment memcacheServiceMethodCallCounter value
				TransactionalMemcacheServiceAspect
				.incrementMemcacheServiceMethodCallCounter( maxLockAcquisitionPeriod);
			}
			catch( InterruptedException exception) {
				throw new TransactionalMemcacheServiceException( 
						String.format(
								"Before exectuion of %1$s to work on cache space of what namespace is %2$s " 
								+ "in non-transaction mode, thread (id: %3$d) is interrupted by other " 
								+ "thread in incresing the value of memcacheServiceMethodCallCounter member " 
								+ "field as tracking the execution.",
								proceedingJoinPoint.getSignature().toString(),
								((TransactionalMemcacheServiceImpl)proceedingJoinPoint.getThis()).getNamespace(),
								Thread.currentThread().getId()
								),
						exception
						);
			}
		}
		
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
			if ( !transactionMode) { 
				// decrement memcacheServiceMethodCallCounter value 
				try {
					TransactionalMemcacheServiceAspect
					.decrementMemcacheServiceMethodCallCounter( maxLockAcquisitionPeriod);
				}
				catch( InterruptedException exception) {
					String nameSpace 
					= ((TransactionalMemcacheServiceImpl)proceedingJoinPoint.getTarget()).getNamespace();
					
					throw new TransactionalMemcacheServiceException( 
							String.format(
									"After exectuion of %1$s to work on cache space of what namespace is " 
									+ "%2$s in non-transaction mode, thread (id: %3$d) is interrupted by " 
									+ "other thread in decreasing the value of memcacheServiceMethodCallCounter " 
									+ "member field as tracking the execution.",
									proceedingJoinPoint.getSignature().toString(),
									nameSpace,
									Thread.currentThread().getId() 
									),
							exception
							);
				}
			}
		}
	} // public Object aroundAdvisedMemcacheServicePublicMethodExecution(ProceedingJoinPoint proceedingJoinPoint)
	
	/**
	 * 
	 * @param proceedingJoinPoint
	 * @return
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of 
	 * methods of TransactionalMemcacheService interface on TransactionalMemcacheServiceImpl instances. 
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of TransactionalMemcacheService interface.
	 */
	protected static Object synchronizedInvocationAmongInstances( 
			final ProceedingJoinPoint proceedingJoinPoint, 
			final long maxLockAcqusitionPeriod, 
			final long maxWaitPeriod, 
			final int maxNotificationTimes
			) 
	{
		// Lock memcacheServiceMethodLock to hold any further call to public methods of 
		// TransactionalMemcacheService interface method ------------------------------------------
		try {
			TransactionalMemcacheServiceAspect.lockMemcacheServiceMethod( maxLockAcqusitionPeriod);
				/* Locked out further execution of public methods of TransactionalMemcacheService 
				 * interface on TransactionalMemcacheServiceImpl instances in non-transaction mode 
				 * by locking memcacheServiceMethodLock member field.
				 */
		}
			catch( InterruptedException exception) {
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Thread (id: %1$d) was interrupted in %2$s mode before obtaining locks on " 
								+ "memcacheServiceMethodLock ReentrantLock member field before actually " 
								+ "executing %3$s.", 
								Thread.currentThread().getId(), 
								(TransactionalMemcacheServiceAspect
										.getTransactionalMemcacheServiceTransactionHelper()
										.isTransactionModeThread() ? "transaction" : "non-transaction"), 
								proceedingJoinPoint.getSignature().toString()
								),
						exception
						);
			}
		// ----------------------------------------------------------------------------------------
		
		try {
			// Waits for termination of execution of TransactionalMemcacheService interface methods 
			// having already been running before locking memcacheServiceMethodLock ---------------
			try {
				TransactionalMemcacheServiceAspect
				.lockMemcacheServiceMethodCallCounter( maxLockAcqusitionPeriod);
					// 
			}
				catch( InterruptedException exception) {
					throw new TransactionalMemcacheServiceException(
							String.format(
									"Thread (id: %1$d) was interrupted in %2$s mode before obtaining lock on " 
									+ "memcacheServiceMethodCallCounterLock ReentrantLock member field " 
									+ "before actually executing %3$s.",
									Thread.currentThread().getId(),
									(TransactionalMemcacheServiceAspect
											.getTransactionalMemcacheServiceTransactionHelper()
											.isTransactionModeThread() ? "transaction" : "non-transaction"),
									proceedingJoinPoint.getSignature().toString()
									),
							exception
							);
				}
			
			try {
				/* Waits for memcacheServiceMethodCallCounter becoming back to 0 in order to avoid for 
				 * memcache to be updated with expired data by already running method of MemcacheService 
				 * interface.
				 */
				TransactionalMemcacheServiceAspect
				.awaitZeroOfMemcacheServiceMethodCallCounter( maxWaitPeriod, maxNotificationTimes);
			}
			finally {
				TransactionalMemcacheServiceAspect.unlockMemcacheServiceMethodCallCounter();
			}
			// ------------------------------------------------------------------------------------
			
			// Invocation of actual clearAll method -----------------------------------------------
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
			TransactionalMemcacheServiceAspect.unlockMemcacheServiceMethod();
		}
	} // protected Object synchronizedInvocationAmongInstances( ProceedingJoinPoint proceedingJoinPoint)

	/**
	 * 
	 * @param proceedingJoinPoint
	 * @return
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of 
	 * methods of TransactionalMemcacheService interface on TransactionalMemcacheServiceImpl instances 
	 * in non-transaction mode. 
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of TransactionalMemcacheService interface in non-transaction mode.
	 */
	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	@Around( 
			value="pointcutAtTransactionalMemcacheServiceClearAllMethodExecution() " 
					+ "&& this( slingong.web.gae.TransactionalMemcacheServiceImpl)"
			)
	public Object aroundAdvisedClearAllMethodExecution( final ProceedingJoinPoint proceedingJoinPoint) {
		boolean transactionMode = 
				TransactionalMemcacheServiceAspect
				.getTransactionalMemcacheServiceTransactionHelper().isTransactionModeThread();
		
		if ( !transactionMode) { // non-transaction mode
			long maxAcquisitionLockPeriod 
			= TransactionalMemcacheServiceAspect.getMaxLockAcquisitionDuration();
			long maxWaitingPeriod 
			= TransactionalMemcacheServiceAspect.getMaxWaitDuration();
			int maxNotificationTimes 
			= TransactionalMemcacheServiceAspect.getMaxNotificationCount();
			
			return TransactionalMemcacheServiceAspect
					.synchronizedInvocationAmongInstances( 
							proceedingJoinPoint, 
							maxAcquisitionLockPeriod, 
							maxWaitingPeriod, 
							maxNotificationTimes);
		}
		else {
		/* transaction mode. 
		 * Not necessary for consideration of synchronization since all works will be done onto ThreeadLocal 
		 * variable once transaction mode has begun.
		 */
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
		}
	} // public Object aroundAdvisedClearAllMethodExecution( ProceedingJoinPoint proceedingJoinPoint)
	
	protected TransactionalMemcacheServiceException constructExceptionForClearAllInvocationRestriction( 
			StackTraceElement stackTraceElement) {
		return new TransactionalMemcacheServiceException( 
				new UnsupportedOperationException( 
						String.format(
								"TransactionalMemcacheServiceSyncHelperImpl.clearAllForNonTransactionMode " 
								+ "method is not allowed to be called from %1$s method of %2$s class. " 
								+ "It must be called from clearAll method of " 
								+ "TransactionalMemcacheServiceBase sub-class in order to compling " 
								+ "the hierarchy of locks for synchronization.",
								stackTraceElement.getMethodName(), 
								stackTraceElement.getClassName()
								)
						)
				);
	}
	
	/**
	 * AspectJ's pointcut to pick up join points at calling clearAllForNonTransactionMode method what is 
	 * called by clearAll method of TransactionalMemcacheService interface 
	 */
	@Pointcut( 
			value="execution( public * slingong.web.gae.TransactionalMemcacheServiceSyncHelperImpl" 
					+ ".clearAllForNonTransactionMode()) " 
			)
	protected static void pointcutAtClearAllForNonTransactionModeMethodExecution() {}
	/**
	 * before-advice to assure that TransactionalMemcacheServiceSyncHelperImpl.clearAllForNonTransactionMode 
	 * method is called from clearAll method of TransactionalMemcacheServiceBase class or its sub-class.
	 */
	@Before( value="pointcutAtClearAllForNonTransactionModeMethodExecution()")
	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	public void beforeAdvisedClearAllForNonTransactionModeMethodExecution() {
		
		StackTraceElement[] stackTraceElementArray = Thread.currentThread().getStackTrace();
		StackTraceElement stackTraceElement = stackTraceElementArray[ 3];
			/* element 0: StackTraceElement for calling Thread.currentThread().getStackTrace() method
			 * element 1: StackTraceElement for this beforeAdvisedClearAllForNonTransactionModeMethodExecution before-advise
			 * element 2: StackTraceElement for calling TransactionalMemcacheServiceSyncHelperImpl.clearAllForNonTransactionMode method (target method)
			 * element 3: StackTraceElement what should be pointing at TransactionalMemcacheServiceImpl.clearAll method
			 */
		
		Class<?> classObj;
		try {
			classObj = Class.forName( stackTraceElement.getClassName());
		}
			catch( Throwable throwable) {
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Failed to obtain Class object for %1$s in assuring that " 
								+ "TransactionalMemcacheServiceSyncHelperImpl.clearAllForNonTransactionMode " 
								+ "method has been called from clearAll method of " + 
								"TransactionalMemcacheServiceBase sub-class class in order to compling " 
								+ "the hierarchy of locks for synchronization.",
								stackTraceElement.getClassName()
								),
						throwable
						);
			}
		
		if ( !TransactionalMemcacheServiceBase.class.isAssignableFrom( classObj)) 
		{
			throw constructExceptionForClearAllInvocationRestriction( stackTraceElement);
		}
		else {
			String methodName = stackTraceElement.getMethodName();
			
			if ( !methodName.startsWith( "clearAll")) {
				throw constructExceptionForClearAllInvocationRestriction( stackTraceElement);
			}
			else if ( !"clearAll".equals( methodName)) {
				if ( !methodName.startsWith( "clearAll_aroundBody")) {
					throw constructExceptionForClearAllInvocationRestriction( stackTraceElement);
				}
				else {
					try {
						classObj = Class.forName( stackTraceElementArray[ 5].getClassName());
					}
						catch( Throwable throwable) {
							throw new TransactionalMemcacheServiceException(
									String.format(
											"Failed to obtain Class object for %1$s in assuring that " 
											+ "TransactionalMemcacheServiceSyncHelperImpl.clearAllForNonTransactionMode " 
											+ "method has been called from clearAll method of " + 
											"TransactionalMemcacheServiceBase sub-class class in order to compling " 
											+ "the hierarchy of locks for synchronization.",
											stackTraceElement.getClassName()
											),
									throwable
									);
						}
					if ( !ProceedingJoinPoint.class.isAssignableFrom( classObj)) 
					{
						throw constructExceptionForClearAllInvocationRestriction( stackTraceElement);
					}
					else if ( !"proceed".equals( stackTraceElementArray[ 5].getMethodName())) {
						throw constructExceptionForClearAllInvocationRestriction( stackTraceElement);
					}
				}
			}
		}
	} // public void beforeAdvisedClearAllForNonTransactionModeMethodExecution()
	
	/**
	 * AspectJ's pointcut to pick up join points at execution of 
	 * TransactionalMemcacheServiceTransactionHelperImpl.switchThreadToNonTransactionMode method 
	 */
	@Pointcut( 
			value="execution( " 
					+ "public * slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl" 
					+ ".switchThreadToNonTransactionMode( ..))"
			)
	protected static void pointcutAtSwitchThreadToNonTransactionModeMethodExecution() {}
	
	/**
	 * Around-advise for synchronizing invocation of 
	 * TransactionalMemcacheServiceTransactionHelperImpl.switchThreadToNonTransactionMode method. <br /> 
	 * @param proceedingJoinPoint
	 * @return
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when this is 
	 * executed in non-transaction mode.
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of methods of 
	 * TransactionalMemcacheService interface on TransactionalMemcacheServiceImpl instances. 
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of TransactionalMemcacheService interface.
	 */
	@Around( value="pointcutAtSwitchThreadToNonTransactionModeMethodExecution()")
	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	public Object aroundAdvisedSwitchThreadToNonTransactionModeMethodExecution( 
			final ProceedingJoinPoint proceedingJoinPoint) {
		
		boolean transactionMode = 
				TransactionalMemcacheServiceAspect
				.getTransactionalMemcacheServiceTransactionHelper().isTransactionModeThread();
		if ( !transactionMode) {
			throw new TransactionalMemcacheServiceException( 
					new UnsupportedOperationException( 
							String.format(
									"In non-transaction mode, it is not supported invoking %1$s.",
									proceedingJoinPoint.getSignature().toString()
									)
							)
					);
		}
		
		long maxAcquisitionLockPeriod 
		= TransactionalMemcacheServiceAspect.getMaxLockAcquisitionDuration();
		long maxWaitingPeriod 
		= TransactionalMemcacheServiceAspect.getMaxWaitDuration();
		int maxNotificationTimes 
		= TransactionalMemcacheServiceAspect.getMaxNotificationCount();
		
		return TransactionalMemcacheServiceAspect
				.synchronizedInvocationAmongInstances( 
						proceedingJoinPoint, 
						maxAcquisitionLockPeriod, 
						maxWaitingPeriod, 
						maxNotificationTimes
						);
	} // public Object aroundAdvisedSwitchThreadToNonTransactionModeMethodExecution( .)
	
	/**
	 * AspectJ's pointcut to pick up join points at execution of 
	 * TransactionalMemcacheServiceTransactionHelperImpl.switchThreadToTransactionMode method 
	 */
	@Pointcut( 
			value="execution( " 
					+ "public * slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl" 
					+ ".switchThreadToTransactionMode( ..))"
			)
	protected static void pointcutAtSwitchThreadToTransactionModeMethodExecution() {}
	
	/**
	 * 
	 * @param proceedingJoinPoint
	 * @return
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when this is 
	 * executed in transaction mode.
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of methods of 
	 * TransactionalMemcacheService interface on TransactionalMemcacheServiceImpl instances. 
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of TransactionalMemcacheService interface.
	 */
	@Around( value="pointcutAtSwitchThreadToTransactionModeMethodExecution()")
	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	public Object aroundAdvisedSwitchThreadToTransactionModeMethodExecution( 
			final ProceedingJoinPoint proceedingJoinPoint) {
		
		boolean transactionMode = 
				TransactionalMemcacheServiceAspect
				.getTransactionalMemcacheServiceTransactionHelper().isTransactionModeThread();
		
		if ( transactionMode) {
			throw new TransactionalMemcacheServiceException( 
					new UnsupportedOperationException( 
							String.format(
									"In transaction mode, it is not supported invoking %1$s.",
									proceedingJoinPoint.getSignature().toString()
									)
							)
					);
			
		}
		
		long maxAcquisitionLockPeriod 
		= TransactionalMemcacheServiceAspect.getMaxLockAcquisitionDuration();
		long maxWaitingPeriod 
		= TransactionalMemcacheServiceAspect.getMaxWaitDuration();
		int maxNotificationTimes 
		= TransactionalMemcacheServiceAspect.getMaxNotificationCount();
		
		return TransactionalMemcacheServiceAspect
				.synchronizedInvocationAmongInstances( 
						proceedingJoinPoint, 
						maxAcquisitionLockPeriod, 
						maxWaitingPeriod, 
						maxNotificationTimes
						);
	} // public Object aroundAdvisedSwitchThreadToTransactionModeMethodExecution( .)
	
	@Pointcut( 
			value="execution( " 
					+ "protected static void slingong.web.gae.TransactionalMemcacheServiceBase+" 
					+ ".setTransactionalMemcacheServiceTransactionHelper( " 
							+ "slingong.web.gae.TransactionalMemcacheServiceTransactionHelper))"
			)
	protected static void pointcutAtSetTransactionalMemcacheServiceTransactionHelperMethodExecution() {}
	
	@Pointcut( 
			value="execution(" 
					+ "protected static void slingong.web.gae.TransactionalMemcacheServiceBase+" 
					+ ".setTransactionalMemcacheServiceHelpers( " 
						+ "slingong.web.gae.TransactionalMemcacheServiceTransactionHelper, " 
						+ "slingong.web.gae.TransactionalMemcacheServiceSyncHelper))"
			)
	protected static void pointcutAtSetTransactionalMemcacheServiceHelpersMethodExecution() {}
	
	@Pointcut( 
			value="cflowbelow( execution( " 
					+ "protected static void slingong.web.gae.TransactionalMemcacheServiceBase+" 
					+ ".setTransactionalMemcacheServiceHelpers( " 
						+ "slingong.web.gae.TransactionalMemcacheServiceTransactionHelper, " 
						+ "slingong.web.gae.TransactionalMemcacheServiceSyncHelper))) " 
					+ "&& within( slingong.web.gae.TransactionalMemcacheServiceBase+)"
			)
	protected static void pointcutAtCFlowBelowSetTransactionalMemcacheServiceHelpersMethodExecution() {}
	
	@Pointcut( 
			value="execution(" 
					+ "protected void slingong.web.gae.TransactionalMemcacheServiceBase+" 
					+ ".setMembers( " 
						+ "java.lang.String, " 
						+ "slingong.web.gae.TransactionalMemcacheServiceTransactionHelper, " 
						+ "slingong.web.gae.TransactionalMemcacheServiceSyncHelper))"
			)
	protected static void pointcutAtSetMembersHelpersMethodExecution() {}
	
	@Around( 
			value="pointcutAtSetMembersHelpersMethodExecution() " 
					+ "|| pointcutAtSetTransactionalMemcacheServiceHelpersMethodExecution()" 
					+ "|| (pointcutAtSetTransactionalMemcacheServiceTransactionHelperMethodExecution() " 
						+ "&& !pointcutAtCFlowBelowSetTransactionalMemcacheServiceHelpersMethodExecution())"
			)
	public Object aroundAdvisedHelperSetterMethodsExecution( final ProceedingJoinPoint proceedingJoinPoint) {
		
		long maxAcquisitionLockPeriod 
		= TransactionalMemcacheServiceAspect.getMaxLockAcquisitionDuration();
		long maxWaitingPeriod 
		= TransactionalMemcacheServiceAspect.getMaxWaitDuration();
		int maxNotificationTimes 
		= TransactionalMemcacheServiceAspect.getMaxNotificationCount();
		
		return TransactionalMemcacheServiceAspect
				.synchronizedInvocationAmongInstances( 
						proceedingJoinPoint, 
						maxAcquisitionLockPeriod, 
						maxWaitingPeriod, 
						maxNotificationTimes
						);
	}
	
}
