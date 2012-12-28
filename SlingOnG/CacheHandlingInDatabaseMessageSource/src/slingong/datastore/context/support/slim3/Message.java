package slingong.datastore.context.support.slim3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Model;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;

@Configurable
@Model
public class Message {
	@Attribute( persistent = false)
	protected static Logger logger = LoggerFactory.getLogger( Message.class);
	
	@Attribute( primaryKey = true)
	private Key key;
		public Key getKey() {
			return key;
		}
		public void setKey( Key key) {
			this.key = key;
		}
	
	private String code;
		public String getCode() {
			return code;
		}
		public void setCode(String code) {
			this.code = code;
		}
	
	// For bidirectional one-to-many relation with Token class
	@Attribute( persistent = false)
	private InverseModelListRef<Token, Message> tokenListRef 
	= new InverseModelListRef<Token, Message>( Token.class, "messageRef", this);
		public final InverseModelListRef<Token, Message> getTokenListRef() {
			return tokenListRef;
		}
		public Map<Locale, String> getTokens() {
			Map<Locale, String> tokensMap = new HashMap<Locale, String>();
			for( Token token : tokenListRef.getModelList()) {
				tokensMap.put( token.getLocale(), token.getContent());
			} // for
			return tokensMap;
		}
		public String getToken( Locale locale) {
			for( Token token : tokenListRef.getModelList()) {
				Locale tokenLocale = token.getLocale();
				if ( locale == null) {
					if ( null == tokenLocale) return null;
				}
				else {
					if ( locale.equals( tokenLocale)) {
						return token.getContent();
					}
				}
			} // for
			return null;
		}

	/**
	 * Returned Message model object has preset key. 
	 */
	@Transactional( propagation=Propagation.REQUIRED)
	public static Message create( String code) {
		Message message = new Message();
		message.setCode( code);
		Future<KeyRange> futureKeys = Datastore.allocateIdsAsync( Message.class, 1);
		try {
			message.setKey( futureKeys.get().getStart());
		}
		catch( Throwable throwable) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format( 
								"Failed to generate key asynchronously for new %1$s entity.%n" +
								"Going to attempt to regenrate it synchronously.", 
								Message.class.getSimpleName()
								), 
						throwable
						);
			}
			message.setKey( Datastore.allocateId( Message.class));
		}
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( message);
		
		return message;
	}
	
	// hashCode and equals methods ----------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Message other = (Message) obj;
		if (code == null) {
			if ( other.code != null) return false;
		} else if (!code.equals(other.code)) return false;
		
		return true;
	}
	// --------------------------------------------------------------------------------------------
}
