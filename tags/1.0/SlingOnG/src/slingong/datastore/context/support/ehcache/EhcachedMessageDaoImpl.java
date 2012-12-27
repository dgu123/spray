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
		
		public synchronized void setEhcacheName(String ehcacheName) {
			this.ehcacheName = ehcacheName;
		}

	protected Ehcache ehcache;
		protected synchronized void obtainEhcache( String messageCacheName) {
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
	        				String.format( 
	        						"Found instance of ehcache named \"%1$s\". " 
									+ "%nBe mindful on sychronization over this ehcache when other instances " 
    								+ "or other class objects already exist to manage it.", 
	        						messageCacheName
	        						)
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
	
	// CachedMessageDao implementation ------------------------------------------------------------
	@Override
	public synchronized Map<Locale, String> getCachedMessages( String messageCode) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return null;
		}
		
		Element element = ehcache.get( messageCode);
		if ( element == null) return null;
		return (Map<Locale, String>)element.getValue();
	}
	
	@Override
	public synchronized String getCachedMessage( String messageCode, Locale locale) {
		Map<Locale, String> messagesMap = getCachedMessages( messageCode);
		if ( messagesMap == null) return null;
		return messagesMap.get( locale);
	}
	
	
	@Override
	public synchronized void setCachedMessage(String messageCode, Locale locale, String message) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		Element element = ehcache.get( messageCode);
		if ( element == null) {
			LinkedHashMap<Locale, String> messagesMap = new LinkedHashMap<Locale, String>();
			messagesMap.put( locale, message);
			element = new Element( messageCode, messagesMap);
			ehcache.put( element);
		}
		else {
			Map<Locale, String> messagesMap = (Map<Locale, String>)element.getValue();
			messagesMap.put( locale, message);
		}
	}
	@Override
	public synchronized void setCachedMessages( String messageCode, Map<Locale, String> messagesMap) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		Element element = new Element( messageCode, messagesMap);
		if ( ehcache.get( messageCode) == null) {
			ehcache.put( element);
		}
		else {
			ehcache.replace( element);
		}
	}
	
	@Override
	public synchronized void removeCachedMessage( String messageCode, Locale locale) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		Element element = ehcache.get( messageCode);
		if ( element != null) {
			Map<Locale, String> messagesMap = (Map<Locale, String>)element.getValue();
			messagesMap.remove( locale);
		}
	}
	@Override
	public synchronized void removeCachedMessages( String messageCode) {
		if ( ehcache == null) obtainEhcache( getEhcacheName());	// lazy ehcache loading
		if ( !Status.STATUS_ALIVE.equals( ehcache.getStatus())) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						"Need to obtain active ehcache instance before using cache.");
			}
			return;
		}
		
		ehcache.remove( messageCode);
	}

	@Override
	public synchronized void clearCachedMessages() {
		clearEhCache();
	}
	// --------------------------------------------------------------------------------------------
} 