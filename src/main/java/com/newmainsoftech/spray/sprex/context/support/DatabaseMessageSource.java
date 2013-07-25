package com.newmainsoftech.spray.sprex.context.support;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.AbstractMessageSource;

public class DatabaseMessageSource extends AbstractMessageSource 
implements InitializingBean, LocaleFallbackHandlerCase {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
		public Logger getLogger() {
			return logger;
		}

	// For handling locale ------------------------------------------------------------------------
	/**
	 * <code>{@link Locale}</code> constant being set as default ultimate fall-back locale. <br />
	 * This constant value is the <code>{@link Locale}</code> object returned by 
	 * <code>{@link Locale#getDefault()}</code> method.
	 */
	public final static Locale DefaultUltimateFallbackLocale;
		static {
			Locale defaultLocale = Locale.getDefault();
			DefaultUltimateFallbackLocale = new Locale( defaultLocale.getLanguage());
		};
		public static Locale getDefaultUltimateFallbackLocale() {
			return DefaultUltimateFallbackLocale;
		}

	private LocaleFallbackHandlerCase localeFallbackHandler 
	= new LanguagePolicyLocaleFallbackHandler( DatabaseMessageSource.getDefaultUltimateFallbackLocale());
		{
			localeFallbackHandler.setFallbackLocalesList( Arrays.asList( Locale.getAvailableLocales()));
		};
		/**
		 * Getter for <code>{@link LocaleFallbackHandlerCase}</code> object being used to resolve 
		 * fall-back locale.<br />
		 * Thread safety: indirectly unsafe - this method self is thread safe but returned object may 
		 * not be. 
		 * 
		 * @return <code>{@link LocaleFallbackHandlerCase}</code> object being used to resolve 
		 * fall-back locale.
		 */
		protected LocaleFallbackHandlerCase getLocaleFallbackHandler() {
			return localeFallbackHandler;
		}
		/**
		 * Setter of <code>{@link LocaleFallbackHandlerCase}</code> object being used to resolve 
		 * fall-back locale.<br />
		 * Thread safety: indirectly unsafe - this method self is thread safe but 
		 * <code>{@link LocaleFallbackHandlerCase}</code> object as argument needs to be assured of 
		 * its thread safety (externally) for whole life cycle of this object. Means given 
		 * <code>{@link LocaleFallbackHandlerCase}</code> object as argument won't be cloned in this 
		 * method.
		 * 
		 * @param localeFallbackHandler
		 */
		public synchronized void setLocaleFallbackHandler( 
				final LocaleFallbackHandlerCase localeFallbackHandler) {
			this.localeFallbackHandler = localeFallbackHandler;
		}
		// Implementation of LocaleFallbackHandlerCase interface ----------------------------------
		@Override
		public List<Locale> getFallbackLocalesList() {
			return getLocaleFallbackHandler().getFallbackLocalesList();
		}
		@Override
		public synchronized void setFallbackLocalesList( final List<Locale> fallbackLocalesList) {
			getLocaleFallbackHandler().setFallbackLocalesList( fallbackLocalesList);
		}
		@Override
		public Locale getUltimateLocale() {
			return getLocaleFallbackHandler().getUltimateLocale();
		}
		@Override
		public synchronized void setUltimateLocale( final Locale ultimateLocale) {
			getLocaleFallbackHandler().setUltimateLocale( ultimateLocale);
		}
		@Override
		public boolean isUltimateLocale( final Locale locale) {
			return getLocaleFallbackHandler().isUltimateLocale( locale);
		}
		@Override
		public Locale getNextFallbackLocale( final Locale locale) {
			return getLocaleFallbackHandler().getNextFallbackLocale( locale);
		} 
		// ----------------------------------------------------------------------------------------

	// --------------------------------------------------------------------------------------------
	
	protected MessageDaoCase messageDao;
		protected MessageDaoCase getMessageDao() {
			return messageDao;
		}
		public synchronized void setMessageDao( MessageDaoCase messageDao) {
			this.messageDao = messageDao;
		}
	
	/**
	 * To get a group of locale unique messages for a code.<br />
	 * 
	 * @param code Message code.
	 * @return A group of locale unique messages for a code. Empty map when no message is found.
	 */
	protected Map<Locale, String> getMessages( final String code) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( code == null) || "".equals( code)) {
			throw new IllegalArgumentException(
					"Value of code input as MessageModel key value for retriving message can be " 
					+ "neither null nor empty string.");
		}
		// ----------------------------------------------------------------------------------------
		
		Map<Locale, String> messagesMap = getMessageDao().getMessages( code);
			if ( messagesMap == null) {
				messagesMap = new HashMap<Locale, String>();
			}
		return messagesMap;
	} // protected Map<Locale, String> getMessages( String code)
	
	/**
	 * To get message for a code and a locale.
	 *  
	 * @param code Message code.
	 * @param locale 
	 * @return Message for a code and a locale. Null when corresponding message is not found.
	 */
	protected String getMessage( final String code, final Locale locale) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( code == null) || "".equals( code)) {
			throw new IllegalArgumentException( 
					"Value of code input as MessageModel key value for retriving message can be " 
					+ "neither null nor empty string.");
		}
		if ( locale == null) {
			throw new IllegalArgumentException( "Null is not allowed as the locale argument value.");
		}
		// ----------------------------------------------------------------------------------------
		
		String message;
			Locale ultimateFallbackLocale = getUltimateLocale();
			Map<Locale, String> messagesMap = getMessageDao().getMessages( code);
			Locale localeObj = locale;
			while( true) {
				message = messagesMap.get( localeObj);
				if ( message != null) {
					if ( !locale.equals( localeObj)) {
						Logger logger = getLogger();
						if ( logger.isDebugEnabled()) {
							logger.debug( 
									String.format(
											"Found message corresponding to %1$s message key for %2$s " 
											+ "fall-back locale but for %3$s locale.", 
											code,
											localeObj.toString(), 
											locale.toString())
									);
						}
					}
					break; //while
				}
				
				if ( ultimateFallbackLocale == null) {
					if ( localeObj == null) {
							Logger logger = getLogger();
							if ( logger.isDebugEnabled()) {
								logger.debug( 
										String.format(
												"No message was found correspoinding to %1$s message key " 
												+ "even in the line of fall-back locales from %2$s locale.", 
												code, 
												locale.toString())
										);
							}
						break; // while
					}
				}
				else if ( ultimateFallbackLocale.equals( localeObj)) {
						Logger logger = getLogger();
						if ( logger.isDebugEnabled()) {
							logger.debug( 
									String.format(
											"No message was found corresponding to %1$s message key " 
											+ "even in the line of fall-back locales from %2$s locale.", 
											code, 
											locale.toString())
									);
						}
					break; // while
				}
				
				localeObj = getNextFallbackLocale( localeObj);
			} // while
		return message;
	} // protected String getMessage( String code, Locale locale)
	
	// Implementation of abstract method of AbstractMessageSource class ---------------------------
	@Override
	protected MessageFormat resolveCode( final String code, final Locale locale) {
		String message = getMessage( code, locale);
		if ( message == null) return null;
		return createMessageFormat( message, locale);
	}
	// --------------------------------------------------------------------------------------------

	public void setMessage( final String code, final Locale locale, final String message) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( message == null) || "".equals( message)) {
			throw new IllegalArgumentException( 
					"Value of message input to add can be neither null nor empty string.");
		}
		if ( ( code == null) || "".equals( code)) {
			throw new IllegalArgumentException( 
					String.format(
							"Value of code input as MessageModel key value to add \"%1$s...\" message " 
							+ "can be neither null nor empty string.", 
							message.substring( 0, 50))
					);
		}
		if ( locale == null) {
			throw new IllegalArgumentException(
					String.format(
							"Null is not allowed as the locale argument value to add \"%1$s...\" message.",
							message.substring( 0, 50))
					);
		}
		// ----------------------------------------------------------------------------------------
		
		getMessageDao().setMessage( code, locale, message);
	}
	
	public void removeMessage( final String code, final Locale locale) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( code == null) || "".equals( code)) {
			throw new IllegalArgumentException( 
					"Value of code input as MessageModel key value to remove message can be " 
					+ "neither null nor empty string.");
		}
		if ( locale == null) {
			throw new IllegalArgumentException( 
					String.format(
							"Null is not allowed as the locale argument value to remove message " 
							+"corresponding message code %1$s.",
							code)
					);
		}
		// ----------------------------------------------------------------------------------------
		
		getMessageDao().removeMessage( code, locale);
	}
	
	public void removeMessages( final String code) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( code == null) || "".equals( code)) {
			throw new IllegalArgumentException( 
					"Value of code input as MessageModel key value to remove message(s) can be " 
					+ "neither null nor empty string.");
		}
		// ----------------------------------------------------------------------------------------
		
		getMessageDao().removeMessages( code);
	}
	
	// InitializingBean interface implementation --------------------------------------------------
	@Override
	public void afterPropertiesSet() throws Exception {
		if ( getMessageDao() == null) {
			throw new BeanInitializationException( "messageDao property value cannot be null.");
		}
	}
	// --------------------------------------------------------------------------------------------

}
