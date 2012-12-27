package slingong.datastore.context.support.gae;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;

/**
 * Domain model holding virtual map consisted of 2 lists:
 * 		- list of cache name
 * 		- list of name of holder holding list of message codes
 * This domain model is used to support the feature of memcache what will hold map of cache name and 
 * name of key what is for list to hold message codes, due for memcache to lack of, under namespace, 
 * dumping all keys and clearing data.
 */
@Model 
public class CacheNameMessageCodesHolderModel {
	@Attribute( persistent=false)
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	@Attribute( primaryKey = true)
	private Key key;
		public Key getKey() {
			return key;
		}
		public void setKey( Key key) {
			this.key = key;
		}
	private List<String> cacheNameList;
		public final List<String> getCacheNameList() {
			return Collections.unmodifiableList( cacheNameList);
		}
		public synchronized final void setCacheNameList(List<String> cacheNameList) {
			if ( logger.isWarnEnabled()) {
				logger.warn( 
						"Using setCacheNameList method to update cacheNameList is not recommended, " 
						+ "in order to keep synch between cacheNameList and messageCodesHolderList."
						);
			}
			this.cacheNameList = cacheNameList;
		}
	private List<String> messageCodesHolderList;
		public final List<String> getMessageCodesHolderList() {
			return Collections.unmodifiableList( messageCodesHolderList);
		}
		public synchronized final void setMessageCodesHolderList(List<String> messageCodesHolderList) {
			if ( logger.isWarnEnabled()) {
				logger.warn( 
						"Using setMessageCodesHolderList method to update messageCodesHolderList is not " 
						+ "recommended, in order to keep synch between cacheNameList and messageCodesHolderList."
						);
			}
			this.messageCodesHolderList = messageCodesHolderList;
		}
		
	// Constructors -------------------------------------------------------------------------------
	public CacheNameMessageCodesHolderModel() {
		this.cacheNameList = new ArrayList<String>();
		this.messageCodesHolderList = new ArrayList<String>();
	}
	public CacheNameMessageCodesHolderModel( Key key) {
		this();
		this.key = key;
	}
	// --------------------------------------------------------------------------------------------
	
	
	public synchronized String getMessageCodesHolder( String cacheName) {
		int cacheIndex = cacheNameList.indexOf( cacheName);
		if ( cacheIndex > -1) {
			return messageCodesHolderList.get( cacheIndex);
		}
		return null;
	}
		
	public synchronized void setMessageCodesHolder( String cacheName, String messageCodesHolder) {
		if ( cacheNameList == null) {
			cacheNameList = new ArrayList<String>();
		}
		if ( messageCodesHolderList == null) {
			messageCodesHolderList = new ArrayList<String>();
		}
		
		int cacheIndex = cacheNameList.indexOf( cacheName);
		if ( cacheIndex < 0) {
			cacheNameList.add( cacheName);
			messageCodesHolderList.add( messageCodesHolder);
		}
		else {
			messageCodesHolderList.set( cacheIndex, messageCodesHolder);
		}
	} // public void setMessageCodesHolder( String cacheName, String messageCodesHolder)
	
	public synchronized void removeCache( String cacheName) {
		int cacheIndex = cacheNameList.indexOf( cacheName);
		if ( cacheIndex > -1) {
			cacheNameList.remove( cacheIndex);
			messageCodesHolderList.remove( cacheIndex);
		}
	}
	
	// hashCode method and equals method ----------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cacheNameList == null) ? 0 : cacheNameList.hashCode());
		return result;
	}
	
	/**
	 * Consider only cacheNameList member field for comparison
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		
		CacheNameMessageCodesHolderModel other = (CacheNameMessageCodesHolderModel) obj;
		List<String> otherCacheNameList = other.getCacheNameList();
		if ( cacheNameList == null) {
			if ( otherCacheNameList != null) return false;
		} 
		else {
			if ( otherCacheNameList == null) return false;
			
			int cacheNameListSize = cacheNameList.size();
			int otherCacheNameListSize = otherCacheNameList.size();
			if ( cacheNameListSize != otherCacheNameListSize) return false;
			
			ArrayList<String> otherCacheNameListCopy = new ArrayList<String>( otherCacheNameListSize);
			Collections.copy( otherCacheNameListCopy, otherCacheNameList);
			ArrayList<String> cacheNameListCopy = new ArrayList<String>( cacheNameListSize);
			Collections.copy( cacheNameListCopy, cacheNameList);
			
			Collections.sort( cacheNameListCopy);
			Collections.sort( otherCacheNameListCopy);
			if ( !cacheNameListCopy.equals( otherCacheNameListCopy)) return false;
		}
		return true;
	}
	// --------------------------------------------------------------------------------------------
} // protected static class CacheNameMessageCodesHolderModel