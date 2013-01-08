/*
 * Copyright (C) 2011-2013 NewMain Softech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.newmainsoftech.spray.slingong.datastore;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.InvalidTimeoutException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.apphosting.api.DeadlineExceededException;

/* 
 * Need to create Aspect to check whether transactional annotation values are supported values. 
 * 	Not supporting values
 * 		
 * 		TransactionDefinition.PROPAGATION_NESTED
 * 		SYNCHRONIZATION_ALWAYS
 * 
 * Also create Aspect to alert the attempt of persisting data in read-only transaction. 
 * 
 * About deadline setting
 * 	- How deadline handling is implemented in AbstractPlatformTransactionManager needs to be researched. 
 * 	- Log the warning, when there is active global transaction, saying that the new setting value for 
 * 	the deadline time-out won't be applied to current active global transaction but new global transaction 
 * 	being created.  
 * 
 * AbstractPlatformTransactionManager:
 * http://static.springsource.org/spring/docs/current/api/org/springframework/transaction/support/AbstractPlatformTransactionManager.html
	AbstractPlatformTransactionManager class provides the following work-flow handling:
	    * determines if there is an existing transaction;
	    * 	This is not ture; AbstractPlatformTransactionManager's default isExistingTransaction returns false for all cases.
	    * applies the appropriate propagation behavior;
	    * suspends and resumes transactions if necessary;
	    * checks the rollback-only flag on commit;
	    * applies the appropriate modification on rollback (actual rollback or setting rollback-only);
	    * triggers registered synchronization callbacks (if transaction synchronization is active). 
 * 
 * http://static.springsource.org/spring/docs/current/spring-framework-reference/html/transaction.html#transaction-programmatic
 * Simply pass the implementation of the PlatformTransactionManager you are using to your bean through 
 * a bean reference. Then, using the TransactionDefinition and TransactionStatus objects you can initiate 
 * transactions, roll back, and commit.
	DefaultTransactionDefinition def = new DefaultTransactionDefinition();
	// explicitly setting the transaction name is something that can only be done programmatically
	def.setName( "SomeTxName");
	def.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRED);
	
	TransactionStatus status = txManager.getTransaction( def);
	try {
	  // execute your business logic here
	}
	catch( MyException ex) {
	  txManager.rollback( status);
	  throw ex;
	}
	txManager.commit( status);
 * 	
 * 
 * http://static.springsource.org/spring/docs/current/spring-framework-reference/html/transaction.html#transaction-strategies
	public interface PlatformTransactionManager {
	  TransactionStatus getTransaction( TransactionDefinition definition) throws TransactionException;
	  void commit( TransactionStatus status) throws TransactionException;
	  void rollback( TransactionStatus status) throws TransactionException;
	}	
 * 
	The TransactionDefinition interface specifies (Understanding these concepts is essential to using 
	the Spring Framework or any transaction management solution):
		- Isolation: 
			The degree to which this transaction is isolated from the work of other transactions. 
			For example, can this transaction see uncommitted writes from other transactions?
		- Propagation: 
			Typically, all code executed within a transaction scope will run in that transaction. 
			However, you have the option of specifying the behavior in the event that a transactional 
			method is executed when a transaction context already exists. 
			For example, code can continue running in the existing transaction (the common case); or 
			the existing transaction can be suspended and a new transaction created. 
			Spring offers all of the transaction propagation options familiar from EJB CMT. 
			To read about the semantics of transaction propagation in Spring, see 
			Section 10.5.7, "Transaction propagation".
		- Timeout: 
			How long this transaction runs before timing out and being rolled back automatically by 
			the underlying transaction infrastructure.
		- Read-only status: 
			A read-only transaction can be used when your code reads but does not modify data. 
			Read-only transactions can be a useful optimization in some cases, such as when you are 
			using Hibernate. 
 * 
	public interface TransactionStatus extends SavepointManager {
		boolean isNewTransaction();
		boolean hasSavepoint();
		void setRollbackOnly();
		boolean isRollbackOnly();
		void flush();
		boolean isCompleted();
	}
 * 
 * http://www.javaworld.com/javaworld/jw-04-2007/jw-04-xa.html
 * TransactionAttribute interface allows the application to specify which exceptions will cause a rollback and which ones will be committed. 
 * 
 * Exceptions handling can be referred at the following page: 
 * 	http://publib.boulder.ibm.com/infocenter/wsdoc400/v6r0/topic/com.ibm.websphere.iseries.doc/info/ae/ae/rdat_dawp02.html
 * 
 * Made debug level log to info level since GAE/J cannot set logging level to debug or below.  
 * For detail refer to http://code.google.com/p/googleappengine/issues/detail?id=4591
 */
public class Slim3PlatformTransactionManager extends AbstractPlatformTransactionManager {
	public static final int GAEJ_REQUEST_DEADLINE = 30; // seconds
	
	public Slim3PlatformTransactionManager() {
		super();
//		setTransactionSynchronization( SYNCHRONIZATION_NEVER);
		if ( isNestedTransactionAllowed()) setNestedTransactionAllowed( false);
			/* Assuring default value of nestedTransactionAllowed member field set to false, although 
			 * current design supports nested transaction by using nested begin-and-commit/rollback.
			 */
		if ( isValidateExistingTransaction()) setValidateExistingTransaction( false);
			/* Assuring default value of validateExistingTransaction member field set to false, although 
			 * current design ignores the read-only setting (since Slim3 does not support restricting 
			 * operation to read-only) and just logs warning for potential violation.
			 */
	}
	
	static class Slim3GlobalTransactionObject implements SmartTransactionObject {
		// Actual Slim3's GlobalTransaction object will be obtained via Slim3 Datastore class
		private String globalTransactionIdStr = null;
		public final String getGlobalTransactionIdStr() {
			return globalTransactionIdStr;
		}
		public final void setGlobalTransactionIdStr(String globalTransactionIdStr) {
			this.globalTransactionIdStr = globalTransactionIdStr;
		}
		
		public Slim3GlobalTransactionObject() {
		}
		
		public Slim3GlobalTransactionObject( String globalTransactionIdStr) {
			this.globalTransactionIdStr = globalTransactionIdStr;
		}
		
		// Implementation of SmartTransactionObject -----------------------------------------------
		@Override
		public void flush() { // Do nothing
		}
		
		private boolean rollbackOnly = false;
		@Override
		public boolean isRollbackOnly() {
			return rollbackOnly;
		}
		public void setRollbackOnly(boolean rollbackOnly) {
			this.rollbackOnly = rollbackOnly;
		}
		// ----------------------------------------------------------------------------------------
	} // static class Slim3GlobalTransactionObject implements SmartTransactionObject
	
	/** 
	 * If Slim3GlobalTransactionObject object corresponding to the particular GlobalTransaction ID string 
	 * does not exist in slim3GtxObjMapThreadLocal, then that means one of the following cases:
	 * 	- Slim3GlobalTransactionObject object has not been created for that GlobalTransaction object yet.
	 * 	- That GlobalTransaction object has already been successfully committed.
	 * 	- That GlobalTransaction object has failed on commit and either one of next cases happened:
	 * 		- Rolled back automatically by beneath Slim3 framework for 
	 * 		  ConcurrentModificationException or DeadlineExceededException.
	 * 		- GlobalTransaction object became inactive.
	 */
	protected static final ThreadLocal<Map<String, Slim3GlobalTransactionObject>> slim3GtxObjMapThreadLocal  
		= new ThreadLocal<Map<String, Slim3GlobalTransactionObject>>() {
			@Override
			protected Map<String, Slim3GlobalTransactionObject> initialValue() {
				return Collections.synchronizedMap( new HashMap<String, Slim3GlobalTransactionObject>());
			}
		};
	
//TODO Need to review the timing of each statement calling sweepSlim3GtxObjMapThreadLocal method
	/**
	 * Clean up elements in slim3GtxObjMapThreadLocal member field to only ones corresponding to 
	 * active GlobalTransaction instances in order to prevent memory leak. 
	 * Thereby, caution needs to be required to call this. There will be timing that GlobalTransaction 
	 * instances is inactive but the transaction on that GlobalTransaction instances is not completed yet. 
	 * For an example, GlobalTransaction object may become inactive after encountering 
	 * ConcurrentModificationException exception at its commit before corresponding transaction becomes  
	 * complete state after roll back attempt for that exception. 
	 */
	protected void sweepSlim3GtxObjMapThreadLocal() {
		Collection<GlobalTransaction> globalTransactions 
		= Collections.synchronizedCollection( Datastore.getActiveGlobalTransactions());
		Set<String> gtxIdSet = new HashSet<String>();
		for( GlobalTransaction gtxObj : globalTransactions) {
			gtxIdSet.add( gtxObj.getId());
		} // for
		synchronized( slim3GtxObjMapThreadLocal) {
			if ( gtxIdSet.size() > 0) {
//TODO I need to test the case that slim3GtxObjMapThreadLocal contains less elements than gtxIdSet
				slim3GtxObjMapThreadLocal.get().keySet().retainAll( gtxIdSet);
			}
			else {
				slim3GtxObjMapThreadLocal.get().clear();
			}
		} // synchronized
	} // protected void sweepSlim3GtxObjMapThreadLocal()
	
	protected enum GlobalTransactionState {
		GlobalTransactionInstance,
		ActiveGlobalTransaction,
		CurrentGlobalTransaction
	}
	
	protected Set<GlobalTransactionState> getGlobalTransactionStates( 
			final GlobalTransaction globalTransaction)
	{
		Set<GlobalTransactionState> gtxStateSet = new HashSet<GlobalTransactionState>();
		
		if ( globalTransaction instanceof GlobalTransaction) {
			gtxStateSet.add( GlobalTransactionState.GlobalTransactionInstance);
			
			GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
			if (gtx instanceof GlobalTransaction) {
				if ( globalTransaction.getId().equals( gtx.getId())) {
					gtxStateSet.add( GlobalTransactionState.CurrentGlobalTransaction);
				}
			}
			
			if ( globalTransaction.isActive()) {
				gtxStateSet.add( GlobalTransactionState.ActiveGlobalTransaction);
			}
		}
		
		return gtxStateSet;
	} // protected Set<GlobalTransactionState> getGlobalTransactionStates( final GlobalTransaction globalTransaction)
	
	protected void validateIsolationLevel( final int isolationLevel) throws TransactionException {
		if ( 
				( isolationLevel != TransactionDefinition.ISOLATION_DEFAULT) && 
				( isolationLevel != TransactionDefinition.ISOLATION_SERIALIZABLE)
			)
		{
			throw new InvalidIsolationLevelException( 
					String.format( 
							"%1$d is not among the supported isolation levels; " +
							"only ISOLATION_DEFAULT (%2$d) and ISOLATION_SERIALIZABLE (%3$d) are supported.", 
							isolationLevel, 
							TransactionDefinition.ISOLATION_DEFAULT, 
							TransactionDefinition.ISOLATION_SERIALIZABLE
							)
					);
		}
	} // protected void validateIsolationLevel( final int isolationLevel) throws TransactionException
	
	protected void setSlim3AsnyncTimeout( final int timeoutToUse) throws TransactionException {
		if ( timeoutToUse == TransactionDefinition.TIMEOUT_DEFAULT) {
			Datastore.deadline( null);
		}
		else if ( (timeoutToUse > 0) || (timeoutToUse < GAEJ_REQUEST_DEADLINE)) {
			Datastore.deadline( (new Integer( timeoutToUse)).doubleValue());
		}
		else {
			throw new InvalidTimeoutException( 
					String.format( 
							"The specified time-out value for the transaction is not valid; " +
							"it should be bigger than 0[sec] and less than %1$d[sec].", 
							GAEJ_REQUEST_DEADLINE
							), 
					timeoutToUse
					);
		}
	} // protected void setSlim3AsnyncTimeout( final int timeoutToUse) throws TransactionException
	
	
	/* Although doBegin method will be called right after newTransactionStatus method, transaction Object 
	 * argument does not refer to the GlobalTransaction instance newly created by newTransactionStatus 
	 * method but refer to the GlobalTransaction instance available before newTransactionStatus method. 
	 * 
	 * doBegin method became doing nothing but sanity check on status of transaction Object local field.
	 * 
	 * As AbstractPlatformTransactionManager class of Spring ver 3.0.4, possible propagation behaviors 
	 * that will reach the doBegin method are:
	 * 	- TransactionDefinition.PROPAGATION_REQUIRED
	 * 	- TransactionDefinition.PROPAGATION_REQUIRES_NEW
	 * 	- TransactionDefinition.PROPAGATION_NESTED 
	 * (non-Javadoc)
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#doBegin(java.lang.Object, org.springframework.transaction.TransactionDefinition)
	 */
	@Override
	protected void doBegin( Object transaction, TransactionDefinition definition) 
		throws TransactionException 
	{
		// Validation on transaction argument -----------------------------------------------------
		/* transaction Object should be either 
		 * 		null
		 * 			either TransactionDefinition.PROPAGATION_REQUIRED, 
		 * 			TransactionDefinition.PROPAGATION_REQUIRES_NEW or TransactionDefinition.PROPAGATION_NESTED
		 * 		active GlobalTransactoin object (one before current one)
		 * 			either TransactionDefinition.PROPAGATION_REQUIRES_NEW or 
		 * 			TransactionDefinition.PROPAGATION_NESTED
		 * 				if TransactionDefinition.PROPAGATION_NESTED case, then 
		 * 				useSavepointForNestedTransaction method returns false
		 */
		GlobalTransaction gtx = (GlobalTransaction)transaction;
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( gtx);
		GlobalTransaction currentGtx = Datastore.getCurrentGlobalTransaction();
		Set<GlobalTransactionState> currentGtxStateSet = getGlobalTransactionStates( currentGtx);
		if ( !currentGtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)) {
			throw new IllegalTransactionStateException(
					String.format(
							"Unexpected system state: getCurrentGlobalTransaction method of Slim3 Datastore " +
							"returned inactive GlobalTransaction instance (ID:%3$s). Expected it to return " +
							"active GlobalTransaction instance as new GlobalTransaction instance has begun " +
							"by newTransactionStatus method.", 
							(currentGtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? currentGtx.getId() : "null")
							)
					);
		}
		String gtxIdStr 
		= (gtxStateSet.contains(GlobalTransactionState.GlobalTransactionInstance) ? gtx.getId() : "null");
		if ( gtxIdStr.equals( currentGtx.getId())) {
			throw new IllegalTransactionStateException(
					String.format(
							"Unexpected system state: the transaction Object argument refers to current " +
							"active GlobalTransaction instance (ID:%1$s). Expected it not to refer to " +
							"current active GlobalTransaction instance rather refer to the GlobalTransaction " +
							"instance available before newTransaction method execution or hold null value.", 
							gtxIdStr
							)
					);
		}
		
		if ( !gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
			switch( definition.getPropagationBehavior()) { 
			case TransactionDefinition.PROPAGATION_REQUIRED:
			case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
			case TransactionDefinition.PROPAGATION_NESTED:
				break;
			default:
				throw new IllegalTransactionStateException(
						String.format(
								"Unexpected system state: found that the %1$s propagation behavior (%2$d) was " +
								"specified when the transaction Object argument holds null value. Expected " +
								"propagation behavior to be either PROPAGATION_REQUIRED (%3$d), " +
								"PROPAGATION_REQUIRES_NEW (%4$d) or PROPAGATION_NESTED (%5$d) when the " +
								"transaction Object argument holds null value.",
								getPropagationBehaviorStr( definition.getPropagationBehavior()), 
								definition.getPropagationBehavior(), 
								TransactionDefinition.PROPAGATION_REQUIRED, 
								TransactionDefinition.PROPAGATION_REQUIRES_NEW, 
								TransactionDefinition.PROPAGATION_NESTED
								)
						);
			} // switch
		}
		else if ( gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)) {
			switch( definition.getPropagationBehavior()) { 
			case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
			case TransactionDefinition.PROPAGATION_NESTED:
				break;
			default:
				throw new IllegalTransactionStateException(
						String.format(
								"Unexpected system state: found that the %1$s propagation behavior (%2$d) was " +
								"specified when the transaction Object argument holds active " +
								"GlobalTransaction instance (ID:%3$s). Expected propagation behavior to be " +
								"either PROPAGATION_REQUIRES_NEW (%4$d) or PROPAGATION_NESTED (%5$d) when " +
								"the transaction Object argument holds active GlobalTransaction instance.",
								getPropagationBehaviorStr( definition.getPropagationBehavior()), 
								definition.getPropagationBehavior(), 
								gtx.getId(), 
								TransactionDefinition.PROPAGATION_REQUIRES_NEW, 
								TransactionDefinition.PROPAGATION_NESTED
								)
						);
			} // switch
		}
		else {
			throw new IllegalTransactionStateException(
					String.format(
							"Unexpected system state: the transaction Object argument holds inactive " +
							"GlobalTransaction instance (ID:%1$s). Expected it to hold either null or " +
							"active GlobalTransaction instance.", 
							gtx.getId()
							)
					);
		}
		// ----------------------------------------------------------------------------------------
	} // protected void doBegin( Object transaction, TransactionDefinition transactionDefinition)

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.support.AbstractPlatformTransactionManager#doCommit(org.springframework.transaction.support.DefaultTransactionStatus)
	 */
	@Override
	protected void doCommit(DefaultTransactionStatus defaultTransactionStatus) throws TransactionException 
	{
		GlobalTransaction gtx = (GlobalTransaction)defaultTransactionStatus.getTransaction();
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( gtx);
		
		// Sanity check on precondition -----------------------------------------------------------
		/*
		 * This will be called at outermost transaction boundary (defaultTransactionStatus.isNewTransaction() 
		 * will return true). 
		 * When defaultTransactionStatus has been set for roll back, roll back doesn't need to be handled 
		 * within doCommit method. 
		 * 	When either defaultTransactionStatus's isGlobalRollbackOnly and shouldCommitOnGlobalRollbackOnly 
		 * 	returns true or its isLocalRollbackOnly returns true, then logic flow won't reach here and 
		 * 	roll back should have been performed by doRollback.
		 */
		if ( defaultTransactionStatus.isRollbackOnly()) {
			throw new TransactionSystemException( 
					String.format( 
							"Unexpected system state: the transaction for the GlobalTransaction " +
							"instance (ID:%1$s) has been marked as roll-back only " +
							"(LocalRollbackOnly:%2$b, GlobalRollbackOnly:%3$b).", 
							(gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? 
									gtx.getId() : "null"), 
							defaultTransactionStatus.isLocalRollbackOnly(), 
							defaultTransactionStatus.isGlobalRollbackOnly()
							)
					);
		}
		if ( !defaultTransactionStatus.isNewTransaction()) {
			throw new TransactionSystemException(
					String.format(
							"Unexpected system state: attempted to commit from participation of an existing " +
							"transaction (of which GlobalTransacion Id is %1$s).", 
							(gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? 
									gtx.getId() : "null") 
							)
					);
		}
		// ----------------------------------------------------------------------------------------
		
		// Sanity check on gtx GlobalTransaction instance -----------------------------------------
		if ( !gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)) {
			String message;
			
			if ( !gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
				message = "Unexpected system state: The GlobalTransaction object passed as the transaction " +
						"Object argument is null."; 
			}
			else {
				message = String.format(
						"Unexpected system state: The GlobalTransaction instance (ID:%1$s) passed as the " +
						"transaction Object argument is inactive.",
						gtx.getId()
						);
			}
			
			throw new IllegalTransactionStateException( message);
		}
		if ( !gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction)) {
			/* The one possible case of the control flow reaching here is that GlobalTransaction object 
			 * is manually instantiated at the outside of Spring transaction framework, and it is left active.
			 * 
			 * In nesting global transaction, easy to yield ConcurrentModificationException without care.
			 * Nesting global transaction should be less necessary, since Slim3 isolate between (Global) 
			 * transactions and cannot change that isolation setting, and this uses Slim3 GlobalTransaction, 
			 * as GAE/J datastore API is not provide suspend feature as well.
			 */
			
			GlobalTransaction currentGtx = Datastore.getCurrentGlobalTransaction();
			if ( logger.isWarnEnabled()) {
				logger.warn( 
						String.format(
								"Though active GlobalTransaction instance (ID:%1$s) is passed as transaction " +
								"Object argument, it's not current GlobalTransaction instance (ID:%2$s).", 
								gtx.getId(), 
								((currentGtx instanceof GlobalTransaction) ? currentGtx.getId() : "null")
								)
						);
			}
		}
		// ----------------------------------------------------------------------------------------
		boolean exceptionUp = false;
		try {
			gtx.commit();
			slim3GtxObjMapThreadLocal.get().remove( gtx.getId());
			if ( logger.isInfoEnabled()) { 
				logger.info( 
						String.format( 
								"Slim3 GlobalTransaction (ID:%1$s) committed.", 
								gtx.getId()
								)
						);
			}
		}
		catch( Throwable throwable) {
			/* Set rollback only flag so calling processRollback method to roll back will occur at 
			 * commit method by AnnotationTransactionAspect, even for other exceptions than ones specified 
			 * for rollbackFor or rollbackForClassname attributes of @Transactional annotation.
			 */
			defaultTransactionStatus.setRollbackOnly();
			
			exceptionUp = true;
			
			String message 
				= String.format(
						"Slim3 GlobalTransaction (ID:%1$s) failed on commit.",
						gtx.getId()
						);
			if ( 
					(throwable instanceof ConcurrentModificationException) || 
					(throwable instanceof DeadlineExceededException)) 
			{
				/* Slim3 should have already rolled back automatically on either
				 * ConcurrentModificationException or DeadlineExceededException.
				 * 
				 * About DeadlineExceededException case: It seems like no harm
				 * will be made even active global transaction at
				 * DeadlineExceededException case left behind. As looking
				 * briefly through the Slim3 source codes, the locks by that
				 * global transaction seems to be released while it rolls back
				 * by DatastoreFilter, and itself seem to be removed from
				 * TheadLocal stack of the global transactions. So, it won't be
				 * returned by such Datastore.getCurrentGlobalTransaction()
				 * method. See at
				 * http://groups.google.com/group/slim3-user/browse_frm
				 * /thread/e3d47d8f28c8e8d3
				 * /9e43553f3b56d1f2?tvc=1#9e43553f3b56d1f2 If it turns out
				 * opposite, then that active global transaction can be cleared
				 * some how at here for DeadlineExceededException case.
				 */

				message 
				= String.format(
								"Slim3 GlobalTransaction (ID:%1$s) failed on commit. %n"
								+ "Slim3 must have already rolled back automatically behind " 
								+ "Spring transaction framework.",
								gtx.getId()
								);
			}
			
			throw new TransactionSystemException( message, throwable);
		} 
		finally {
			if ( gtx.isActive()) {
				if ( exceptionUp) {
					if ( logger.isInfoEnabled()) {
						logger.info( 
								String.format( 
										"GlobalTransaction instance (ID:%1$s) remains active after exception " +
										"durring commit attempt. That GlobalTransaction instance has not " +
										"been removed yet from slim3GtxObjMapThreadLocal member field.", 
										gtx.getId(), 
										this.getClass().getName()
										)
								);
					}
				}
				else {
					if ( logger.isWarnEnabled()) {
						logger.warn( 
								String.format( 
										"Unexpected system state: GlobalTransaction instance (ID:%1$s) " +
										"remains active even after commit attempt. That GlobalTransaction " +
										"instance was removed from slim3GtxObjMapThreadLocal member field.", 
										gtx.getId(), 
										this.getClass().getName()
										)
								);
					}
				}
			}
		}
	} // protected void doCommit(DefaultTransactionStatus defaultTransactionStatus)

	/*
	 * (non-Javadoc) Returns object containing information about existing
	 * transaction what has already started before the current getTransaction
	 * call.
	 * 
	 * @see
	 * org.springframework.transaction.support.AbstractPlatformTransactionManager#doGetTransaction() 
	 * 
	 * doGetTransaction method will be called from getTransaction method what will return 
	 * the currently active transaction according to Spring's PlatformTransactionManager document.
	 */
	@Override
	protected Object doGetTransaction() throws TransactionException {
		GlobalTransaction currentGtx = Datastore.getCurrentGlobalTransaction();
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( currentGtx);
		if ( !gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)) {
			return null;
		}
		Slim3GlobalTransactionObject gtxObj = slim3GtxObjMapThreadLocal.get().get( currentGtx.getId());
		if ( gtxObj == null) {
			/* Case that current GlobalTransaction instance has been instantiated manually outside of 
			 * Spring transaction framework.
			 */
			if ( logger.isWarnEnabled()) {
				logger.warn( 
						String.format(
								"GlobalTransaction instance (ID:%1$s), what was given by Slim3 as current " +
								"GlobalTransaction instance, is not among ones managed by %2$s.", 
								currentGtx.getId(), 
								this.getClass().getName()
								)
						);
			}
			return null;
		}
		
		return currentGtx;
	} // protected Object doGetTransaction() throws TransactionException

	protected String getPropagationBehaviorStr( final int propagationBehavior) {
		String propagationBehaviorStr;
		switch( propagationBehavior) {
		case TransactionAttribute.PROPAGATION_MANDATORY:
			propagationBehaviorStr = "PROPAGATION_MANDATORY";
			break;
		case TransactionAttribute.PROPAGATION_NESTED:
			propagationBehaviorStr = "PROPAGATION_NESTED";
			break;
		case TransactionAttribute.PROPAGATION_NEVER:
			propagationBehaviorStr = "PROPAGATION_NEVER";
			break;
		case TransactionAttribute.PROPAGATION_NOT_SUPPORTED:
			propagationBehaviorStr = "PROPAGATION_NOT_SUPPORTED";
			break;
		case TransactionAttribute.PROPAGATION_REQUIRED:
			propagationBehaviorStr = "PROPAGATION_REQUIRED";
			break;
		case TransactionAttribute.PROPAGATION_REQUIRES_NEW:
			propagationBehaviorStr = "PROPAGATION_REQUIRES_NEW";
			break;
		case TransactionAttribute.PROPAGATION_SUPPORTS:
			propagationBehaviorStr = "PROPAGATION_SUPPORTS";
			break;
		default:
			propagationBehaviorStr = "Unkown";
		} // switch
		
		return propagationBehaviorStr;
	} // protected String getPropagationBehaviorStr( int propagationBehavior)
	
	protected GlobalTransaction beginGlogalTransaction( 
			GlobalTransaction gtx, Set<GlobalTransactionState> gtxStateSet, TransactionDefinition definition) 
	throws CannotCreateTransactionException 
	{
		setSlim3AsnyncTimeout( definition.getTimeout());
		
		GlobalTransaction newGtx;
		try {
			newGtx = Datastore.beginGlobalTransaction();
		}
		catch( Throwable throwable) {
			String message;
			if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
				message 
				= String.format(
						"Failure to begin new Slim3 GlobalTransaction instance under %1$s propagation " +
						"behavior (%2$d) over the transaction what has already begun GlobalTransaction " +
						"instance (ID:%3$s)", 
						getPropagationBehaviorStr( definition.getPropagationBehavior()), 
						definition.getPropagationBehavior(),
						gtx.getId()
						);
			}
			else {
				message 
				= String.format( 
						"Failure to begin new Slim3 GlobalTransaction instance under " +
						"%1$s propergation behavior (%2$d)", 
						getPropagationBehaviorStr( definition.getPropagationBehavior()), 
						definition.getPropagationBehavior()
						);					
			}
			throw new CannotCreateTransactionException( message, throwable);
		}
		
		
		String newGtxIdStr = newGtx.getId();
		
		synchronized( slim3GtxObjMapThreadLocal) {
			Slim3GlobalTransactionObject gtxObj = new Slim3GlobalTransactionObject( newGtxIdStr);
			slim3GtxObjMapThreadLocal.get().put( newGtxIdStr, gtxObj);
		} // synchronized
		
		if ( logger.isInfoEnabled()) {
			logger.info( 
					String.format( 
							"Slim3 GlobalTransaction (ID:%1$s) began successfully under " +
							"%2$s propergation behavior (%3$d).", 
							newGtx.getId(), 
							getPropagationBehaviorStr( definition.getPropagationBehavior()), 
							definition.getPropagationBehavior()
							)
					);
		}
		
		return newGtx;
	} // protected GlobalTransaction beginGlogalTransaction( ...)
	
	protected boolean validateCurrentActiveGlobalTransactionToParticipate( 
			final GlobalTransaction gtx, final TransactionDefinition definition
			) throws IllegalTransactionStateException 
	{
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( gtx);
		if ( 
				( !gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)) || 
				( !gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction)) 
			)
		{
			throw new IllegalTransactionStateException( 
					String.format( 
							"Though %1$s propergation behavior (%2$d) was specified, " +
							"the GlobalTransaction instance (ID:%3$s) (what has been begun this " +
							"transaction before) is %4$s %5$s one. Expected it to be current active " +
							"one.", 
							getPropagationBehaviorStr( definition.getPropagationBehavior()), 
							definition.getPropagationBehavior(), 
							gtx.getId(),
							(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"),
							(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive")
							)
					);
		}
		
		return true;
	} // protected boolean validateCurrentActiveGlobalTransactionToParticipate( TransactionDefinition definition)
	
	@Override
	protected DefaultTransactionStatus newTransactionStatus(
			TransactionDefinition definition, Object transaction, boolean newTransaction,
			boolean newSynchronization, boolean debug, Object suspendedResources) 
	{
		// Validate isolation level and time-out settings 
		validateIsolationLevel( definition.getIsolationLevel());
		
		boolean actualNewSynchronization 
		= newSynchronization && !TransactionSynchronizationManager.isSynchronizationActive();
		
		GlobalTransaction gtx = (GlobalTransaction)transaction;
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( gtx);
		
		GlobalTransaction newGtx;
		
		switch( definition.getPropagationBehavior()) {
		case TransactionAttribute.PROPAGATION_NESTED:
			if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
				if ( !isNestedTransactionAllowed()) {
					throw new TransactionSystemException(
							String.format( 
									"Unexpected system state: the value of nestedTransactionAllowed boolean " +
									"member field is false. It should have been checked early as true within " +
									"handleExistingTransaction method, in order to provide the nested " +
									"propagation behavior over the transaction (what has already begun " +
									"GlobalTransaction instance (ID:%1$s)) by nested begin and commit/rollback calls.",
									(gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? gtx.getId() : "null")
									)
							);
				}
				if ( logger.isInfoEnabled()) {
					logger.info( 
							String.format(
									"Going to provide nested propagation behavior by nested begin and " +
									"commit/rollback calls on new GlobalTransaction instance over the " +
									"transaction what has already begun GlobalTransaction instance (ID:%1$s).", 
									gtx.getId()
									)
							);
				}
			}
			// Enter to below logic (for TransactionAttribute.PROPAGATION_REQUIRES_NEW case)
		case TransactionAttribute.PROPAGATION_REQUIRES_NEW:
			// Sanity check on system state
			if ( !newTransaction) {
				throw new IllegalTransactionStateException( 
						String.format(
								"Unexpected system state: the value of newTransaction boolean member field " +
								"is false for %1$s propagation behavior (%2$d). Exptected it to be true.", 
								getPropagationBehaviorStr( definition.getPropagationBehavior()), 
								definition.getPropagationBehavior()
								)
						);
			}
				
			// Sanity check on current GlobalTransaction instances
			if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
				validateCurrentActiveGlobalTransactionToParticipate( gtx, definition);
			}
			
			// begin new GlobalTransaction
			newGtx = beginGlogalTransaction( gtx, gtxStateSet, definition);
			
			return new DefaultTransactionStatus(
					newGtx, newTransaction, actualNewSynchronization,
					definition.isReadOnly(), debug, suspendedResources
					);
		case TransactionAttribute.PROPAGATION_REQUIRED:
			if ( newTransaction) {
				// Sanity check on current GlobalTransaction instances
				if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
					throw new IllegalTransactionStateException( 
							String.format(
									"Unexpected system state: the value of newTransaction boolean member " +
									"field is true what direct to create new transaction for %1$s " +
									"propagation behavior (%2$d) though this transaction has already begun " +
									"the GlobalTransaction instance (ID:%3$s). Exptected it to be false and " +
									"participate to the existing transaction.", 
									getPropagationBehaviorStr( definition.getPropagationBehavior()), 
									definition.getPropagationBehavior(), 
									gtx.getId()
									)
							);
				}
				
				// begin new GlobalTransaction
				newGtx = beginGlogalTransaction( gtx, gtxStateSet, definition);
				
				return new DefaultTransactionStatus(
						newGtx, newTransaction, actualNewSynchronization,
						definition.isReadOnly(), debug, suspendedResources
						);
			}
			else {
				// Sanity check on current GlobalTransaction instances
				validateCurrentActiveGlobalTransactionToParticipate( gtx, definition);
				
				return new DefaultTransactionStatus(
						gtx, newTransaction, actualNewSynchronization,
						definition.isReadOnly(), debug, suspendedResources
						);
			}
		case TransactionAttribute.PROPAGATION_NEVER:
			if ( newTransaction && !gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
				return new DefaultTransactionStatus(
						null, newTransaction, actualNewSynchronization,
						definition.isReadOnly(), debug, suspendedResources
						);
			}
			else {
				throw new IllegalTransactionStateException( 
						String.format(
								"Unexpected system state: for %1$s propagation behavior (%2$d), the value of " +
								"newTransaction boolean member field is expected to be true (actual: %3$b) " +
								"and transaction Object argument is expected to hold current active " +
								"GlobalTransaction instance (actual: %4$s %5$s one (ID:%6$s)).", 
								getPropagationBehaviorStr( definition.getPropagationBehavior()), 
								definition.getPropagationBehavior(), 
								newTransaction, 
								(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"), 
								(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive"),
								(gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? gtx.getId() : "null")
								)
						);
			}
		case TransactionAttribute.PROPAGATION_NOT_SUPPORTED:
			if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
				throw new IllegalTransactionStateException( 
						String.format( 
								"Unexpected system state: for %1$s propagation behavior (%2$d), the " +
								"transaction Object argument is expected to hold null value (actual: " +
								"%3$s %4$s GlobalTransaction instance (ID:%5$s)).",
								getPropagationBehaviorStr( definition.getPropagationBehavior()), 
								definition.getPropagationBehavior(), 
								(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"), 
								(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive"),
								gtx.getId()
								)
						);
			}
			
			// Create DeafultTransactionState with null transaction
			return new DefaultTransactionStatus(
					null, newTransaction, actualNewSynchronization,
					definition.isReadOnly(), debug, suspendedResources
					);
		case TransactionAttribute.PROPAGATION_SUPPORTS:
			if ( newTransaction) {
				if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
					throw new IllegalTransactionStateException( 
							String.format(
									"Unexpected system state: for %1$s propagation behavior (%2$d), when the " +
									"newTransaction is true, the transaction Object argument is expected to " +
									"hold null value (actual: %3$s %4$s GlobalTransaction instance (ID:%5$s)).",
									getPropagationBehaviorStr( definition.getPropagationBehavior()), 
									definition.getPropagationBehavior(), 
									(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"), 
									(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive"),
									gtx.getId()
									)
							);
				}
				return new DefaultTransactionStatus(
						null, newTransaction, actualNewSynchronization,
						definition.isReadOnly(), debug, suspendedResources
						);
			}
			else {
				if ( 
						!gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) || 
						!gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction)
						)
				{
					throw new IllegalTransactionStateException( 
							String.format(
									"Unexpected system state: for %1$s propagation behavior (%2$d), when the " +
									"newTransaction is false, the transaction Object argument is expected to " +
									"hold current active GlobalTransaction instance (actual: %3$s %4$s " +
									"GlobalTransaction instance (ID:%5$s)).",
									getPropagationBehaviorStr( definition.getPropagationBehavior()), 
									definition.getPropagationBehavior(), 
									(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"), 
									(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive"),
									(gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? gtx.getId() : "null")
									)
							);
				}
				return new DefaultTransactionStatus(
						gtx, newTransaction, actualNewSynchronization,
						definition.isReadOnly(), debug, suspendedResources
						);
			}
			
		case TransactionAttribute.PROPAGATION_MANDATORY:
			if ( 
					newTransaction || 
					!gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) || 
					!gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction)
					) 
			{
				throw new IllegalTransactionStateException( 
						String.format( 
								"Unexpected system state: for %1$s propagation behavior (%2$d), the " +
								"newTransaction is expected to be false (actual: %3$b) and the transaction " +
								"Object argument is expected to hold current active GlobalTransaction " +
								"instance (actual: %3$s %4$s GlobalTransaction instance (ID:%5$s)).",
								getPropagationBehaviorStr( definition.getPropagationBehavior()), 
								definition.getPropagationBehavior(), 
								(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"), 
								(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive"),
								(gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance) ? gtx.getId() : "null")
								)
						);
			}
			
			return new DefaultTransactionStatus(
					gtx, newTransaction, actualNewSynchronization,
					definition.isReadOnly(), debug, suspendedResources
					);
			
		default:
			throw new IllegalTransactionStateException(
					String.format(
							"Unknown propagation behavior (%1$d) is specified. Supported propagation " +
							"behaviors is either PROPAGATION_MANDATORY (%2$d), PROPAGATION_NESTED (%3$d), " +
							"PROPAGATION_NEVER (%4$d), PROPAGATION_NOT_SUPPORTED (%5$d), " +
							"PROPAGATION_REQUIRED (%6$d), PROPAGATION_REQUIRES_NEW (%7$d), " +
							"or PROPAGATION_SUPPORTS (%8$d).",
							definition.getPropagationBehavior(),
							TransactionAttribute.PROPAGATION_MANDATORY, 
							TransactionAttribute.PROPAGATION_NESTED, 
							TransactionAttribute.PROPAGATION_NEVER, 
							TransactionAttribute.PROPAGATION_NOT_SUPPORTED, 
							TransactionAttribute.PROPAGATION_REQUIRED, 
							TransactionAttribute.PROPAGATION_REQUIRES_NEW, 
							TransactionAttribute.PROPAGATION_SUPPORTS
							)
					);
			
		} // switch
	} // protected DefaultTransactionStatus newTransactionStatus( ......)
	
	/*
	 * isExistingTransaction method would be called by getTransaction method
	 * what will return the currently active transaction according to Spring's
	 * PlatformTransactionManager document.
	 * 
	 * If this isExistingTransaction method is not called from other methods than getTransaction method, 
	 * then it can strip down to return false when transaction Object argument is null, and return true 
	 * when transaction Object argument is GlobalTransaction instance.
	 * 
	 * @see
	 * http://static.springsource.org/spring/docs/current/api/org/springframework
	 * /transaction/PlatformTransactionManager.html#getTransaction%28org.
	 * springframework.transaction.TransactionDefinition%29 I take that
	 * "the currently active transaction" means the inner-most active
	 * transaction. (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.transaction.support.AbstractPlatformTransactionManager
	 * #isExistingTransaction(java.lang.Object)
	 */
	@Override
	protected boolean isExistingTransaction( Object transaction) throws TransactionException {
		GlobalTransaction gtx = (GlobalTransaction)transaction;
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( gtx);
		if ( gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
			if ( slim3GtxObjMapThreadLocal.get().get( gtx.getId()) == null) {
				if ( logger.isWarnEnabled()) {
					logger.warn( 
							String.format(
									"GlobalTransaction instance (ID:%1$s), what is not among ones managed " +
									"by %2$s, was given as transaction Object argument.", 
									gtx.getId(), 
									this.getClass().getName()
									)
							);
				}
				
				return false;
			}
			else { // GlobalTransaction instance held by transaction Object argument is one managed by this
				if ( 
						!gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) || 
						!gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)
						) 
				{
					if ( logger.isWarnEnabled()) {
						logger.warn( 
								String.format(
										"GlobalTransaction instance (ID:%1$s) given as transaction Object " +
										"argument is %2$s %3$s instance.", 
										gtx.getId(), 
										(gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction) ? "current" : "not-current"), 
										(gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction) ? "active" : "inactive") 
										)
								);
					}
				}
				
				return true;
			}
		}
		
		return false;
	} // protected boolean isExistingTransaction( Object transaction) throws TransactionException

	/*
	 * Called by processRollback method or doRollbackOnCommitException method.
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.transaction.support.AbstractPlatformTransactionManager
	 * #doSetRollbackOnly(org.springframework.transaction.support.
	 * DefaultTransactionStatus)
	 */
	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException 
	{
		String message
		= String.format( "Setting roll-back-only flag(s) up:");
			
		/*
		 * Set AbstractTransactionStatus's rollbackOnly member field to true
		 * what will make DefaultTransactionStatus's isLocalRollbackOnly method
		 * returns true. AbstractPlatformTransactionManager's commit method
		 * initiate roll back when its isLocalRollbackOnly method returns true.
		 * 
		 * However, the analysis of the AbstractPlatformTransactionManager's
		 * logic flow makes me doubt calling this method will never occurs:
		 * 
		 * About the case called by processRollback method (that will be called
		 * by commit method): commit method calls processRollback method when
		 * DefaultTransactionStatus's isLocalRollbackOnly method returns true.
		 * DefaultTransactionStatus's isLocalRollbackOnly method is also called by
		 * processRollback method, in order for participating transaction to set
		 * rollbackOnly member field what is done by calling this
		 * doSetRollbackOnly method. Hence, it doesn't make much sense for this
		 * case since, in that logic only, this will never be called.
		 * 
		 * About the case called by doRollbackOnCommitException method (that
		 * will be called by processRollback method): When it's participating
		 * transaction and globalRollbackOnParticipationFailure flag value is
		 * true, then this method is called by doRollbackOnCommitException
		 * method. The default value of globalRollbackOnParticipationFailure
		 * flag is true what means that the transaction originator cannot make
		 * the transaction commit anymore once the transaction marked globally
		 * as rollback-only. However, doRollbackOnCommitException method is
		 * called by processCommit method and, according to Spring's
		 * AbstractPlatformTransactionManager document (@see
		 * http://static.springsource
		 * .org/spring/docs/current/api/org/springframework
		 * /transaction/support/AbstractPlatformTransactionManager
		 * .html#setRollbackOnCommitFailure%28boolean%29) the roll back on
		 * commit failure typically not necessary and thus to be avoided, as it
		 * can potentially override the commit exception with a subsequent
		 * rollback exception. I would take this as it means rather
		 * intentionally initiating roll back after recognizing commit failure
		 * is the recommended way than letting Spring automatically roll back
		 * after commit failure, however it does not make sense since, because
		 * commit is automatically handled by Spring, how it can know when and
		 * how to roll back. It should be automatically rolled back by Spring
		 * when failure occurs on the commit that Spring automatically performs.
		 */
		status.setRollbackOnly();
		message
		= message + String.format( "%n%tDefaultTransactionStatus object is marked as roll-back-only.");
		
		GlobalTransaction gtx = (GlobalTransaction) status.getTransaction();
		if ( gtx instanceof GlobalTransaction) {
			String gtxIdStr = gtx.getId();
			Slim3GlobalTransactionObject slim3GtxObj = slim3GtxObjMapThreadLocal.get().get( gtxIdStr);
			if (slim3GtxObj != null) {
				slim3GtxObj.setRollbackOnly( true);
				slim3GtxObjMapThreadLocal.get().put( gtxIdStr, slim3GtxObj);
				message = message 
				+ String.format( 
						"%n%tSlim3GlobalTransactionObject object for GlobalTransaction instance (ID:%1$s) " +
						"is marked as roll-back-only.", 
						gtxIdStr
						);
			}
			else {
				if ( logger.isWarnEnabled()) {
					logger.warn( 
							String.format(
									"Inconsistent system state: GlobalTransaction instance (ID:%1$s) held " +
									"by state DefaultTransactionStatus argument is not among ones managed " +
									"by %2$s.", 
									gtxIdStr, this.getClass().getName()
									)
							);
				}
			}
		}
		
		if ( logger.isInfoEnabled()) logger.info( message);
		
	} // protected void doSetRollbackOnly(DefaultTransactionStatus status)

	protected GlobalTransaction findGlobalTransaction( final String gtxIdStr) {
		Slim3GlobalTransactionObject gtxObj = slim3GtxObjMapThreadLocal.get().get( gtxIdStr);
		if ( gtxObj == null) return null;
		
		for( GlobalTransaction gtx : Datastore.getActiveGlobalTransactions()) {
			if ( gtxIdStr.equals( gtx.getId())) return gtx;
		} // for
		
		sweepSlim3GtxObjMapThreadLocal();
		return null;
	} // /protected GlobalTransaction findGlobalTransaction( final String gtxIdStr)
	
	/*
	 * (non-Javadoc)
	 * Called by processRollback method or doRollbackOnCommitException method
	 * 
	 * @see
	 * org.springframework.transaction.support.AbstractPlatformTransactionManager
	 * #
	 * doRollback(org.springframework.transaction.support.DefaultTransactionStatus
	 * )
	 */
	@Override
	protected void doRollback( DefaultTransactionStatus defaultTransactionStatus) throws TransactionException 
	{
		// Sanity check on precondition -----------------------------------------------------------
		/* When logic flow got here, it should be not the case of participating existing transaction that  
		 * means the target method is the outer most transactional method what began a transaction process 
		 * at the first, and that also means newTransaction boolean member field of DefaultTransactionStatus 
		 * object should be true.
		 */
		if ( !defaultTransactionStatus.isNewTransaction()) {
			// Could be caused by unaware code change on org.springframework.transaction.support.AbstractPlatformTransactionManager
			throw new TransactionSystemException(
					String.format( 
							"Unexpected system state: attempting to roll back from participation of an " +
							"existing transaction (of which GlobalTransacion Id is %1$s).", 
							((defaultTransactionStatus.getTransaction() instanceof GlobalTransaction) ? 
									((GlobalTransaction)defaultTransactionStatus.getTransaction()).getId() :
									"null"
									)
							)
					);
		}
		// ----------------------------------------------------------------------------------------
		
		// Check on GlobalTransaction instance ----------------------------------------------------
		GlobalTransaction gtx = (GlobalTransaction)defaultTransactionStatus.getTransaction();
		Set<GlobalTransactionState> gtxStateSet = getGlobalTransactionStates( gtx);
		if ( !gtxStateSet.contains( GlobalTransactionState.GlobalTransactionInstance)) {
			throw new TransactionSystemException( 
					"Inconsistent status: defaultTransactionStatus argument is not holding " +
					"GlobalTransaction instance."
					);
		}
		
		String gtxIdStr = gtx.getId();
		Map<String, Slim3GlobalTransactionObject> slim3GtxObjMap = slim3GtxObjMapThreadLocal.get();
		Slim3GlobalTransactionObject slim3GtxObj = slim3GtxObjMap.get( gtxIdStr);
		if ( slim3GtxObj == null) {
			if ( logger.isErrorEnabled()) {
				logger.error( 
						String.format( 
								"Encountered inconsistent status: GlobalTransaction instance (ID:%1$s) " +
								"held by defaultTransactionStatus argument is not among ones managed " +
								"by %2$s.", 
								gtxIdStr, 
								this.getClass().getName()
								)
						);
			}
		}
		
		if ( !gtxStateSet.contains( GlobalTransactionState.ActiveGlobalTransaction)) {
			/* Reminder: gtx here can be inactive by the ConcurrentModificationException case. 
			 * However, it's not 100% certain that, in ConcurrentModificationException case, 
			 * gtx here will be always inactive.
			 */
			
			slim3GtxObjMapThreadLocal.get().remove( gtxIdStr);
			
			if ( logger.isWarnEnabled()) {
				logger.warn(  
						String.format(
								"Skipping to perform roll back on GlobalTransaction (ID:%1$s) since " +
								"it has been already in inactive state.", 
								gtxIdStr
								)
						);
			}
			
			return;
		}
		
		
		if ( !( gtxStateSet.contains( GlobalTransactionState.CurrentGlobalTransaction))) {
			/* Changed from throwing TransactionSystemException to logging incident in order to 
			 * avoid leaving GlobalTransaction instance active as much as possible by proceeding further.
			 */
			GlobalTransaction currentGtx = Datastore.getCurrentGlobalTransaction();
			
			if ( logger.isErrorEnabled()) {
				logger.error( 
						String.format( 
								"Encountered inconsistent status: GlobalTransaction object (ID:%1$s) held " +
								"by defaultTransactionStatus argument is not current active GlobalTransaction " +
								"instance (ID:%2$s).", 
								gtxIdStr, 
								((currentGtx instanceof GlobalTransaction) ? currentGtx.getId() : "null") 
								)
						);
			}
		}
		// ----------------------------------------------------------------------------------------
		
		try {
			if ( logger.isInfoEnabled()) {
				logger.info(
						String.format( "Rolling back on Slim3 GlobalTransaction (ID:%1$s)", gtxIdStr)
						);
			}
			
			/* Note: There are the cases that GlobalTransaction instance has already been rolled back by 
			 * Slim3 behind scene of Spring transaction framework when commit failed and GlobalTransaction 
			 * instance remains active state. Even the control flow reached in those cases, there should be 
			 * no harm being made by rolling back here again. 
			 */
			gtx.rollback();
		}
		catch( Throwable throwable) {
			String message 
			= String.format( 
					"Rolling back on Slim3 GlobalTransaction (ID:%1$s) failed.", 
					gtxIdStr
					);
			throw new TransactionSystemException( message, throwable);
		}
		finally {
			slim3GtxObjMapThreadLocal.get().remove( gtxIdStr);
/* Not necessary to call defaultTransactionStatus.setCompleted method in this block because 
 * it will be called later in cleanupAfterCompletion method.
			defaultTransactionStatus.setCompleted();
 */
			if ( gtx.isActive()) {
				if ( logger.isErrorEnabled()) {
					logger.error( 
							String.format( 
									"Slim3 GlobalTransaction (ID:%1$s) remains active " +
									"after the attempt of rolling back.", 
									gtxIdStr
									)
							);
				}
			}
		}
	} // protected void doRollback(DefaultTransactionStatus defaultTransactionStatus)

	/*
	 * Cannot implements savepoint. (since jiql (Java JDBC wrapper for Google
	 * BigTable) doesn't also support savepoint, I think that GAE/J probably
	 * doesn't have the feature implemented.
	 * http://code.google.com/p/jiql/source
	 * /browse/trunk/jiql/source/org/jiql/jdbc/jiqlConnection.java?r=676 ) No
	 * savepoint support means nested transaction needs to be handled by nested
	 * begin and commit/rollback calls if going to support nested transaction.
	 * JtaTransactionManager may be referenced for that matter.
	 * JtaTransactionManager causes a further invocation of doBegin despite an
	 * already existing transaction to support nested transaction. JTA
	 * implementations might support nested transactions via further
	 * UserTransaction.begin() invocations.
	 * 
	 * Decided not to support nested transaction. Come to think of it, it's not
	 * necessary to support nested transaction as long as global transaction is
	 * provided. Matter of fact, nested transaction with Slim3 easily encounters
	 * ConcurrentModificationException and not worthy to support.
	 */
	@Override
	protected boolean useSavepointForNestedTransaction() {
		return false;
	}
}
