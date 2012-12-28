package slingong.datastore.context.support;

import java.util.Locale;

/**
 * To hand over message newly created or updated in database to cache
 * @author ay
 */
public class MessageEvent {
	/**
	 * Need to be not null
	 */
	public final String key;
	/**
	 * When both of locale and message are null, then all messages for key will be removed from cache. 
	 */
	public final Locale locale;
	/**
	 * When message is null, then message will be removed from cache.
	 */
	public final String message;

	// Constructor(s) -------------------------------------------------------------------------
	public MessageEvent(String key, Locale locale, String message) {
		this.key = key;
		this.locale = locale;
		this.message = message;
	}
	// ----------------------------------------------------------------------------------------
} // protected static class MessageEvent