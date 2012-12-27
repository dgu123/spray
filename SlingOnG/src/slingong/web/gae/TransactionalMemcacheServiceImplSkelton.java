package slingong.web.gae;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.Stats;

/**
 * Skelton class implemented MemcacheService. 
 * This is just for weaving this file rather than weaving .jar file containing MemcacheService class 
 * provided Google.
 * @author Arata Yamamoto
 */
public abstract class TransactionalMemcacheServiceImplSkelton implements TransactionalMemcacheService {
	protected TransactionalMemcacheServiceImplSkelton() {
	}
	
	@Override
	public void clear() {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void clearAll() {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public boolean contains(Object key) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public boolean delete(Object key) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public boolean delete(Object key, long millisNoReAdd) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Set<T> deleteAll(Collection<T> keys) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Set<T> deleteAll(Collection<T> keys, long millisNoReAdd) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public Object get(Object key) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Map<T, Object> getAll(Collection<T> keys) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public ErrorHandler getErrorHandler() {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public IdentifiableValue getIdentifiable(Object key) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Map<T, IdentifiableValue> getIdentifiables(Collection<T> arg0) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public String getNamespace() {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public Stats getStatistics() {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public Long increment(Object key, long delta) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public Long increment(Object key, long delta, Long initialValue) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Map<T, Long> incrementAll(Map<T, Long> offsets) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Map<T, Long> incrementAll(Collection<T> keys, long delta) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Map<T, Long> incrementAll(Map<T, Long> offsets, Long initialValue) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Map<T, Long> incrementAll(Collection<T> keys, long delta, Long initialValue) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void put(Object key, Object value) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void put(Object key, Object value, Expiration expires) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public boolean put(Object key, Object value, Expiration expires, SetPolicy policy) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void putAll(Map<?, ?> values) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void putAll(Map<?, ?> values, Expiration expires) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Set<T> putAll(Map<T, ?> values, Expiration expires, SetPolicy policy) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public boolean putIfUntouched(Object key, IdentifiableValue oldValue, Object newValue) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public boolean putIfUntouched( 
			Object key, IdentifiableValue oldValue, Object newValue, Expiration expires) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Set<T> putIfUntouched(Map<T, CasValues> values) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public <T> Set<T> putIfUntouched(Map<T, CasValues> values, Expiration expiration) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void setErrorHandler(ErrorHandler handler) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

	@Override
	public void setNamespace(String newNamespace) {
		throw new TransactionalMemcacheServiceException(
				new UnsupportedOperationException( 
						"Not having implemented yet or not being supported."
						)
				);
	}

}
