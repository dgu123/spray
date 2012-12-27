package slingong.datastore.context.support.gae;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.jsr107cache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import slingong.datastore.context.support.CachedMessageDao;
import slingong.web.gae.JCacheService;

public class JCacheMessageDao implements CachedMessageDao {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	protected static String messageMemcacheName = JCacheMessageDao.class.getName() + ".messageCache";
		public static String getMessageMemcacheName() {
			return messageMemcacheName;
		}
		/**
		 * Also initialize messageMemcache additionally to set messageMemcacheName value
		 * @param messageMemcacheName
		 */
		public synchronized static void setMessageMemcacheName( String messageMemcacheName) {
			messageMemcache = null;
			JCacheMessageDao.messageMemcacheName = messageMemcacheName;
		}

	protected static Cache messageMemcache = null;
	
	// --------------------------------------------------------------------------------------------
	protected synchronized static Map<Locale, String> getMessagesMap( String key) {
		if ( messageMemcache == null) {
			messageMemcache = JCacheService.getCache( messageMemcacheName);
		}
		
		return (Map<Locale, String>)messageMemcache.get( key);
	}
	protected synchronized static void setMessageToMemcache( String key, Locale locale, String message) {
		Map<Locale, String> messagesMap = getMessagesMap( key);		
		if ( messagesMap == null) {
			messagesMap = new LinkedHashMap<Locale, String>();
		}
		messagesMap.put( locale, message);
		messageMemcache.put( key, messagesMap);
	}
	protected synchronized static void setMessagesMap( String key, Map<Locale, String> messagesMap) {
		if ( messageMemcache == null) {
			messageMemcache = JCacheService.getCache( messageMemcacheName);
		}
		messageMemcache.put( key, messagesMap);
	}
	protected synchronized static void removeMessageFromMemcache( String key, Locale locale) {
		Map<Locale, String> messagesMap = getMessagesMap( key);		
		
		if ( messagesMap != null) {
			messagesMap.remove( locale);
			setMessagesMap( key, messagesMap);
		}
	}
	protected synchronized static void removeMessagesMap( String key) {
		if ( messageMemcache == null) {
			messageMemcache = JCacheService.getCache( messageMemcacheName);
		}
		
		messageMemcache.remove( key);
	}
	protected synchronized static void clearMessageMemcache() {
		messageMemcache.clear();
	}
	// --------------------------------------------------------------------------------------------
	
	// CachedMessageDao implementation ------------------------------------------------------------
	@Override
	public String getCachedMessage( String key, Locale locale) {
		Map<Locale, String> messageMap = getCachedMessages( key);
		if ( messageMap == null) return null;
		
		return messageMap.get( locale);
	}

	@Override
	public Map<Locale, String> getCachedMessages( String key) {
		return getMessagesMap( key);
	}

	@Override
	public synchronized void setCachedMessage( String key, Locale locale, String message) {
		setMessageToMemcache( key, locale, message);
	}

	@Override
	public synchronized void setCachedMessages( String key, Map<Locale, String> messagesMap) {
		setMessagesMap( key, messagesMap);
	}

	@Override
	public synchronized void removeCachedMessage( String key, Locale locale) {
		removeMessageFromMemcache( key, locale);
	}

	@Override
	public synchronized void removeCachedMessages( String key) {
		removeMessagesMap( key);		
	}

	/**
	 * Caution this will clear whole memcache, not only memcache named after value of messageMemcacheName
	 */
	@Override
	public synchronized void clearCachedMessages() {
		/* clear method of Cache object will clear whole memcache, not only specific namespace memcache.
		 * This is due to the bug http://code.google.com/p/googleappengine/issues/detail?id=5935 
		 * Even that bug is fixed, because design of Memcache low level API, clear method of Cache object 
		 * will clear whole memcache most likely.  
		 */
		messageMemcache.clear();
		
//TODO implement the pair of key (something like string numbering this class name) and value (Map to hold memcache name as key and list of message codes as value)
/* Implementation idea: When it is detected that any message uses the key value what is identical to 
 * the key mentioned above, then save the above value (Map to hold memcache name as key and list of message 
 * codes as value) with new key value.
 */
	}
	// --------------------------------------------------------------------------------------------
}
