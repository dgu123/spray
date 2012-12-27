package slingong.web.gae;

public class TransactionalMemcacheServiceCommonConstant {
	/**
	 * 1000 long value what can be used as default value of lock acquisition duration. 
	 * When unit is msec, value will be interpreted to 1sec.
	 */
	public static final long DefaultMaxLockAcquisitionDuration = 1000L; // msec
	
	/**
	 * 1000 long value what can be used as default value of waiting period for condition.
	 * When unit is msec, value will be interpreted to 1sec.
	 */
	public static final long DefaultMaxWaitDuration = 1000L; // msec
	
	public static final int DefaultMaxNotificationCount = 100;

	public static final Copier copier = new CopierImpl();
	
	public static final TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper 
	= new TransactionalMemcacheServiceTransactionHelperImpl( 
			TransactionalMemcacheServiceCommonConstant.copier);
	
	public static final TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper 
	= new TransactionalMemcacheServiceSyncHelperImpl();
	
	public static final MethodCallLogEvent methodCallLogEvent = new MethodCallLogEvent();
}
