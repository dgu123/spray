package slingong.web.gae;

import java.io.Serializable;
import java.util.Set;

public interface TransactionalMemcacheServiceSyncHelper {

	/**
	 * @param memcacheNamespace
	 * @return
	 */
	public Set<Serializable> getKeySet(final String memcacheNamespace); 

	// For handling clearAll method execution -----------------------------------------------------	
	/**
	 * clearAll method for non-transaction mode. <br />
	 * clear whole memcache and TransactionalMemcacheServiceSyncHelperImpl.keysMap after locking out  
	 * the execution of all methods of MemcacheService interface and after confirming the termination of 
	 * already-running methods of MemcacheService interface.
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when this is 
	 * called in transaction mode. <br />
	 * TransactionalMemcacheServiceException when could not lock out further execution of methods of 
	 * MemcacheService interface via TransactionalMemcacheServiceImpl instances in non-transaction mode. <br />
	 * TransactionalMemcacheServiceException when failed to confirm termination of already-running methods 
	 * of MemcacheService interface.
	 */
	public void clearAllForNonTransactionMode(); 

}