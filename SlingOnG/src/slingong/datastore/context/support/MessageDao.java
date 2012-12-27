package slingong.datastore.context.support;

import java.util.Locale;
import java.util.Map;
import java.util.Set;


// Interface to DAO layer ---------------------------------------------------------------------
/**
 * In order for DAO layer to adapt to DatabaseMessageSource, 
 * DAO layer needs to implement MessageDao interface
 * @author Arata Yamamoto
 */
public interface MessageDao {
	public String getMessage( String code, Locale locale);
	public Map<Locale, String> getMessages( String code);
	
	public void setMessage( String code, Locale locale, String message);
	
	public void removeMessage( String code, Locale locale);
	public void removeMessages( String code);
}