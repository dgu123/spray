package slingong.web.gae;

import com.google.appengine.api.memcache.MemcacheService;

public interface TransactionalMemcacheService extends MemcacheService {
	public void clear(); 
}
