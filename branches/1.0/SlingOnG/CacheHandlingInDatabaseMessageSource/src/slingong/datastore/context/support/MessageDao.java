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
	
	/**
	 * In order to synchronize cache used in DatabaseMessageSource to database, 
	 * this method in implementation class must be annotated with @EventTriger for MessageEvent event class, 
	 * and this method must be used to create/update message in database 
	 * @param code
	 * @param locale
	 * @param message
	 */
	public MessageEvent setMessage( String code, Locale locale, String message);
//TODO Need to create aspect to compel @EventTriger annotation on MessageDao.setMessage method.
	/**
	 * In order to synchronize cache used in DatabaseMessageSource to database, 
	 * this method in implementation class must be annotated with @EventTriger for MessageEvent event class,  
	 * and this method must be used to delete message from database 
	 * @param code
	 * @param locale
	 */
	public MessageEvent removeMessage( String code, Locale locale);
//TODO Need to create aspect to compel @EventTriger annotation on MessageDao.removeMessage method.
	
	/**
	 * In order to synchronize cache used in DatabaseMessageSource to database, 
	 * this method in implementation class must be annotated with @EventTriger for MessageEvent event class,  
	 * and this method must be used to delete messages from database 
	 * @param code
	 * @param locale
	 */
	public MessageEvent removeMessages( String code);
//TODO Need to create aspect to compel @EventTriger annotation on MessageDao.removeMessage method.
}