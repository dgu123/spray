package slingong.datastore.context.support;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import com.newmainsoftech.aspectjutil.eventmanager.label.EventListener;

/* Needs to create AOP event-driven engine first before this. 
 * 	It's for adding new message added to database to cache. 
 * 	So, this DatabaseMessageSource implements the listener of that AOP event-driven engine on new message event, 
 * 	instead of using time duration refresh mechanism.
 *  And add that message to message cache.
 */
@EventListener
public class DatabaseMessageSource extends AbstractMessageSource 
implements ResourceLoaderAware, InitializingBean {
	protected final Logger logger = LoggerFactory.getLogger( this.getClass());
	
	// For handling locale ------------------------------------------------------------------------
	protected List<Locale> fallbackLocalesList = null;
		public List<Locale> getFallbackLocalesList() {
			return fallbackLocalesList;
		}
		protected Map<String, Set<Locale>> languageMappedLocales = null;
		protected Map<String, Set<Locale>> countryMappedLocales = null;
		public void setFallbackLocalesList( final List<Locale> fallbackLocalesList) {
			List<Locale> listOfFallbackLocales = null;
			Map<String, Set<Locale>> localesMappedByLanguage = null;
			Map<String, Set<Locale>> localesMappedByCountry = null;
			if ( fallbackLocalesList != null) {
				listOfFallbackLocales 
				= Collections.synchronizedList( new ArrayList<Locale>( fallbackLocalesList));
				
				localesMappedByLanguage 
				= Collections.synchronizedMap( new LinkedHashMap<String, Set<Locale>>());
				localesMappedByCountry
				= Collections.synchronizedMap( new LinkedHashMap<String, Set<Locale>>());
				
				for( Locale locale : listOfFallbackLocales) {
					String languageStr = locale.getISO3Language();
					if ( languageStr.length() > 0) {
						Set<Locale> localesSet = localesMappedByLanguage.get( languageStr);
						if ( localesSet == null) {
							localesSet = new LinkedHashSet<Locale>();
							localesMappedByLanguage.put( languageStr, localesSet);
						}
						if ( !localesSet.contains( locale)) {
							localesSet.add( locale);
						}
					}
					String countryStr = locale.getISO3Country();
					if ( countryStr.length() > 0) {
						Set<Locale> localesSet = localesMappedByCountry.get( countryStr);
						if ( localesSet == null) {
							localesSet = new LinkedHashSet<Locale>();
							localesMappedByCountry.put( countryStr, localesSet);
						}
						if ( !localesSet.contains( locale)) {
							localesSet.add( locale);
						}
					}
				} // for
			}
			
			synchronized( this) {
				this.fallbackLocalesList = Collections.unmodifiableList( listOfFallbackLocales);
				this.languageMappedLocales = Collections.unmodifiableMap( localesMappedByLanguage);
				this.countryMappedLocales = Collections.unmodifiableMap( localesMappedByCountry);
				this.notifyAll();
			} // synchronized( this)
		} // public final void setFallbackLocalesList( List<Locale> fallbackLocalesList)
	
	/**
	 * To control order of how fall back locale will be chosen.
	 */
	public static enum FallbackPreceding {
		/**
		 * When the next locales are considerably available for fall back: en-US, sp-US, en-UK, 
		 * the order of fall back with this LANGUAGE preceding will be en-US -> en-UK -> en 
		 */
		LANG_OVER_COUNTRY,
		/**
		 * When the next locales are considerably available for fall back: en-US, sp-US, en-UK, 
		 * the order of fall back with this LANGUAGE preceding will be en-US -> sp-US -> US 
		 */
		COUNTRY_OVER_LANG, 
		/**
		 * When the next locales are considerably available for fall back: en-US, sp-US, en-UK,
		 * the order of fall back with this LANGUAGE preceding will be en-US -> en 
		 */
		LANGUAGE,
		/**
		 * When the next locales are considerably available for fall back: en-US, sp-US, en-UK, 
		 * the order of fall back with this LANGUAGE preceding will be en-US -> US 
		 */
		COUNTRY;
	}
	/**
	 * To control order of how fall back locale will be chosen in execution of 
	 * default implementation of getNextFallbackLocale method.
	 * Default value is FallbackPreceding.NONE.
	 */
	protected FallbackPreceding fallbackPreceding = FallbackPreceding.LANGUAGE;
		public final FallbackPreceding getFallbackPreceding() {
			return FallbackPreceding.valueOf( fallbackPreceding.toString());
		}
		public synchronized void setFallbackPreceding( FallbackPreceding fallbackPreceding) {
			if ( fallbackPreceding == null) {
				throw new InvalidPropertyException(
						this.getClass(), "fallbackPreceding", 
						"The value of fallbackPreceding argument cannot be null.");
			}
			this.fallbackPreceding = fallbackPreceding;
		}
	/**
	 * To control whether null locale can be used as ultimate fall back locale in execution of 
	 * default implementation of getNextFallbackLocale method.
	 * When this is set to false, the ultimate fall back locale will be the last locale in fallbackLocalesList.
	 * 
	 * Default is false.
	 */
	protected AtomicBoolean nullUltimateFallbackLocaleAllowed = new AtomicBoolean( false);
		public boolean getNullUltimateFallbackLocaleAllowed() {
			return nullUltimateFallbackLocaleAllowed.get();
		}
		public void setNullUltimateFallbackLocaleAllowed( boolean nullUltimateFallbackLocaleAllowed) {
			this.nullUltimateFallbackLocaleAllowed.set( nullUltimateFallbackLocaleAllowed);
		}
	
	/*
	 * 	If input locale object has language, country, and variant specified,
	 * 		then search fallbackLocalesList for input locale object
	 * 			If found
	 * 				From next element in fallbackLocalesList, search for locale object with different variant 
	 * 					If such locale object is found
	 * 						then return such locale object from fallbackLocalesList
	 * 					If not
	 * 						then return locale object with same language and country
	 * 			If not
	 * 				then search locale object with different variant in fallbackLocalesList
	 * 					If such locale object is found
	 * 						then return such locale object from fallbackLocalesList
	 * 					If not
	 *						then return locale object with same language and country
	 * 	If input locale object has language and country specified, 
	 * 		If fallbackPreceding is LANGUAGE
	 * 			then return Locale object with only same language
	 * 		If fallbackPreceding is LANG_OVER_COUNTRY
	 * 			then search fallbackLocalesList for input locale object
	 * 				If found
	 * 					From next element in fallbackLocalesList, search for locale object with same language
	 * 						If found
	 * 							then return such locale object from fallbackLocalesList
	 * 						If not
	 * 							then return Locale object with only same language
	 * 				If not 
	 * 					then search fallbackLocalesList for locale object with same language
	 * 						If such locale object is found
	 * 							then return such locale object from fallbackLocalesList
	 * 						If not
	 * 							then return Locale object with only same language
	 * 		If fallbackPreceding is COUNTRY
	 * 			then return Locale object with only same country
	 * 		If fallbackPreceding is COUNTRY_OVER_LANG
	 * 			then search fallbackLocalesList for input locale object
	 * 				If found
	 * 					From next element in fallbackLocalesList, search for locale object with same country
	 * 						If found
	 * 							then return such locale object from fallbackLocalesList
	 * 						If not
	 * 							then return Locale object with only same country
	 * 				If not
	 * 					then search fallbackLocalesList for locale object with same country 
	 * 						If such locale object is found
	 * 							then return such locale object from fallbackLocalesList
	 * 						If not
	 * 							then return Locale object with only same country
	 *  If input locale object has language and variant specified,
	 * 		then search fallbackLocalesList for input locale object
	 * 			If found
	 * 				From next element in fallbackLocalesList, search for locale object with same language, no country, and different variant
	 * 					If found
	 * 						then return such locale object from fallbackLocalesList
	 * 					If not
	 * 						then return Locale object with only same language
	 * 			If not 
	 * 				then search fallbackLocalesList for locale object with same language, no country, and different variant
	 * 					If such locale object is found
	 * 						then return such locale object from fallbackLocalesList
	 * 					If not
	 * 						then return Locale object with only same language
	 *  If input locale object has country and variant specified,
	 * 		then search fallbackLocalesList for input locale object
	 * 			If found
	 * 				From next element in fallbackLocalesList, search for locale object with no language, same country, and different variant
	 * 					If found
	 * 						then return such locale object from fallbackLocalesList
	 * 					If not
	 * 						then return Locale object with only country language
	 * 			If not 
	 * 				then search fallbackLocalesList for locale object with no language, same country, and different variant
	 * 					If such locale object is found
	 * 						then return such locale object from fallbackLocalesList
	 * 					If not
	 * 						then return Locale object with only country language
	 * 	If input locale object has only language specified, 
	 * 		If nullLocaleAllowed is false
	 * 			then return last Locale object from fallbackLocalesList
	 * 		If not
	 * 			then return null
	 * 	If input locale object has only country specified, 
	 * 		If nullLocaleAllowed is false
	 * 			then return last Locale object from fallbackLocalesList
	 * 		If not
	 * 			then return null
	 * 	If input locale object has only variant specified, 
	 * 		If nullLocaleAllowed is false
	 * 			then return last Locale object from fallbackLocalesList
	 * 		If not
	 * 			then return null
	 */
	protected Locale getNextFallbackLocale( Locale locale) {
		String language = locale.getISO3Language();
		String country = locale.getISO3Country();
		String variant = locale.getVariant();
		
		if ( ( language.length() > 0) && ( country.length() > 0) && ( variant.length() > 0)) {
			if ( countryMappedLocales == null) return new Locale( language, country);
			Locale backupLocale = null;
			boolean inputLocaleMarkerFlag = false;
			synchronized( countryMappedLocales) {
				Set<Locale> localesSet = countryMappedLocales.get( country);
				for( Locale localeObj : localesSet) {
					if ( !inputLocaleMarkerFlag) {
						if ( locale.equals( localeObj)) {
							inputLocaleMarkerFlag = true;
							continue; // for
						}
						else {
							if ( !language.equals( localeObj.getISO3Language())) continue; // for
							if ( localeObj.getVariant().length() < 1) continue; 
							if ( backupLocale == null) backupLocale = localeObj;
							continue; // for
						}
					}
					else {
						if ( !language.equals( localeObj.getISO3Language())) continue; // for
						if ( localeObj.getVariant().length() < 1) continue;
						countryMappedLocales.notifyAll();
						return localeObj;
					}
				} // for
				
				countryMappedLocales.notifyAll();
			} // synchronized( countryMappedLocales)
			
			if ( backupLocale != null) return backupLocale; 
			return new Locale( language, country);
		}
		else if ( ( language.length() > 0) && ( country.length() > 0)) {
			Set<Locale> localesSet;
			boolean inputLocaleMarkerFlag = false;
			Locale backupLocale = null;
			
			switch( fallbackPreceding) {
			case LANGUAGE:
				return new Locale( language);
			case LANG_OVER_COUNTRY:
				if ( languageMappedLocales == null) {
					return new Locale( language);
				}
				synchronized( languageMappedLocales) {
					localesSet = languageMappedLocales.get( language);
					for( Locale localeObj : localesSet) {
						if ( !inputLocaleMarkerFlag) {
							if ( locale.equals( localeObj)) {
								inputLocaleMarkerFlag = true;
								continue; // for
							}
							else {
								if ( backupLocale == null) backupLocale = localeObj;
								continue; // for
							}
						}
						else {
							languageMappedLocales.notifyAll();
							return localeObj;
						}
					} // for
					
					languageMappedLocales.notifyAll();
				} // synchronized( languageMappedLocales)
				
				if ( backupLocale != null) return backupLocale; 
				return new Locale( language);
			case COUNTRY:
				return new Locale( country);
			case COUNTRY_OVER_LANG:
				if ( countryMappedLocales == null) {
					return new Locale( country);
				}
				synchronized( countryMappedLocales) {
					localesSet = countryMappedLocales.get( country);
					for( Locale localeObj : localesSet) {
						if ( !inputLocaleMarkerFlag) {
							if ( locale.equals( localeObj)) {
								inputLocaleMarkerFlag = true;
								continue; // for
							}
							else {
								if ( backupLocale == null) backupLocale = localeObj;
								continue; // for
							}
						}
						else {
							countryMappedLocales.notifyAll();
							return localeObj;
						}
					} // for
					
					countryMappedLocales.notifyAll();
				} // synchronized( countryMappedLocales)
				
				if ( backupLocale != null) return backupLocale; 
				return new Locale( language);
			default:
				throw new InvalidPropertyException( 
						this.getClass(), 
						"fallbackPreceding", 
						String.format(
								"\"%1$s\" is the unkown value as fallbackPreceding property value.", 
								fallbackPreceding.toString()
								)
								);
			} // fallbackPreceding
		}
		else if ( ( language.length() > 0) && ( variant.length() > 0)) {
			if ( languageMappedLocales == null) {
				return new Locale( language);
			}
			boolean inputLocaleMarkerFlag = false;
			Locale backupLocale = null;
			synchronized( languageMappedLocales) {
				Set<Locale> localesSet = languageMappedLocales.get( language);
				for( Locale localeObj : localesSet) {
					if ( !inputLocaleMarkerFlag) {
						if ( locale.equals( localeObj)) {
							inputLocaleMarkerFlag = true;
							continue; // for
						}
						else {
							if ( localeObj.getVariant().length() < 1) continue; 
							if ( backupLocale == null) backupLocale = localeObj;
							continue; // for
						}
					}
					else {
						if ( localeObj.getVariant().length() < 1) continue; 
						languageMappedLocales.notifyAll();
						return localeObj;
					}
				} // for
				
				languageMappedLocales.notifyAll();
			} // synchronized( languageMappedLocales)
			
			if ( backupLocale != null) return backupLocale; 
			return new Locale( language);
		}
		else if ( ( country.length() > 0) && ( variant.length() > 0)) {
			if ( countryMappedLocales == null) {
				return new Locale( country);
			}
			boolean inputLocaleMarkerFlag = false;
			Locale backupLocale = null;
			synchronized( countryMappedLocales) {
				Set<Locale> localesSet = countryMappedLocales.get( country);
				for( Locale localeObj : localesSet) {
					if ( !inputLocaleMarkerFlag) {
						if ( locale.equals( localeObj)) {
							inputLocaleMarkerFlag = true;
							continue; // for
						}
						else {
							if ( localeObj.getVariant().length() < 1) continue; 
							if ( backupLocale == null) backupLocale = localeObj;
							continue; // for
						}
					}
					else {
						if ( localeObj.getVariant().length() < 1) continue; 
						countryMappedLocales.notifyAll();
						return localeObj;
					}
				} // for
				
				countryMappedLocales.notifyAll();
			} // synchronized( countryMappedLocales)
			
			if ( backupLocale != null) return backupLocale; 
			return new Locale( country);
		}
		else {
			if ( !nullUltimateFallbackLocaleAllowed.get()) {
				synchronized( fallbackLocalesList) {
					if ( ( fallbackLocalesList != null) && ( fallbackLocalesList.size() > 0)) {
						Locale localeObj = fallbackLocalesList.get( fallbackLocalesList.size() - 1);
						fallbackLocalesList.notifyAll();
						return localeObj;
					}
					
					fallbackLocalesList.notifyAll();
				} // synchronized( fallbackLocalesList)
				
				return locale;
			}
			else return null;
		}
	} // protected Locale getNextFallbackLocale( Locale locale)
	// --------------------------------------------------------------------------------------------
	
	protected MessageDao messageDao;
		public final void setMessageDao( MessageDao messageDao) {
			this.messageDao = messageDao;
		}
	
	protected Map<Locale, String> getMessages( String code) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( code == null) || "".equals( code)) {
			throw new InvalidDataAccessApiUsageException(
					"MessageModel key value for retriving the message cannot be null.");
		}
		// ----------------------------------------------------------------------------------------
		
		Map<Locale, String> messagesMap = messageDao.getMessages( code);
		if ( messagesMap == null) return null;
		
		return messagesMap;
	} // protected Map<Locale, String> getMessages( String code)

	protected String getMessage( String code, Locale locale) {
		// Validate input arguments ---------------------------------------------------------------
		if ( ( code == null) || "".equals( code)) {
			throw new InvalidDataAccessApiUsageException(
					"MessageModel key value for retriving the message cannot be null.");
		}
		if ( ( locale == null) && !nullUltimateFallbackLocaleAllowed.get()) {
			throw new InvalidDataAccessApiUsageException(
					"Null is not allowed as the locale argument value when the " 
					+ "the nullUltimateFallbackLocaleAllowed property value is false.");
		}
		// ----------------------------------------------------------------------------------------
		
		String message;
		Map<Locale, String> messagesMap = messageDao.getMessages( code);
		Locale localeObj = locale;
		Locale localeCopy = null;
		while( true) {
			message = messagesMap.get( localeObj);
			if ( message != null) {
				break; //while
			}
			
			if ( localeObj == null) {
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"No message was found for %1$s key down the line of locales from " 
									+ "the %2$s locale to null.", 
									code, 
									locale.toString()
									)
							);
				}
				break; //do
			}
			localeCopy = localeObj;
			localeObj = getNextFallbackLocale( localeObj);
			if ( localeObj.equals( localeCopy)) {
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"No message was found for %1$s down the line of locales from " 
									+ "the %2$s locale to the %3$s locale.", 
									code, 
									locale.toString(), 
									localeObj.toString()
									)
							);
				}
				break;
			}
		} // while
//TODO Shall I return code when message == null even at this point instead of returning null?
		return message;
	} // protected String getMessage( String code, Locale locale)
	
	@Override
	protected MessageFormat resolveCode( String code, Locale locale) {
//TODO need to test the case that no message in database including 
		return createMessageFormat( getMessage( code, locale), locale);
	}

	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		return getMessage( code, locale);
	}
	
	// ResourceLoaderAware interface implementation -----------------------------------------------
	@Override
	public void setResourceLoader( ResourceLoader arg0) {
		// TODO Auto-generated method stub
	}
	// --------------------------------------------------------------------------------------------

	// InitializingBean interface implementation --------------------------------------------------
	@Override
	public void afterPropertiesSet() throws Exception {
		if ( messageDao == null) {
			throw new BeanInitializationException( "messageDao property value cannot be null.");
		}
	}
	// --------------------------------------------------------------------------------------------

}

//TODO set fallback language and fallback to it when message is not found  
//TODO study ReloadableResourceBundleMessageSource code to make this reloadable for when message in database is modified.
