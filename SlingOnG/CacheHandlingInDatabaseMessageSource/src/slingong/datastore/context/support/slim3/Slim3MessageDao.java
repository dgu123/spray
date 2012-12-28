package slingong.datastore.context.support.slim3;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import slingong.datastore.context.support.MessageDao;
import slingong.datastore.context.support.MessageEvent;

public class Slim3MessageDao implements MessageDao {
	protected final Logger logger = LoggerFactory.getLogger( this.getClass());
	
	/**
	 * Caution: since this is not using query by Slim3 transaction, this cannot be called from 
	 * transactional execution.
	 * Although this has Propagation.NOT_SUPPORTED specified as propagation setting, current 
	 * Slim3PlatformTransactionManager will throw IllegalTransactionStateException exception 
	 * when there is active global transaction.
	 */
	@Override
	@Transactional( propagation=Propagation.NOT_SUPPORTED)
	public Map<Locale, String> getMessages( String code) {
		MessageMeta messageMeta = MessageMeta.get();
		List<Message> messagesList 
		= Datastore.query( messageMeta).filter( messageMeta.code.equal( code)).asList();
		if ( messagesList.size() < 1) return null;
		if ( messagesList.size() > 1) {
			if ( logger.isWarnEnabled()) {
				logger.warn(
						String.format(
								"Found more than one Message entity (%1$d entities) for code %2$s. " 
								+ "Going to use the first Message entity from the list.",
								messagesList.size(), 
								code
								)
						);
			}
		}
		Message message = messagesList.get( 0);
		return message.getTokens();
	} // public Map<Locale, String> getMessages(String code)
	
	@Override
	public String getMessage( String code, Locale locale) {
		Map<Locale, String> map = getMessages( code);
		if ( map == null) return null;
		return map.get( locale);
	}

	@Override
	public MessageEvent setMessage( String code, Locale locale, String message) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageEvent removeMessage( String code, Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MessageEvent removeMessages(String code) {
		// TODO Auto-generated method stub
		return null;
	}
}
