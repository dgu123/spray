package slingong.datastore.context.support;

import java.util.Locale;
import java.util.Map;

public interface CachedMessageDao {
	public String getCachedMessage( String key, Locale locale);
	public Map<Locale, String> getCachedMessages( String key);
	public void setCachedMessage( String key, Locale locale, String message);
	public void setCachedMessages( String key, Map<Locale, String> messagesMap);	
	public void removeCachedMessage( String key, Locale locale);
	public void removeCachedMessages( String key);
	public void clearCachedMessages();
}