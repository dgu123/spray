package slingong.datastore.context.support.ehcache;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.ObjectExistsException;
import net.sf.ehcache.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;

import slingong.datastore.context.support.CachedMessageDao;

public class EhcachedMessageDaoImpl implements CachedMessageDao {
	protected final Logger logger = LoggerFactory.getLogger( this.getClass());
		
	protected final CacheManager cacheManager;
	    public final CacheManager getCacheManager() {
			return cacheManager;
		}
	
	public final static String DEFAULT_MESSAGE_CACHE_NAME = "messageCache";
	protected String ehcacheName = DEFAULT_MESSAGE_CACHE_NAME;
		public final String getEhcacheName() {
			return ehcacheName;
		}

	protected Ehcache ehcache;
		public synchronized void obtainEhcache( String messageCacheName) {
	        final CacheManager cacheManager = getCacheManager();
	        
	        ehcache = cacheManager.getEhcache( messageCacheName);
	        if ( ehcache == null) {
	        	if ( logger.isInfoEnabled()) {
	                logger.info(
	                		String.format(
	                				"Going to create ehcache named \"%1$s\" from the defaultCache " 
	    							+ "since such ehcache was not found.", 
	    							messageCacheName
	                				)
	                		);
	        	}
	            try {
	                cacheManager.addCache( messageCacheName);
	            }
	            catch ( ObjectExistsException exception) {
	            	if ( logger.isDebugEnabled()) {
	                    logger.debug( 
	                    		String.format(
	                            		"Going to proceed by using ehcache (named \"%1$s\") created ahead by " 
	                					+ "other after detecting race condition of creating it.", 
	                					messageCacheName
	                    				),
	                    		exception
	                    		);
	            	}
	            }
	            ehcache = cacheManager.getEhcache( messageCacheName);
	        }
	        else {
	        	if ( logger.isDebugEnabled()) {
	        		logger.debug( 
	        				String.format( "Found instance of ehcache named \"%1$s\".", messageCacheName)
	        				);
	        	}
	        }
	        
	        ehcacheName = messageCacheName;
	    } // public synchronized void obtainEhcache( String messageCacheName)
		
		public synchronized void clearEhCache() {
			boolean activeEhcacheFlag = false;
			if ( ehcache != null) {
				if ( Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
					activeEhcacheFlag = true;
					
					ehcache.removeAll();
				}
			}
			if ( !activeEhcacheFlag) {
				if ( logger.isWarnEnabled()) {
					logger.warn(
							"Need to obtain active ehcache instance before using cache.");
				}
			}
		}
		
		public synchronized void disposeEhCache() {
			if ( ehcache == null) return;
			if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) return;
			
			cacheManager.removeCache( ehcache.getName());
			ehcache = null;
		}
	
	// Constructor(s) -----------------------------------------------------------------------------
	public EhcachedMessageDaoImpl( CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
	
	public EhcachedMessageDaoImpl( CacheManager cacheManager, String messageCacheName) {
		this.cacheManager = cacheManager;
		this.ehcacheName = messageCacheName;
	}
	// --------------------------------------------------------------------------------------------
/*	
	protected static class CachedMessageKey implements Serializable {
		private static final long serialVersionUID = 20110821L;
		public String code;
		public Locale locale;
		
		public CachedMessageKey( String code, Locale locale) {
			this.code = code;
			this.locale = locale;
		}
		
		// hashCode and equals methods ------------------------------------------------------------
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((code == null) ? 0 : code.hashCode());
			result = prime * result + ((locale == null) ? 0 : locale.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			EhcachedMessageDaoImpl.CachedMessageKey other = (EhcachedMessageDaoImpl.CachedMessageKey) obj;
			if (code == null) {
				if (other.code != null) return false;
			} else if (!code.equals(other.code)) return false;
			if (locale == null) {
				if (other.locale != null) return false;
			} else if (!locale.equals(other.locale)) return false;
			return true;
		}
		// ----------------------------------------------------------------------------------------
	}
*/
	// CachedMessageDao implementation ------------------------------------------------------------
	@Override
	public String getCachedMessage(String key, Locale locale) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return null;
		}
		
		Element element = ehcache.get( key);
		if ( element == null) return null;
		Map<Locale, String> messagesMap = (Map<Locale, String>)element.getValue();
//TODO need to research whether LinkedHashMap supports null key
		return messagesMap.get( locale);
	}
	@Override
	public Map<Locale, String> getCachedMessages( String key) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return null;
		}
		
		Element element = ehcache.get( key);
		if ( element == null) return null;
		return (Map<Locale, String>)element.getValue();
	}
	
	@Override
	public void setCachedMessage(String key, Locale locale, String message) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		Element element = ehcache.get( key);
		if ( element == null) {
			LinkedHashMap<Locale, String> messagesMap = new LinkedHashMap<Locale, String>();
			messagesMap.put( locale, message);
			element = new Element( key, messagesMap);
			ehcache.put( element);
		}
		else {
			Map<Locale, String> messagesMap = (Map<Locale, String>)element.getValue();
			messagesMap.put( locale, message);
		}
	}

	@Override
	public void removeCachedMessage(String key, Locale locale) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		Element element = ehcache.get( key);
		if ( element != null) {
			Map<Locale, String> messagesMap = (Map<Locale, String>)element.getValue();
			messagesMap.remove( locale);
		}
	}
	@Override
	public void removeCachedMessages( String key) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		ehcache.remove( key);
	}

	@Override
	synchronized public void clearCachedMessages() {
		clearEhCache();
	}
	// --------------------------------------------------------------------------------------------
} 