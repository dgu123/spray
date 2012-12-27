package slingong.web.gae;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;

/**
 * Generally do not use this until http://code.google.com/p/googleappengine/issues/detail?id=5935 is 
 * resolved unless the isolation among caches obtained by this getCache method is not necessary.
 * @author Arata Yamamoto
 */
public class JCacheService {
	protected static Logger logger = LoggerFactory.getLogger( JCacheService.class);

	protected static CacheManager cacheManager = null;
	protected static CacheFactory cacheFactory = null;
	public static Cache getCache( String cacheName) {
		if ( cacheManager == null) {
			try {
				cacheManager = CacheManager.getInstance();
			}
			catch( Throwable throwable) {
				if ( logger.isErrorEnabled()) {
					logger.error( 
							"Failure to obtain CacheManager instance in getting memcache.",
							throwable
							);
				}
				return null;
			}
		}
		
		Cache cache = null;
		try {
			cache = cacheManager.getCache( cacheName);
			if ( cache == null) {
				if ( cacheFactory == null) {
					cacheFactory = cacheManager.getCacheFactory();
				}
				cache = cacheFactory.createCache( Collections.emptyMap());
				if ( ( cacheName != null) && !"".equals( cacheName)) {
					cacheManager.registerCache( cacheName, cache);
					if ( logger.isDebugEnabled()) {
						logger.debug( 
								String.format( "Created memcache named %1$s", cacheName)
								);
					}
				}
				else {
					if ( logger.isDebugEnabled()) {
						logger.debug( "Created memcache without name.");
					}
				}
			}
		}
		catch( Throwable throwable) {
			if ( logger.isErrorEnabled()) {
				logger.error( 
						String.format( 
								"Failure in creating memcache %1$s.", 
								( ( ( cacheName != null) && !"".equals( cacheName)) ? 
										"named \"" + cacheName + "\"" : "with null name")
								), 
						throwable
						);
			}
		}
		
		return cache;
	} // public static Cache getCache( String cacheName)

}
