package slingong.web.gae;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.newmainsoftech.aspectjutil.eventmanager.label.EventListener;
import com.newmainsoftech.aspectjutil.eventmanager.label.EventTrigger;

/**
 * Helper class for synchronization among TransactionalMemcacheServiceImpl instances. <br />
 * Because this uses mainly static member fields for tasks and only differences among multiple instances of 
 * this class can be value having set to maxLockAcquisitionDuration member field, multiple instances of this 
 * class are usually unnecessary. Didn't make this as singleton and consisted of all methods as static 
 * method, in order to have more scalability by programming to interface and have this more testable.
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@EventListener
class TransactionalMemcacheServiceSyncHelperImpl implements TransactionalMemcacheServiceSyncHelper  {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	protected static Map<String, Set<Serializable>> keysMap 
	= new ConcurrentHashMap<String, Set<Serializable>>();
	
		/**
		 * <b>Thread safety:</b> thread safe. 
		 * @param memcacheNamespace
		 * @return
		 */
		@Override
		public Set<Serializable> getKeySet( final String memcacheNamespace) {
			Set<Serializable> keysSet 
			= TransactionalMemcacheServiceSyncHelperImpl.keysMap.get( memcacheNamespace);
			if ( keysSet == null) {
				keysSet = new CopyOnWriteArraySet<Serializable>();
				TransactionalMemcacheServiceSyncHelperImpl.keysMap.put( memcacheNamespace, keysSet);
			}
			return keysSet;
		} // public Set<Serializable> getKeySet( final String memcacheNamespace)

		
	// For handling clearAll method execution -----------------------------------------------------	
	/**
	 * clearAll method for non-transaction mode. <br />
	 * clear whole memcache and TransactionalMemcacheServiceSyncHelperImpl.keysMap after locking out  
	 * the execution of all methods of MemcacheService interface and after confirming the termination of 
	 * already-running methods of MemcacheService interface.
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when this is 
	 * called in transaction mode. <br />
	 * @throws TransactionalMemcacheServiceException when could not lock out further execution of methods of 
	 * MemcacheService interface via TransactionalMemcacheServiceImpl instances in non-transaction mode. <br />
	 * @throws TransactionalMemcacheServiceException when failed to confirm termination of already-running 
	 * methods of MemcacheService interface.
	 */
	@Override
	@EventTrigger( value=MethodCallLogEvent.class) // event for just logging
	public void clearAllForNonTransactionMode() {
		MemcacheServiceFactory.getMemcacheService().clearAll();
		TransactionalMemcacheServiceSyncHelperImpl.keysMap.clear();
	} // public void clearAllForNonTransactionMode()
	
	// --------------------------------------------------------------------------------------------
}
