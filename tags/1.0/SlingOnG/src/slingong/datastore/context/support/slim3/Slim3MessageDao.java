package slingong.datastore.context.support.slim3;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.jsr107cache.Cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyRange;

import slingong.datastore.Slim3PlatformTransactionManager;
import slingong.datastore.context.support.CachedMessageDao;
import slingong.datastore.context.support.MessageDao;
import slingong.datastore.context.support.MessageEvent;
import slingong.datastore.context.support.gae.JCacheMessageDao;

//TODO need to confirm whether execution of method annotated with @Transactional will yield error without transaction manager
//TODO need to autowire Slim3PlatformTransactionManager object and need to set synchronization to not SYNCHRONIZATION_NEVER 
//TODO need to think of transaction exception handler to clean up element on what exception occurs or  
// to clean up whole cache in order for cache to keep synch with datastore
/**
 * Layer of access point to MessageModel class and TokenModel class objects.
 * All back-end GAE/J big-table operations for MessageModel entity and TokenModel entity are done by 
 * this layer. Thereby, transactional access methods for MessageModel entity and TokenModel entity are 
 * concentrated to this, and should not be in anywhere else.
 * Also, cache handling for MessageModel class and TokenModel class objects is done by this.
 * Prerequisite:
 * Slim3PlatformTransactionManager bean has been injected to application context.
 * @author Arata Yamamoto
 */
public class Slim3MessageDao implements MessageDao, InitializingBean {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	protected static final Object cacheLockObj = new Object();
	// For local cache configuration --------------------------------------------------------------
	/**
	 * Wait to inject locallyCachedMessageDao until http://code.google.com/p/googleappengine/issues/detail?id=5929 is resolved,  
	 * in order to avoid that local caches among each app instances become out of sync.
	 * Additionally unless http://code.google.com/p/ehcache-spring-annotations/issues/detail?id=57&can=1&q=app%20engine 
	 * is really actually fixed (this may not be fixed if Spring 3.1 is released) or Spring 3.1 is released, 
	 * EhcachedMessageDaoImpl cannot be injected.
	 */
	protected CachedMessageDao locallyCachedMessageDao = null;
	/**
	 * Wait to inject locallyCachedMessageDao until http://code.google.com/p/googleappengine/issues/detail?id=5929 is resolved,  
	 * in order to avoid that local caches among each app instances become out of sync.
	 * Additionally unless http://code.google.com/p/ehcache-spring-annotations/issues/detail?id=57&can=1&q=app%20engine 
	 * is really actually fixed (this may not be fixed if Spring 3.1 is released) or Spring 3.1 is released, 
	 * EhcachedMessageDaoImpl cannot be injected.
	 */
	public synchronized void setLocallyCachedMessageDao( CachedMessageDao locallyCachedMessageDao) {
		this.locallyCachedMessageDao = locallyCachedMessageDao;
	}
	// --------------------------------------------------------------------------------------------
	// For memcache -------------------------------------------------------------------------------
	protected CachedMessageDao memcacheMessageDao = null;
		public synchronized void setMemcacheMessageDao( CachedMessageDao memcacheMessageDao) {
			this.memcacheMessageDao = memcacheMessageDao;
		}
	// --------------------------------------------------------------------------------------------
	
	// Regarding MessageModel entity --------------------------------------------------------------
	/**
	 * Factory method for MessageModel object backed up by MessageModel entity.
	 * (Thereby, returned MessageModel model object has preset key.) 
	 */
	@Transactional( propagation=Propagation.REQUIRED)
	protected MessageModel createMessageModelEntity( String messageCode) {
		MessageModel messageModel = new MessageModel();
		Key key = Datastore.createKey( MessageModel.class, messageCode);
		messageModel.setKey( key);
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( messageModel);
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"Created MessageModel entity in datastore for message code: %1$s.", 
							messageCode
							)
					);
		}
		return messageModel;
	}
	
	@Transactional( propagation=Propagation.REQUIRED)
	protected MessageModel getMessageModelEntity( String messageCode) {
		Key messageKey = Datastore.createKey( MessageModel.class, messageCode);
		return Datastore.getCurrentGlobalTransaction().get( MessageModel.class, messageKey);
	}
	
	// --------------------------------------------------------------------------------------------

	// Regarding TokenModel entity ----------------------------------------------------------------
	/**
	 * Factory method for TokenModel object backed up by TokenModel entity.
	 * (Thereby, returned TokenModel model object has preset key.) 
	 */
	@Transactional( propagation=Propagation.REQUIRED)
	protected TokenModel createTokenModelEntity( 
			MessageModel messageModel, final Locale locale, final String content) {
		
		TokenModel tokenModel = new TokenModel();
		tokenModel.setLocale( locale);
		tokenModel.setContent( content);
		Future<KeyRange> futureKeys 
		= Datastore.allocateIdsAsync( messageModel.getKey(), TokenModel.class, 1);
		try {
			tokenModel.setKey( futureKeys.get().getStart());
		}
		catch( Throwable throwable) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format( 
								"Failed to generate key asynchronously for new %1$s entity.%n" +
								"Going to attempt to regenrate it synchronously.", 
								TokenModel.class.getSimpleName()
								), 
						throwable
						);
			}
			
			tokenModel.setKey( Datastore.allocateId( messageModel.getKey(), MessageModel.class));
		}
		tokenModel.getMessageRef().setModel( messageModel);
		messageModel.getTokenListRef().getModelList().add( tokenModel);
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( tokenModel);	// link to tokenModel in messageModel will be automatically updated in datastore without saving here.
		
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"Created TokenModel entity in datastore for message code: %1$s and locale: %2$s.", 
							messageModel.getMessageCode(), 
							locale.toString()
							)
					);
		}
		return tokenModel;
	}
	// --------------------------------------------------------------------------------------------
	
	protected enum MessageCacheSynchronizerOperationType {
		Get, Set, RemoveLocaleMessage, RemoveMessageMap;
	}
	protected class MessageCacheSynchronizer extends TransactionSynchronizationAdapter {
		
		protected Logger logger = LoggerFactory.getLogger( this.getClass());
		
		protected String messageCode;
		protected Locale locale;
		protected Map<Locale, String> messagesMap;
		protected MessageCacheSynchronizerOperationType messageCacheSynchronizerOperationType;
		
		public MessageCacheSynchronizer( 
				String messageCode, Locale locale, Map<Locale, String> messagesMap, 
				MessageCacheSynchronizerOperationType messageCacheSynchronizerOperationType) {
			this.messageCode = messageCode;
			this.locale = locale;
			this.messagesMap = messagesMap;
			this.messageCacheSynchronizerOperationType = messageCacheSynchronizerOperationType;
		}
		
		protected void removeLocaleMessage() {
			synchronized( cacheLockObj) {
				if ( memcacheMessageDao != null) {
					memcacheMessageDao.removeCachedMessage( messageCode, locale);
				}
				if ( locallyCachedMessageDao != null) {
					locallyCachedMessageDao.removeCachedMessage( messageCode, locale);
				}
				cacheLockObj.notifyAll();
			} // synchronized( cacheLockObj)
		}
		protected void removeMessageMap() {
			synchronized( cacheLockObj) {
				if ( memcacheMessageDao != null) {
					memcacheMessageDao.removeCachedMessages( messageCode);
				}
				if ( locallyCachedMessageDao != null) {
					locallyCachedMessageDao.removeCachedMessages( messageCode);
				}
				cacheLockObj.notifyAll();
			} // synchronized( cacheLockObj)
		}
		
		@Override
		public void afterCompletion( int status) {
			switch( status) {
			case TransactionSynchronization.STATUS_COMMITTED:
				switch( messageCacheSynchronizerOperationType) {
				case Get:
				case Set:
					// Set cache
					synchronized( cacheLockObj) {
						if ( memcacheMessageDao != null) {
							memcacheMessageDao.setCachedMessages( messageCode, messagesMap);
						}
						if ( locallyCachedMessageDao != null) {
							locallyCachedMessageDao.setCachedMessages( messageCode, messagesMap);
						}
						cacheLockObj.notifyAll();
					} // synchronized( cacheLockObj)
					break;
				case RemoveLocaleMessage:
					removeLocaleMessage();
					break;
				case RemoveMessageMap:
					removeMessageMap();
					break;
				} // switch( messageCacheSynchronizerOperationType)
			case TransactionSynchronization.STATUS_ROLLED_BACK:
				// Do nothing;
				break;
			default:	
				// Delete entry from cache in order to let fall back to datastore at next operation
				switch( messageCacheSynchronizerOperationType) {
				case Get:
					// Do nothing since both of local cache and memcache have not held message any way
					break;
				case Set:
				case RemoveLocaleMessage:
					removeLocaleMessage();
					break;
				case RemoveMessageMap:
					removeMessageMap();
					break;
				}
				break;
			} // switch
		} // public void afterCompletion( int status)
	} // protected class MessageCacheSynchronizer extends TransactionSynchronizationAdapter
	
	// MessageDao implementation ------------------------------------------------------------------
	
	@Transactional( propagation=Propagation.REQUIRED)
	protected Map<Locale, String> getMessagesWoker( String messageCode) {
		Map<Locale, String> tokensMap = null;
		
		MessageModel messageModel = getMessageModelEntity( messageCode);
		if ( messageModel == null) return null;
		
		tokensMap = messageModel.getContents();
		
		TransactionSynchronization transactionSynchronization 
		= new MessageCacheSynchronizer( 
				messageCode, null, tokensMap, MessageCacheSynchronizerOperationType.Get);
		TransactionSynchronizationManager.registerSynchronization( transactionSynchronization);
		
		return tokensMap;
	} // protected Map<Locale, String> getMessagesWoker( String messageCode)
	
	@Override
	public Map<Locale, String> getMessages( String messageCode) {
		Map<Locale, String> tokensMap = null;
		
		// Check on cache -------------------------------------------------------------------------
		synchronized( cacheLockObj) {
			// Check on local cache at first ------------------------------------------------------
			if ( locallyCachedMessageDao != null) {
				tokensMap = locallyCachedMessageDao.getCachedMessages( messageCode);
				if ( tokensMap != null) {
					cacheLockObj.notifyAll();
					return tokensMap;
				}
			}
			// ------------------------------------------------------------------------------------
			
			// Check on memcache ------------------------------------------------------------------
			if ( memcacheMessageDao != null) {
				tokensMap = (Map<Locale, String>)memcacheMessageDao.getCachedMessages( messageCode);
				if ( ( tokensMap != null) && ( locallyCachedMessageDao != null)) {
					// update local cache
					if ( locallyCachedMessageDao.getCachedMessages( messageCode) == null) {
						locallyCachedMessageDao.setCachedMessages( messageCode, tokensMap);
					}
					cacheLockObj.notifyAll();
					return tokensMap;
				}
			}
			// ------------------------------------------------------------------------------------
		} // synchronized( cacheLockObj)
		// ----------------------------------------------------------------------------------------
		
		return getMessagesWoker( messageCode);
	} // public Map<Locale, String> getMessages(String messageCode)
	
	@Override
	public String getMessage( String messageCode, Locale locale) {
		Map<Locale, String> tokensMap = getMessages( messageCode);
		if ( tokensMap == null) return null;
		return tokensMap.get( locale);
	}

	@Override
	@Transactional( propagation=Propagation.REQUIRED)
	public void setMessage( String messageCode, Locale locale, String messageContent) {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		
		MessageModel messageModel = getMessageModelEntity( messageCode);
		if ( messageModel == null) {
			messageModel = createMessageModelEntity( messageCode);	// Create message entity
			createTokenModelEntity( messageModel, locale, messageContent);	// Create token entity and update message entity
		}
		else {
			TokenModel tokenModel = messageModel.getToken( locale);
			tokenModel.setContent( messageContent);	// Update token entity
			gtx.put( tokenModel);	// Save update on only token entity to datastore
		}
		
		TransactionSynchronization transactionSynchronization 
		= new MessageCacheSynchronizer( 
				messageCode, locale, messageModel.getContents(), MessageCacheSynchronizerOperationType.Set);
		TransactionSynchronizationManager.registerSynchronization( transactionSynchronization);
		
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"Having set new message in datastore for message code: %1$s and locale: %2$s.", 
							messageCode, 
							locale.toString()
							)
					);
		}
	} // public void setMessage( String messageCode, Locale locale, String messageContent)
	
	@Override
	@Transactional( propagation=Propagation.REQUIRED)
	public void removeMessage( String messageCode, Locale locale) {
		MessageModel messageModel = getMessageModelEntity( messageCode);
		TokenModel tokenModel = messageModel.getToken( locale);
		messageModel.getTokenListRef().getModelList().remove( tokenModel);
		Datastore.getCurrentGlobalTransaction().delete( tokenModel.getKey());
		
		TransactionSynchronization transactionSynchronization 
		= new MessageCacheSynchronizer( 
				messageCode, locale, messageModel.getContents(), 
				MessageCacheSynchronizerOperationType.RemoveLocaleMessage);
		TransactionSynchronizationManager.registerSynchronization( transactionSynchronization);
		
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"Removed message in datastore for message code: %1$s and locale: %2$s.", 
							messageCode, 
							locale.toString()
							)
					);
		}
	} // public void removeMessage( String messageCode, Locale locale)

	@Override
	@Transactional( propagation=Propagation.REQUIRED)
	public void removeMessages( String messageCode) {
		MessageModel messageModel = getMessageModelEntity( messageCode);
		Set<Key> keysSet = new HashSet<Key>();
		keysSet.add( messageModel.getKey());
		if ( messageModel.getTokenListRef().getModelList().size() > 0) {
			for( TokenModel tokenModel : messageModel.getTokenListRef().getModelList()) {
				keysSet.add( tokenModel.getKey());
			} // for
		}
		Datastore.getCurrentGlobalTransaction().delete( keysSet);
		messageModel.getTokenListRef().getModelList().clear();
		
		TransactionSynchronization transactionSynchronization 
		= new MessageCacheSynchronizer( 
				messageCode, null, messageModel.getContents(), 
				MessageCacheSynchronizerOperationType.RemoveMessageMap);
		TransactionSynchronizationManager.registerSynchronization( transactionSynchronization);
		
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"Removed all locale messages in datastore for message code: %1$s.", 
							messageCode 
							)
					);
		}
	} // public void removeMessages( String messageCode)
	// --------------------------------------------------------------------------------------------
	
	// InitializingBean interface implementation --------------------------------------------------
	/**
	 * 
	 */
	@Autowired( required=true)
	protected Slim3PlatformTransactionManager slim3PlatformTransactionManager;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if ( !( slim3PlatformTransactionManager instanceof Slim3PlatformTransactionManager)) {
			throw new BeanInitializationException( 
					String.format( 
							"Could not acknowlege existance of %1$s bean what this %2$s bean depends on.", 
							Slim3PlatformTransactionManager.class.getName(), 
							this.getClass().getSimpleName()
							)
					);
		}
	}
	// --------------------------------------------------------------------------------------------
}
