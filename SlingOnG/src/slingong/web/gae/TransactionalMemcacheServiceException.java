package slingong.web.gae;

import com.newmainsoftech.aspectjutil.eventmanager.label.EventTrigger;

/**
 * Class of exception thrown by TransactionalMemcacheService implementation. <br />
 * TransactionalMemcacheServiceException extends RuntimeException. <br />
 * Wraps real cause of failure when there is such. 
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
public class TransactionalMemcacheServiceException extends RuntimeException {
	private static final long serialVersionUID = -20120124L;
	
	protected static TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper 
	= new TransactionalMemcacheServiceTransactionHelperImpl( 
			TransactionalMemcacheServiceCommonConstant.copier);
		public static TransactionalMemcacheServiceTransactionHelper getTransactionalMemcacheServiceTransactionHelper() {
			return transactionalMemcacheServiceTransactionHelper;
		}
		public static void setTransactionalMemcacheServiceTransactionHelper(
				TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper) {
			TransactionalMemcacheServiceException.transactionalMemcacheServiceTransactionHelper 
			= transactionalMemcacheServiceTransactionHelper;
		}

	/**
	 * Called by each constructor of TransactionalMemcacheServiceException object to perform the following:
	 * <ul>
	 * <li>Call TransactionalMemcacheServiceTransactionHelperImpl.clearTransactionResiduals method</li>
	 * <li>Trigger TransactionalMemcacheServiceException event listened by TransactionalMemcacheServiceImpl 
	 * instance to remove ThreadLocal object to avoid memory leak.</li>
	 * </ul>
	 */
	@EventTrigger( value = TransactionalMemcacheServiceException.class)
	protected void atConstructionBeforeBeingThrown() {
		TransactionalMemcacheServiceException
		.transactionalMemcacheServiceTransactionHelper.clearTransactionResiduals();
	}
	
	/**
	 * Construct TransactionalMemcacheServiceException runtime exception object with message specified by 
	 * message argument. 
	 * @param message
	 */
	public TransactionalMemcacheServiceException( String message) {
		super( message);
		atConstructionBeforeBeingThrown();
	}
	/**
	 * Construct TransactionalMemcacheServiceException runtime exception object wrapping throwable object.
	 * @param throwable
	 */
	public TransactionalMemcacheServiceException( Throwable throwable) {
		super( throwable);
		atConstructionBeforeBeingThrown();
	}
	/**
	 * Construct TransactionalMemcacheServiceException runtime exception object wrapping throwable object 
	 * with message specified by message argument.
	 * @param message
	 * @param throwable
	 */
	public TransactionalMemcacheServiceException( String message, Throwable throwable) {
		super( message, throwable);
		atConstructionBeforeBeingThrown();
	}
}
