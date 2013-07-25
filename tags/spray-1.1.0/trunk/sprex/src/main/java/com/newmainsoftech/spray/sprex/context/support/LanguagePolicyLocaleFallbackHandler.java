package com.newmainsoftech.spray.sprex.context.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Default out-of-box implementation of <code>{@link LocaleFallbackHandlerCase}</code> interface. 
 * 
 * @author Arata Y.
 */
public class LanguagePolicyLocaleFallbackHandler implements LocaleFallbackHandlerCase {
	private Locale ultimateLocale;
		/**
		 * Thread safe.
		 */
		@Override
		public synchronized Locale getUltimateLocale() {
			if ( ultimateLocale == null) return null;
			return (Locale)(ultimateLocale.clone());
		}
		/**
		 * Set ultimate fall-back locale. <br />
		 * Not thread safe only regarding <code>ultimateLocale</code> input.
		 * 
		 * @param ultimateLocale <code>{@link Locale}</code> object being set as ultimate 
		 * fall-back locale.
		 */
		@Override
		public synchronized void setUltimateLocale( final Locale ultimateLocale) {
			if ( ultimateLocale == null) {
				this.ultimateLocale = null;
			}
			else {
				this.ultimateLocale = (Locale)(ultimateLocale.clone());
			}
		}
	public LanguagePolicyLocaleFallbackHandler( final Locale ultimateLocale) {
		setUltimateLocale( ultimateLocale);
	}
		
	/**
	 * Check whether <code>locale</code> input value is the ultimate fall-back locale having set 
	 * by <code>{@link #setUltimateLocale(Locale)}</code> method.<br />
	 * Not thread safe only regarding <code>locale</code> input.
	 * 
	 * @return boolean value to identify whether <code>locale</code> input value is the ultimate 
	 * fall-back locale having set with <code>{@link #setUltimateLocale(Locale)}</code> method.
	 */
	@Override
	public boolean isUltimateLocale( final Locale locale) {
		if ( locale == null) {
			if (getUltimateLocale() == null) return true;
			return false;
		}
		else {
			Locale localeCopy = (Locale)locale.clone();
			return localeCopy.equals( getUltimateLocale());
		}
	}

	private Map<LinkedList<String>, Locale> languageLocalesMap;
		private Map<LinkedList<String>, Locale> getLanguageLocalesMap() {
			return languageLocalesMap;
		}
		private synchronized void setLanguageLocalesMap( 
				final Map<LinkedList<String>, Locale> languageLocalesMap) {
			this.languageLocalesMap = languageLocalesMap;
		}
		private LinkedList<String> constructLocaleKey( 
				final String language, final String country, final String script, final String variant) {
			LinkedList<String> localeKey = new LinkedList<String>(); 
				localeKey.add( language);
				localeKey.add( country);
				localeKey.add( script);
				localeKey.add( variant);
			return localeKey;
		}
	private List<Locale> fallbackLocalesList;
		/**
		 * Return copy of fall-back locale list having set by 
		 * <code>{@link #setFallbackLocalesList(List)}</code> method.
		 * Thread safe.
		 * 
		 * @return Copy of <code>{@link List}</code> of fall-back <code>{@link Locale}</code> objects
		 */
		@Override
		public synchronized List<Locale> getFallbackLocalesList() {
			if ( fallbackLocalesList == null) {
				return new ArrayList<Locale>();
			}
			return new ArrayList<Locale>( fallbackLocalesList);
		}
		/**
		 * Set <code>{@link List}</code> of fall-back locale. <br />
		 * The order of locales in the <code>fallbackLocalesList</code> input is not matter; 
		 * regarding the fall-back order among locales, refer to 
		 * <code>{@link #getNextFallbackLocale(Locale)}</code> method JavaDoc.<br />
		 * Though, for thread safety (to avoid concurrent modification), <code>fallbackLocalesList</code> 
		 * input value will be cloned inside of this method, still not thread safe only regarding 
		 * <code>fallbackLocalesList</code> input until completion of cloning part.
		 * 
		 * @param fallbackLocalesList <code>{@link List}</code> of fall-back locale.
		 */
		@Override
		public synchronized void setFallbackLocalesList( final List<Locale> fallbackLocalesList) {
			if ( fallbackLocalesList == null) {
				throw new IllegalArgumentException( 
						"Value of fallbackLocalesList input cannot be null.");
			}
			
			this.fallbackLocalesList 
			= Collections.synchronizedList( new ArrayList<Locale>( fallbackLocalesList));
			
			Map<LinkedList<String>, Locale> lagnuageLocalesMap 
			= new HashMap<LinkedList<String>, Locale>();
				for( Locale locale : fallbackLocalesList) {
					LinkedList<String> localeKey 
					= constructLocaleKey( 
							locale.getLanguage(), 
							locale.getCountry(), 
							locale.getScript(), 
							locale.getVariant());
					lagnuageLocalesMap.put( localeKey, locale);
				}
			setLanguageLocalesMap( lagnuageLocalesMap);
		}
	
	/**
	 * For <code>locale</code> input, return fall-back locale from fall-back locale list having 
	 * set by <code>{@link #setFallbackLocalesList(List)}</code> method.<br />
	 * The logic of selecting fall-back locale being used:
	 * <ol>
	 *  <li>Locale matching language, country, script.</li>
	 *  <li>Locale matching language, country.</li>
	 *  <li>Language locale for same language.</li>
	 * </ol>
	 * Not thread safe only regarding locale input.
	 * 
	 * @param locale {@link Locale} to find next locale in the fall-back locale ladder.
	 * @return Next fall-back locale (or ultimate fall-back locale).
	 */
	@Override
	public synchronized Locale getNextFallbackLocale( final Locale locale) {
		if ( locale == null) {
			throw new IllegalArgumentException( "Value of locale input cannot be null");
		}
		Locale localeCopy = (Locale)locale.clone();
		if ( isUltimateLocale( localeCopy)) return localeCopy;
		
		Locale fallbackLocale = null;
			int count = 0;
			String language = localeCopy.getLanguage();
			String country = localeCopy.getCountry();
			String script = localeCopy.getScript();
			while( count < 3) {
				LinkedList<String> localeKey = null;
				switch( count) {
				case 0:
					localeKey = constructLocaleKey( language, country, script, "");
					break;
				case 1:
					localeKey = constructLocaleKey( language, country, "", "");
					break;
				case 2:
					localeKey = constructLocaleKey( language, "", "", "");
					break;
				} // switch
				fallbackLocale = getLanguageLocalesMap().get( localeKey);
				if ( (fallbackLocale != null) && (!locale.equals( fallbackLocale))) {
					return fallbackLocale;
				}
				count++;
			} // while
			
		return getUltimateLocale();
	}
	
}