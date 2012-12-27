package slingong.datastore.context.support.gae;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.jsr107cache.Cache;

import org.apache.commons.collections.map.HashedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import slingong.datastore.context.support.CachedMessageDao;
import slingong.web.gae.JCacheService;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

public class MemcacheServiceMessageDao implements CachedMessageDao {
	protected static Logger logger = LoggerFactory.getLogger( MemcacheServiceMessageDao.class);
	
	protected static String messageMemcacheName 
	= MemcacheServiceMessageDao.class.getName() + ".messageCache";
		public static String getMessageMemcacheName() {
			return messageMemcacheName;
		}
		/**
		 * Also initialize messageMemcache additionally to set messageMemcacheName value
		 * @param messageMemcacheName
		 */
		public synchronized static void setMessageMemcacheName( final String messageMemcacheName) {
			messageMemcache = null;
			MemcacheServiceMessageDao.messageMemcacheName = messageMemcacheName;
		}
	protected static MemcacheService messageMemcache = null;
	
	// --------------------------------------------------------------------------------------------
	// To have memcache what will hold map of cache name and name of key what is for list to hold 
	// message codes, due to lack of, under namespace, dumping all keys and clearing data.
	
		// On datastore ---------------------------------------------------------------------------
		public static final String CacheNameMessageCodesHolderKeyName 
			= MemcacheServiceMessageDao.class.getName() + ".cacheNameMessageCodesHolderKey";
		
		@Transactional( propagation=Propagation.REQUIRED)
		protected synchronized static CacheNameMessageCodesHolderModel readCacheNameMessageCodesHolderModel() {
			GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
			Key cacheNameMessageCodesHolderKey 
			= Datastore.createKey( 
					CacheNameMessageCodesHolderModel.class, CacheNameMessageCodesHolderKeyName);
			
			CacheNameMessageCodesHolderModel cacheNameMessageCodesHolderModel 
			= (CacheNameMessageCodesHolderModel)gtx.getOrNull( 
					CacheNameMessageCodesHolderModel.class, cacheNameMessageCodesHolderKey);
			
				if ( cacheNameMessageCodesHolderModel == null) {
					if ( logger.isDebugEnabled()) {
						logger.debug(
								"No CacheNameMessageCodesHolderModel entity persisted in datastore is found."
								);
					}
					
					cacheNameMessageCodesHolderModel 
					= new CacheNameMessageCodesHolderModel( cacheNameMessageCodesHolderKey);
					gtx.put( cacheNameMessageCodesHolderModel);
				}
			
			return cacheNameMessageCodesHolderModel;
		} // protected synchronized static CacheNameMessageCodesHolderModel readCacheNameMessageCodesHolderModel()
		
		@Transactional( propagation=Propagation.REQUIRED)
		protected synchronized static void putCacheNameMessageCodesHolder ( 
				final String cacheName, final String messageCodesHolder) {
			CacheNameMessageCodesHolderModel cacheNameMessageCodesHolderModel 
			= (CacheNameMessageCodesHolderModel)readCacheNameMessageCodesHolderModel();
			
			cacheNameMessageCodesHolderModel.setMessageCodesHolder( cacheName, messageCodesHolder);
			
			GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
			gtx.put( cacheNameMessageCodesHolderModel);
			
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"Persisted pair of cache name (\"%1$s\") and name (\"%2$s\") of key what " 
								+ "is for list to hold message codes what will be a element of " 
								+ "CacheNameMessageCodesHolderModel object", 
								cacheName, messageCodesHolder
								)
						);
			}
		} // protected synchronized static void putCacheNameMessageCodesHolder
		// ----------------------------------------------------------------------------------------
		// On memcache ----------------------------------------------------------------------------
		protected static MemcacheService cacheNameMessageCodesHolderMemcache = null;
		protected static final String CacheNameMessageCodesHolderMemcacheName 
		= MemcacheServiceMessageDao.class.getName() + ".cacheNameMessageCodesHolderMemcache";
		protected static final String KeyNameForCacheNameMessageCodesHolderMap 
		= CacheNameMessageCodesHolderMemcacheName + ".cacheNameMessageCodesHolderMap";
		protected synchronized static void initCacheNameMessageCodesHolderMemcache() {
			if ( cacheNameMessageCodesHolderMemcache == null) {
				cacheNameMessageCodesHolderMemcache 
				= MemcacheServiceFactory.getMemcacheService( CacheNameMessageCodesHolderMemcacheName);
			}
			
			// Read data from datastore
			CacheNameMessageCodesHolderModel cacheNameMessageCodesHolderModel 
			= readCacheNameMessageCodesHolderModel();
			
			// Save data to cacheNameMessageCodesHolderMemcache
			Map<String, String> cacheNameMessageCodesHolderMap = new LinkedHashMap<String, String>(); 
			for( String registeredCacheName : cacheNameMessageCodesHolderModel.getCacheNameList()) {
				cacheNameMessageCodesHolderMap.put( 
						registeredCacheName, 
						cacheNameMessageCodesHolderModel.getMessageCodesHolder( registeredCacheName)
						);
			} // for
			cacheNameMessageCodesHolderMemcache.put( 
					KeyNameForCacheNameMessageCodesHolderMap, cacheNameMessageCodesHolderMap);
		} // protected synchronized static void initCacheNameMessageCodesHolderMemcache()
		
		protected synchronized static String getKeyNameOfCacheNameMessageCodesHolder( 
				final String cacheName) {
			String messageCodesHolderName = null;
			boolean cacheNameMessageCodesHolderMemcacheInitialized = false;
			for( int index = 0; 
				!cacheNameMessageCodesHolderMemcacheInitialized 
				&& ( messageCodesHolderName == null) 
				&& ( index < 2); 
				index++) 
			{
				if ( index > 0) {
					// Initialized cacheNameMessageCodesHolderMemcache by reading its data from datastore 
					// and try again, since messageCodesHolderName might become null because 
					// data in cacheNameMessageCodesHolderMemcache was evicted 
					initCacheNameMessageCodesHolderMemcache();
					cacheNameMessageCodesHolderMemcacheInitialized = true;
				}
				
				if ( cacheNameMessageCodesHolderMemcache == null) {
					initCacheNameMessageCodesHolderMemcache();
					cacheNameMessageCodesHolderMemcacheInitialized = true;
				}
				
				Map<String, String> cacheNameMessageCodesHolderMap 
				= (Map<String, String>)cacheNameMessageCodesHolderMemcache.get( 
						KeyNameForCacheNameMessageCodesHolderMap);
				messageCodesHolderName = cacheNameMessageCodesHolderMap.get( cacheName);
			} // for
			
			return messageCodesHolderName;
		} // protected synchronized static String getKeyNameOfCacheNameMessageCodesHolder( String cacheName)
		
		protected void changeKeyNameForKeysListName( 
				final String cacheName, final String newKeyName) {
			
			if ( ( null == newKeyName) || "".equals( newKeyName)) {
				throw new IllegalArgumentException( 
						String.format(
								"%1$s is not acceptable as the value of newKeyName argument.", 
								( null == newKeyName ? "Null" : "Empty string")
								)
						);
			}
			
			
			
			
			String messageCodesHolderName = getKeyNameOfCacheNameMessageCodesHolder( cacheName);
			
			
/* Commented out for compilation			
			newKeyNameCopy 
			= DefaultKeyNameForKeysListName + "." + String.valueOf( Calendar.getInstance().getTime());
*/			
		} // protected void changeKeyNameOfCacheNameMessageCodesHolder( String cacheName, String newKeyName)

		// ----------------------------------------------------------------------------------------
	// --------------------------------------------------------------------------------------------
	

	public static final String DefaultKeyNameForKeysListName 
	= MemcacheServiceMessageDao.class.getName() + ".keyForKeysList";

	protected synchronized static MemcacheService getMemcacheService( String cacheName) {
		if ( cacheName.equals( CacheNameMessageCodesHolderMemcacheName)) {
			throw new IllegalArgumentException(
					String.format( "\"%1$s\" is pre-reserved cache name.", cacheName)
					);
		}
		
		MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService( cacheName);
		String messageCodesHolderName = getKeyNameOfCacheNameMessageCodesHolder( cacheName);
		
		if ( messageCodesHolderName == null) {
			// Register cacheName since it's new cache --------------------------------------------
			// Put empty list to hold message codes for DefaultKeyNameForKeysListName into memcacheService
			memcacheService.put( DefaultKeyNameForKeysListName, new ArrayList<String>());
			
			// Update datastore
			putCacheNameMessageCodesHolder( cacheName, DefaultKeyNameForKeysListName);
			
			// Update cacheNameMessageCodesHolderMemcache memcache
			Map<String, String> cacheNameMessageCodesHolderMap 
			= (Map<String, String>)cacheNameMessageCodesHolderMemcache.get( 
					KeyNameForCacheNameMessageCodesHolderMap);
			cacheNameMessageCodesHolderMap.put( cacheName, DefaultKeyNameForKeysListName);
			cacheNameMessageCodesHolderMemcache.put( 
					KeyNameForCacheNameMessageCodesHolderMap, cacheNameMessageCodesHolderMap);
			// ------------------------------------------------------------------------------------
		}
		else {
			if ( !memcacheService.contains( messageCodesHolderName)) {
				// Adding empty list for messageCodesHolderName since the list 
				// had been evicted from memcacheService
				memcacheService.put( messageCodesHolderName, new ArrayList<String>());
			}
		}
		
		return memcacheService;
	} // protected synchronized static MemcacheService getMemcacheService( String cacheName)
	

	// Actual worker methods mirroring CachedMessageDao interface methods -------------------------
	protected synchronized static Map<Locale, String> getMessagesMap( String key) {
		if ( messageMemcache == null) {
			messageMemcache = getMemcacheService( messageMemcacheName);
		}
		
		return (Map<Locale, String>)messageMemcache.get( key);
	}
	
	protected synchronized static void setMessageToMemcache( String key, Locale locale, String message) {
/* Commented out for compilation		
		String keyNameOfCacheNameMessageCodesHolder 
		= getKeyNameOfCacheNameMessageCodesHolder( getMessageMemcacheName());
		if ( key.equals( keyNameOfCacheNameMessageCodesHolder)) {
			throw new IllegalArgumentException( 
					String.format()
					);
			
		}
*/		
		
		
		Map<Locale, String> messagesMap = getMessagesMap( key);		
		if ( messagesMap == null) {
			messagesMap = new LinkedHashMap<Locale, String>();
		}
		messagesMap.put( locale, message);
		messageMemcache.put( key, messagesMap);
	}
/* Commented out for compilation	
	protected synchronized static void setMessagesMap( String key, Map<Locale, String> messagesMap) {
		if ( messageMemcache == null) {
			messageMemcache = JCacheService.getCache( messageMemcacheName);
		}
		messageMemcache.put( key, messagesMap);
	}
*/	
	protected synchronized static void removeMessageFromMemcache( String key, Locale locale) {
		Map<Locale, String> messagesMap = getMessagesMap( key);		
/* Commented out for compilation	
		if ( messagesMap != null) {
			messagesMap.remove( locale);
			setMessagesMap( key, messagesMap);
		}
*/
	}
/* Commented out for compilation
	protected synchronized static void removeMessagesMap( String key) {
		if ( messageMemcache == null) {
			messageMemcache = JCacheService.getCache( messageMemcacheName);
		}
		
		messageMemcache.remove( key);
	}
	protected synchronized static void clearMessageMemcache() {
		messageMemcache.clear();
	}
*/
	// --------------------------------------------------------------------------------------------
	
	
	// CachedMessageDao implementation ------------------------------------------------------------
	@Override
	public String getCachedMessage(String key, Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Map<Locale, String> getCachedMessages(String key) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setCachedMessage(String key, Locale locale, String message) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setCachedMessages(String key, Map<Locale, String> messagesMap) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void removeCachedMessage(String key, Locale locale) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void removeCachedMessages(String key) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void clearCachedMessages() {
		// TODO Auto-generated method stub
		
	}
	// --------------------------------------------------------------------------------------------
}
