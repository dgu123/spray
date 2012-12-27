package slingong.datastore.context.support.slim3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Model;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;

@Configurable
@Model
public class MessageModel {
	@Attribute( primaryKey = true)
	private Key key;
		public Key getKey() {
			return key;
		}
		public void setKey( Key key) {
			this.key = key;
		}
		public String getMessageCode() {
			return key.getName();
		}
	
	// For bidirectional one-to-many relation with TokenModel class
	@Attribute( persistent = false)
	private InverseModelListRef<TokenModel, MessageModel> tokenListRef 
	= new InverseModelListRef<TokenModel, MessageModel>( TokenModel.class, "messageRef", this);
		public final InverseModelListRef<TokenModel, MessageModel> getTokenListRef() {
			return tokenListRef;
		}
		public Map<Locale, String> getContents() {
			Map<Locale, String> tokensMap = new HashMap<Locale, String>();
			for( TokenModel token : tokenListRef.getModelList()) {
				tokensMap.put( token.getLocale(), token.getContent());
			} // for
			return tokensMap;
		}
		public TokenModel getToken( Locale locale) {
			for( TokenModel token : tokenListRef.getModelList()) {
				Locale tokenLocale = token.getLocale();
				if ( locale == null) {
					if ( null == tokenLocale) return token;
					continue; // for
				}
				else {
					if ( locale.equals( tokenLocale)) {
						return token;
					}
					continue; // for
				}
			} // for
			return null;
		}
		public String getContent( Locale locale) {
			TokenModel token = getToken( locale);
			if ( token == null) return null;
			return token.getContent();
		}

	// hashCode and equals methods ----------------------------------------------------------------
	// Only key member field is considered in hashCode and equals methods 
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		MessageModel other = (MessageModel) obj;
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key)) return false;
		return true;
	}
	// --------------------------------------------------------------------------------------------
}
