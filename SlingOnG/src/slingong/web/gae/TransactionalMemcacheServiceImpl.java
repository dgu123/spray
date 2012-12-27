package slingong.web.gae;

import com.newmainsoftech.aspectjutil.eventmanager.label.EventListener;


/**
 * Note: During transaction mode, if exception is thrown by TransactionalMemcacheServiceImpl, 
 * then basically it is impossible to recover from it to export changes during transaction to 
 * memcache. 
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@EventListener
public class TransactionalMemcacheServiceImpl extends TransactionalMemcacheServiceBase {
	public final static String DefaultKeySetKey 
	= TransactionalMemcacheServiceImpl.class.getName() + ".keySetKey";
	
	// Constructors -------------------------------------------------------------------------------
	protected TransactionalMemcacheServiceImpl( 
			final String memcacheNamespace, 
			final String keySetKey, 
			final TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper, 
			final TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
			) 
	{
		super( 
				memcacheNamespace, 
				keySetKey, 
				transactionalMemcacheServiceTransactionHelper, 
				transactionalMemcacheServiceSyncHelper
				);
	}
	// --------------------------------------------------------------------------------------------
}
