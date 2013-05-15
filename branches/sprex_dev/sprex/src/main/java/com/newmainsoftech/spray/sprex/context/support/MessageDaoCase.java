package com.newmainsoftech.spray.sprex.context.support;

import java.util.Locale;
import java.util.Map;


// Interface to DAO layer ---------------------------------------------------------------------
/**
 * Contract between {@link DatabaseMessageSource} and DAO layer. 
 * @author Arata Yamamoto
 */
public interface MessageDaoCase {
	/**
	 * @param code
	 * @param locale
	 * @return Message string or null.
	 */
	public String getMessage( String code, Locale locale);
	/**
	 * @param code
	 * @return Map with entries of locale and message pair, or empty map.
	 */
	public Map<Locale, String> getMessages( String code);
	
	public void setMessage( String code, Locale locale, String message);
	
	public void removeMessage( String code, Locale locale);
	public void removeMessages( String code);
}