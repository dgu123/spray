package slingong.web.gae;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newmainsoftech.aspectjutil.eventmanager.label.EventListener;
import com.newmainsoftech.aspectjutil.eventmanager.label.EventTrigger;


/**
 * Helper class for transaction handling of TransactionalMemcacheServiceImpl class. <br /> 
 * Because this works only on static member fields, even different multiple instances of this class yield 
 * the exact same result with single instance. Didn't make this as singleton and consisted of all 
 * methods as static method, in order to have more scalability by programming to interface and have this 
 * more testable.
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@EventListener
class TransactionalMemcacheServiceTransactionHelperImpl 
implements TransactionalMemcacheServiceTransactionHelper {
	
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	protected Copier copier;
		protected Copier getCopier() {
			return copier;
		}
		protected void setCopier( Copier copier) {
			this.copier = copier;
		}
		
	// Constructors -------------------------------------------------------------------------------
	public TransactionalMemcacheServiceTransactionHelperImpl( Copier copier) {
		this.copier = copier;
	}
	public TransactionalMemcacheServiceTransactionHelperImpl() {}
	// --------------------------------------------------------------------------------------------
	
	// For transaction handling -------------------------------------------------------------------
	public static class TransactionStash {
		protected boolean transactionMode = false;
		protected boolean clearAllInvocationInsident = false;
		protected boolean memcacheClearedForClearAllInvocation = false;
		
		public TransactionStash() {
		}
	}
	protected static ThreadLocal<TransactionStash> transactionStashThreadLocal 
	= new ThreadLocal<TransactionalMemcacheServiceTransactionHelperImpl.TransactionStash>() {
		@Override
		protected TransactionStash initialValue() {
			return new TransactionStash();
		}
	};
		protected TransactionStash getTransactionStash() {
			return TransactionalMemcacheServiceTransactionHelperImpl.transactionStashThreadLocal.get();
		}
		/**
		 * Performs followings:
		 * <ul>
		 * <li>Remove transactionStashThreadLocal ThreadLocal member field.</li>
		 * <li>Clean up CopierImpl by calling CopierImpl.turnOffCopier method</li>
		 * <li>Call NonTransactionModeEvent.removeTheadLocal method</li>
		 * </ul>
		 * Called at the following points: 
		 * <ul>
		 * <li>Constructors of TransactionalMemcacheServiceException</li>
		 * <li>TransactionalMemcacheServiceTransactionHelperImpl.switchThreadToNonTransactionMode method</li>
		 * </ul>
		 */
		public void clearTransactionResiduals() {
			TransactionalMemcacheServiceTransactionHelperImpl.transactionStashThreadLocal.remove();
			copier.turnOffCopier();
			TransactionalMemcacheServiceTransactionHelperImpl.nonTransactionModeEvent.removeTheadLocal();
		}
	
	/**
	 * Check if transaction mode or not.
	 * If it's not transaction mode, then call clearTransactionResiduals method.
	 * @return true when transaction mode.
	 */
	public boolean isTransactionModeThread() {
		boolean transactionMode = getTransactionStash().transactionMode;
		if ( !transactionMode) clearTransactionResiduals();
		return transactionMode;
	} // public boolean isTransactionModeThread()
	
	/**
	 * 
	 * @return
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when not 
	 * transaction mode.
	 */
	public boolean hasClearAllInvokedInTransaction() {
		if ( !isTransactionModeThread()) {
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							"Invocation of hasClearAllInvokedInTransaction method is not allowed in " 
							+ "non-transaction mode." 
							)
					);
		}
		
		TransactionStash transactionStash = getTransactionStash();
		return transactionStash.clearAllInvocationInsident;
	} // public static boolean hasClearAllInvokedInTransaction()
	
	public boolean hasMemcacheClearedForClearAllInvocation() {
		if ( !isTransactionModeThread()) {
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							"Invocation of hasMemcacheClearedForClearAllInvocation method is not allowed in " 
							+ "non-transaction mode." 
							)
					);
		}
		
		TransactionStash transactionStash = getTransactionStash();
		return transactionStash.memcacheClearedForClearAllInvocation;
	}
	
	public void setMemcacheClearedForClearAllInvocation() {
		if ( !isTransactionModeThread()) {
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							"Invocation of hasMemcacheClearedForClearAllInvocation method is not allowed in " 
							+ "non-transaction mode." 
							)
					);
		}
		
		TransactionStash transactionStash = getTransactionStash();
		transactionStash.memcacheClearedForClearAllInvocation = true;
	}
	
	/**
	 * Empty event class just for signaling purpose of clearAll method invocation during transaction mode.
	 * @author Arata Yamamoto
	 */
	public static class ClearAllInTransactionEvent {}
	
	/**
	 * Trigger ClearAllInTransactionEvent.class event what should be listened by 
	 * TransactionalMemcacheServiceImpl instances to handle clearAll method invocation 
	 * during transaction mode.
	 */
	@EventTrigger( value=ClearAllInTransactionEvent.class)
	public void clearAllInTransaction() {
		if ( !isTransactionModeThread()) {
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							"Invocation of clearAllInTransaction method is not allowed in " 
							+ "non-transaction mode." 
							)
					);
		}
		
		if ( logger.isInfoEnabled()) {
			logger.info( 
					String.format( 
							"clearAll method is called in transaction mode: " 
							+ "changes (what have been made so far during transaction and supporse to be " 
							+ "exported to memcache in all namespaces at the end of transaction) are " 
							+ "cleared out, and whole memcache (all namespaces) will be cleared once later " 
							+ "at the end of transaction."
							)
					);
		}
		
		getTransactionStash().clearAllInvocationInsident = true;
			// clearAllInvocationInsident member field will be referred in 
			// TransactionalMemcacheServiceImpl.switchToNonTransactionMode method to actually perform 
			// clearing whole memcache later
		
		// Do not clear transactionHandler.mainCacheSnapshot at here
	} // public void clearAllInTransaction()
	
	
	/**
	 * Event class for switching to non-transaction mode. 
	 * switchThreadToNonTransactionMode method triggers this event by calling trigerNonTransactionModeEvent method. 
	 * <b>Lister of NonTransactionModeEvent event cannot throw exception</b> since transaction has been 
	 * committed or rolled back at time when event is triggered (unless transaction system is global 
	 * transaction using 2 phase commit such as JTA what is not available on GAE/J at the moment of when 
	 * this has been written.) 
	 */
	public static class NonTransactionModeEvent {
		protected static ThreadLocal<Boolean> booleanThreadLocal 
		= new ThreadLocal<Boolean>() {
			@Override
			protected Boolean initialValue() {
				return true;
			}
		};
			protected void removeTheadLocal() {
				NonTransactionModeEvent.booleanThreadLocal.remove();
			}
			
		public boolean isTransactionChangeToBeSaved() {
			return NonTransactionModeEvent.booleanThreadLocal.get();
		}
		protected void setTransactionChangeToBeSaved( boolean saveTransactionChanges) {
			NonTransactionModeEvent.booleanThreadLocal.set( Boolean.valueOf( saveTransactionChanges));
		}
		
		@EventTrigger( value=NonTransactionModeEvent.class)
		protected void doTrigerNonTransactionModeEvent() {
		}
		
		protected void trigerNonTransactionModeEvent( boolean saveTransactionChanges) {
			setTransactionChangeToBeSaved( saveTransactionChanges);
			doTrigerNonTransactionModeEvent();
		}
	} // public static class NonTransactionModeEvent
	protected static NonTransactionModeEvent nonTransactionModeEvent = new NonTransactionModeEvent();
		public NonTransactionModeEvent getNonTransactionModeEvent() {
			return TransactionalMemcacheServiceTransactionHelperImpl.nonTransactionModeEvent;
		}

	/**
	 * Switch to transaction mode. 
	 * Trigger NonTransactionModeEvent event. Each TransactionalMemcacheServiceImpl instance handles actual 
	 * saving changes made during transaction to memcache by catching this event.   
	 * <b>No matter of the transaction result whether committed or rolled back, this method must be 
	 * executed</b> in order to avoid memory leak by ThreadLocal member fields used during transaction 
	 * mode. <br />
	 * Thread safety: thread safe. The part of synchronization is taken care by advising this in 
	 * TransactionalMemcacheServiceAspect class. <br />
	 * @param saveTransactionChanges when true, save changes made during transaction.
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when this is 
	 * called in non-transaction mode.
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of methods of 
	 * MemcacheService interface via TransactionalMemcacheServiceImpl instances in non-transaction mode. 
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of MemcacheService interface.
	 */
	public void switchThreadToNonTransactionMode( boolean saveTransactionChanges) {
		/* Trigger NonTransactionModeEvent to initiate execution of switchToNonTransactionMode method
		 * of each TransactionalMemcacheServiceImpl instance.
		 */
		nonTransactionModeEvent.trigerNonTransactionModeEvent( saveTransactionChanges);
		clearTransactionResiduals();
		// Don't need to set transactionStash.transactionMode to false here since it's taken care of in 
		// clearTransactionResiduals method.
	} // public static void switchThreadToNonTransactionMode( boolean saveTransactionChanges)
	
	/**	
	 * Event class for event being triggered by switchThreadToTransactionMode method
	 */
	public static class TransactionModeEvent {
		@EventTrigger( value=TransactionModeEvent.class)
		protected void triggerTransactionModeEvent() {
		}
	}
	protected TransactionModeEvent transactionModeEvent = new TransactionModeEvent();
	
	/**
	 * Trigger TransactionStash event. <br />
	 * The part of synchronization with other methods are taken care by advising this in 
	 * TransactionalMemcacheServiceAspect class. <br />
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when this is 
	 * executed in transaction mode.
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of methods of 
	 * MemcacheService interface via TransactionalMemcacheServiceImpl instances. 
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of MemcacheService interface.
	 */
	public void switchThreadToTransactionMode() {
		/* Trigger TransactionModeEvent to initiate execution of switchToTransactionMode method
		 * of each TransactionalMemcacheServiceImpl instance.
		 */
		transactionModeEvent.triggerTransactionModeEvent();
		
		TransactionStash transactionStash = getTransactionStash();
		transactionStash.transactionMode = true;
	} // public void switchThreadToTransactionMode()
	// --------------------------------------------------------------------------------------------
}
