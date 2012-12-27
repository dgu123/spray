package slingong.web.gae;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl.ClearAllInTransactionEvent;
import slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl.NonTransactionModeEvent;
import slingong.web.gae.TransactionalMemcacheServiceTransactionHelperImpl.TransactionModeEvent;

import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.Stats;
import com.newmainsoftech.aspectjutil.eventmanager.EventInfo;
import com.newmainsoftech.aspectjutil.eventmanager.label.EventListener;
import com.newmainsoftech.aspectjutil.eventmanager.label.EventTrigger;
import com.newmainsoftech.aspectjutil.eventmanager.label.OnEvent;

//TODO Let all non-transaction parts' handling in MemcacheService interface method except get* methods process via Future asynchronously.  
@EventListener
public abstract class TransactionalMemcacheServiceBase extends TransactionalMemcacheServiceImplSkelton {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	protected long maxLockAcquisitionDuration 
	= TransactionalMemcacheServiceCommonConstant.DefaultMaxLockAcquisitionDuration;
		public long getMaxLockAcquisitionDuration() {
			return maxLockAcquisitionDuration;
		}
		public void setMaxLockAcquisitionDuration( long lockAcquisitionPeriod) {
			if ( lockAcquisitionPeriod < 1L) {
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"maxLockAcquisitionDuration long member field (current value: " 
									+ "%1$d [msec]) will be set to %2$d [msec] what is not " 
									+ "recommended value.",
									this.maxLockAcquisitionDuration, 
									lockAcquisitionPeriod
									)
							);
				}
			}
			
			this.maxLockAcquisitionDuration = lockAcquisitionPeriod;
		} // public void setLockAcquisitionDuration( long maxLockAcquisitionDuration)
	
	protected long maxWaitDuration 
	= TransactionalMemcacheServiceCommonConstant.DefaultMaxWaitDuration;
		public long getMaxWaitDuration() {
			return maxWaitDuration;
		}
		public void setMaxWaitDuration(long maxWaitPeriod) {
			if ( maxWaitPeriod < 1L) {
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"maxWaitDuration long member field (current value: %1$d [msec]) will " 
									+ "be set to %2$d [msec] what is not recommended value.",
									this.maxWaitDuration, 
									maxWaitPeriod
									)
							);
				}
			}
			
			this.maxWaitDuration = maxWaitPeriod;
		}

	protected int maxNotificationCount 
	= TransactionalMemcacheServiceCommonConstant.DefaultMaxNotificationCount;
		public int getMaxNotificationCount() {
			return maxNotificationCount;
		}
		public void setMaxNotificationCount(int maxNotificationCount) {
			if ( maxNotificationCount < 1L) {
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"maxNotificationCount int member field (current value: %1$d) " 
									+ "will be set to %2$d what is not recommended value.",
									this.maxNotificationCount, 
									maxNotificationCount
									)
							);
				}
			}
			
			this.maxNotificationCount = maxNotificationCount;
		}

	protected final MemcacheService mainCache;
		public final String memcacheNamespace;
		/**
		 * Being used to synchronize access to mainChache and keysSet entry in 
		 * keysMap static member field in TransactionalMemcacheServiceBase sub-class instance
		 */
		protected ReentrantLock mainCacheLock = new ReentrantLock();
		
		/**
		 * key for keysSet entry in mainCache <br />
		 */
		protected String keySetKey;
			public String getKeySetKey() {
				return keySetKey;
			}
			
			public void setKeySetKey( String newKeysSetKey) {
				if ( ( newKeysSetKey == null) || ( "".equals( newKeysSetKey))) {
					throw new TransactionalMemcacheServiceException(
							new IllegalArgumentException( 
									"keysSetKey argument cannot be null or emptry string.")
							);
				}
				
				String oldKeySetKey = getKeySetKey();
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( oldKeySetKey);
				if ( keySetInMainCache != null) {
					mainCache.put( newKeysSetKey, keySetInMainCache);
					mainCache.delete( oldKeySetKey);
				}
				this.keySetKey = newKeysSetKey;
			}
			
			protected ReentrantLock keySetKeyLock = new ReentrantLock();

			protected int keySetKeyLockingCounter = 0;
				protected ReentrantLock keySetKeyLockingCounterLock = new ReentrantLock();
				protected Condition keySetKeyLockingCounterLockCondition = keySetKeyLockingCounterLock.newCondition();
			
			
	protected static Copier copier = TransactionalMemcacheServiceCommonConstant.copier;
		protected static Copier getCopier() {
			return TransactionalMemcacheServiceBase.copier;
		}
		protected static void setCopier( Copier copier) {
			
			if ( copier == null) {
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException( "copier argument cannot be null")
						);
			}
			
			TransactionalMemcacheServiceBase.copier = copier;
		}

	protected static TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper 
	= TransactionalMemcacheServiceCommonConstant.transactionalMemcacheServiceTransactionHelper;
		protected static TransactionalMemcacheServiceTransactionHelper getTransactionalMemcacheServiceTransactionHelper() {
			return transactionalMemcacheServiceTransactionHelper;
		}
		protected static class SetTransactionalMemcacheServiceTransactionHelperEvent {
		}
		@EventTrigger( value=SetTransactionalMemcacheServiceTransactionHelperEvent.class)
		protected static void setTransactionalMemcacheServiceTransactionHelper(
				TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper) {
			
			if ( transactionalMemcacheServiceTransactionHelper == null) {
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException( 
								"transactionalMemcacheServiceTransactionHelper argument cannot be null."
								)
						);
			}
			
			TransactionalMemcacheServiceBase.transactionalMemcacheServiceTransactionHelper 
			= transactionalMemcacheServiceTransactionHelper;
		}

	protected static TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper 
	= TransactionalMemcacheServiceCommonConstant.transactionalMemcacheServiceSyncHelper;
		protected static TransactionalMemcacheServiceSyncHelper getTransactionalMemcacheServiceSyncHelper() {
			return transactionalMemcacheServiceSyncHelper;
		}

	protected static void setTransactionalMemcacheServiceHelpers(
			TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper,
			TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
			) 
	{
		if ( transactionalMemcacheServiceTransactionHelper != null) {
			TransactionalMemcacheServiceBase
				.setTransactionalMemcacheServiceTransactionHelper( 
						transactionalMemcacheServiceTransactionHelper);
		}
		if ( transactionalMemcacheServiceSyncHelper != null) {
			TransactionalMemcacheServiceBase.transactionalMemcacheServiceSyncHelper 
			= transactionalMemcacheServiceSyncHelper;
		}
	}
	
	protected void setMembers( 
			String keysSetKey, 
			TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper,
			TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
			) 
	{
		if ( ( keysSetKey != null) && ( !"".equals( keysSetKey))) {
			Set<Serializable> keySetInMainCache 
			= (Set<Serializable>)mainCache.get( getKeySetKey());
			if ( keySetInMainCache != null) {
				mainCache.put( keysSetKey, keySetInMainCache);
			}
			
			this.keySetKey = keysSetKey;
		}
		
		if ( transactionalMemcacheServiceTransactionHelper != null) {
			TransactionalMemcacheServiceBase
				.setTransactionalMemcacheServiceTransactionHelper( 
						transactionalMemcacheServiceTransactionHelper);
		}
		if ( transactionalMemcacheServiceSyncHelper != null) {
			TransactionalMemcacheServiceBase.transactionalMemcacheServiceSyncHelper 
			= transactionalMemcacheServiceSyncHelper;
		}
	}
		
	// Constructors -------------------------------------------------------------------------------
	
	protected String generateKeySetKeySuffix() {
		String keySetKeySuffix = "_TransactionalMemcacheServiceKeysSetKey:";
		
		String suffixWorkerStr = Long.toHexString( System.currentTimeMillis());
			while( suffixWorkerStr.length() < 12) {
				suffixWorkerStr = "0" + suffixWorkerStr;
			} // while
		keySetKeySuffix = keySetKeySuffix + suffixWorkerStr;
	
		suffixWorkerStr = Long.toHexString( System.identityHashCode( this));
			while( suffixWorkerStr.length() < 8) {
				suffixWorkerStr = "0" + suffixWorkerStr;
			} // while
		keySetKeySuffix = keySetKeySuffix + ":" + suffixWorkerStr;
		
		return keySetKeySuffix;
	}
	
	protected static int maxKeySetKeyGenerationTimes = 3;
		public static int getMaxKeySetKeyGenerationTimes() {
			return maxKeySetKeyGenerationTimes;
		}
		public static void setMaxKeySetKeyGenerationTimes( int maxKeySetKeyGenerationTimes) {
			TransactionalMemcacheServiceBase.maxKeySetKeyGenerationTimes = maxKeySetKeyGenerationTimes;
		}
		
	/**
	 * 
	 * @param memcacheNamespace
	 * @param keySetKey when this is null, new keySetKey value will be automatically generated.
	 * @param transactionalMemcacheServiceTransactionHelper
	 * @param transactionalMemcacheServiceSyncHelper
	 */
	protected TransactionalMemcacheServiceBase( 
			final String memcacheNamespace, 
			final String keySetKey, 
			final TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper, 
			final TransactionalMemcacheServiceSyncHelper transactionalMemcacheServiceSyncHelper
			) 
	{
		super();
		
		this.memcacheNamespace = memcacheNamespace;
		this.mainCache = MemcacheServiceFactory.getMemcacheService( memcacheNamespace);

		String keySetKeyCopy = keySetKey;
		boolean isValidKeySetKey = true;
		if ( ( keySetKey == null) || ( "".equals( keySetKey))) {
			isValidKeySetKey = false;
			if ( ( memcacheNamespace == null) || "".equals( memcacheNamespace)) {
				keySetKeyCopy = "memcache";
			}
		}
		
		int keySetKeySuffixGenerationCounter = 0;
		int maxKeySetKeyGenerationCounts 
		= TransactionalMemcacheServiceBase.getMaxKeySetKeyGenerationTimes();
		boolean isKeySetEntryFoundValid = false;
		do {
			if ( !isValidKeySetKey) {
				keySetKeyCopy = keySetKeyCopy + generateKeySetKeySuffix();
			}
			
			Object keySetObj = this.mainCache.get( keySetKeyCopy);
			if ( keySetObj != null) {
				if ( logger.isInfoEnabled()) {
					logger.info(
							String.format(
									"Found the existing entry with the key, of what the key value (%1$s) " 
									+ "is specified by keySetKey argument, in memcache (of what namespace " 
									+ "is %2$s). The value of that entry is: %3$s",
									keySetKeyCopy, 
									memcacheNamespace, 
									keySetObj.toString()
									)
							);
				}
				
				if ( !( keySetObj instanceof Set)) {
					if ( isValidKeySetKey) {
						throw new TransactionalMemcacheServiceException(
								new IllegalArgumentException( 
										String.format(
												"The existing entry with the key, of what the key value " 
												+ "(%1$s) is specfied by keySetKey argument, in memcache (of " 
												+ "what namespace is %2$s) seems to have already been used " 
												+ "for different purpose than storing the set of keys for that " 
												+ "namespace. The value of that entry is: %3$s", 
												keySetKeyCopy, 
												memcacheNamespace, 
												keySetObj.toString()
												)
										)
								);
					}
					else {
						if ( ++keySetKeySuffixGenerationCounter > maxKeySetKeyGenerationCounts) {
							throw new TransactionalMemcacheServiceException(
									String.format(
											"Failed to automatically generating the key value for the " 
											+ "entry being used to hold the set of keys in the memcache " 
											+ "(of what namespace is %1$s), although %2$d time(s) attempted " 
											+ "it. Please specify the value for keySetKey String argument " 
											+ "manually other than null or empty string.", 
											memcacheNamespace, 
											maxKeySetKeyGenerationCounts
											)
									);
						}
					}
				}
			}
			else {
				isKeySetEntryFoundValid = true;
			}
		} while( !isKeySetEntryFoundValid);
		
		this.keySetKey = keySetKeyCopy;
		
		TransactionalMemcacheServiceBase.setTransactionalMemcacheServiceHelpers( 
				transactionalMemcacheServiceTransactionHelper, 
				transactionalMemcacheServiceSyncHelper
				);
		
		syncKeysSetWithMainCache( null, this.keySetKey);
	}
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Dummy class to be used as alternative to null since memcache allows to have null for key and value.
	 * Warning: Do not use for actual key and value to store to memcache. 
	 * @author ay
	 */

	public static class NoObject implements Serializable {
		private static final long serialVersionUID = -20111022L;
		
		@Override
		public boolean equals( Object obj) {
			if ( this == obj) return true;
			if ( obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			return true;
		}
	}
	
	protected static class SearchResult<T extends Object> {
		protected boolean isFound = false;
		
		protected T cachedObj = null;

		public boolean isFound() {
			return isFound;
		}

		public void setFound( boolean isFound) {
			this.isFound = isFound;
		}

		public T getCachedObj() {
			return cachedObj;
		}

		public void setCachedObj( T cachedObj) {
			this.cachedObj = cachedObj;
		}
	} // protected static class SearchResult
	
	
	/**
	 * Get copy of key from keysSet input. <br />
	 * <b>Thread safety:</b> Conditionally thread safe; thread safety on key input and keysSet input 
	 * needs to be assured.
	 * @param key: original for searching copy in keysSet input
	 * @param copiedKeysSet
	 * @return copy of key when it's found in keysSet. NoObject object when it's not. 
	 * Null when key input is null.
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when key input 
	 * object is NoObject instance.
	 */
	protected <T extends Object> SearchResult<T> getKeyCopyFromSet( 
			final T key, final Set<Serializable> copiedKeysSet) {
		
		SearchResult<T> searchResult = new SearchResult<T>();
		
		if ( key == null) {
			searchResult.setFound( true);
			return searchResult;
		}
		
		if ( ( copiedKeysSet != null) && (copiedKeysSet.contains( key))) {
			for( Serializable copiedKey : copiedKeysSet) {
				if ( key.equals( copiedKey)) {
					searchResult.setCachedObj( (T)copiedKey);
					break; // for
				}
			} // for
		}
		
		return searchResult;
	} 
	
	/**
	 * To acquire lock on mainCacheLock member field. <br />
	 * <b>Thread safety:</b> thread safe. Lock on and hold the lock of mainCacheLock member field.
	 * @throws TransactionalMemcacheServiceException when lock is not acquired within the duration having 
	 * set to lockAcqusitionDuration member field. <br />
	 * @throws TransactionalMemcacheServiceException wrapping InterruptedException when thread is 
	 * interrupted by other while attempting to acquire log on mainCacheLock member field.
	 */
	protected void acquireMainCacheLock() {
		long lockAcquisitionPeriod = getMaxLockAcquisitionDuration();
		try {
			boolean tryLockResult 
			= mainCacheLock.tryLock( lockAcquisitionPeriod, TimeUnit.MILLISECONDS);
				if ( !tryLockResult) {
					int queueLength = mainCacheLock.getQueueLength();
					
					throw new TransactionalMemcacheServiceException(
							String.format(
									"Could not acquire lock on mainCacheLock member field within " 
									+ "the duration of %1$d [msec] in order to work on mainCache " 
									+ "member field of what namespace is %2$s in %3$s mode. " 
									+ "The number of other threads waiting the same lock is about %4$d.",
									lockAcquisitionPeriod,
									memcacheNamespace,
									(transactionalMemcacheServiceTransactionHelper.isTransactionModeThread() 
											? "transction" : "non-transaction"),
									queueLength
									)
							);
				}
		}
			catch( InterruptedException exception) {
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Interrupted before acquiring lock on mainCacheLock ReentrantLock member " 
								+ "field in order to work on mainCache member field of what namespace is " 
								+ "%1$s namespace in %2$s mode.",
								memcacheNamespace,
								(transactionalMemcacheServiceTransactionHelper.isTransactionModeThread() 
										? "transction" : "non-transaction")
								),
						exception
						);
			}
	} // protected void acquireMainCacheLock()
	
	/**
	 * Synchronize keySet entry in mainCache and keysSet entry in keysMap static member field. <br />
	 * Return pre-existing copy of key found during synchronization process. <br />
	 * For synchronizing keySet entry in mainCache and keysSet entry in keysMap static member field, 
	 * this goes along the following algorithm:
	 * <ol>
	 * <li>When size of keySet entry in mainCache is 0 and size of keysSet entry in keysMap static member 
	 * field is not zero, <br />
	 * then clear keysSet entry in keysMap static member field.</li>
	 * <li>When both of keySet entry in mainCache and keysSet entry in keysMap static member field 
	 * contains data entry , <br />
	 * then add data entry what only keySet entry in mainCache contains to keysSet entry in keysMap 
	 * static member field, and add data entry what only keysSet entry in keysMap static member field 
	 * contains to keySet entry in mainCache.
	 * </li>
	 * <li>When keySet entry in mainCache contains data entry and the size of keysSet entry in keysMap 
	 * static member field is zero, <br />
	 * then copy keySet entry in mainCache to keysSet entry in keysMap static member field.
	 * </li>
	 * <li>When keySet entry in mainCache is null, <br />
	 * then copy keysSet entry in keysMap static member field to keySet entry in mainCache.</li>
	 * </ol>
	 * <b>Thread safety:</b> Conditionally thread safe; thread safety on key input needs to be assured. 
	 * Locks on mainCacheLock member field.
	 * @param key : When NoObject instance, searching deep copy is skipped and return NoObject object.
	 * @param keysSetKey
	 * @return NoObject object for the following cases:
	 * <ul>
	 * <li>key input is not found in either keySet entry in mainCache and 
	 * keysSet entry in keysMap static member field during synchronization process.</li>
	 * <li>key input is NoObject instance.</li>
	 * </ul>
	 * Null when key input is null. 
	 * Otherwise, return pre-existing copy of key object. <br /> 
	 * Note: This does not check whether returned pre-existing copy of key object is actually 
	 * referring to exact same key Object input.
	 */
	protected <T extends Object> SearchResult<T> syncKeysSetWithMainCache( 
			final T key, final String keysSetKey) {
		
		SearchResult<T> searchResult = new SearchResult<T>();
		if ( key == null) {
			searchResult.setFound( true);
		}
	
		acquireMainCacheLock(); 
		
		try {
			Set<Serializable> keysSet 
			= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
			
			Set<Serializable> keySetInMainCache 
			= (Set<Serializable>)mainCache.get( keysSetKey);
			
			if ( keySetInMainCache != null) {
				// Get deep copy of key from keySet in mainCache
				if ( !searchResult.isFound()) {
					searchResult = getKeyCopyFromSet( key, keySetInMainCache);
				}
				
				if ( ( keySetInMainCache.size() == 0) && ( keysSet.size() != 0)) {
					// Clear keysSet entry in keysMap static member field since clear methods have 
					// been executed by one on other app instance
					
					// Before clearing keysSet entry in keysMap static member field, 
					// try to get deep copy of key from it if it has not been found in previous attempt.
					if ( !searchResult.isFound()) {
					// key wasn't found in keySet in mainCache
						// Try to get copied key from keysSet member field before clearing it
						searchResult = getKeyCopyFromSet( key, keysSet);
					}
					
					// Clear keysSet member field 
					keysSet.clear();
				}
				else { // keySetInMainCache.size() != 0 || keysSet.size() == 0
					if ( keySetInMainCache.size() != 0) {
						if ( keysSet.size() != 0) {
							
							// Try to get deep copy of key from keysSet entry in keysMap static member field 
							// if it has not been found in previous attempt.
							if ( !searchResult.isFound()) {
							// key wasn't found in keySet in mainCache								
								// Get copied key from keysMap 
								searchResult = getKeyCopyFromSet( key, keysSet);
							}
							
							// Sync keySet in mainCache and keysSet member field							
							if ( !keysSet.containsAll( keySetInMainCache)) {
								// Add missing keys from keySet in mainCache to keysMap
								HashSet<Serializable> hashSet = new HashSet<Serializable>( keySetInMainCache);
								hashSet.retainAll( keysSet);
								keysSet.addAll( hashSet);
							}
							if ( !keySetInMainCache.containsAll( keysSet)) {
								HashSet<Serializable> hashSet = new HashSet<Serializable>( keysSet);
								hashSet.retainAll( keySetInMainCache);
								keysSet.removeAll( hashSet);
							}
						}
						else { // mainCache has been updated by other app instance.
							
							// Import keySet in mainCache to keysSet member field 
							keysSet.addAll( keySetInMainCache);
						}
					}
					
					// Confirm with mainCache's entries after synchronizing keysSet member field 
					// and keysSet in mainCache
					Map<Serializable, Object> entries = mainCache.getAll( keysSet);
					if ( entries.size() < keysSet.size()) {
						keysSet.retainAll( entries.keySet());
					}
					
					// update keysSet in mainCache
					mainCache.put( keysSetKey, keysSet);
				}
			}
			else {
			// keySetInMainCache == null; keysSet entry in mainCache has been evicted or clearAll method 
			// has been invoked.
				if ( logger.isInfoEnabled()) {
					logger.info( 
							String.format(
									"Clearing keysSet entry (for %1$s namespace of memcache) in " 
									+ "TransactionalMemcacheServiceSyncHelperImpl.keysMap static member field " 
									+ "because of detecting that the keysSet entry does not exist in " 
									+ "mainCache member field of that namespace. This status can " 
									+ "happen either 1) during construction of %2$s instance, 2) when " 
									+ "keysSet entry was evicted from mainCache, or 3) when clearAll " 
									+ "method was invoked.",
									memcacheNamespace,
									this.toString()
									)
							);
				}
				
				// Import keysSet entry in keysMap static member field to mainCache
				if ( keysSet.size() > 0) {
					if ( !searchResult.isFound()) {
						// Get copied key from keysMap 
						searchResult = getKeyCopyFromSet( key, keysSet);
					}
					
					keysSet.clear();
				}
				
				// update keysSet in mainCache
				mainCache.put( keysSetKey, keysSet);
			}
			
			return searchResult;
		}
		finally {
			mainCacheLock.unlock();
		}
	}
	
	
	/**
	 * Used only for transaction mode of TransactionalMemcacheServiceBase sub-class instance to hold 
	 * the following objects for each data entry in cache:
	 * <ul>
	 * <li>value object</li>
	 * <li>Expiration object</li>
	 * <li>SetPolicy object</li>
	 * </ul>
	 */
	protected static class ValueExpirationSetPolicy {
	/* ValueExpirationSetPolicy will be only used during transaction mode of 
	 * TransactionalMemcacheServiceBase sub-class. 
	 * Thereby, basically there should be no need for consideration of synchronization. 
	 */
		protected Copier copier;
			protected Copier getCopier() {
				return copier;
			}
			/**
			 * @param copier
			 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException 
			 * when copier input is null.
			 */
			protected void setCopier( Copier copier) {
				if ( copier == null) {
					throw new TransactionalMemcacheServiceException(
							new IllegalArgumentException( "copier input cannot be null.")
							);
				}
				
				this.copier = copier;
			}
			
		protected Object value = null;
			/**
			 * When there is possibility of that object returned by this is leaked out to external code, 
			 * use getValueCopy method instead. 
			 * Since this returns direct reference to stored value object, in order to avoid indirect 
			 * modification on stored value object in transaction cache by leaking reference to stored 
			 * value object out to external code, this should be used only within TransactionalMemcacheServiceBase 
			 * sub-class. 
			 * @return direct reference to stored value object. 
			 */
			protected Object getValue() {
				return value;
			}
			/**
			 * Return deep copy of stored value object in order to keep the state of transaction caache 
			 * from indirect modification on stored value object by leaking reference to stored value 
			 * object out to external code and modified there. <br />
			 * Thread safety: conditional thread safe; as long as object stored at value member field is 
			 * thread safe. 
			 * @return Deep copy of stored value object.
			 * @throws Throwable
			 */
			public Object getValueCopy() throws Throwable {
				if ( value == null) return null;
				return copier.generateCopy( value);
			}
			
			/**
			 * Store value argument to value member field.  
			 * Use this method only when value argument is a deep copy of other object and known as 
			 * no reference to it will exist. 
			 * In general, use setCopiedValue method instead.
			 * @param value
			 */
			protected void setValue( Object value) {
				this.value = value;
			}
			
			/**
			 * Store deep copy of value argument to value member field. <br />
			 * Thread safety: conditional thread safe; as long as thread safety on value input is assured.
			 * @param value
			 * @throws Throwable
			 */
			public void setCopiedValue( Object value) throws Throwable {
				this.value = copier.generateCopy( (Serializable)value);
			}
			
		protected Long expiration;
			public Expiration getExpiration() {
				if ( expiration == null) {
					return null;
				}
				else {
					return Expiration.onDate( new Date( expiration.longValue()));
				}
			}
			public void setExpiration( Expiration expiration) {
				if ( expiration != null) {
					this.expiration = Long.valueOf( expiration.getMillisecondsValue());
				}
				else {
					this.expiration = null;
				}
			}
		protected SetPolicy setPolicy;
			public SetPolicy getSetPolicy() {
				return setPolicy;
			}
			public void setSetPolicy( SetPolicy setPolicy) {
				if ( setPolicy == null) {
					this.setPolicy = SetPolicy.SET_ALWAYS;
				}
				else {
					this.setPolicy = setPolicy;
				}
			}
			
		// Constructors -----------------------------------------------------------------------
		public ValueExpirationSetPolicy( 
				Serializable value, Expiration expiration, SetPolicy setPolicy, Copier copier) 
		throws Throwable {
			setCopier( copier);
			setCopiedValue( value);
			setExpiration( expiration);
			setSetPolicy( setPolicy);
		}
		
		public ValueExpirationSetPolicy( Expiration expiration, SetPolicy setPolicy, Copier copier) {
			setCopier( copier);
			setValue( null);
			setExpiration( expiration);
			setSetPolicy( setPolicy);
		}
		
		public ValueExpirationSetPolicy( Copier copier) {
			this( null, null, copier);
		}
		// ------------------------------------------------------------------------------------
	} // public static class ValueExpirationSetPolicy
	
	/**
	 * TransactionHandler will be only used to handle transaction mode of TransactionalMemcacheServiceBase 
	 * sub-class. 
	 * Thereby, there should be no need for consideration of synchronization. 
	 */
	protected static class TransactionHandler {
		protected boolean transactionMode = false;
		
		/**
		 * This will hold mainCache snapshot at the beginning of transaction.
		 */
		protected final Map<Serializable, Object> mainCacheSnapShot;
		protected boolean snapshotEnabled = true;
		/**
		 * This will hold changes made over mainCacheSnapShot during transaction
		 */
		protected final Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache; 
		/**
		 * This will hold key of entries of what the value has been modified during transaction. 
		 */
		protected final Map<Serializable, Serializable> keysMapForTransaction; 
		
		protected boolean clearInvocationInsident = false;
		
		// Stream objects being used for creating deep copy of object -----------------------------
		protected FastByteArrayOutputStream fastByteArrayOutputStream = null; 
		protected ObjectOutputStream objectOutputStream = null; 
		protected FastByteArrayInputStream fastByteArrayInputStream = null;
		protected byte[] streamHeaderByteArray = null;
		// ----------------------------------------------------------------------------------------		

		public TransactionHandler() {
			mainCacheSnapShot = new LinkedHashMap<Serializable, Object>();
			transactionLocalCache  = new LinkedHashMap<Serializable, ValueExpirationSetPolicy>();
			keysMapForTransaction = new LinkedHashMap<Serializable, Serializable>();
		}
		
	} // protected static class TransactionHandler
	protected ThreadLocal<TransactionHandler> transactionHandlerThreadLocal 
	= new ThreadLocal<TransactionalMemcacheServiceBase.TransactionHandler>() {
		@Override
		protected TransactionHandler initialValue() {
			return new TransactionHandler();
		}
	};
		
		@OnEvent( value = {TransactionalMemcacheServiceException.class}, amongInstancesInThread = false)
		protected void removeTransactionHandlerThreadLocal( EventInfo eventInfo) {
			transactionHandlerThreadLocal.remove();
		}
	
	/**
	 * Change to non-transaction mode from transaction mode. <br /> 
	 * Data modification and inquiry made in cache during transaction will be exported to 
	 * mainCache MemcacheService member field as the end of transaction.<br />
	 * <u>Should be invoked only via listening NonTransactionModeEvent event triggered by 
	 * NonTransactionModeEvent.switchThreadToNonTransactionMode method.</u> <br />
	 * <b>Precondition:</b> no running method of MemcacheService interface; this is necessity not 
	 * only from the task of exporting data to mainCache but also from possibility of performing 
	 * the same task of clearAll method for the case when clearAll method had been invoked during 
	 * transaction. And this precondition is satisfied when this method is invoked indirectly by 
	 * TransactionalMemcacheServiceTransactionHelperImpl.switchThreadToNonTransactionMode method 
	 * (what invokes indirectly NonTransactionModeEvent.doTrigerNonTransactionModeEvent method that 
	 * triggers NonTransactionModeEvent event what this method listens.)<br />
	 * <b>Thread safety:</b> thread safe. Lock on mainCacheLock member field.
	 * @param eventInfo
	 */
	@OnEvent( value={NonTransactionModeEvent.class}, amongInstancesInThread=false)
	protected void switchToNonTransactionMode( EventInfo eventInfo) {
		String keyForKeysSetInMainCache = getKeySetKey();
		
		// Sync keysSet entry in keysMap member field with keysSet entry in mainCache
		syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
		
		if ( transactionalMemcacheServiceTransactionHelper
				.getNonTransactionModeEvent().isTransactionChangeToBeSaved()) {
			
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			
			Map<Serializable, Object> mainCacheSnapshot 
			= transactionHandler.mainCacheSnapShot;
				Set<Serializable> keysSetInSnapshot 
				= new LinkedHashSet<Serializable>( mainCacheSnapshot.keySet());
			Map<Serializable, Serializable> keysMapForTransaction 
			= transactionHandler.keysMapForTransaction;
				Set<Serializable> keysProcessedInTransaction 
				= transactionHandler.keysMapForTransaction.keySet();
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			
			Set<Serializable> addedKeysExternally = null;
			Set<Serializable> removedKeysExternally = null;
			Set<Serializable> keysForConcurrentModificationCheck = null;
			Map<Serializable, Object> mainCacheMap = null;
			
			acquireMainCacheLock();			
			try {
				// Prepare list of keys for what entries value in mainCache has been modified 
				// by external code during transaction, in order to later delete such entry from 
				// mainCache and transactionLocalCache ----------------------------------------
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				if ( keysSet.size() > 0) {
					mainCacheMap = mainCache.getAll( keysSet);
				}
				if (( mainCacheMap != null) && ( mainCacheMap.size() > 0)) { 
					Set<Serializable> keysSetInMainCache = mainCacheMap.keySet();
					
					addedKeysExternally = new LinkedHashSet<Serializable>( keysSetInMainCache);
					addedKeysExternally.removeAll( keysSetInSnapshot);
						/* Decided eliminate all entries added to mainCache by external code 
						 * during transaction.
						 * If turn over that decision to eliminate only entries also modified in 
						 * transaction process, then make below statement out of comment:
						 * addedKeysExternally.retainAll( keysProcessedInTransaction);
						 */
						// At here, just delete entry from keysMapForTransaction
						keysProcessedInTransaction.removeAll( addedKeysExternally);
							// later actually remove from transactionLocalCache and mainCache 
					
					removedKeysExternally = new LinkedHashSet<Serializable>( keysSetInSnapshot);
					removedKeysExternally.removeAll( keysSetInMainCache);
						/* Decided eliminate all entries removed from mainCache by external code 
						 * during transaction.
						 * If turn over that decision to eliminate only entries also modified in 
						 * transaction process, then make below statement out of comment:
						 * removedKeysExternally.retainAll( keysProcessedInTransaction);
						 */
						// At here, just delete entry from keysMapForTransaction
						keysProcessedInTransaction.removeAll( removedKeysExternally);
							// later actually remove from transactionLocalCache and mainCache 
					
					// From transactionLocalCache, remove entries added or removed 
					// by external code during transaction
					transactionLocalCache.keySet().retainAll( keysProcessedInTransaction);
						// later actually remove from mainCache 
					
						
					keysForConcurrentModificationCheck 
					= new LinkedHashSet<Serializable>( keysSetInSnapshot);
					keysForConcurrentModificationCheck.retainAll( keysSetInMainCache);
						// Check whether entry for same key will be modified during transaction
						keysForConcurrentModificationCheck.retainAll( keysProcessedInTransaction);
						// If modified, later delete entry from mainCache and transactionLocalCache
						// If not, do nothing
				}
				/* Just for comment purpose
				else {
					// keys set in mainCache has been evicted, 
					// or clear method or clearAll method has been invoked
					// or no data entry has not been added to mainCache yet at all
				}
				*/
				// ----------------------------------------------------------------------------
			
			
				LinkedHashSet<Serializable> keysForDeletion = new LinkedHashSet<Serializable>();
				LinkedHashMap<Expiration, LinkedHashMap<SetPolicy, LinkedHashMap<Serializable, Object>>> linkedHashMap
				= new LinkedHashMap<Expiration, LinkedHashMap<SetPolicy, LinkedHashMap<Serializable, Object>>>();
				LinkedHashMap<SetPolicy, LinkedHashMap<Serializable, Object>> expriationEntriesMap;
				LinkedHashMap<Serializable, Object> setPolicyEntriesMap;
				
				// Construct linkedHashMap with entries of key and value added/updated during transaction
				Iterator<Serializable> keyIterator = keysProcessedInTransaction.iterator();
				while( keyIterator.hasNext()) {
					Serializable key = keyIterator.next();
					
					if ( keysForConcurrentModificationCheck != null) {
						if ( keysForConcurrentModificationCheck.contains( key)) { 
							// Check whether entry in mainCache has been updated external code during transaction
							if ( mainCacheMap != null) {
								if ( mainCacheMap.get( key) != null) {
									if ( !mainCacheMap.get( key).equals( mainCacheSnapshot.get( key))) {
										// entry in mainCache has been updated external code during transaction
										
										// Remove entry with key from transactionLocalCache and keysMapForTransaction
										transactionLocalCache.remove( key);
										keyIterator.remove();
										
										// Add key to keysForDeletion to remove entry from mainCache later
										keysForDeletion.add( key);
										
										continue; // whlie
									}
								}
								else if ( mainCacheSnapshot.get( key) != null) {
									// entry in mainCache has been updated external code during transaction
									
									// Remove entry with key from transactionLocalCache and keysMapForTransaction
									transactionLocalCache.remove( key);
									keyIterator.remove();
									
									// Add key to keysForDeletion to remove entry from mainCache later
									keysForDeletion.add( key);
									
									continue; // whlie
								}
							}
						}
					}
					
					ValueExpirationSetPolicy valueExpirationSetPolicy 
					= transactionLocalCache.get( key);
					if ( valueExpirationSetPolicy == null) { // entry deleted during transaction 
						keysForDeletion.add( key);
					}
					else {
						Object value = valueExpirationSetPolicy.getValue();
							// Because going to save to mainCache, don't need to use getValueCopy method instead here
						
						Expiration expiration = valueExpirationSetPolicy.getExpiration();
						expriationEntriesMap = linkedHashMap.get( expiration);
							if ( expriationEntriesMap == null) {
								expriationEntriesMap 
								= new LinkedHashMap<MemcacheService.SetPolicy, LinkedHashMap<Serializable,Object>>();
								linkedHashMap.put( expiration, expriationEntriesMap);
							}
							
						SetPolicy setPolicy = valueExpirationSetPolicy.getSetPolicy();
						setPolicyEntriesMap = expriationEntriesMap.get( setPolicy);
							if ( setPolicyEntriesMap == null) {
								setPolicyEntriesMap = new LinkedHashMap<Serializable, Object>();
								expriationEntriesMap.put( setPolicy, setPolicyEntriesMap);
							}
						setPolicyEntriesMap.put( key, value);
					}
				} // while( keyIterator.hasNext())
				
				if ( transactionalMemcacheServiceTransactionHelper.hasClearAllInvokedInTransaction()) 
				{
				// clearAll method has been invoked in transaction
					// Clear whole memcache and keysMap static member field only once at all.
					if ( !transactionalMemcacheServiceTransactionHelper.hasMemcacheClearedForClearAllInvocation()) {
						// There should be no other running method of MemcacheService interface.
						
						// At here, clear only whole memcache and keysMap static member field since 
						// transactionLocalCache and keysMapForTransaction fields were already cleared 
						// in TransactionalMemcacheServiceBase.clearAll method
						mainCache.clearAll();
						TransactionalMemcacheServiceSyncHelperImpl.keysMap.clear();
						
						transactionalMemcacheServiceTransactionHelper.setMemcacheClearedForClearAllInvocation();
					}
					
					// Reconstruct keysSet entry in keysMap static member field
					keysSet.clear();
					keysSet = transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
					
					// Reconstruct keysSet entry in mainCache
					mainCache.put( keyForKeysSetInMainCache, keysSet);
				}
				else if ( transactionHandler.clearInvocationInsident) {
				// clear method has been invoked in transaction
					
					// At here, clear only mainCache since transactionLocalCache and 
					// keysMapForTransaction fields were already cleared in clear method.
					// Clear only data entries existed at time of mainCache's 
					// snapshot was taken.
					keysSet.removeAll( keysSetInSnapshot);
					mainCache.deleteAll( keysSetInSnapshot);
					mainCache.put( keyForKeysSetInMainCache, keysSet);
				}
				else {
					// From mainCache, remove entries eliminated during transaction -------------------
					mainCache.deleteAll( keysForDeletion);
					
					keysSet.removeAll( keysForDeletion);
					
					Set<Serializable> keySetInMainCache 
					= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
					if ( keySetInMainCache != null) {
					// neither mainCache has been most likely cleared or keysSet entry in mainCache has been evicted
						keySetInMainCache.removeAll( keysForDeletion);
						mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
					}
					/*
					else { 
					// If mainCache has been cleared or keysSet entry in mainCache has been evicted, 
					// then do nothing since codes here are for handling for deletion of data entries. 
					}
					*/
					// --------------------------------------------------------------------------------
				}
				
				// Update mainCache by constructed linkedHashMap ----------------------------------
				for( Expiration expiration : linkedHashMap.keySet()) {
					expriationEntriesMap = linkedHashMap.get( expiration);
					for( SetPolicy setPolicy : expriationEntriesMap.keySet()) {
						mainCache.putAll( 
								expriationEntriesMap.get( setPolicy), expiration, setPolicy);
							// Although com.google.appengine.api.memcache.AsyncMemcacheServiceImpl (http://googleappengine.googlecode.com/svn/trunk/java/src/main/com/google/appengine/api/memcache/AsyncMemcacheServiceImpl.java) 
							// does not accept null for SetPolicy, it should not be concern here since  
							// ValueExpirationSetPolicy.getSetPolicy method will not return null.
						
						Set<Serializable> setPolicyKeys 
						= expriationEntriesMap.get( setPolicy).keySet();
						
						keysSet.addAll( setPolicyKeys);
						
						Set<Serializable> keySetInMainCache 
						= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
						if ( keySetInMainCache == null) {
						// either mainCache has been most likely cleared or 
						// keysSet entry in mainCache has been evicted
							// After sync keysSet entry in keysMap static member field with 
							// data entries in mainCache, reconstruct of keysSet entry in mainCache 
							// from keysSet entry in keysMap static member field
							syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
						}
						else {
							keySetInMainCache.addAll( setPolicyKeys);
							mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
						}
					} // for
				} // for
				// --------------------------------------------------------------------------------
			}
			finally {
				mainCacheLock.unlock();
			}
		}
		
		// Sync keysSet entry in keysMap static member field with keysSet in mainCache at last
		syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
		
		// Remove ThreadLocal member fields to avoid memory leak.
		transactionHandlerThreadLocal.remove();
	} // public void switchToNonTransactionMode( boolean saveChanges)
	
	/**
	 * Change to transaction mode from non-transaction mode<br />
	 * Must be executed by listening NonTransactionModeEvent event triggered by 
	 * TransactionalMemcacheServiceTransactionHelperImpl.switchThreadToTransactionMode 
	 * as the beginning of transaction. <br />
	 * <b>Event participation:</b> Being executed by TransactionModeEvent.class event. <br /> 
	 * <b>Thread safety:</b> Locks on mainCacheLock member field<br />
	 * @param eventInfo
	 */
	@OnEvent( value={TransactionModeEvent.class}, amongInstancesInThread=false)
	protected void switchToTransactionMode( EventInfo eventInfo) {
		
		TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
		
		acquireMainCacheLock();			
		try {
			String keysSetKey = getKeySetKey();
			
			// Sync keysSet member field with keysSet in mainCache
			syncKeysSetWithMainCache( null, keysSetKey);
			
			// Obtain list of keys currently in mainCache and values for those keys
			Set<Serializable> keysSet 
			= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
			
			Map<Serializable, Object> map = mainCache.getAll( keysSet);
			
			// Prepare mainCacheSnapShot
			Map<Serializable, Object> mainCacheSnapShot = transactionHandler.mainCacheSnapShot;
			mainCacheSnapShot.clear();
			mainCacheSnapShot.putAll( map);
			
			// Prepare transactionLocalCache
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			transactionLocalCache.clear();
			for( Entry<Serializable, Object> entry : map.entrySet()) {
				
				ValueExpirationSetPolicy valueExpirationSetPolicy 
				= new ValueExpirationSetPolicy( null, null, getCopier());
				valueExpirationSetPolicy.setValue( entry.getValue());
				
				transactionLocalCache.put( entry.getKey(), valueExpirationSetPolicy); 
			} // for
			
			// Prepare keysMapForTransaction
			Map<Serializable, Serializable> keysMapForTransaction
			= transactionHandler.keysMapForTransaction;
			keysMapForTransaction.clear();
			
			// Sync keysSet with keys currently in mainCache
			if ( map.size() != keysSet.size()) {
				// Do not assign map.keySet() to keysSet directly
				keysSet.clear();
				keysSet.addAll( map.keySet());
				
				mainCache.put( keysSetKey, keysSet);
			}
		}
		finally {
			mainCacheLock.unlock();
		}
	} // public void switchToTransactionMode()
	
		
	// For generating copy of object --------------------------------------------------------------
	/**
	 * Check whether classObj input is cloneable.
	 * @param classObj
	 * @return
	 */
	public static boolean isCloneableClass( final Class<?> classObj) {
		try {
			classObj.asSubclass( Cloneable.class);
		}
		catch( ClassCastException exception) {
			return false;
		}
		
		return true;
	} // protected boolean isCloneableClass( final Class<?> classObj)
	/**
	 * Get public clone method from classObj input.
	 * @param classObj
	 * @return
	 */
	public static Method getCloneMethod( final Class<?> classObj) {
		try {
			Method cloneMethod = classObj.getMethod( "clone", new Class<?>[]{});
			cloneMethod.setAccessible( true);
			return cloneMethod;
		}
		catch( NoSuchMethodException exception) {
			return null;
		}
	} // protected Method getCloneMethod( final Class<?> classObj)

	@EventListener
	protected static class SerializableChecker {
		protected FastByteArrayOutputStream fastByteArrayOutputStreamForSerializabilityCheck = null;
		protected ObjectOutputStream objectOutputStreamForSerializabilityCheck = null;
		
		protected TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper;
			public TransactionalMemcacheServiceTransactionHelper getTransactionalMemcacheServiceTransactionHelper() {
				return transactionalMemcacheServiceTransactionHelper;
			}
			public void setTransactionalMemcacheServiceTransactionHelper(
					TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper) {
				
				this.transactionalMemcacheServiceTransactionHelper 
				= transactionalMemcacheServiceTransactionHelper;
			}
			@OnEvent( value={SetTransactionalMemcacheServiceTransactionHelperEvent.class})
			protected void setTransactionalMemcacheServiceTransactionHelper( EventInfo eventInfo) {
				TransactionalMemcacheServiceTransactionHelper transactionalMemcacheServiceTransactionHelper
				= (TransactionalMemcacheServiceTransactionHelper)eventInfo.getArgs()[ 0];
				setTransactionalMemcacheServiceTransactionHelper( 
						transactionalMemcacheServiceTransactionHelper);
			}

		/**
		 * Check whether target obj Object input is serializable by writing it to ObjectOutputStream.
		 * When obj input is null, then this will pass even the class of target obj input 
		 * has not implemented Serializable interface. Thereby, casting target obj to Serializable is 
		 * necessary beside this to know whether target obj Object input is serializable accurately. <br /> 
		 * <b>Thread safety:</b> Conditional thread safe; thread safety on obj input needs to be provided. 
		 * @param obj 
		 * @return true when obj Object input is serializable or null.
		 * @throws TransactionalMemcacheServiceException wrapping Throwable caused during serialization. 
		 */
		protected synchronized boolean isSerializable( final Object obj) {
			if ( obj == null) return true;
			
			try {
				if ( objectOutputStreamForSerializabilityCheck == null) {
					fastByteArrayOutputStreamForSerializabilityCheck = new FastByteArrayOutputStream();
					objectOutputStreamForSerializabilityCheck 
					= new ObjectOutputStream( fastByteArrayOutputStreamForSerializabilityCheck);
				}
				
				objectOutputStreamForSerializabilityCheck.writeObject( obj);
				objectOutputStreamForSerializabilityCheck.flush();
				return true;
			}
			catch( NotSerializableException exception) {
				return false;
			}
			catch( InvalidClassException exception) {
				return false;
			}
			catch( Throwable throwable) {
				if ( getTransactionalMemcacheServiceTransactionHelper() == null) {
					setTransactionalMemcacheServiceTransactionHelper( 
							new TransactionalMemcacheServiceTransactionHelperImpl());
				}
				boolean transactionMode 
				= getTransactionalMemcacheServiceTransactionHelper().isTransactionModeThread();
				throw new TransactionalMemcacheServiceException( 
						String.format(
								"Failure in checking serializablity on %1$s during %2$s mode. " 
								+ "The type of %1$s is %2$s.", 
								obj.toString(),
								(transactionMode ? "transaction" : "non-transaction"), 
								obj.getClass().toString()
								),
						throwable
						);
			}
			finally {
				try {
					fastByteArrayOutputStreamForSerializabilityCheck.reset();
					objectOutputStreamForSerializabilityCheck.reset();
				}
				catch( IOException exception) {
					fastByteArrayOutputStreamForSerializabilityCheck = null;
					objectOutputStreamForSerializabilityCheck = null;
				}
			}
		} // protected synchronized boolean isSerializable( final Object obj)
	} // protected static class SerializableChecker
	protected SerializableChecker serializableChecker = new SerializableChecker();
	
	/**
	 * Check whether input obj is serializable by casting it to Serializable interface.
	 * Note: 
	 * <ul>
	 * <li>When target obj Object input is collection or Object arrays, this will pass even 
	 * such object contains non serializable element or non serializable member field in element.</li>
	 * <li>Also this will pass when obj Ojbect input is null.</li>
	 * </ul>
	 * Thread safety: Conditional thread safe; thread safety on obj input needs to be provided.
	 * @param obj
	 * @return true when obj is serializable. true when obj is null.
	 */
	public static boolean briefSerializabilityCheck( final Object obj) {
		if ( obj == null) return true;
		
		try {
			Serializable.class.cast( obj);
			return true;
		}
		catch( ClassCastException exception) {
			return false;
		}
	} // public static boolean briefSerializabilityCheck( final Object obj)
	
	/**
	 * Generate a copy of obj input by a sequence of serializing and de-serializing. <br />
	 * <b>Thread safety:</b> thread safe as long as thread safety on obj input is assured.
	 * @param obj
	 * @return deep copy of obj input
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when 
	 * obj Serializable input is NoObject instance. TransactionalMemcacheServiceException wrapping 
	 * Throwable caused during serializing and de-serializing.
	 */
	protected <T extends Object> T generateCopy( final T obj) {
		if ( obj instanceof NoObject) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException( 
					new IllegalArgumentException(
							String.format(
									"Due for NoObject instance to be disallowed for key or value being " 
									+ "stored into cache, there should be no necessity of genrating copy " 
									+ "of NoObject instance. (Encountered this while working on the cache " 
									+ "space of what namespace is %1$s during %2$s mode.)",
									memcacheNamespace, 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		
		boolean throwableFlag = false;
		try {
			return copier.generateCopy( obj);
		}
		catch( Throwable throwable) {
			throwableFlag = true;
			
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					String.format(
							"Failure in generating copy of %1$s by serializing and de-serializing during " 
							+ "%2$s mode working on the cache space of what namespace is %3$s.", 
							obj.toString(),
							( transactionMode ? "transaction" : "non-transaction"),
							memcacheNamespace							
							),
					throwable
					);
		}
		finally {
			if ( !throwableFlag) {
				if ( !transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
					// When non-transaction mode, remove ThraedLocal member field in CopierImpl class at each time to avoid memery leak.
					getCopier().turnOffCopier();
				}
			}
		}
	} // protected Object generateCopy( Serializable obj)
	
	/**
	 * <b>Thread safety:</b> Conditional thread safe; thready safety on key input needs to be provided.
	 * @param key
	 * @param value: value for key. Being used just for logging purpose.
	 * @return Copy of key.
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when encountering  
	 * either cases: <br />
	 * <ul>
	 * <li>key input object is NoObject.</li>
	 * <li>key input object is not serializable.</li>
	 * <li>Generated copy of key input object is not identified as equivalent to key input object.</li>
	 * </ul>
	 */
	protected <T extends Object> T generateKeyCopy( final T key, final Object value) {
		if ( key == null) return null;
		if ( key instanceof NoObject) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException( 
					new IllegalArgumentException(
							String.format(
									"Due for NoObject instance to be disallowed for key or value being " 
									+ "stored into cache, there should be no necessity of genrating copy " 
									+ "of NoObject instance. (Encountered this while working on the cache " 
									+ "space of what namespace is %1$s during %2$s mode.)",
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		
		// Check whether key class has override equals method by comparing with deep copied one -
		// ------------------------------------------------------------------------------------
		if ( !briefSerializabilityCheck( key)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException( 
					new IllegalArgumentException( 
							String.format( 
									"key object (%1$s) is not serializable. " 
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %2$s during %3$s mode.)", 
									key.toString(),
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									),
							new NotSerializableException( key.getClass().toString())
							)
					);
		}
		
		T keyCopy = generateCopy( key);
//TODO Need to implement comparison logic for array when either key object is array or key object contains array
		if ( !key.equals( keyCopy)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException(
							String.format(
									"Deep copied key (%1$s) was not identified as equal to orignal key (%2$s). "
									+ "They need to be identified as equal. The key is to cache the value: %3$s. " 
									+ "The class of the key is %4$s. " 
									+ "(Encountered this while working on the cache space of what namespace " 
									+ "is %5$s during %6$s mode.)", 
									( keyCopy == null) ? "null" : keyCopy.toString(),
									key.toString(),
									( value == null) ? "null" : value.toString(),
									key.getClass().toString(),
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		// ------------------------------------------------------------------------------------
		
		return keyCopy;
	} // protected Object generateKeyCopy( final Object key, final Object value)

	/**
	 * Try to find copy of key in keysMapForTransaction for transaction mode or 
	 * keysSet entry in keysMap static member field for non-transaction mode.
	 * When it's not found, then generate one. <br />
	 * <b>Thread safety:</b> Thread safe as long as synchronization is assured with keyObj input.
	 * @param keyObj
	 * @param value : value for key. Being used just for logging purpose.
	 * @return copy of keyObj
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when keyObj input is 
	 * NoObject instance or deep copy of keyObj input is not identified as equivalent. 
	 * TransactionalMemcacheServiceException when it is detected that cache has held object what keyObj 
	 * input refers to. 
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing.
	 */
	protected <T extends Object> T getKeyCopy( final T keyObj, final Object value) {
		if ( keyObj == null) return null;
		if ( keyObj instanceof NoObject) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException( 
					new IllegalArgumentException(
							String.format(
									"It is not allowed using a NoObject instance as key to cache " 
									+ "%1$s value. "
									+ "(Encountered this while working on the cache space of what namespace " 
									+ "is %2$s during %3$s mode.)", 
									( (value == null) ? "null" : value.toString()),
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		
		T keyCopy = null;
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			Map<Serializable, Serializable> keysMapForTransaction 
			= transactionHandlerThreadLocal.get().keysMapForTransaction;
			if ( keysMapForTransaction.containsKey( keyObj)) {
				keyCopy = (T)keysMapForTransaction.get( keyObj);
			}
		}
		else {
			Set<Serializable> keysSet 
			= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
			if ( keysSet.contains( keyObj)) {
				for( Object keyInKeysSet : keysSet) {
					if ( keyObj.equals( keyInKeysSet)) {
						keyCopy = (T)keyInKeysSet;
						break;
					}
				} // for
			}
		}
		
		if ( keyCopy != null) {
			if ( keyCopy == keyObj) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException( 
						String.format(
								"Given key object (%1$s) to be being cached refers to one of key objects " 
								+ "held in field used to manage cache. This should not have happened; " 
								+ "instead, it is designed as saving relation status of key and value " 
								+ "at some point of time that only deep copy of each key is held in such " 
								+ "field. Probably key copy in cache has been sneakily leaking out to " 
								+ "external code. The class of the key is %2$s. " 
								+ "And The key was to cache value: %3$s. "
								+ "(Encountered this while working on the cache space of what namespace is " 
								+ "%4$s during %5$s mode.)", 
								keyObj.toString(),
								keyObj.getClass().toString(),
								( ( value == null) ? "null" : value.toString()),
								memcacheNamespace,
								( transactionMode ? "transaction" : "non-transaction")
								)
						);
			}
		}
		else {
			keyCopy = generateCopy( keyObj);
			if ( keyObj != null) {
				if ( !keyObj.equals( keyCopy)) {
					boolean transactionMode 
					= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
					throw new TransactionalMemcacheServiceException( 
							new IllegalArgumentException(
									String.format(
											"Deep copied key (%1$s) was not identified as equal to orignal key (%2$s). "
											+ "They need to be identified as equal. The key is to cache value: %3$s. " 
											+ "The class of the key is %4$s." 
											+ "(Encountered this while working on the cache space of what " 
											+ "namespace is %5$s during %6$s mode.)", 
											keyCopy.toString(), 
											( ( keyObj == null) ? "null" : keyObj.toString()),
											( ( value == null) ? "null" : value.toString()),
											keyObj.getClass().getName(), 
											memcacheNamespace,
											( transactionMode ? "transaction" : "non-transaction")
											)
									)
							);
				}
			}
		}
		
		return keyCopy;
	} // protected <T extends Object> T getKeyCopy( final T keyObj, final Object value)
	
	
	/**
	 * Should be used only for transaction mode. <br />
	 * <b>Thread safety:</b> Since this will be used only in transaction mode, basically thread safe. 
	 * However thread safety on value input needs to be provided. <br /> 
	 * @param value : should be one handed over from external code. shouldn't be NoObject instance.
	 * @param key : key being used to cache value for. Being used just for logging purpose. 
	 * @return : deep copy of value
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException when it's 
	 * not in transaction mode. TransactionalMemcacheServiceException when value input refer to data 
	 * in transactionLocalCache since direct reference to data of entry in transactionLocalCache 
	 * should not be occurred.
	 */
	protected <T extends Object> T getValueCopy( final T value, final Object key) {
		if ( !transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			throw new TransactionalMemcacheServiceException( 
					new UnsupportedOperationException(
							String.format(
									"findValueCopy method cannot be used for non-transaction mode. " 
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %2$s.)", 
									memcacheNamespace
									)
							)
					);
		}
		
		if ( value == null) return null;
		
		T valueCopy = null;
		
		Set<Entry<Serializable, ValueExpirationSetPolicy>> entriesSet 
		= transactionHandlerThreadLocal.get().transactionLocalCache.entrySet(); 
		for( Entry<Serializable, ValueExpirationSetPolicy> entry : entriesSet) {
			ValueExpirationSetPolicy valueExpirationSetPolicy = entry.getValue();
			Object cachedValue = valueExpirationSetPolicy.getValue();
			
			if ( !value.equals( cachedValue)) continue; // for
			
			if ( value == cachedValue) {
//TODO Shall I make copy of such value here too and switch to the copy in valueArrayList, instead of throwing exception? 
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException(
						String.format(
								"Given value object (%1$s) to be being cached refers to one " 
								+ " of values in cache. This should not happen; instead, " 
								+ "cache should hold deep copy of each value as saving status " 
								+ "representing some point of time. Probably value copy in cache has " 
								+ "been sneakily leaking out to external code. The class of the value " 
								+ "is %2$s. And The key to cache that value is: %3$s. " 
								+ "(Encountered this while working on the cache space of what namespace " 
								+ "is %4$s during %5$s mode.)", 
								value.toString(),
								value.getClass().toString(),
								( ( key == null) ? "null" : key.toString()),
								memcacheNamespace,
								( transactionMode ? "transaction" : "non-transaction")
								)
						);
			}
			else {
				valueCopy = (T)cachedValue;
				break; // for
			}
		} // for
		
		if ( valueCopy == null) {
			valueCopy = generateCopy( value);
		}
		
		return valueCopy;
	} // protected <T extends Object> T getValueCopy( final T value, final Object key)
	
	// --------------------------------------------------------------------------------------------
	
	
	/**
	 * Get stack trace string representation from the specified range in StackTraceElement array input.
	 * <b>Thread safety:</b> thread safe. stackTraceElementArray should not be changed during 
	 * execution.
	 * @param stackTraceElementArray
	 * @param startIndex 
	 * Advise for startIndex value : skip first 2 elements in stackTraceElementArray because 
	 * first element is from getStackTrace execution, 
	 * next elements is about trace of calling to method in where stacktrace was captured.
	 * @param count
	 * @return String of stack traces from the specified range in stackTraceElementArray input.
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when 
	 * stackTraceElementArray input is null, startIndex < 0, startIndex >= stackTraceElementArray.length 
	 * or count < 0.
	 */
	protected String getCallSequenceStr( 
			final StackTraceElement[] stackTraceElementArray, int startIndex, int count) {
		// Validity check on arguments ------------------------------------------------------------
		if ( stackTraceElementArray == null) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException( 
							String.format(
									"stackTraceElementArray argument cannot be null. "
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %2$s during %3$s mode.)", 
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		if ( ( startIndex < 0) || ( startIndex >= stackTraceElementArray.length)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException(
							String.format(
									"Invalid value (%1$d) of startIndex int argument. It had gotten to " 
									+ "be in the range between 0 to %2$d. "
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %3$s during %4$s mode.)", 
									startIndex, 
									(stackTraceElementArray.length - 1),
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		if ( count < 1) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException(
							String.format(
									"Invalid value (%1$d) of count int argument. It had gotten to " 
									+ "be bigger than 0. "
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %2$s during %3$s mode.)", 
									count, 
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		// ----------------------------------------------------------------------------------------
		
		String callingMethodsInfo = "";
		int maxStackIndex = startIndex + count;
		if ( stackTraceElementArray.length < maxStackIndex) {
			maxStackIndex = stackTraceElementArray.length;
		}
		for( int stackIndex = startIndex; stackIndex < maxStackIndex; stackIndex++) {
			String message 
			= String.format(
					"%n%1$cStackTraceElement[ %2$d]: %3$s",
					'\t',
					stackIndex,
					stackTraceElementArray[ stackIndex].toString()
					);
			
			callingMethodsInfo = callingMethodsInfo + message;
		} // for
		
		return callingMethodsInfo;
	} // protected String getCallSequenceStr( ...)
	
	/**
	 * Perform the following validations:
	 * <ul>
	 * <li>keyObj is not NoObject instance</li>
	 * <li>keyObj is not equals to keySetKey value</li>
	 * <li>type of keyObj has implemented Serializable interface</li>
	 * </ul>
	 * <b>Thread safety:</b> thread safe (although immutability of keyObj input should be assured through 
	 * execution.)
	 * @param keyObj
	 * @param keysSetKey
	 * @param valueObj : Just for logging purpose.
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in the following cases:
	 * <ul>
	 * <li>keyObj input is NoObject instance</li>
	 * <li>keyObj input equals to keySetKey value</li>
	 * <li>type of keyObj has not implemented Serializable interface</li>
	 * </ul>
	 */
	protected void validateKey( final Object keyObj, final String keysSetKey, final Object valueObj) {
		if ( keyObj == null) {
			if ( logger.isInfoEnabled()) {
				StackTraceElement[] stackTraceElementArray = Thread.currentThread().getStackTrace();
				String callingMethodsInfo = getCallSequenceStr( stackTraceElementArray, 3, 3);
				logger.info( 
						String.format(
								"It is not recommneded using null as key (to cache %1$s value " 
								+ "into the cache space of what namespace is %2$s.) " 
								+ "Caching was attempted at next call sequence: %3$s",
								( (valueObj == null) ? "null" : valueObj.toString()),
								memcacheNamespace, 
								callingMethodsInfo
								)
						);
			}
		}
		else {
			// Deny NoObject instance as key
			if ( keyObj instanceof NoObject) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException( 
								String.format(
										"It is not allowed using a NoObject instance as key (to cache " 
										+ "%1$s value into the cache space of what namespace is %2$s.) " 
										+ "Encountered during %3$s mode.",
										( (valueObj == null) ? "null" : valueObj.toString()),
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction")
										)
								)
						);
			}
			
			if ( keysSetKey.equals( keyObj)) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException(
								String.format( 
										"%1$s is reserved and not allowed to be used as key to cache " 
										+ "value (%2$s) into the cache space of what namespace is %3$s. " 
										+ "Encountered during %4$s mode.",
										keysSetKey, 
										( (valueObj == null) ? "null" : valueObj.toString()),
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction")
										)
								)
						);
			}
			
			// Briefly check serializability of keyObj 
			if ( !briefSerializabilityCheck( keyObj)) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException( 
								String.format( 
										"Type (%1$s) of key (%2$s) being used to cache %3$s value into " 
										+ "the cache space (of what namespace is %4$s) is not serializable. " 
										+ "Encountered during %5$s mode.", 
										keyObj.getClass().getName(), 
										keyObj.toString(),
										( valueObj == null) ? "null" : valueObj.toString(),
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction")
										)
								)
						);
			}
		}
	}
	
	/**
	 * Get deep copy of keyObj. <br />
	 * <b>Thread safety:</b> conditional thread safe; thread saefty on keyObj input needs to be provided.
	 * @param keyObj
	 * @param valueObj : just for logging purpose
	 * @return Deep copy of keyObj. null when keyObj is null.
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when keyObj is 
	 * NoObject instance, keyObj is not serializable or deep copy of keyObj input is not identified as 
	 * equivalent. 
	 * TransactionalMemcacheServiceException when it is detected that cache has held object what keyObj 
	 * input refers to. 
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing.
	 */
	protected <T extends Object> T prepKeyCached( final T keyObj, final Object valueObj) {
		if ( keyObj == null) {
			return null;
		}
		else {
			// Get copy of keyObj
			return getKeyCopy( keyObj, valueObj);
		}
	} // protected Serializable prepKeyCached( final Object keyObj, final Object valueObj)
	
	/**
	 * Perform the following validations:
	 * <ul>
	 * <li>valueObj is not NoObject instance</li>
	 * <li>type of valueObj has implemented Serializable interface</li>
	 * </ul>
	 * <b>Thread safety:</b> thread safe (although immutability of valueObj input should be assured through 
	 * execution.)
	 * @param valueObj
	 * @param keyObj
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in the following cases:
	 * <ul>
	 * <li>valueObj input is NoObject instance</li>
	 * <li>type of valueObj has not implemented Serializable interface</li>
	 * </ul>
	 */
	protected void validateValue( final Object valueObj, final Object keyObj) {
		if ( valueObj == null) {
			if ( logger.isDebugEnabled()) {
				StackTraceElement[] stackTraceElementArray = Thread.currentThread().getStackTrace();
				String callingMethodsInfo = getCallSequenceStr( stackTraceElementArray, 3, 3);
				logger.debug( 
						String.format(
								"It is not recommneded caching null as value (for %1$s key " 
								+ "into the cache space of what namespace is %2$s.) "
								+ "Caching was attempted at next call sequence: %3$s",
								( (valueObj == null) ? "null" : valueObj.toString()),
								memcacheNamespace,
								callingMethodsInfo
								)
						);
			}
		}
		else {
			// Deny NoObject instance as value
			if ( valueObj instanceof NoObject) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException( 
								String.format(
										"It is not allowed to cache a NoObject instance as value " 
										+ "(for %1$s key into the cache space of what namespace is %2$s.) " 
										+ "Encountered during %3$s mode.", 
										( (keyObj == null) ? "null" : keyObj.toString()),
										memcacheNamespace,
										( transactionMode ? "transaction" : "non-transaction")
										)
								)
						);
			}
			
			// Briefly check serializability of valueObj 
			if ( !briefSerializabilityCheck( valueObj)) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				throw new TransactionalMemcacheServiceException(
						new IllegalArgumentException( 
								String.format( 
										"Type (%1$s) of value (%2$s) being cached for %3$s key into " 
										+ "the cache space (of what namespace is %4$s) is not serializable. " 
										+ "Encountered during %5$s mode.", 
										valueObj.getClass().getName(), 
										valueObj.toString(),
										( keyObj == null) ? "null" : keyObj.toString(), 
										memcacheNamespace,
										( transactionMode ? "transaction" : "non-transaction")
										)
								)
						);
			}
		}
	} // protected void validateValue( final Object valueObj, final Object keyObj)
	
	/**
	 * Being called from putForTransaction method. <br />
	 * Perform the followings:
	 * <ul>
	 * <li>For transaction mode, return deep copy of valueObj by either finding pre-existence 
	 * of deep copy object or generating one. Since this method is called only from putForTransaction 
	 * method (deep copy won't be handed over to external code), can reuse the same deep copy when it's 
	 * exist.</li>
	 * <li>For non-transction mode, just return valueObj after serializability check since no necessity 
	 * of creating deep copy in non-transction mode.</li>
	 * </ul>
	 * <b>Thread safety:</b> Thread safe as long as thread safety is assured with valueObj input. 
	 * Lock on serializableChecker instance.
	 * @param valueObj : should be one handed over from external code. 
	 * @param keyObj : Just for logging purpose only.
	 * @return Deep copy of valueObj when transaction mode, just return valueObj when non-transaction mode.
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when valueObj is 
	 * NoObject instance or is not serializable. 
	 * TransactionalMemcacheServiceException when valueObj input refer to data in transactionLocalCache 
	 * since direct reference to data of entry in transactionLocalCache should not be occurred.
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing.
	 */
	protected <T extends Object> T prepValueCached( final T valueObj, final Object keyObj) {
		if ( valueObj == null) {
			return null;
		}
		
		T valueCopy = null;
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) { 
		// in transaction mode
			// Get copy of valueObj
			valueCopy = getValueCopy( valueObj, keyObj);
		}
		else { // in not-transaction mode
			// Check serializability by actually serializing
			if ( !serializableChecker.isSerializable( valueObj)) {
				throw new TransactionalMemcacheServiceException( 
						new IllegalArgumentException( 
								String.format( 
										"Type (%1$s) of value (%2$s) being cached for %3$s key into " 
										+ "the cache space (of what namespace is %4$s) is not serializable. " 
										+ "Encountered during non-transaction mode.", 
										valueObj.getClass().getName(), 
										valueObj.toString(),
										( keyObj == null) ? "null" : keyObj.toString(), 
										memcacheNamespace
										)
								)
						);
			}
			// Not necessary to actually generate deep copy for non-transaction mode
			valueCopy = valueObj;
		}
		
		return valueCopy;
	} // protected Serializable prepValueCached( final Object valueObj, final Object keyObj)
	
	/**
	 * Perform clearing data cached during transaction for target namespace of cache. 
	 * Called by clear method and clearForTransaction( EventInfo eventInfo) method. <br />
	 * <b>Thread safety:</b> thread safe.
	 */
	protected void clearForTransaction() {
		TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
		transactionHandler.transactionLocalCache.clear();
		transactionHandler.keysMapForTransaction.clear();
		transactionHandler.clearInvocationInsident = true;
			// will be ignored later when clearAll method invocation set this true 
		// Do not clear transactionHandler.mainCacheSnapshot at here
	}
	
	/**
	 * Perform clearing data cached during transaction for target namespace of cache. 
	 * Being executed by ClearAllInTransactionEvent event triggered by 
	 * TransactionalMemcacheServiceTransactionHelperImpl.clearAll method. <br />
	 * <b>Thread safety:</b> thread safe.
	 */
	@OnEvent( value=ClearAllInTransactionEvent.class, amongInstancesInThread=false)
	protected void clearForTransaction( EventInfo eventInfo) {
		clearForTransaction();
	}
	// --------------------------------------------------------------------------------------------
	
	
	/**
	 * From mainCache, delete entries of what key is found in keys set in mainCache and keys set 
	 * for mainCache (specific namespace) in keysMap static member field.
	 * And clear keys set in mainCache and keys set for mainCache (specific namespace) 
	 * in keysMap static member field. <br />
	 * <b>Thread safety:</b> thread safe. Locks on mainCacheLock member field.
	 */
	public void clear() {
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			if ( logger.isInfoEnabled()) {
				String message 
				= String.format(
						"clear method is called in transaction mode: " 
						+ "changes (what have been made so far during transaction and supporse to be " 
						+ "exported to %1$s namespace cache at the end of transaction) are cleared out, " 
						+ "and all acknowledged entries in %1$s namespace cache are cleared too.",
						mainCache.getNamespace()
						);
				logger.info( String.format( message));
			}
			
			clearForTransaction();
		}
		else { // non-transaction mode
			if ( logger.isInfoEnabled()) {
				String message 
				= String.format(
						"clear method is called in non-transaction mode: " 
						+ "all acknowledged entries in %1$s namespace cache are cleared.",
						mainCache.getNamespace()
						);
				logger.info( String.format( message));
			}
			
			acquireMainCacheLock();
			try {
				String keyForKeysSetInMainCache = getKeySetKey();
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				if ( keySetInMainCache != null) {
					mainCache.deleteAll( keySetInMainCache);
					keysSet.removeAll( keySetInMainCache);
				}
				mainCache.deleteAll( keysSet);
				
				keysSet.clear();
				mainCache.put( keyForKeysSetInMainCache, keysSet);
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public void clear()
	
	// Overwritten methods of MemcacheService interface methods -----------------------------------
	/**
	 * Clear whole memcache regardless namespace.
	 * Clear keysMap static member field.
	 * <b>Thread safety:</b> thread safe. 
	 */
	@Override
	public void clearAll() {
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			transactionalMemcacheServiceTransactionHelper.clearAllInTransaction();
		}
		else {
			transactionalMemcacheServiceSyncHelper.clearAllForNonTransactionMode();
		}
	} // public void clearAll()
	
	
	@Override
	public String getNamespace() {
		return memcacheNamespace;
	}


	/**
	 * Store key, value, expires, and policy into transactionLocalCache member field of 
	 * transactionHandlerThreadLocal ThreadLocal static member field.
	 * Store key and its deep copy into keysMapForTransaction member field of 
	 * transactionHandlerThreadLocal ThreadLocal static member field. <br />
	 * <b>Thread safety:</b> conditional thread safe; thread safety on key and value inputs need to be 
	 * provided. 
	 * @param key
	 * @param value
	 * @param expires
	 * @param policy
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException when key input is 
	 * NoObject instance, key input is not serializable, deep copy of key input is not identified as
	 * equivalent, value input is NoObject instance or is not serializable. 
	 * TransactionalMemcacheServiceException when it is detected that key input or value input refer to 
	 * data in transactionLocalCache since direct reference to data of entry in transactionLocalCache 
	 * should not be occurred.
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing.
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException when policy is not 
	 * either null or SET_ALWAYS.
	 */
	protected <K,V extends Object> void putForTrnsaction(
			final K key, final V value, final Expiration expires, final SetPolicy policy) 
	{
		ValueExpirationSetPolicy valueExpirationSetPolicy 
		= new ValueExpirationSetPolicy( expires, policy, getCopier());
		
		Serializable keyCopy;
		V valueCopy;
		boolean transactionMode;
		switch( valueExpirationSetPolicy.getSetPolicy()) {
		case SET_ALWAYS:
			keyCopy = (Serializable)prepKeyCached( key, value);
			valueCopy = prepValueCached( value, key);

			valueExpirationSetPolicy.setValue( valueCopy);
			
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			transactionHandler.transactionLocalCache.put( keyCopy, valueExpirationSetPolicy);
			transactionHandler.keysMapForTransaction.put( keyCopy, keyCopy);
			
			return;
		case ADD_ONLY_IF_NOT_PRESENT:
		case REPLACE_ONLY_IF_PRESENT:
			transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							String.format(
									"Failure in caching data entry of key (%1$s) and value (%2$s) " 
									+ "into the cache space (of what namespace is %3$s.) " 
									+ "SetPolicy's %4$s is not supported in transaction mode. " 
									+ "Encountered during %5$s mode.",
									((key == null) ? "null" : key.toString()),
									((value == null) ? "null" : value.toString()),
									memcacheNamespace,
									valueExpirationSetPolicy.getSetPolicy().toString(), 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		default:
			transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			// Throw a UnsupportedOperationException wrapped by TransactionalMemcacheServiceException
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							String.format(
									"Failure in caching data entry of key (%1$s) and value (%2$s) " 
									+ "into the cache space (of what namespace is %3$s.) " 
									+ "Unrecognized SetPolicy enum value: %4$s."
									+ "Encountered during %5$s mode.",
									((key == null) ? "null" : key.toString()),
									((value == null) ? "null" : value.toString()),
									memcacheNamespace,
									valueExpirationSetPolicy.getSetPolicy(), 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		} // switch( setPolicy)
	} // protected void putForTrnsaction( ....)
	
	/**
	 * <b>Thread safety:</b> conditional thread safe; thread safety on key and value inputs need to be 
	 * provided. Locks on mainCacheLock member field. 
	 * @param key
	 * @param value
	 * @param expires
	 * @param policy
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in following cases:
	 * <ul>
	 * <li>key input is NoObject instance</li>
	 * <li>key input equals to keysSetKey</li>
	 * <li>key input is not serializable</li>
	 * <li>deep copy of key input is not identified as equivalent</li>
	 * <li>value input is NoObject instance</li>
	 * <li>value input is not serializable</li>
	 * </ul>
	 * TransactionalMemcacheServiceException when it is detected that key input or value input refer to 
	 * data in transactionLocalCache since direct reference to data of entry in transactionLocalCache 
	 * should not be occurred. <br />
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing. <br />
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException when policy is not 
	 * either null or SET_ALWAYS.
	 */
	@Override
	public boolean put( 
			final Object key, final Object value, final Expiration expires, final SetPolicy policy) {
		
		String keyForKeysSetInMainCache = getKeySetKey();
		
		validateKey( key, keyForKeysSetInMainCache, value);
		validateValue( value, key);
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) { // transaction mode
			putForTrnsaction( key, value, expires, policy);
			return true;
		}
		else { // logics for non-transaction mode
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				SearchResult<Object> searchResult = syncKeysSetWithMainCache( key, keyForKeysSetInMainCache);
				Object keyCopy = searchResult.getCachedObj();
				if ( !searchResult.isFound()) {
					keyCopy = generateKeyCopy( key, value);
				}
				else if (( keyCopy == key) && (key != null)) {
					boolean transactionMode 
					= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
					throw new TransactionalMemcacheServiceException( 
							String.format(
									"Given key object (%1$s) to be being cached refers to one of key objects " 
									+ "held in field used to manage cache. This should not have happened; " 
									+ "instead, it is designed as saving relation status of key and value " 
									+ "at some point of time that only deep copy of each key is held in such " 
									+ "field. Probably key copy in cache has been sneakily leaking out to " 
									+ "external code. The class of the key is %2$s. " 
									+ "And The key was to cache value: %3$s. "
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %4$s during %5$s mode.)", 
									key.toString(),
									key.getClass().toString(),
									( ( value == null) ? "null" : value.toString()), 
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							);
				}
				
				boolean result = mainCache.put( key, value, expires, policy);
				if ( result) {
					Set<Serializable> keysSet 
					= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
					keysSet.add( (Serializable)keyCopy);
					
					Set<Serializable> keySetInMainCache 
					= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
					if ( keySetInMainCache == null) {
					// either mainCache has been most likely cleared or keysSet entry in mainCache has been evicted
						
						// After sync keysSet entry in keysMap static member field with 
						// data entries in mainCache, reconstruct of keysSet entry in mainCache 
						// from keysSet entry in keysMap static member field
						syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
					}
					else {
						keySetInMainCache.add( (Serializable)key);
						mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
					}
				}
				return result;
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public boolean put( Object key, Object value, Expiration expires, SetPolicy policy)
	
	/**
	 * <b>Thread safety:</b> conditional thread safe; thread safety on key and value inputs need to be 
	 * provided. Locks mainCacheLock member field.
	 * @param key
	 * @param value
	 * @param expires
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in following cases:
	 * <ul>
	 * <li>key input is NoObject instance</li>
	 * <li>key input equals to keysSetKey</li>
	 * <li>key input is not serializable</li>
	 * <li>deep copy of key input is not identified as equivalent</li>
	 * <li>value input is NoObject instance</li>
	 * <li>value input is not serializable</li>
	 * </ul>
	 * TransactionalMemcacheServiceException when it is detected that key input or value input refer to 
	 * data in transactionLocalCache since direct reference to data of entry in transactionLocalCache 
	 * should not be occurred. <br />
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing. <br />
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException when policy is not 
	 * either null or SET_ALWAYS.
	 */
	@Override
	public void put( final Object key, final Object value, final Expiration expires) {
		
		String keyForKeysSetInMainCache = getKeySetKey();
		
		validateKey( key, keyForKeysSetInMainCache, value);
		validateValue( value, key);
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) { // transaction mode
			putForTrnsaction( key, value, expires, null);
		}
		else { // logics for non-transaction mode
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				SearchResult<Object> searchResult = syncKeysSetWithMainCache( key, keyForKeysSetInMainCache);
				Object keyCopy = searchResult.getCachedObj(); 
				if ( !searchResult.isFound()) {
					keyCopy = generateKeyCopy( key, value);
				}
				else if (( keyCopy == key) && (key != null)) {
					boolean transactionMode 
					= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
					throw new TransactionalMemcacheServiceException( 
							String.format(
									"Given key object (%1$s) to be being cached refers to one of key objects " 
									+ "held in field used to manage cache. This should not have happened; " 
									+ "instead, it is designed as saving relation status of key and value " 
									+ "at some point of time that only deep copy of each key is held in such " 
									+ "field. Probably key copy in cache has been sneakily leaking out to " 
									+ "external code. The class of the key is %2$s. " 
									+ "And The key was to cache value: %3$s. "
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %4$s during %5$s mode.)", 
									key.toString(),
									key.getClass().toString(),
									( ( value == null) ? "null" : value.toString()),
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							);
				}

				mainCache.put( key, value, expires);
				
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				keysSet.add( (Serializable)keyCopy);
				
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				if ( keySetInMainCache == null) {
				// either mainCache has been most likely cleared or keysSet entry in mainCache has been evicted
					
					// After sync keysSet entry in keysMap static member field with 
					// data entries in mainCache, reconstruct of keysSet entry in mainCache 
					// from keysSet entry in keysMap static member field
					syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				}
				else {
					keySetInMainCache.add( (Serializable)key);
					mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
				}
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public void put( Object key, Object value, Expiration expires)
	
	/**
	 * <b>Thread safety:</b> conditional thread safe; thread safety on key and value inputs needs to be 
	 * provided. Locks on mainCacheLock member field.
	 * @param key
	 * @param value
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in following cases:
	 * <ul>
	 * <li>key input is NoObject instance</li>
	 * <li>key input equals to keysSetKey</li>
	 * <li>key input is not serializable</li>
	 * <li>deep copy of key input is not identified as equivalent</li>
	 * <li>value input is NoObject instance</li>
	 * <li>value input is not serializable</li>
	 * </ul>
	 * TransactionalMemcacheServiceException when it is detected that key input or value input refer to 
	 * data in transactionLocalCache since direct reference to data of entry in transactionLocalCache 
	 * should not be occurred. <br />
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serializing and de-serializing. <br />
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException when policy is not 
	 * either null or SET_ALWAYS.
	 */
	@Override
	public void put( final Object key, final Object value) {
		
		String keyForKeysSetInMainCache = getKeySetKey();
		
		validateKey( key, keyForKeysSetInMainCache, value);
		validateValue( value, key);
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) { // transaction mode
			putForTrnsaction( key, value, null, null);
		}
		else { // logics for non-transaction mode
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				SearchResult<Object> searchResult = syncKeysSetWithMainCache( key, keyForKeysSetInMainCache); 
				Object keyCopy = searchResult.getCachedObj();
				if ( !searchResult.isFound()) {
					keyCopy = generateKeyCopy( key, value);
				}
				else if (( keyCopy == key) && (key != null)) {
					boolean transactionMode 
					= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
					throw new TransactionalMemcacheServiceException( 
							String.format(
									"Given key object (%1$s) to be being cached refers to one of key objects " 
									+ "held in field used to manage cache. This should not have happened; " 
									+ "instead, it is designed as saving relation status of key and value " 
									+ "at some point of time that only deep copy of each key is held in such " 
									+ "field. Probably key copy in cache has been sneakily leaking out to " 
									+ "external code. The class of the key is %2$s. " 
									+ "And The key was to cache value: %3$s. "
									+ "(Encountered this while working on the cache space of what " 
									+ "namespace is %4$s during %5$s mode.)", 
									key.toString(),
									key.getClass().toString(),
									( ( value == null) ? "null" : value.toString()),
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							);
				}
				
				mainCache.put( key, value);
				
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				keysSet.add( (Serializable)keyCopy);
				
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				if ( keySetInMainCache == null) { 
				// either mainCache has been most likely cleared or keysSet entry in mainCache has been evicted
					
					// After sync keysSet entry in keysMap static member field with 
					// data entries in mainCache, reconstruct of keysSet entry in mainCache 
					// from keysSet entry in keysMap static member field
					syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				}
				else {
					keySetInMainCache.add( (Serializable)key);
					mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
				}
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public void put( Object key, Object value)
	
	/**
	 * Perform the following validations:
	 * <ul>
	 * <li>Whether entries input is serializable by serializing and de-serializing.</li>
	 * <li>Whether deep copy of keys set from entries input is identified as equivalent to original keys set.</li>
	 * <li>Return deep copy of entries</li>
	 * </ul>
	 * <b>Thread safety:</b> thread safe as long as thread safety on entries input is assured.
	 * @param entries  
	 * @return deep copy of keys set from values input. 
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in the following cases: 
	 * <ul>
	 * <li>entries input is null</li>
	 * <li>entries input contains NoObject instance</li>
	 * <li>keys set from entries input contains keySetKey value</li>
	 * <li>type of entries input has not implemented Serializable interface</li>
	 * <li>generated deep copy of keys set from entries input is not identified as equivalent to original keys set</li>
	 * </ul> 
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serialization of entries input.
	 */
	protected <T> Map<T, ?> varidateKeysAndValues( final Map<T, ?> entries, final String keysSetKey) {
		if ( entries == null) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException( 
					new IllegalArgumentException(
							String.format( 
									"entries Map argument is null. It has gotten to be a Map instance " 
									+ "in order to add data entries into the cache of what " 
									+ "namespace is %1$s. Encountered during %2$s mode.", 
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		
		// Check for NoObject entry -----------------------------------------------------------
		NoObject noObject = new NoObject();
		Set<T> keysSet = entries.keySet();
		Collection<?> valuesCollection = entries.values();
		
		if ( keysSet.contains( noObject)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException( 
							String.format(
									"Found NoObject instance as key for %1$s value in entries Map " 
									+ "argument: %2$s" 
									+ "%nIt is not allowed using a NoObject instance as key (to cache " 
									+ "%1$s value into the cache space of what namespace is %3$s.) " 
									+ "Encountered during %4$s mode.",
									( (entries.get( noObject) == null) 
											? "null" : entries.get( noObject).toString()),
									entries.toString(),
									memcacheNamespace, 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		if ( valuesCollection.contains( noObject)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException( 
							String.format(
									"Found NoObject instance as value in entries Map argument: %1$s" 
									+ "%nIt is not allowed cacheing NoObject instance into the cache space " 
									+ "(of what namespace is %2$s.) Encountered during %3$s mode.",
									entries.toString(),
									memcacheNamespace, 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		// ------------------------------------------------------------------------------------
		// Check on keySetKey as key ----------------------------------------------------------
		if ( keysSet.contains( keysSetKey)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException(
							String.format( 
									"Found %1$s as key for %2$s value in entries Map argument: %3$s" 
									+ "%n%1$s is reserved and not allowed to be used as key to cache " 
									+ "value (%2$s) into the cache space of what namespace is %4$s. " 
									+ "Encounterd during %5$s mode.", 
									keysSetKey,
									( (entries.get( keysSetKey) == null) 
											? "null" : entries.get( keysSetKey).toString()),
									entries.toString(),
									memcacheNamespace, 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		// ------------------------------------------------------------------------------------
		// Check for serializability briefly --------------------------------------------------
		if ( !briefSerializabilityCheck( entries)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException(
							String.format( 
									"Since entries Map argument is not serializable, failed to cache " 
									+ "data entries into the the cache space (of what namespace is %1$s) " 
									+ "from entries Map argument: %2$s"
									+ "%n(Encountered during %3$s mode.)", 
									memcacheNamespace,
									entries.toString(),
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		// ------------------------------------------------------------------------------------
		
		Map<T, ?>  entriesCopy = generateCopy( entries);
		Set<T> keysSetCopy = entriesCopy.keySet();

		// Check equivalence between original and copy ----------------------------------------
		if ( !keysSet.equals( keysSetCopy)) {
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			throw new TransactionalMemcacheServiceException(
					new IllegalArgumentException(
							String.format(
									"There is/are key(s) of what deep copy was not identified as equal " 
									+ "to orignal in entries Map argument:%1$s. "
									+ "(Encountered this while working on the cache space of what namespace " 
									+ "is %2$s during %3$s mode.)", 
									entries.toString(), 
									memcacheNamespace,
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		}
		// ------------------------------------------------------------------------------------
		
		// Check for null entry ---------------------------------------------------------------
		String stackTraceStr = null;
		if ( keysSet.contains( null)) {
			if ( logger.isInfoEnabled()) {
				if ( stackTraceStr == null) {
					stackTraceStr = getCallSequenceStr( Thread.currentThread().getStackTrace(), 3, 3); 
				}
				
				logger.info( 
						String.format(
								"Found null key for %1$s value in entries Map argument: %2$s" 
								+ "%nIt is not recommended using null as key to cache %1$s value into " 
								+ "the cache space of what namespace is %3$s. " 
								+ "%nCaching was attempted at next call sequence: %n%4$s",
								( (entries.get( null) == null) ? "null" : entries.get( null).toString()),
								entries.toString(),
								memcacheNamespace,
								stackTraceStr
								)
						);
			}
		}
		if ( valuesCollection.contains( null)) {
			if ( logger.isDebugEnabled()) {
				if ( stackTraceStr == null) {
					stackTraceStr = getCallSequenceStr( Thread.currentThread().getStackTrace(), 3, 3); 
				}
				
				logger.debug( 
						String.format(
								"Found null value in entries Map argument: %1$s" 
								+ "%nIt is not recommended caching null as value into " 
								+ "the cache space of what namespace is %2$s. " 
								+ "%nCaching was attempted at next call sequence: %n%3$s",
								entries.toString(),
								memcacheNamespace,
								stackTraceStr
								)
						);
			}
		}
		// ------------------------------------------------------------------------------------
		
		return entriesCopy;
	} // protected Map<Serializable, Object> varidateKeysAndValues( final Map<?, ?> entries)
	
	/**
	 * Put data entries in entriesCopy input transactionLocalCache member field of 
	 * transactionHandlerThreadLocal ThreadLocal static member field together with expires input and 
	 * policy input.
	 * <b>Thread safety:</b> thread safe as long as thread safety on entriesCopy input is assured.
	 * @param entriesCopy : each entry held in entriesCopy input must hold deep copy objects of ones handed over from external code.
	 * @param expires
	 * @param policy
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException 
	 * when entriesCopy input contains value of what policy is neither null or SET_ALWAYS.
	 */
	protected <T> void putAllForTrnsaction( 
			final Map<T, ?> entriesCopy, 
			final Expiration expires, 
			final SetPolicy policy
			) 
	{
		SetPolicy policyObj = policy;
			if ( policyObj == null) {
				policyObj = SetPolicy.SET_ALWAYS;
			}
		
		switch( policyObj) {
		case SET_ALWAYS:
			
			for( Entry<T, ?> entry : entriesCopy.entrySet()) {

				Serializable keyCopy = (Serializable)entry.getKey();
				Object valueCopy = entry.getValue();
				
				ValueExpirationSetPolicy valueExpirationSetPolicy 
				= new ValueExpirationSetPolicy( expires, policyObj, getCopier());
				valueExpirationSetPolicy.setValue( valueCopy);
				
				TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
				transactionHandler.transactionLocalCache.put( keyCopy, valueExpirationSetPolicy);
				transactionHandler.keysMapForTransaction.put( keyCopy, keyCopy);
			} // for
			
			return;
		case ADD_ONLY_IF_NOT_PRESENT:
		case REPLACE_ONLY_IF_PRESENT:
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							String.format(
									"Failure in caching data entries from %1$s into the cache space " 
									+ "(of what namespace is %2$s.) " 
									+ "SetPolicy's %3$s is not supported in transaction mode.",
									entriesCopy.toString(),
									memcacheNamespace,
									policyObj.toString()
									)
							)
					);
		default:
			boolean transactionMode 
			= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
			// Throw a UnsupportedOperationException wrapped by TransactionalMemcacheServiceException
			throw new TransactionalMemcacheServiceException(
					new UnsupportedOperationException(
							String.format(
									"Failure in caching data entries from %1$s into the cache space " 
									+ "(of what namespace is %2$s) due to unrecognized SetPolicy enum " 
									+ "value: %3$s. "
									+ "%n(Encountered this during %4$s mode.)", 
									entriesCopy.toString(),
									memcacheNamespace,
									policyObj.toString(), 
									( transactionMode ? "transaction" : "non-transaction")
									)
							)
					);
		} // switch( setPolicy)
	} // protected void putAllForTrnsaction( ...)
	
	/**
	 * <b>Thread safety:</b> conditional thread safe; thready safety on entries input needs to be provided. 
	 * Locks on mainCacheLock member field.
	 * @param entries
	 * @param expires
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in the following cases: 
	 * <ul>
	 * <li>entries input is null</li>
	 * <li>entries input contains NoObject instance</li>
	 * <li>keys set from entries input contains keySetKey value</li>
	 * <li>type of entries input has not implemented Serializable interface</li>
	 * <li>generated deep copy of keys set from entries input is not identified as equivalent to original keys set</li>
	 * </ul> 
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serialization of entries input. <br />
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException 
	 * when entries input contains value of what policy is neither null or SET_ALWAYS.
	 */
	@Override
	public void putAll( final Map<?, ?> entries, final Expiration expires) {
		String keyForKeysSetInMainCache = getKeySetKey();
		
		Map<?, ?> entriesCopy = varidateKeysAndValues( entries, keyForKeysSetInMainCache);
			
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			putAllForTrnsaction( entriesCopy, expires, null);
		}
		else { // logics for non-transaction mode
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				
				mainCache.putAll( entriesCopy, expires);
				
				Set<Serializable> entriesKeysSetCopy = (Set<Serializable>)entriesCopy.keySet();
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				
				// Update keysSet entry in keysMap static member field
				keysSet.addAll( entriesKeysSetCopy);
				
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				if ( keySetInMainCache == null) {
				// either mainCache has been most likely cleared or keysSet entry in mainCache has been evicted
					
					// After sync keysSet entry in keysMap static member field with 
					// data entries in mainCache, reconstruct of keysSet entry in mainCache 
					// from keysSet entry in keysMap static member field
					syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				}
				else {
					keySetInMainCache.addAll( entriesKeysSetCopy);
					mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
				}
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public void putAll(Map<?, ?> values, Expiration expires)
	/**
	 * <b>Thread safety:</b> conditional thread safe; thread safety on values input needs to be provided. 
	 * Locks on mainCacheLock member field.
	 * @param entries
	 * @param expires
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in the following cases: 
	 * <ul>
	 * <li>entries input is null</li>
	 * <li>entries input contains NoObject instance</li>
	 * <li>keys set from entries input contains keySetKey value</li>
	 * <li>type of entries input has not implemented Serializable interface</li>
	 * <li>generated deep copy of keys set from entries input is not identified as equivalent to original keys set</li>
	 * </ul> 
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serialization of entries input. <br />
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException 
	 * when entries input contains value of what policy is neither null or SET_ALWAYS.
	 */
	@Override
	public void putAll( final Map<?, ?> values) {
		putAll( values, null);
	} // public void putAll( final Map<?, ?> values)
	/**
	 * <b>Thread safety:</b> conditional thread safe; thread safety on entries input needs to be provided 
	 * and about expires input and policy input as well. Locks on mainCacheLock member field.
	 * @param entries
	 * @param expires
	 * @param policy
	 * @throws TransactionalMemcacheServiceException wrapping IllegalArgumentException in the following cases: 
	 * <ul>
	 * <li>entries input is null</li>
	 * <li>entries input contains NoObject instance</li>
	 * <li>keys set from entries input contains keySetKey value</li>
	 * <li>type of entries input has not implemented Serializable interface</li>
	 * <li>generated deep copy of keys set from entries input is not identified as equivalent to original keys set</li>
	 * </ul> 
	 * TransactionalMemcacheServiceException wrapping Throwable caused during serialization of entries input. <br />
	 * TransactionalMemcacheServiceException wrapping UnsupportedOperationException 
	 * when entries input contains value of what policy is neither null or SET_ALWAYS.
	 */
	@Override
	public <T> Set<T> putAll( final Map<T, ?> entries, final Expiration expires, final SetPolicy policy) {
		String keyForKeysSetInMainCache = getKeySetKey();
		
		Map<T, ?> entriesCopy = varidateKeysAndValues( entries, keyForKeysSetInMainCache);
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			putAllForTrnsaction( entriesCopy, expires, policy);
			return entries.keySet();
		}
		else { // non-transaction mode
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				
				// Add data entries in values into mainCache
				Set<T> addedKeysSet = mainCache.putAll( entriesCopy, expires, policy);
				
				Set<Serializable> entriesKeysSetCopy = (Set<Serializable>)entriesCopy.keySet();
				entriesKeysSetCopy.retainAll( addedKeysSet);
				
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				// Update keysSet entry in keysMap static member field
				keysSet.addAll( entriesKeysSetCopy);
				
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				if ( keySetInMainCache == null) { 
				// either mainCache has been most likely cleared or keysSet entry in mainCache has been evicted
					
					// After sync keysSet entry in keysMap static member field with 
					// data entries in mainCache, reconstruct of keysSet entry in mainCache 
					// from keysSet entry in keysMap static member field
					syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				}
				else {
					keySetInMainCache.addAll( entriesKeysSetCopy);
					mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
				}
				
				return addedKeysSet;
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public <T> Set<T> putAll( Map<T, ?> values, Expiration expires, SetPolicy policy)
	
	@Override
	public boolean putIfUntouched(
			final Object key, 
			final IdentifiableValue oldValue, final Object newValue, 
			final Expiration expires) {
		
		boolean transactionMode = transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						String.format(
								"putIfUntouched method is not supported. "
								+ "(Encountered this while working on the cache space of what namespace " 
								+ "is %2$s during %3$s mode.)", 
								memcacheNamespace,
								( transactionMode ? "transaction" : "non-transaction")
								)
						)
				);
	}
	@Override
	public boolean putIfUntouched( 
			final Object key, final IdentifiableValue oldValue, final Object newValue) {
		
		boolean transactionMode = transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						String.format(
								"putIfUntouched method is not supported. "
								+ "(Encountered this while working on the cache space of what namespace " 
								+ "is %2$s during %3$s mode.)", 
								memcacheNamespace,
								( transactionMode ? "transaction" : "non-transaction")
								)
						)
				);
	}
	
	/**
	 * 
	 * @param key
	 * @throws TransactionalMemcacheServiceException for next cases:
	 * <ul>
	 * <li>current thread is not in transaction mode.</li>
	 * <li>key input refers to data in transactionLocalCache since direct reference to data of entry in transactionLocalCache should not be occurred.</li>
	 * <li>deep copy of key input held in keysMapForTransaction is not identifeid as equivalent to key input</li>
	 * </ul>
	 */
	protected void integrityCheckOnKeysMapForTransaction( Object key) {
		if ( !transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
			
			throw new TransactionalMemcacheServiceException( 
					new UnsupportedOperationException(
							String.format(
									"It is not allowed to invoke integrityCheckOnKeysMapForTransaction " 
									+ "in non-transaction mode. It was called to check for %1$s key in " 
									+ "the cache space of what namespace is %2$s.", 
									((key == null) ? "null" : key.toString()),
									memcacheNamespace
									)
							)
					);
		}
		
		TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
		
		Map<Serializable, Serializable> keysMapForTransaction 
		= transactionHandler.keysMapForTransaction;
		Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
		= transactionHandler.transactionLocalCache;
		
		if ( keysMapForTransaction.containsKey( key)) {
			Serializable cachedKey = keysMapForTransaction.get( key);
			if ( key == null) {
				if ( key != cachedKey) {
					String message 
					= String.format(
							"Found during transaction mode that keysMapForTransaction field of " 
							+ "transactionHandlerThreadLocal ThreadLocal static member field holds " 
							+ "non equivalent object (%1$s) as deep copy of key input (%2$s)." 
							+ "This should not happened; keysMapForTransaction field should hold " 
							+ "only deep copy object of key identified as equivalent to " 
							+ "original key object. ", 
							( (cachedKey == null) ? "null" : cachedKey.toString()),
							( (key == null) ? "null" : key.toString())
							);
					
					if ( transactionLocalCache.containsKey( key)) {
						message 
						= message + String.format(
								"The cached value for that key is : %1$s. ",
								(( transactionLocalCache.get( key) == null) 
										? "null" : transactionLocalCache.get( key).toString())
								);
					}
					
					message 
					= message + String.format(
							"(Encountered this while working on the cache space of what namespace " 
							+ "is %1$s during transaction mode.)", 
							memcacheNamespace
							);
					
					throw new TransactionalMemcacheServiceException( message);
				}
			}
			else { // key != null
				if ( key == cachedKey) {
					String message 
					= String.format(
							"Detected that object (%1$s) referred by key input has been held in " 
							+ "keysMapForTransaction field of transactionHandlerThreadLocal " 
							+ "ThreadLocal static member field. This should not have happened; " 
							+ "instead, it is designed as saving only deep copy of each key. " 
							+ "It may be that key copy in cache has been sneakily leaking out to " 
							+ "external code. The class of the key is %2$s. ", 
							key.toString(),
							key.getClass().toString()
							);
					
					if ( transactionLocalCache.containsKey( key)) {
						message 
						= message + String.format(
								"The cached value for that key is : %1$s. ",
								(( transactionLocalCache.get( key) == null) 
										? "null" : transactionLocalCache.get( key).toString())
								);
					}
					
					message 
					= message + String.format(
							"(Encountered this while working on the cache space of what namespace " 
							+ "is %1$s during transaction mode.)", 
							memcacheNamespace
							);
					
					throw new TransactionalMemcacheServiceException( message);
				}
				else if ( !key.equals( cachedKey)) {
					String message 
					= String.format(
							"Found during transaction mode that keysMapForTransaction field of " 
							+ "transactionHandlerThreadLocal ThreadLocal static member field holds " 
							+ "non equivalent object (%1$s) as deep copy of key input (%2$s)." 
							+ "This should not happened; keysMapForTransaction field should hold " 
							+ "only deep copy object of key identified as equivalent to " 
							+ "original key object. ", 
							( (cachedKey == null) ? "null" : cachedKey.toString()),
							( (key == null) ? "null" : key.toString())
							);
					if ( transactionLocalCache.containsKey( key)) {
						message 
						= message + String.format(
								"The cached value for that key is : %1$s. ",
								(( transactionLocalCache.get( key) == null) 
										? "null" : transactionLocalCache.get( key).toString())
								);
					}
					message 
					= message + String.format(
							"(Encountered this while working on the cache space of what namespace " 
							+ "is %1$s during transaction mode.)", 
							memcacheNamespace
							);
					
					throw new TransactionalMemcacheServiceException( message);
				}
			}
		}
	}

	/**
	 * @return true when key input is found in cache. 
	 * false when it's not found or for the following cases:
	 * <ul>
	 * <li>when key input is NoObject instance</li>
	 * <li>key input equals value of keySetKey member field</li>
	 * </ul> 
	 * @throws TransactionalMemcacheServiceException for next cases:
	 * <ul>
	 * <li>key input refers to data in transactionLocalCache since direct reference to data of entry in transactionLocalCache should not be occurred.</li>
	 * <li>deep copy of key input held in keysMapForTransaction is not identifeid as equivalent to key input</li>
	 * </ul>
	 */
	@Override
	public boolean contains( Object key) {
		
		String keyForKeysSetInMainCache = getKeySetKey();
		
		// Validation on key input ----------------------------------------------------------------
		if ( key instanceof NoObject) {
			if ( logger.isInfoEnabled()) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				String callingMethodsInfo 
				= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
				
				logger.info( 
						String.format(
								"Detected the attempt of obtaining value for NoObject instance key " 
								+ "from cache space (of what namespace is %1$s) in %2$s mode. " 
								+ "Due for NoObject instance to be disallowed for key or value being " 
								+ "stored into cache, there should be no necessity of such attempt. " 
								+ "The attempt was made at next call sequence: %n%3$s",
								memcacheNamespace, 
								( transactionMode ? "transaction" : "non-transaction"),
								callingMethodsInfo
								)
						);
			}
			return false;
		}
		else if ( keyForKeysSetInMainCache.equals( key)) {
			if ( logger.isInfoEnabled()) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				String callingMethodsInfo 
				= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
				
				logger.info( 
						String.format(
								"Detected the attempt of obtaining value for %1$s key " 
								+ "from cache space (of what namespace is %2$s) in %3s mode. " 
								+ "Because %1$s is reserved and not allowed to be used as key of data " 
								+ "entry in the cache, there should be no necessity of such attempt. "
								+ "The attempt was made at next call sequence: %n%4$s",
								keyForKeysSetInMainCache, 
								memcacheNamespace, 
								( transactionMode ? "transaction" : "non-transaction"), 
								callingMethodsInfo
								)
						);
			}
			return false;
		}
		// ----------------------------------------------------------------------------------------
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
		// in transaction mode
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			
			Map<Serializable, Serializable> keysMapForTransaction 
			= transactionHandler.keysMapForTransaction;
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			
			// Integrity check on keysMapForTransaction -------------------------------------------
			integrityCheckOnKeysMapForTransaction( key);
			// ------------------------------------------------------------------------------------
			
			if ( transactionLocalCache.containsKey( key)) {
				return true;
			}
			else {
				return false;
			}
		}
		else { // non-transaction mode

			acquireMainCacheLock();

			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				SearchResult<Object> searchResult 
				= syncKeysSetWithMainCache( key, keyForKeysSetInMainCache);
				
				if ( searchResult.isFound()) {
					Object keyCopy = searchResult.getCachedObj();
					if (( keyCopy == key) && (key != null)) {
						Object value = mainCache.get( key);
						boolean transactionMode 
						= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
						throw new TransactionalMemcacheServiceException( 
								String.format(
										"Given key object (%1$s) refers to one of key objects " 
										+ "held in field used to manage cache. This should not have happened; " 
										+ "instead, it is designed as saving relation status of key and value " 
										+ "at some point of time that only deep copy of each key is held in such " 
										+ "field. Probably key copy in cache has been sneakily leaking out to " 
										+ "external code. The class of the key is %2$s. " 
										+ "And The key was for cached value: %3$s. "
										+ "(Encountered this while working on the cache space of what " 
										+ "namespace is %4$s during %5$s mode.)", 
										key.toString(),
										key.getClass().toString(),
										( ( value == null) ? "null" : value.toString()),
										memcacheNamespace,
										( transactionMode ? "transaction" : "non-transaction")
										)
								);
					}
					
					return true;
				}
				else {
					return false;
				}
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	}
	
	/**
	 * <b>Thread safety:</b> Thread safe. Locks on mainCacheLock member field.
	 * @param key
	 * @return value for key. NoObject instance for next cases: 
	 * <ul>
	 * <li>key input is not found</li>
	 * <li>key input is a NoOjbect instance</li>
	 * <li>key input equals value of keySetKey member field</li>
	 * </ul> 
	 * @throws TransactionalMemcacheServiceException for next cases:
	 * <ul>
	 * <li>key input refers to data in transactionLocalCache since direct reference to data of entry in transactionLocalCache should not be occurred.</li>
	 * <li>deep copy of key input held in keysMapForTransaction is not identifeid as equivalent to key input</li>
	 * <li>transactionLocalCache holds null instead of a ValueExpirationSetPolicy instance as value for key input</li>
	 * <li>found when cached value for key input is a NoObject instance</li>
	 * </ul>
	 */
	@Override
	public Object get( Object key) {
		
		String keyForKeysSetInMainCache = getKeySetKey();
		
		// Validation on key input ----------------------------------------------------------------
		if ( key instanceof NoObject) {
			if ( logger.isInfoEnabled()) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				String callingMethodsInfo 
				= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
				
				logger.info( 
						String.format(
								"Detected the attempt of obtaining value for NoObject instance key " 
								+ "from cache space (of what namespace is %1$s) in %2$s mode. " 
								+ "Due for NoObject instance to be disallowed for key or value being " 
								+ "stored into cache, there should be no necessity of such attempt. " 
								+ "The attempt was made at next call sequence: %n%3$s",
								memcacheNamespace, 
								( transactionMode ? "transaction" : "non-transaction"),
								callingMethodsInfo
								)
						);
			}
			return new NoObject();
		}
		else if ( keyForKeysSetInMainCache.equals( key)) {
			if ( logger.isInfoEnabled()) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				String callingMethodsInfo 
				= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
				
				logger.info( 
						String.format(
								"Detected the attempt of obtaining value for %1$s key " 
								+ "from cache space (of what namespace is %2$s) in %3s mode. " 
								+ "Because %1$s is reserved and not allowed to be used as key of data " 
								+ "entry in the cache, there should be no necessity of such attempt. "
								+ "The attempt was made at next call sequence: %n%4$s",
								keyForKeysSetInMainCache, 
								memcacheNamespace, 
								( transactionMode ? "transaction" : "non-transaction"), 
								callingMethodsInfo
								)
						);
			}
			return new NoObject();
		}
		// ----------------------------------------------------------------------------------------
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
		// in transaction mode
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			
			Map<Serializable, Serializable> keysMapForTransaction 
			= transactionHandler.keysMapForTransaction;
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			
			// Integrity check on keysMapForTransaction -------------------------------------------
			integrityCheckOnKeysMapForTransaction( key);
			// ------------------------------------------------------------------------------------
			
			if ( transactionLocalCache.containsKey( key)) {
				ValueExpirationSetPolicy valueExpirationSetPolicy = transactionLocalCache.get( key);
					// Integrity check on valueExpirationSetPolicy --------------------------------
					if ( valueExpirationSetPolicy == null) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Detected null value for key (%1$s) in cache space (of what " 
										+ "namespace is %2$s) during transaction mode. This should not " 
										+ "have happened and cache should have held ValueExpirationSetPolicy " 
										+ "instance holding cached real value for key instead.", 
										((key == null) ? "null" : key.toString()), 
										memcacheNamespace
										)
								);
						
					}
					else if ( valueExpirationSetPolicy.getValue() instanceof NoObject) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Found that value cached space (of what namespace is %1$s) for " 
										+ "key (%2$s) is NoObject instance in transaction mode. This should " 
										+ "not have happended because NoObject instance is disallowed for " 
										+ "key or value being stored into cache.",
										memcacheNamespace, 
										((key == null) ? "null" : key.toString())
										)
								);
					}
					// ----------------------------------------------------------------------------
				try {
					return valueExpirationSetPolicy.getValueCopy();
				}
				catch( Throwable throwable) {
					throw new TransactionalMemcacheServiceException(
							String.format(
									"Failure in generating copy of %1$s (what is stored value for %2$s key " 
									+ "in cache) by serializing and de-serializing during transaction mode " 
									+ "working on the cache space of what namespace is %3$s.",
									(( valueExpirationSetPolicy.getValue() == null) 
										? "null" : valueExpirationSetPolicy.getValue().toString()),
									(( key == null) ? "null" : key.toString()),
									memcacheNamespace							
									),
							throwable
							);
				}
			}
			else {
				return new NoObject();
			}
		}
		else { // non-transaction mode
			
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				if ( keysSet.contains( key)) {
					Object value = mainCache.get( key);
					
					if ( value instanceof NoObject) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Found that value cached space (of what namespace is %1$s) for key " 
										+ "(%2$s) is NoObject instance in non-transaction mode. This " 
										+ "should not have happended because NoObject instance is " 
										+ "disallowed for key or value being stored into cache.",
										memcacheNamespace, 
										((key == null) ? "null" : key.toString())
										)
								);
					}
					
					return value;
				}
				else {
					return new NoObject();
				}
			}
			finally {
				mainCacheLock.unlock();
			}
		}
	} // public Object get( Object key)
	
	/**
	 * <b>Thread safety:</b> conditional thread safe; thread safety on keys input needs to be provided.  
	 * Locks on mainCacheLock member field.
	 * @param keys
	 * @throws TransactionalMemcacheServiceException for next cases:
	 * <ul>
	 * <li>key input refers to data in transactionLocalCache since direct reference to data of entry in transactionLocalCache should not be occurred.</li>
	 * <li>deep copy of key input held in keysMapForTransaction is not identifeid as equivalent to key input</li>
	 * <li>transactionLocalCache holds null instead of a ValueExpirationSetPolicy instance as value for key input</li>
	 * <li>found when cached value for key input is a NoObject instance</li>
	 * </ul>
	 */
	@Override
	public <T> Map<T, Object> getAll( Collection<T> keys) {
		if ( keys == null) {
			return null;
		}
		
		Set<T> keysSet = new LinkedHashSet<T>( keys);
			// Validation on keys input -----------------------------------------------------------
			boolean hasLogged = false;
			NoObject noObject = new NoObject();
			while( keysSet.contains( noObject)) {
				if ( !hasLogged) {
					hasLogged = true;
					
					if ( logger.isInfoEnabled()) {
						String callingMethodsInfo 
						= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
						boolean transactionMode 
						= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
						logger.info( 
								String.format(
										"Found NoObject instance(s) in keys input: %1$s. " 
										+ "%nSkipping NoObject instance as key to inquiry value in cache. "
										+ "Due for NoObject instance to be disallowed for key or value being " 
										+ "stored into cache, there should be no necessity of attempt of " 
										+ "obtaining value for NoObject instance key from cache (of what " 
										+ "namespace is %2$s.) The attempt was made in %3$s mode at next " 
										+ "call sequence: %n%4$s",
										keys.toString(), 
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction"),
										callingMethodsInfo
										)
								);
					}
				}
				keysSet.remove( noObject);
			} // while
			
			String keyForKeysSetInMainCache = getKeySetKey();			
			hasLogged = false;
			while( keysSet.contains( keyForKeysSetInMainCache)) {
				if ( !hasLogged) {
					hasLogged = true;
					
					if ( logger.isInfoEnabled()) {
						String callingMethodsInfo 
						= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
						boolean transactionMode 
						= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
						logger.info( 
								String.format(
										"Found %1$s in keys input: %2$s. "
										+ "%nSkipping %1$s as key to inquiry value in cache. "
										+ "%1$s is reserved and not allowed to be used as key to cache " 
										+ "a value into the cache space of what namespace is %3$s. " 
										+ "The attempt was made in %4$s mode at next " 
										+ "call sequence: %n%5$s",
										keyForKeysSetInMainCache, 
										keys.toString(), 
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction"),
										callingMethodsInfo
										)
								);
					}
				}
				keysSet.remove( keyForKeysSetInMainCache);
			} // while
			// ------------------------------------------------------------------------------------
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
		// in transaction mode
			
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			keysSet.retainAll( transactionLocalCache.keySet());
			
			Map<Serializable, Serializable> keysMapForTransaction 
			= transactionHandler.keysMapForTransaction;
			
			Map<T, Object> map = new LinkedHashMap<T, Object>();
			for( Object key : keysSet) {
				ValueExpirationSetPolicy valueExpirationSetPolicy = transactionLocalCache.get( key);
					if ( valueExpirationSetPolicy == null) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Detected null value for key (%1$s) in cache space (of what " 
										+ "namespace is %2$s) during transaction mode. This should not " 
										+ "have happened and cache should have held ValueExpirationSetPolicy " 
										+ "instance holding cached real value for key instead.", 
										((key == null) ? "null" : key.toString()), 
										memcacheNamespace
										)
								);
					}
				
				Object value = valueExpirationSetPolicy.getValue();
					if ( value instanceof NoObject) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Found NoObject instance as value for key (%1$s) in cache (of what " 
										+ "namespace is %2$s) during transaction mode. This should not have " 
										+ "happended because NoObject instance is disallowed as key or " 
										+ "value being stored into cache.",
										( (key == null) ? "null" : key.toString()), 
										memcacheNamespace
										)
								);
					}
					if ( value != null) {
						// Do give copy of value object, not value object itself.
						try {
							value = valueExpirationSetPolicy.getValueCopy();
						}
						catch( Throwable throwable) {
							throw new TransactionalMemcacheServiceException(
									String.format(
											"Failure in generating copy of %1$s (what is stored value for %2$s key " 
											+ "in cache) by serializing and de-serializing during transaction mode " 
											+ "working on the cache space of what namespace is %3$s.",
											value.toString(),
											(( key == null) ? "null" : key.toString()),
											memcacheNamespace							
											),
									throwable
									);
						}
					}
				
				// Integrity check on keysMapForTransaction -------------------------------------------
				integrityCheckOnKeysMapForTransaction( key);
				// ------------------------------------------------------------------------------------
				
				map.put( (T)key, value);
			} // for
			
			return map;
		}
		else { // non-transaction mode
			
			Map<T, Object> map;
			Set<Serializable> keysSetInKeysMap;
			
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				
				map = mainCache.getAll( keysSet);
				
				keysSetInKeysMap 
				= new LinkedHashSet<Serializable>( 
						transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace));
			}
			finally {
				mainCacheLock.unlock();
			}
			
			if ( !keysSetInKeysMap.containsAll( map.keySet())) {
				if ( logger.isInfoEnabled()) {
					LinkedHashSet<T> notAwareKeysSet 
					= new LinkedHashSet<T>( map.keySet());
					notAwareKeysSet.removeAll( keysSetInKeysMap);
					
					logger.info( 
							String.format(
									"Detected in non-transaction mode that memcache of what " 
									+ "namespace is %1$s holds unaware data %2$s. " 
									+ "(Although, this can be just simple timing matter.) " 
									+ "Eliminated unaware data %2$s from returning Map " 
									+ "object. %3$s: %4$s",
									memcacheNamespace,
									((notAwareKeysSet.size() > 1) ? "enttries" : "entry"), 
									((notAwareKeysSet.size() > 1) 
											? "Keys of unaware etries" : "Key of unaware entry"),
									notAwareKeysSet.toString()
									)
							);
				}
			}
			map.keySet().retainAll( keysSetInKeysMap);
			
			return map;
		}
	} // public <T> Map<T, Object> getAll( Collection<T> keys)

	/**
	 * Not supported.
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException
	 */
	@Override
	public boolean delete(Object key, long millisNoReAdd) {
		boolean transactionMode = transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						String.format(
								"delete method with millisNoReAdd long argument is not supported. "
								+ "(Encountered this while working on the cache space of what namespace " 
								+ "is %1$s during %2$s mode.)", 
								memcacheNamespace,
								( transactionMode ? "transaction" : "non-transaction")
								)
						)
				);
	} // public boolean delete(Object key, long millisNoReAdd)

	/**
	 * Delete entry for key input from cache. <br />
	 * <b>Thread safety:</b> Thread safe, though thread safety on keys input needs to be provided.  
	 * Locks on mainCacheLock member field.
	 * @param key : should be neither NoObject instance nor identical value to keySetKey member field
	 * @return true when entry for key input is successfully deleted from cache. Otherwise false. 
	 * @throws TransactionalMemcacheServiceException for next cases:
	 * <ul>
	 * <li>key input refers to data in transactionLocalCache since direct reference to data of entry in 
	 * transactionLocalCache should not be occurred.</li>
	 * <li>deep copy of key input held in keysMapForTransaction is not identifeid as equivalent to key input</li>
	 * <li>transactionLocalCache holds null instead of a ValueExpirationSetPolicy instance as value for key input</li>
	 * <li>found when cached value for key input is a NoObject instance</li>
	 * </ul>
	 */
	@Override
	public boolean delete( Object key) {
		String keyForKeysSetInMainCache = getKeySetKey();
		
		// Validation on key input ----------------------------------------------------------------
		if ( key == null) {
			if ( logger.isDebugEnabled()) {
				boolean transactionMode 
				= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
				StackTraceElement[] stackTraceElementArray = Thread.currentThread().getStackTrace();
				String callingMethodsInfo = getCallSequenceStr( stackTraceElementArray, 2, 3);
				logger.debug( 
						String.format(
								"Attempting to delete data entry of what key is null from the cache of " 
								+ "what namespace is %1$s during %2$s mode. It is not recommneded using " 
								+ "null as key. Attempt is made at next call sequence: %3$s",
								memcacheNamespace, 
								( transactionMode ? "transaction" : "non-transaction"),								
								callingMethodsInfo
								)
						);
			}
		}
		else {
			// Deny NoObject instance as key
			if ( key instanceof NoObject) {
				if ( logger.isInfoEnabled()) {
					boolean transactionMode 
					= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
					String callingMethodsInfo 
					= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
					
					logger.info( 
							String.format(
									"Detected the attempt of deleting entry for NoObject instance key " 
									+ "from cache space (of what namespace is %1$s) in %2$s mode. " 
									+ "Due for NoObject instance to be disallowed for key or value being " 
									+ "stored into cache, there should be no necessity of such attempt. " 
									+ "The attempt was made at next call sequence: %n%3$s",
									memcacheNamespace, 
									( transactionMode ? "transaction" : "non-transaction"),
									callingMethodsInfo
									)
							);
				}
				return false;
			}
			else if ( keyForKeysSetInMainCache.equals( key)) {
				if ( logger.isInfoEnabled()) {
					boolean transactionMode 
					= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
					String callingMethodsInfo 
					= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
					
					logger.info( 
							String.format(
									"Detected the attempt of deleting entry for %1$s key " 
									+ "from cache space (of what namespace is %2$s) in %3s mode. " 
									+ "Because %1$s is reserved and not allowed to be used as key of data " 
									+ "entry in the cache, there should be no necessity of such attempt. "
									+ "The attempt was made at next call sequence: %n%4$s",
									keyForKeysSetInMainCache, 
									memcacheNamespace, 
									( transactionMode ? "transaction" : "non-transaction"), 
									callingMethodsInfo
									)
							);
				}
				return false;
			}
		}
		// ----------------------------------------------------------------------------------------
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
		// in transaction mode
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			
			if ( transactionLocalCache.keySet().contains( key)) {
				LinkedHashSet<Serializable> transactionLocalCacheKeySet 
				= new LinkedHashSet<Serializable>( transactionLocalCache.keySet()); 
				transactionLocalCacheKeySet.retainAll( Collections.singleton( key));
				Serializable keyCopy = transactionLocalCacheKeySet.iterator().next();
					if ( key != null) {
						if ( key == keyCopy) {
							String message 
							= String.format(
									"Detected that the key of a data entry having been held in " 
									+ "transactionLocalCache field of transactionHandlerThreadLocal " 
									+ "ThreadLocal static member field refers to the same object that " 
									+ "the key input does: %1$s. This should not have happened; " 
									+ "instead, it is designed for transactionLocalCache field to hold deep " 
									+ "copy of each key instead of orignal one, so modification onto " 
									+ "origianl object will not affect to data entry in " 
									+ "transactionLocalCache field. It may be that key copy in cache has " 
									+ "been sneakily leaking out to external code. " 
									+ "The class of the key is %2$s. And the cached value for that key is " 
									+ ": %3$s. "
									+ "(Encountered this while working on the cache space of what namespace " 
									+ "is %4$s during transaction mode.)", 
									key.toString(),
									key.getClass().toString(),
									(( transactionLocalCache.get( key) == null) 
											? "null" : transactionLocalCache.get( key).toString()),
									memcacheNamespace
									);
							throw new TransactionalMemcacheServiceException( message);
						}
						else if ( !key.equals( keyCopy)) {
							String message 
							= String.format(
									"Found that the key (%1$s) of a data entry having been held in " 
									+ "transactionLocalCache field of transactionHandlerThreadLocal " 
									+ "ThreadLocal static member field is not identified as equivalment " 
									+ "to the orignal key input (%2$s) as deep copy. Data entry in " 
									+ "transactionLocalCache field should have held only deep copy object " 
									+ "identified as equivalent to original object. ",
									( (keyCopy == null) ? "null" : keyCopy.toString()),
									( (key == null) ? "null" : key.toString())
									);
							if ( key != null) {
								message = message + String.format(
										"The class of key input: %1$s. ", 
										key.getClass().toString()
										);
							}
							if ( keyCopy != null) {
								message = message + String.format(
										"The class of cached deep copy of key object: %1$s. ", 
										keyCopy.getClass().toString()
										);
							}
							message = message + String.format(
									"(Encountered this while working on the cache of what " 
									+ "namespace is %1$s during transaction mode.)", 
									memcacheNamespace
									);
							
							throw new TransactionalMemcacheServiceException( message);
						}
					}
					
				ValueExpirationSetPolicy valueExpirationSetPolicy = transactionLocalCache.remove( key);
					// Integrity check on valueExpirationSetPolicy --------------------------------
					if ( valueExpirationSetPolicy == null) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Detected null value for key (%1$s) in cache space (of what " 
										+ "namespace is %2$s) during transaction mode. This should not " 
										+ "have happened and cache should have held ValueExpirationSetPolicy " 
										+ "instance holding cached real value for key instead.", 
										((key == null) ? "null" : key.toString()), 
										memcacheNamespace
										)
								);
						
					}
					else if ( valueExpirationSetPolicy.getValue() instanceof NoObject) {
						throw new TransactionalMemcacheServiceException(
								String.format(
										"Found that value cached space (of what namespace is %1$s) for " 
										+ "key (%2$s) is NoObject instance in transaction mode. This " 
										+ "should not have happended because NoObject instance is " 
										+ "disallowed for key or value being stored into cache.",
										memcacheNamespace, 
										((key == null) ? "null" : key.toString())
										)
								);
					}
					// ----------------------------------------------------------------------------
				
				// Add key to keysMapForTransaction
				Map<Serializable, Serializable> keysMapForTransaction 
				= transactionHandler.keysMapForTransaction;
				if ( !keysMapForTransaction.containsKey( key)) {
					keysMapForTransaction.put( keyCopy, keyCopy);
				}
				
				return true;
			}
			else {
				return false;
			}
		}
		else { // non-transaction mode
			
			boolean localKeyRemovalResult;
			boolean entryRemovalResult;
			boolean keyRemovalResult;
			
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				
				Set<Serializable> keysSet 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);
				
				localKeyRemovalResult = keysSet.remove( key);
				entryRemovalResult = mainCache.delete( key);
				
				// Update keysSet entry in mainCache
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				if ( keySetInMainCache == null) {
				// either mainCache has been most likely cleared or 
				// keysSet entry in mainCache has been evicted
					// After sync keysSet entry in keysMap static member field with 
					// data entries in mainCache, reconstruct of keysSet entry in mainCache 
					// from keysSet entry in keysMap static member field
					syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
					keyRemovalResult = localKeyRemovalResult;
				}
				else {
					keyRemovalResult = keySetInMainCache.remove( key);
					mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
				}
			}
			finally {
				mainCacheLock.unlock();
			}
				
			if ( localKeyRemovalResult ^ entryRemovalResult ^ keyRemovalResult) {
				if ( logger.isDebugEnabled()) {
					String message = String.format(
							"Although it could be just simple timing issue, in deleting data entry " 
							+ "for %1$s key from cache space (of what namespace is %2$s) in " 
							+ "non-transaction mode, detected the inconsistent state among data " 
							+ "entry in mainCache, keysSet entry in mainCache and keysSet entry in " 
							+ "keysMap static member field: %n%3$c%4$s%n%3$c%5$s%n%3$c%6$s", 
							( (key == null) ? "null" : key.toString()),
							memcacheNamespace, 
							'\t',
							(entryRemovalResult ? 
									"deletion of data entry from mainCache succeeded" 
									: "deletion of data entry from mainCache failed"),
							(keyRemovalResult ? 
									"deletion of key from keysSet entry in mainCache succeeded" 
									: "deletion of key from keysSet entry in mainCache failed"),
							(localKeyRemovalResult ? 
									"deletion of key from keysSet entry in keysMap static member field succeeded" 
									: "deletion of key from keysSet entry in keysMap static member field failed")
							);
					
					logger.debug( message);
				}
			}
			
			return entryRemovalResult;
		}
	} // public boolean delete( Object key)
	
	/**
	 * Not supported.
	 * @throws TransactionalMemcacheServiceException wrapping UnsupportedOperationException
	 */
	@Override
	public <T> Set<T> deleteAll(Collection<T> keys, long millisNoReAdd) {
		boolean transactionMode = transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						String.format(
								"deleteAll method with millisNoReAdd long argument is not supported. "
								+ "(Encountered this while working on the cache space of what namespace " 
								+ "is %1$s during %2$s mode.)", 
								memcacheNamespace,
								( transactionMode ? "transaction" : "non-transaction")
								)
						)
				);
	}

	@Override
	public <T> Set<T> deleteAll( Collection<T> keys) {
		
		String keyForKeysSetInMainCache = getKeySetKey();
		
		// Validation on key input ----------------------------------------------------------------
		if ( keys == null) {
			return null;
		}
		Set<T> keysSet = new LinkedHashSet<T>( keys);
			boolean hasLogged = false;
			NoObject noObject = new NoObject();
			while( keysSet.contains( noObject)) {
				if ( !hasLogged) {
					hasLogged = true;
					
					if ( logger.isInfoEnabled()) {
						String callingMethodsInfo 
						= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
						boolean transactionMode 
						= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
						logger.info( 
								String.format(
										"Found NoObject instance(s) in keys input: %1$s. " 
										+ "%nSkipping NoObject instance as key to inquiry value in cache. "
										+ "Due for NoObject instance to be disallowed for key or value being " 
										+ "stored into cache, there should be no necessity of attempt of " 
										+ "obtaining value for NoObject instance key from cache (of what " 
										+ "namespace is %2$s.) The attempt was made in %3$s mode at next " 
										+ "call sequence: %n%4$s",
										keys.toString(), 
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction"),
										callingMethodsInfo
										)
								);
					}
				}
				keysSet.remove( noObject);
			} // while
			
			hasLogged = false;
			while( keysSet.contains( keyForKeysSetInMainCache)) {
				if ( !hasLogged) {
					hasLogged = true;
					
					if ( logger.isInfoEnabled()) {
						String callingMethodsInfo 
						= getCallSequenceStr( Thread.currentThread().getStackTrace(), 2, 3);
						boolean transactionMode 
						= transactionalMemcacheServiceTransactionHelper.isTransactionModeThread();
						logger.info( 
								String.format(
										"Found %1$s in keys input: %2$s. "
										+ "%nSkipping %1$s as key to inquiry value in cache. "
										+ "%1$s is reserved and not allowed to be used as key to cache " 
										+ "a value into the cache space of what namespace is %3$s. " 
										+ "The attempt was made in %4$s mode at next " 
										+ "call sequence: %n%5$s",
										keyForKeysSetInMainCache, 
										keys.toString(), 
										memcacheNamespace, 
										( transactionMode ? "transaction" : "non-transaction"),
										callingMethodsInfo
										)
								);
					}
				}
				keysSet.remove( keyForKeysSetInMainCache);
			} // while
		// ----------------------------------------------------------------------------------------
		
		if ( transactionalMemcacheServiceTransactionHelper.isTransactionModeThread()) {
		// in transaction mode
			TransactionHandler transactionHandler = transactionHandlerThreadLocal.get();
			
			Map<Serializable, ValueExpirationSetPolicy> transactionLocalCache 
			= transactionHandler.transactionLocalCache;
			keysSet.retainAll( transactionLocalCache.keySet());
			
			// Generate deep copy of keys to add those to keysMapForTransaction later 
			Set<Serializable> keysSetCopy = (Set<Serializable>)generateCopy( (Serializable)keysSet);
			
			transactionLocalCache.keySet().removeAll( keysSet);
			
			// Add key to keysMapForTransaction
			Map<Serializable, Serializable> keysMapForTransaction 
			= transactionHandler.keysMapForTransaction;
			keysSetCopy.removeAll( keysMapForTransaction.keySet());
			for( Serializable keyCopy : keysSetCopy) {
				keysMapForTransaction.put( keyCopy, keyCopy);
			} // for
			
			return keysSet;
		}
		else { // non-transaction mode
			
			Set<T> deletedKeysSet;
			Set<T> deletedKeysFromKeySetInMainCache;
			
			acquireMainCacheLock();
			try {
				// Mandatory to sync keysSet in mainCache and keysSet entry in keysMap at beginning
				syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
				
				Set<Serializable> keysSetInKeysMap 
				= transactionalMemcacheServiceSyncHelper.getKeySet( memcacheNamespace);

				deletedKeysSet = mainCache.deleteAll( keysSet);
				
				// Update keysSet entry in mainCache
				Set<Serializable> keySetInMainCache 
				= (Set<Serializable>)mainCache.get( keyForKeysSetInMainCache);
				if ( keySetInMainCache == null) {
				// either mainCache has been most likely cleared or 
				// keysSet entry in mainCache has been evicted
					
					keysSet.retainAll( keysSetInKeysMap);
					keysSetInKeysMap.removeAll( keysSet);
					
					// After sync keysSet entry in keysMap static member field with 
					// data entries in mainCache, reconstruct of keysSet entry in mainCache 
					// from keysSet entry in keysMap static member field
					syncKeysSetWithMainCache( null, keyForKeysSetInMainCache);
					
					deletedKeysFromKeySetInMainCache = deletedKeysSet;
				}
				else {
					deletedKeysFromKeySetInMainCache = new LinkedHashSet<T>( keysSet);
					deletedKeysFromKeySetInMainCache.retainAll( keySetInMainCache);
					keySetInMainCache.removeAll( deletedKeysFromKeySetInMainCache);
					mainCache.put( keyForKeysSetInMainCache, keySetInMainCache);
					
					keysSet.retainAll( keysSetInKeysMap);
					keysSetInKeysMap.removeAll( keysSet);
				}
				
			}
			finally {
				mainCacheLock.unlock();
			}
			
			if ( logger.isDebugEnabled()) {
				if ( 
						!deletedKeysSet.containsAll( keysSet)
						|| !keysSet.containsAll( deletedKeysSet) 
						|| !deletedKeysFromKeySetInMainCache.containsAll( keysSet)
						|| !keysSet.containsAll( deletedKeysFromKeySetInMainCache)
						) 
				{
					String message = String.format(
							"Although it could be just simple timing issue, in deleting data " 
							+ "entries for keys from cache space (of what namespace is %1$s) in " 
							+ "non-transaction mode, detected the inconsistent state among data " 
							+ "entry in mainCache, keysSet entry in mainCache and keysSet entry " 
							+ "in keysMap static member field. " 
							+ "%n%2$cKeys deleted from mainCache: %3$s" 
							+ "%n%2$cKeys deleted from keysSet entry in mainCache: %4$s" 
							+ "%n%2$cKeys deleted from keysSet entry in keysMap static member field: %5$s", 
							memcacheNamespace, 
							'\t',
							deletedKeysSet.toString(),
							deletedKeysFromKeySetInMainCache.toString(),
							keysSet.toString()
							);
					
					logger.debug( message );
				}
			}
			
			return deletedKeysSet;
		}
	}
	// --------------------------------------------------------------------------------------------

	// Just delegates of MemcacheService interface methods ----------------------------------------
	@Override
	public ErrorHandler getErrorHandler() {
		return mainCache.getErrorHandler();
	}
	
	@Override
	public void setErrorHandler( ErrorHandler handler) {
		mainCache.setErrorHandler( handler);
	}
	
	@Override
	public Stats getStatistics() {
		return mainCache.getStatistics();
	}
	// --------------------------------------------------------------------------------------------
}
