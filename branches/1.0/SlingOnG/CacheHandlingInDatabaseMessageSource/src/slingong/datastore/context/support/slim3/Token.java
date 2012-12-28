package slingong.datastore.context.support.slim3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.Text;

@Model
public class Token {
	@Attribute( persistent = false)
	protected static Logger logger = LoggerFactory.getLogger( Token.class);
	
	@Attribute( primaryKey = true)
	private Key key;
		public Key getKey() {
			return key;
		}
		public void setKey( final Key key) {
			this.key = key;
		}

	private String language;
	private String country;
	private String variant;
		public Locale getLocale() {
			return new Locale( language, country, variant);
		}
		public void setLocale( final Locale locale) {
			this.language = locale.getISO3Language();
			this.country = locale.getISO3Country();
			this.variant = locale.getDisplayVariant();
		}
		
		public String getLanguage() {
			return language;
		}
		public void setLanguage( final String language) {
			this.language = language;
		}
		public String getCountry() {
			return country;
		}
		public void setCountry( final String country) {
			this.country = country;
		}
		public String getVariant() {
			return variant;
		}
		public void setVariant( final String variant) {
			this.variant = variant;
		}
	
	private List<String> contentsList;
		public String getContent() {
			if ( contentsList == null) return null;
			StringBuffer stringBuffer = new StringBuffer();
			synchronized( contentsList) {
				for( String content : contentsList) {
					stringBuffer.append( content);
				} // for
				contentsList.notifyAll();
			} // synchronized( contentsList)
			return stringBuffer.toString();
		}
		public final static int MaxStringLengthLimit = 500;
		protected void addContent( String contentWorkingCopy, final List<String> contentsList) {
			if ( contentWorkingCopy.length() > MaxStringLengthLimit) {
				do {
					String contentToken = contentWorkingCopy.substring( 0, MaxStringLengthLimit);
					int lastSbcsSpaceIndex = contentToken.lastIndexOf( " ");
					int lastDbcsSpaceIndex = contentToken.lastIndexOf( "　");
					if (( lastSbcsSpaceIndex < 0) && ( lastDbcsSpaceIndex < 0)) {
						contentsList.add( contentToken);
						contentWorkingCopy 
						= contentWorkingCopy.substring( MaxStringLengthLimit);
					}
					else if ( lastSbcsSpaceIndex < lastDbcsSpaceIndex) {
						contentsList.add( contentToken.substring( 0, lastDbcsSpaceIndex));
						contentWorkingCopy 
						= contentWorkingCopy.substring( lastDbcsSpaceIndex);
					}
					else {
						contentsList.add( contentToken.substring( 0, lastSbcsSpaceIndex));
						contentWorkingCopy 
						= contentWorkingCopy.substring( lastSbcsSpaceIndex);
					}
				} while( contentWorkingCopy.length() > MaxStringLengthLimit);
				contentsList.add( contentWorkingCopy);
			}
			else {
				contentsList.add( contentWorkingCopy);
			}
		} // protected void addContent( String contentsWorkingCopy, final List<String> contentsList)
		public void setContent( final String content) {
			if ( contentsList == null) {
				contentsList = new ArrayList<String>();
			}
			synchronized( contentsList) {
				contentsList.clear();
				String contentWorkingCopy = content;
				addContent( contentWorkingCopy, contentsList);
				
				contentsList.notifyAll();
			} // synchronized( contentsList)
		} // public void setContent( final String contents)
		public void appendContent( final String content) {
			if ( contentsList == null) {
				setContent( content);
			}
			else {
				synchronized( contentsList) {
					String contentWorkingCopy = "";
					if ( contentsList.size() > 0) {
						// Optimize length of String element in contentsList in order to minimize exploding indexes
						int contentsListIndex = contentsList.size() - 1;
						contentWorkingCopy = contentsList.get( contentsListIndex);
						contentWorkingCopy = contentWorkingCopy 
								+ content.substring( 0, MaxStringLengthLimit - contentWorkingCopy.length());
						contentsList.remove( contentsListIndex);
						contentsList.add( contentWorkingCopy);
						contentWorkingCopy 
						= content.substring( MaxStringLengthLimit - contentWorkingCopy.length());
					}
					else contentWorkingCopy = content;
					
					addContent( contentWorkingCopy, contentsList);
							
					contentsList.notifyAll();
				} // synchronized( contentsList)
			}
		} // public void appendContents( final String contents)
		public List<String> getContentsList() {
			return Collections.synchronizedList( contentsList);
		}
		public void setContentsList( final List<String> contentsList) {
			this.contentsList = contentsList;
		}

	// For bidirectional one-to-many relation with Message class
	private ModelRef<Message> messageRef = new ModelRef<Message>( Message.class);
		public final ModelRef<Message> getMessageRef() {
			return messageRef;
		}
		
//TODO Move to DAO layer. And doesn't need to check whether Transactional annotation works on constructor.
	/**
	 * Returned Token model object has preset key. 
	 */
	@Transactional( propagation=Propagation.REQUIRED)
	public static Token create( final Locale locale, final String content) {
		Token token = new Token();
		token.setLocale( locale);
		token.setContent( content);
		Future<KeyRange> futureKeys = Datastore.allocateIdsAsync( Token.class, 1);
		try {
			token.setKey( futureKeys.get().getStart());
		}
		catch( Throwable throwable) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format( 
								"Failed to generate key asynchronously for new %1$s entity.%n" +
								"Going to attempt to regenrate it synchronously.", 
								Token.class.getSimpleName()
								), 
						throwable
						);
			}
			token.setKey( Datastore.allocateId( Message.class));
		}
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( token);
		
		return token;
	}
	
	// hashCode and equals methods ----------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((country == null) ? 0 : country.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((messageRef == null) ? 0 : messageRef.hashCode());
		result = prime * result + ((variant == null) ? 0 : variant.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Token other = (Token) obj;
		if (country == null) {
			if (other.country != null) return false;
		} else if (!country.equals(other.country)) return false;
		if (language == null) {
			if (other.language != null) return false;
		} else if (!language.equals(other.language)) return false;
		if (messageRef == null) {
			if (other.messageRef != null) return false;
		} else if (!messageRef.equals(other.messageRef)) return false;
		if (variant == null) {
			if (other.variant != null) return false;
		} else if (!variant.equals(other.variant)) return false;
		return true;
	}
	// --------------------------------------------------------------------------------------------
}
