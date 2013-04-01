package com.newmainsoftech.spray.sprex.context.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguagePolicyLocaleFallbackHandlerTest {
	Logger logger = LoggerFactory.getLogger( this.getClass());
		Logger getLogger() {
			return logger;
		}
	/**
	 * Test whether <code>{@link Locale}</code>, of what getter method for member field returns null, 
	 * exists. <br />
	 * If such <code>{@link Locale}</code> object can exist, then implemented logic inside of  
	 * <code>{@link LanguagePolicyLocaleFallbackHandler#isUltimateLocale(Locale)}</code> method needs to be  
	 * modified.
	 * 
	 * @throws Throwable
	 */
	@Test
	public void test_Locale_getters() throws Throwable {
		List<Locale> localesList = new ArrayList<Locale>();
			localesList.addAll( Arrays.asList( Locale.getAvailableLocales()));
		Iterator<Locale> localesIterator = localesList.listIterator();
		while( localesIterator.hasNext()) {
			Locale locale = localesIterator.next();
			Assert.assertNotNull(
					String.format(
							"%1$s of %2$s locale is null.",
							"Language",
							locale.toString()), 
					locale.getLanguage());
			Assert.assertNotNull(
					String.format(
							"%1$s of %2$s locale is null.",
							"Country",
							locale.toString()), 
					locale.getCountry());
			Assert.assertNotNull(
					String.format(
							"%1$s of %2$s locale is null.",
							"Script",
							locale.toString()), 
					locale.getScript());
			Assert.assertNotNull(
					String.format(
							"%1$s of %2$s locale is null.",
							"Variant",
							locale.toString()), 
					locale.getVariant());
		} // while
	}

	TreeMap<String, Locale> getCountryTreeLocalesMap() {
		TreeMap<String, Locale> treeMap = new TreeMap<>();
			String language;
			String country;
			String script;
			String variant;
			for( Locale locale : Locale.getAvailableLocales()) {
				language = locale.getLanguage();
				country = locale.getCountry();
				script = locale.getScript();
				variant = locale.getVariant();
				treeMap.put( 
						country
							.concat( ",").concat( language)
							.concat( ",").concat( script)
							.concat( ",").concat( variant), 
						locale);
			} // for
		return treeMap;
	}
	
	/**
	 * @return {@link Map} for countries what have been used with multiple locales. <br />
	 * The key is country string and the value will be the list of locales. The returned map 
	 * will not contain country what has been used only with single locale.  
	 */
	Map<String, List<Locale>> getCountryWithMultiLocales() {
		Map<String, List<Locale>> countryWithMultiLocalesMap = new HashMap<String, List<Locale>>(); 
			TreeMap<String, Locale> treeMap = getCountryTreeLocalesMap();
			String country = null;
			int countryCount = 0;
			ArrayList<String> keyList = new ArrayList<String>( treeMap.keySet());
			for( int index = 0; index < keyList.size(); index++) {
				String key = keyList.get( index);
				country = key.substring( 0, key.indexOf( ","));
					if ( country.length() < 1) continue;
				List<Locale> localeList = new ArrayList<Locale>();
					SortedMap<String, Locale> sortedMap 
					= treeMap.subMap( key, country.concat( "Z,,,"));
					for( Locale locale : sortedMap.values()) {
						localeList.add( locale);
					} // for
				if ( localeList.size() > 1) {
					countryWithMultiLocalesMap.put( country, localeList);
					index = index + localeList.size() - 1;
					countryCount++;
				}
			} // for
			Assert.assertTrue(
					"Test data error: Could not find any country with multiple locales.",
					(countryCount > 0)
					);
			
		return countryWithMultiLocalesMap;
	}
	
	TreeMap<String, Locale> getScriptTreeLocalesMap() {
		TreeMap<String, Locale> treeMap = new TreeMap<>();
			String language;
			String country;
			String script;
			String variant;
			for( Locale locale : Locale.getAvailableLocales()) {
				language = locale.getLanguage();
				country = locale.getCountry();
				script = locale.getScript();
				variant = locale.getVariant();
				treeMap.put( 
						script
							.concat( ",").concat( language)
							.concat( ",").concat( country)
							.concat( ",").concat( variant), 
						locale);
			} // for
		return treeMap;
	}
	/**
	 * @return {@link Map} for scripts what have been used with multiple locales. <br />
	 * ex. [sr_ME_#Latn, sr_BA_#Latn, sr__#Latn, sr_RS_#Latn]<br />
	 * The key is script string and the value will be the list of locales. The returned map 
	 * will not contain script what has been used only with single locale. 
	 */
	public Map<String, List<Locale>> getScriptWithMultiLocales() {
		Map<String, List<Locale>> scriptWithMultiLocalesMap = new HashMap<String, List<Locale>>(); 
			TreeMap<String, Locale> treeMap = getScriptTreeLocalesMap();
			String script = null;
			int scriptCount = 0;
			ArrayList<String> keyList = new ArrayList<String>( treeMap.keySet());
			for( int index = 0; index < keyList.size(); index++) {
				String key = keyList.get( index);
				script = key.substring( 0, key.indexOf( ","));
					if ( script.length() < 1) continue;
				List<Locale> localeList = new ArrayList<Locale>();
					SortedMap<String, Locale> sortedMap 
					= treeMap.subMap( key, script.concat( "Z,,,"));
					for( Locale locale : sortedMap.values()) {
						localeList.add( locale);
					} // for
				if ( localeList.size() > 1) {
					scriptWithMultiLocalesMap.put( script, localeList);
					index = index + localeList.size() - 1;
					scriptCount++;
				}
			} // for
			Assert.assertTrue(
					"Test data error: Could not find any script with multiple locales.",
					(scriptCount > 0)
					);
			
		return scriptWithMultiLocalesMap;
	}
	
	TreeMap<String, Locale> getLanguageTreeLocalesMap() {
		TreeMap<String, Locale> treeMap = new TreeMap<>();
			String language;
			String country;
			String script;
			String variant;
			for( Locale locale : Locale.getAvailableLocales()) {
				language = locale.getLanguage();
				country = locale.getCountry();
				script = locale.getScript();
				variant = locale.getVariant();
				treeMap.put( 
						language
							.concat( ",").concat( country)
							.concat( ",").concat( script)
							.concat( ",").concat( variant), 
						locale);
			} // for
		return treeMap;
	}
	
	/**
	 * @return {@link Map} for languages what have been used with multiple locales. <br />
	 * The key is language string and the value will be the list of locales. The returned map 
	 * will not contain language what has been used only with language-only locale.  
	 */
	public Map<String, List<Locale>> getLanguageWithMultiLocales() {
		Map<String, List<Locale>> languageWithMultiLocalesMap = new HashMap<String, List<Locale>>(); 
			TreeMap<String, Locale> treeMap = getLanguageTreeLocalesMap();
			String language = null;
			
			ArrayList<String> keyList = new ArrayList<String>( treeMap.keySet());
			int languageCount = 0;
			for( int index = 0; index < keyList.size(); index++) {
				String key = keyList.get( index);
					if (key.substring( key.indexOf( ",")).length() > 3) {
						continue; // for
							/* There seems to be language with what language-only locale is not found 
							 * available among system-provided locales. For an example, hr_IN. 
							 */
					}
				language = key.substring( 0, key.indexOf( ","));
					Assert.assertTrue( 
							String.format(
									"Unexpected to encounter key holding no language data: \"%1$s\"", 
									key),
							( language.length() > 0));
				List<Locale> localeList = new ArrayList<Locale>();
					SortedMap<String, Locale> sortedMap 
					= treeMap.subMap( key, language.concat( "Z,,,"));
					for( Locale locale : sortedMap.values()) {
						localeList.add( locale);
					} // for
				if ( localeList.size() > 1) {
					languageWithMultiLocalesMap.put( language, localeList);
					index = index + localeList.size() - 1;
					languageCount++;
				}
			} // for
			Assert.assertTrue(
					"Test data error: Could not find any language with multiple locales.",
					(languageCount > 0)
					);
			
		return languageWithMultiLocalesMap;
	}
	
	/**
	 * @return {@link Map} for variant of locales. <br />
	 * The key is variant string and the value will be the list of locales.   
	 */
	public Map<String, List<Locale>> getVariantLocales() {
		List<Locale> localesList = new ArrayList<Locale>();
			localesList.addAll( Arrays.asList( Locale.getAvailableLocales()));
			
		Map<String, List<Locale>> variantLocalesMap = new HashMap<String, List<Locale>>(); 
			for( Locale locale : localesList) {
				String variant = locale.getVariant();
					if ( "".equals( variant)) continue; //for
				List<Locale> localeList = variantLocalesMap.get( variant);
					if ( localeList == null) {
						localeList = new ArrayList<Locale>();
					}
					localeList.add( locale);
					variantLocalesMap.put( variant, localeList);
			} // for
			
		return variantLocalesMap;
	}
	
	@Test
	public void test_setUltimateLocale() {
		Map<String, List<Locale>> localesMap = getLanguageWithMultiLocales();
			Iterator<List<Locale>> localeIterator = localesMap.values().iterator();
			List<Locale> localeList = localeIterator.next();
			Locale testLocale = localeList.get( 0);
				LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler 
				= new LanguagePolicyLocaleFallbackHandler( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
				if ( localeIterator.hasNext()) {
					localeList = localeIterator.next();
					testLocale = localeList.get( 0);
				}
				else {
					Assert.assertTrue( 
							String.format(
									"Sufficient test data: only one locale (%1$s) available in system.",
									testLocale.toString()), 
							( localeList.size() > 1));
					testLocale = localeList.get( 1);
				}
				languagePolicyLocaleFallbackHandler.setUltimateLocale( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
		localesMap = getCountryWithMultiLocales();
			testLocale = localesMap.values().iterator().next().get( 0);
				languagePolicyLocaleFallbackHandler 
				= new LanguagePolicyLocaleFallbackHandler( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
				if ( localeIterator.hasNext()) {
					localeList = localeIterator.next();
					testLocale = localeList.get( 0);
				}
				else {
					Assert.assertTrue( 
							String.format(
									"Sufficient test data: only one locale with country (%1$s) " 
									+ "available in system.",
									testLocale.toString()), 
							( localeList.size() > 1));
					testLocale = localeList.get( 1);
				}
				languagePolicyLocaleFallbackHandler.setUltimateLocale( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
		localesMap = getScriptWithMultiLocales();
			testLocale = localesMap.values().iterator().next().get( 0);
				languagePolicyLocaleFallbackHandler 
				= new LanguagePolicyLocaleFallbackHandler( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
				if ( localeIterator.hasNext()) {
					localeList = localeIterator.next();
					testLocale = localeList.get( 0);
				}
				else {
					Assert.assertTrue( 
							String.format(
									"Sufficient test data: only one locale with script (%1$s) " 
									+ "available in system.",
									testLocale.toString()), 
							( localeList.size() > 1));
					testLocale = localeList.get( 1);
				}
				languagePolicyLocaleFallbackHandler.setUltimateLocale( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
		localesMap = getVariantLocales();
			testLocale = localesMap.values().iterator().next().get( 0);
				languagePolicyLocaleFallbackHandler 
				= new LanguagePolicyLocaleFallbackHandler( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
				if ( localeIterator.hasNext()) {
					localeList = localeIterator.next();
					testLocale = localeList.get( 0);
				}
				else {
					Assert.assertTrue( 
							String.format(
									"Sufficient test data: only one locale with variant (%1$s) " 
									+ "available in system.",
									testLocale.toString()), 
							( localeList.size() > 1));
					testLocale = localeList.get( 1);
				}
				languagePolicyLocaleFallbackHandler.setUltimateLocale( testLocale);
				Assert.assertEquals( 
						testLocale, 
						languagePolicyLocaleFallbackHandler.getUltimateLocale());
	}
	
	
	
	@Test
	public void test_isUltimateLocale() {
		List<Locale> availableLocalesList = new ArrayList<Locale>();
			availableLocalesList.addAll( Arrays.asList( Locale.getAvailableLocales()));
		
		// Test for language-only locale with fall-back locale list with 
		// other locales with same language, country, script and/or variant	
		{
			Map<String, List<Locale>> languageWithMultiLocalesMap = getLanguageWithMultiLocales();
			int languageLocaleCounter = 0;
			int sameLanguageLocaleCounter = 0;
			for( String language : languageWithMultiLocalesMap.keySet()) {
				List<Locale> localeList = languageWithMultiLocalesMap.get( language);
				Locale languageOnlyLocale = null;
					for( Locale locale : localeList) {
						if ( "".equals( locale.getCountry())
								&& "".equals( locale.getScript())
								&& "".equals( locale.getVariant())) {
							languageOnlyLocale = locale;
						}
					} // for
					if ( languageOnlyLocale != null) {
						
						LanguagePolicyLocaleFallbackHandler languagePolicyLocaleComparator 
						= new LanguagePolicyLocaleFallbackHandler( languageOnlyLocale);
						
						for( Locale locale : localeList) {
							if ( languageOnlyLocale.equals( locale)) {
								++languageLocaleCounter;
								Assert.assertTrue( 
										String.format( 
												"isUltimateLocale method could not identified %1$s " 
												+ "as ultimate fall-back locale.",
												locale), 
										languagePolicyLocaleComparator.isUltimateLocale( locale));
							}
							else {
								++sameLanguageLocaleCounter;
								Assert.assertFalse( 
										String.format( 
												"isUltimateLocale method misidentified %1$s as " 
												+ "ultimate fall-back locale instead of %2$s.",
												locale, 
												languageOnlyLocale), 
										languagePolicyLocaleComparator.isUltimateLocale( locale));
							}
						} // for
					}
			} // for
			
			Assert.assertTrue( 
					"Insufficient test data: no language-only locale has been detected and " 
					+ "tested with.", 
					( languageLocaleCounter > 0));
			Assert.assertTrue( 
					"Insufficient test data: test used only language-only locale(s) but other locales " 
					+ "as input to identify the language-only locale as ultimage fall-back locale.", 
					( sameLanguageLocaleCounter > 0));
		}
		
		{
			Map<String, List<Locale>> countryLocalesMap = getCountryWithMultiLocales();
			Set<Entry<String, List<Locale>>> entrySet = countryLocalesMap.entrySet();
			int countryLocaleCounter = 0;
			int testLocaleCounter = 0;
			for( Entry<String, List<Locale>> entry : entrySet) {
				for( Locale countryLocale : entry.getValue()) {
					LanguagePolicyLocaleFallbackHandler languagePolicyLocaleComparator 
					= new LanguagePolicyLocaleFallbackHandler( countryLocale);
						Assert.assertTrue(
								String.format( 
										"isUltimateLocale method could not identified %1$s " 
										+ "as ultimate fall-back locale.",
										countryLocale), 
								languagePolicyLocaleComparator.isUltimateLocale( countryLocale)
								);
						countryLocaleCounter++;
						
						for( Locale locale : availableLocalesList) {
							if ( !countryLocale.equals( locale)) {
								Assert.assertFalse(
										String.format( 
												"isUltimateLocale method misidentified %1$s as " 
												+ "ultimate fall-back locale instead of %2$s.",
												locale,
												countryLocale), 
										languagePolicyLocaleComparator.isUltimateLocale( locale)
										);
								++testLocaleCounter;
							}
						} // for
				} // for
			} // for
			
			Assert.assertTrue( 
					"Insufficient test data: no locale with country property has been detected and " 
					+ "tested with.", 
					( countryLocaleCounter > 0));
			Assert.assertTrue( 
					"Insufficient test data: test used only locale(s) with country property but other " 
					+ "locales as input to identify the locale with country property as ultimage " 
					+ "fall-back locale.", 
					( testLocaleCounter > 0));
		}
		
		// Test for locale, what has language, country, script and variant, with fall-back locale 
		// list with other locales with same language, same country, same script or same variant
		{
			Map<String, List<Locale>> variantLocalesMap = getVariantLocales();
			Set<Entry<String, List<Locale>>> entrySet = variantLocalesMap.entrySet();
			Iterator<Entry<String, List<Locale>>> entryIterator = entrySet.iterator();
			int variantLocaleCounter = 0;
			int testLocaleCounter = 0;
			while( entryIterator.hasNext()) {
				Entry<String, List<Locale>> entry = entryIterator.next();
				List<Locale> variantLocaleList = entry.getValue();
				for( Locale variantLocale : variantLocaleList) {
					LanguagePolicyLocaleFallbackHandler languagePolicyLocaleComparator 
					= new LanguagePolicyLocaleFallbackHandler( variantLocale);
						Assert.assertTrue(
								String.format( 
										"isUltimateLocale method could not identified %1$s " 
										+ "as ultimate fall-back locale.",
										variantLocale), 
								languagePolicyLocaleComparator.isUltimateLocale( variantLocale)
								);
						++variantLocaleCounter;
					for( Locale locale : availableLocalesList) {
						if ( !variantLocale.equals( locale)) {
							Assert.assertFalse(
									String.format( 
											"isUltimateLocale method misidentified %1$s as " 
											+ "ultimate fall-back locale instead of %2$s.",
											locale,
											variantLocale), 
									languagePolicyLocaleComparator.isUltimateLocale( locale)
									);
							++testLocaleCounter;
						}
					} // for
				} // for
			} // while
					
			Assert.assertTrue( 
					"Insufficient test data: no locale with variant property has been detected and " 
					+ "tested with.", 
					( variantLocaleCounter > 0));
			Assert.assertTrue( 
					"Insufficient test data: test used only locale(s) with variant property but other " 
					+ "locales as input to identify the locale with variant property as ultimage " 
					+ "fall-back locale.", 
					( testLocaleCounter > 0));
		}
			
		// Test for locale, what has language, country, script, with fall-back locale list 
		// with other locales with same language, same country, same script or same variant
		{
			Map<String, List<Locale>> scriptWithMultiLocalesMap = getScriptWithMultiLocales();
			Set<Entry<String, List<Locale>>> entrySet = scriptWithMultiLocalesMap.entrySet();
			int scriptLocaleCounter = 0;
			int testLocaleCounter = 0;
			for( Entry<String, List<Locale>> entry : entrySet) {
				for( Locale scriptLocale : entry.getValue()) {
					LanguagePolicyLocaleFallbackHandler languagePolicyLocaleComparator 
					= new LanguagePolicyLocaleFallbackHandler( scriptLocale);
						Assert.assertTrue(
								String.format( 
										"isUltimateLocale method could not identified %1$s " 
										+ "as ultimate fall-back locale.",
										scriptLocale), 
								languagePolicyLocaleComparator.isUltimateLocale( scriptLocale)
								);
						scriptLocaleCounter++;
						
						for( Locale locale : availableLocalesList) {
							if ( !scriptLocale.equals( locale)) {
								Assert.assertFalse(
										String.format( 
												"isUltimateLocale method misidentified %1$s as " 
												+ "ultimate fall-back locale instead of %2$s.",
												locale,
												scriptLocale), 
										languagePolicyLocaleComparator.isUltimateLocale( locale)
										);
								++testLocaleCounter;
							}
						} // for
				} // for
			} // for
			
			Assert.assertTrue( 
					"Insufficient test data: no locale with script property has been detected and " 
					+ "tested with.", 
					( scriptLocaleCounter > 0));
			Assert.assertTrue( 
					"Insufficient test data: test used only locale(s) with script property but other " 
					+ "locales as input to identify the locale with script property as ultimage " 
					+ "fall-back locale.", 
					( testLocaleCounter > 0));
		}
	}

	static class LocaleComparator implements Comparator<Locale> {
		@Override
		public int compare( final Locale locale1, final Locale locale2) {
			if ( ( locale1 == null) && ( locale2 == null)) return 0;
			if ( locale1 == null) return -1;
			if ( locale2 == null) return 1;
			
			if ( locale1.equals( locale2)) return 0;
			String language1 = locale1.getLanguage();
			String language2 = locale2.getLanguage();
				if ( !language1.equals( language2)) {
					return language1.compareTo( language2);
				}
			String country1 = locale1.getCountry();
			String country2 = locale2.getCountry();
				if ( !country1.equals( country2)) {
					return country1.compareTo( country2);
				}
			String script1 = locale1.getScript();
			String script2 = locale2.getScript();
				if ( !script1.equals( script2)) {
					return script1.compareTo( script2);
				}
			String variant1 = locale1.getVariant();
			String variant2 = locale2.getVariant();
				Assert.assertNotEquals( 
						String.format(
								"Not supported test scenario: compare method of %1$s needs to be updated " 
								+ "to handle custom locale (either %2$s or %3$s) what has other extra " 
								+ "property than language, country, script and variant.",
								this.getClass().getSimpleName(),
								locale1.toString(),
								locale2.toString()), 
						0, 
						variant1.compareTo( variant2));
				return variant1.compareTo( variant2);
		}
	}
	
	/**
	 * Test case of providing locale, of what language won't be found in fall-back locale list, to 
	 * <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.<br />
	 */
	@Test
	public void test_getNextFallbackLocale_for_no_same_language_locale_in_locale_list_case() {
		Locale ultimateFallbackLocale = null;
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler 
		= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
		
		TreeMap<String, Locale> treeMap = getLanguageTreeLocalesMap();
		String[] keyArray = treeMap.keySet().toArray( new String[]{});
			Assert.assertTrue(
					String.format(
							"Test data error: not enough available languages found " 
							+ "among system provided locales: %1$s.",
							treeMap.values().toString()),
					(keyArray.length > 1));
		String lastLanguage = "";
		int testLanguageCount = 0;
		for( int index = 0; index < (keyArray.length - 1); index++) {
			String key = keyArray[ index];
			String language = key.substring( 0, key.indexOf( ","));
				Assert.assertNotEquals(
						String.format(
								"Test data error: failed to pick up 2 different languages out of " 
								+ "available system provided locales; one for construct locale list and " 
								+ "other language locale being fed to getNextFallbackLocale method of " 
								+ "%1$s. It occured with %2$s key (at %3$d data out of %4$s).",
								languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
								key, 
								index, 
								Arrays.toString( keyArray)),
						lastLanguage, language);
			SortedMap<String, Locale> sortedMap 
			= treeMap.subMap( language.concat( ",,,"), language.concat( "Z,,,"));
			List<Locale> localeList = new ArrayList<Locale>( sortedMap.values());
				if ( ( index + localeList.size()) ==  keyArray.length) {
					break; // for
				}
			languagePolicyLocaleFallbackHandler.setFallbackLocalesList( localeList);
			Assert.assertEquals(
					ultimateFallbackLocale,
					languagePolicyLocaleFallbackHandler
					.getNextFallbackLocale( 
							treeMap.get( keyArray[ index + localeList.size()])
							)
					);
			index = index + localeList.size() - 1;
			testLanguageCount++;
		} // for
		Assert.assertTrue(
				String.format(
						"Test data error: practically no test has been performed: failed to construct " 
						+ "locale list for one language and pick a (language-only) locale for other " 
						+ "language out of available system provided locales (what is %1$s).",
						treeMap.values().toString()),
				(testLanguageCount > 0));
		Logger logger = getLogger();
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					String.format(
							"Tested locales of %1$d different languages.",
							testLanguageCount)
					);
		}
	}
	
	boolean fed_ultimate_fallback_locale( final List<Locale>[] localeListArray) {
		Map<String, List<Locale>> languageLocalesMap = getLanguageWithMultiLocales();
		
		Locale ultimateFallbackLocale;
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler;
		boolean isTested = false;
			for( int index = 0; index < localeListArray.length; index++) {
				ultimateFallbackLocale = localeListArray[ index].get( 0);
				List<Locale> localeList = languageLocalesMap.get( ultimateFallbackLocale.getLanguage());
				if ( localeList == null) continue;
				languagePolicyLocaleFallbackHandler 
				= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
				languagePolicyLocaleFallbackHandler.setFallbackLocalesList( localeList);
				Assert.assertEquals(
						ultimateFallbackLocale, 
						languagePolicyLocaleFallbackHandler
						.getNextFallbackLocale( ultimateFallbackLocale));
				isTested = true;
				break; // for
			} // for
		return isTested;
	}
	
	/**
	 * Test case of providing ultimate fall-back locale to 
	 * <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.<br />
	 */
	@Test
	public void test_getNextFallbackLocale_being_fed_ultimate_fallback_locale() {
		{
			Map<String, List<Locale>> variantLocalesMap = getVariantLocales();
			List<Locale>[] listArray 
			= (List<Locale>[])(variantLocalesMap.values().toArray( new ArrayList[]{}));
			boolean isTested = fed_ultimate_fallback_locale( listArray);
				Assert.assertTrue(
						String.format(
								"Test data error: could not find any locale with %1$s additionally " 
								+ "other locales with same language among system provided locales. " 
								+ "Available locales with %1$s among system provided locales: %2$s.",
								"variant",
								Arrays.toString( listArray)),
						isTested);
		}
		{
			Map<String, List<Locale>> scriptLocalesMap = getScriptWithMultiLocales();
			List<Locale>[] listArray 
			= (List<Locale>[])(scriptLocalesMap.values().toArray( new ArrayList[]{}));
			boolean isTested = fed_ultimate_fallback_locale( listArray);
				Assert.assertTrue(
						String.format(
								"Test data error: could not find any locale with %1$s additionally " 
								+ "other locales with same language among system provided locales. " 
								+ "Available locales with %1$s among system provided locales: %2$s.",
								"script",
								Arrays.toString( listArray)),
						isTested);
		}
		{
			ArrayList<List<Locale>> localeListList = new ArrayList<List<Locale>>();
				TreeMap<String, Locale> treeMap = getCountryTreeLocalesMap();
				List<String> keyList = new ArrayList<String>( treeMap.keySet());
				for( String key : keyList) {
					String country = key.substring( 0, key.indexOf( ","));
						if ( country.length() < 1) continue; // skip language-only locale
					if ( key.substring( key.indexOf( ",", (key.indexOf( ",") + 1))).length() > 2) continue;
						// key consisted only of country and language.
					ArrayList<Locale> localeList = new ArrayList<Locale>();
						localeList.add( treeMap.get( key));
					localeListList.add( localeList);
				} // for
			boolean isTested 
			= fed_ultimate_fallback_locale( (List<Locale>[])localeListList.toArray( new ArrayList[]{}));
			Assert.assertTrue(
					String.format(
							"Test data error: could not find any locale with %1$s additionally " 
							+ "other locales with same language among system provided locales. " 
							+ "Available locales with %1$s among system provided locales: %2$s.",
							"country",
							localeListList.toString()),
					isTested);
		}
		{
			ArrayList<List<Locale>> localeListList = new ArrayList<List<Locale>>();
				Map<String, List<Locale>> languageLocalesMap = getLanguageWithMultiLocales();
				TreeMap<String, Locale> treeMap = getLanguageTreeLocalesMap();
				for( String language : languageLocalesMap.keySet()) {
					Locale locale = treeMap.get( language.concat( ",,,"));
					ArrayList<Locale> localeList = new ArrayList<Locale>();
						localeList.add( locale);
					localeListList.add( localeList);
				}
			boolean isTested 
			= fed_ultimate_fallback_locale( (List<Locale>[])localeListList.toArray( new ArrayList[]{}));
			Assert.assertTrue(
					String.format(
							"Test data error: could not find any language-only locale additionally " 
							+ "other locales with same language among system provided locales. " 
							+ "Available language-only locales among system provided locales: %1$s.",
							localeListList.toString()),
					isTested);
		}
	}

	/**
	 * Test case of providing language-only locale but not identical to ultimate fall-back locale 
	 * to <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.<br />
	 */
	@Test
	public void test_getNextFallbackLocale_being_fed_language_only_locale() {
		Locale ultimateFallbackLocale = null;
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler 
		= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
			languagePolicyLocaleFallbackHandler
			.setFallbackLocalesList( Arrays.asList( Locale.getAvailableLocales()));
		
		TreeMap<String, Locale> treeMap = getLanguageTreeLocalesMap();
		int languageCount = 0;
		for( String key : treeMap.keySet()) {
			if ( key.substring( key.indexOf( ",")).length() > 3) continue; // for
				// key contains only language data
			Locale locale = treeMap.get( key);
			Assert.assertEquals(
					ultimateFallbackLocale, 
					languagePolicyLocaleFallbackHandler.getNextFallbackLocale( locale));
			languagePolicyLocaleFallbackHandler.setUltimateLocale( locale);
			ultimateFallbackLocale = locale;
			languageCount++;
		}
		Assert.assertTrue(
				String.format(
						"Test data error: no language-only locale was found among available system " 
						+ "provided locales: %1$s",
						treeMap.toString()),
				( languageCount > 0));
	}

	/**
	 * Test case of providing language+country locale but not identical to ultimate fall-back locale 
	 * to <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.<br />
	 * Regarding locale list configuration, it holds locales with same language except language-only 
	 * locale, and the order of locales in it is not fall-back order. <br />
	 * Test confirms whether ultimate fall-back locale is returned by 
	 * <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.
	 */
	@Test
	public void test_getNextFallbackLocale_being_fed_LanguageCountry_locale() {
		Logger logger = getLogger();
		
		Locale ultimateFallbackLocale = null;
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler 
		= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
		
		Map<String, List<Locale>> languageWithMultiLocalesMap = getLanguageWithMultiLocales();
		TreeMap<String, Locale> countryTreeLocalesMap = getCountryTreeLocalesMap();
		TreeMap<String, Locale> languageTreeLocalesMap = getLanguageTreeLocalesMap();
		int countryCount = 0;
		for( String key : countryTreeLocalesMap.keySet()) {
			if ( key.substring( key.indexOf( ",", (key.indexOf( ",") + 1))).length() > 2) continue; //for
			if ( key.substring( 0, key.indexOf( ",")).length() < 1) continue; // for
				// key consisted of country+language only
			Locale locale = countryTreeLocalesMap.get( key);
			String language = locale.getLanguage();
			List<Locale> localeList = languageWithMultiLocalesMap.get( language);
				if ( localeList == null) {
					if ( logger.isDebugEnabled()) {
						logger.debug(
								String.format(
										"Skipping to use %1$s for testing %2$s.getNextFallbackLocale method " 
										+ "because that is only one locale with that language.",
										locale.toString(),
										languagePolicyLocaleFallbackHandler.getClass().getSimpleName())
								);
					}
					continue; // for
				}
				Locale languageLocale = languageTreeLocalesMap.get( language.concat( ",,,"));
				localeList.remove( languageLocale);	// remove language-only locale out of localeList
				Collections.shuffle( localeList);	// Randomize order of locales in localeList
			languagePolicyLocaleFallbackHandler.setFallbackLocalesList( localeList);
			Assert.assertEquals( 
					String.format(
							"%1$s.getNextFallbackLocale method did not return expected fall-back locale " 
							+ "%2$s what has been set as ultimate fall-back locale. The given fall-back " 
							+ "locale list was %3$s.",
							languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
							((ultimateFallbackLocale == null) ? "null" : ultimateFallbackLocale.toString()),
							localeList.toString()),
					ultimateFallbackLocale,
					languagePolicyLocaleFallbackHandler.getNextFallbackLocale( locale));
			ultimateFallbackLocale = locale;
			languagePolicyLocaleFallbackHandler.setUltimateLocale( ultimateFallbackLocale);
			countryCount++;
		} // for
		Assert.assertTrue(
				String.format(
						"Test data error: no locale with country data was found among available system " 
						+ "provided locales: %1$s",
						countryTreeLocalesMap.toString()),
				( countryCount > 0));
	}
	
	/**
	 * Test case of providing language+country+script locale but not identical to ultimate fall-back locale 
	 * to <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.<br />
	 * Regarding locale list configuration, it holds only language+country+script+variant locale for same 
	 * language. <br />
	 * Test confirms whether ultimate fall-back locale is returned by 
	 * <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.
	 */
	@Test
	@Ignore // Need to create custom locales for test since there are not appropriate ones among system provided ones.
	// Syste provided locales with script: {Latn=[sr__#Latn, sr_BA_#Latn, sr_ME_#Latn, sr_RS_#Latn]}. 
	// Syste provided locales with variant: {JP=[ja_JP_JP_#u-ca-japanese], NY=[no_NO_NY], TH=[th_TH_TH_#u-nu-thai]}.
	public void test_getNextFallbackLocale_being_fed_LanguageCountryScript_locale() {
		Locale ultimateFallbackLocale = null;
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler;
		Map<String, List<Locale>> scriptLocalesMap = getScriptWithMultiLocales();
		TreeMap<String, Locale> languageTreeLocalesMap = getLanguageTreeLocalesMap();
		int scriptLocaleCount = 0;
		for( List<Locale> localeList : scriptLocalesMap.values()) {
			for( Locale scriptLocale : localeList) {
				SortedMap<String, Locale> sortedMap 
				= languageTreeLocalesMap.subMap( 
						scriptLocale.getLanguage()
						.concat( ",").concat( scriptLocale.getCountry()).concat( ",,"),
						scriptLocale.getLanguage()
						.concat( ",").concat( scriptLocale.getCountry()).concat( "Z,,")
						);
				List<Locale> variantLocaleList = new ArrayList<Locale>( sortedMap.values());
					ListIterator<Locale> listIterator = variantLocaleList.listIterator();
					while( listIterator.hasNext()) {
						Locale locale = listIterator.next();
						if ( "".equals( locale.getVariant())) {
							listIterator.remove();
						}
					} // while
					if ( variantLocaleList.size() < 1) continue; // for
				languagePolicyLocaleFallbackHandler 
				= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
				languagePolicyLocaleFallbackHandler.setFallbackLocalesList( variantLocaleList);
				Assert.assertEquals(
						String.format(
								"Unexpected fall-back locale returned by %1$s.getNextFallbackLocale " 
								+ "method for test giving language+country+script locale and locale " 
								+ "list holds only language+country+script+variant locale for same " 
								+ "language. %nlocale list used: %2$s",
								languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
								variantLocaleList.toString()),
						ultimateFallbackLocale,
						languagePolicyLocaleFallbackHandler.getNextFallbackLocale( scriptLocale));
				ultimateFallbackLocale = scriptLocale;
				languagePolicyLocaleFallbackHandler.setUltimateLocale( ultimateFallbackLocale);
				scriptLocaleCount++;
			} // for
		} // for
		Assert.assertTrue(
				String.format(
						"Test data error: no language, for what system provies locale with script and " 
						+ "locale with variant, was found. %nLocales with script: %1$s. %nLocales with " 
						+ "variant: %2$s.",
						scriptLocalesMap.toString(),
						getVariantLocales().toString()),
				( scriptLocaleCount > 0));
	}
	
	static enum LocaleType {
		LanguageOnly( "language only", 0), 
		LanguageCountry( "language+country", 1), 
		LanguageCountryScript( "language+country+script", 2), 
		LanguageCountryVariant( "language+country+variant", 3),
		LanguageCountryScriptVariant( "language+country+script+variant", 4);
		
		final String localeTypeStr;
			public String getLocaleTypeStr() {
				return localeTypeStr;
			}
		final int localeTypeIndex;
			public int getLocaleTypeIndex() {
				return localeTypeIndex;
			}

		private LocaleType( final String localeTypeStr, int localeTypeIndex) {
			this.localeTypeStr = localeTypeStr;
			this.localeTypeIndex = localeTypeIndex;
		}
	}
	
	void do_test_getNextFallbackLocale_by_giving_locales_but_LanguageCountryScript_locale(
			final LocaleType localeType, final Map<String, List<Locale>> localesMap) {
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler;
		TreeMap<String, Locale> languageTreeLocalesMap = getLanguageTreeLocalesMap();
		int languageOnlyLocaleCount = 0;
		int differentLanaugeLocaleCount = 0;
		Map<String, List<Locale>> variantLocalesMap = getVariantLocales();
		int variantLocaleCount = 0;
		for( List<Locale> localeList : localesMap.values()) {
			for( Locale ultimateFallbackLocale : localeList) {
				switch( localeType) {
				case LanguageOnly:
					if ( !"".equals( ultimateFallbackLocale.getCountry())
							|| !"".equals( ultimateFallbackLocale.getScript()) 
							|| !"".equals( ultimateFallbackLocale.getVariant())) {
						continue; // for
					}	// ultimateFallbackLocale consists of language only data
					break;
				case LanguageCountry:
					if ( !"".equals( ultimateFallbackLocale.getScript()) 
							|| !"".equals( ultimateFallbackLocale.getVariant())) {
						continue; // for
					}	// ultimateFallbackLocale consists of language+country data
					break;
				case LanguageCountryScript:
					if ( !"".equals( ultimateFallbackLocale.getVariant())) {
						continue; // for
					}	// ultimateFallbackLocale consists of language+country+script data
					break;
				case LanguageCountryVariant:
					if ( !"".equals( ultimateFallbackLocale.getScript())) {
						continue; // for
					}	// ultimateFallbackLocale consists of language+country+variant data
					break;
				case LanguageCountryScriptVariant:
					break;
				}
				
				languagePolicyLocaleFallbackHandler
				= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
					languagePolicyLocaleFallbackHandler
					.setFallbackLocalesList( Arrays.asList( Locale.getAvailableLocales()));
				
				String language = ultimateFallbackLocale.getLanguage();
				Locale languageLocale = languageTreeLocalesMap.get( language.concat( ",,,"));
				if ( languageLocale != null) {
					Assert.assertEquals(
							String.format(
									"Unexpected fall-back locale returned by %1$s.getNextFallbackLocale " 
									+ "method for test giving language-only locale (%2$s) when ultimage " 
									+ "fall-back locale has been set to %3$s for same language.",
									languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
									languageLocale.toString(), 
									ultimateFallbackLocale.toString()),
							ultimateFallbackLocale, 
							languagePolicyLocaleFallbackHandler.getNextFallbackLocale( languageLocale));
					languageOnlyLocaleCount++;
				}
				
				{
					SortedMap<String, Locale> sortedMap 
					= languageTreeLocalesMap.subMap( language.concat( ",,,"), language.concat( "Z,,,"));
					String lastKey = sortedMap.lastKey();
					String nextKey = languageTreeLocalesMap.higherKey( lastKey);
						if ( nextKey == null) {
							nextKey = languageTreeLocalesMap.firstKey();
						}
					languageLocale 
					= languageTreeLocalesMap.get( 
							nextKey.substring( 0, nextKey.indexOf( ",")).concat( ",,,"));
					if ( languageLocale != null) {
						Assert.assertEquals(
								String.format(
										"Unexpected fall-back locale returned by %1$s.getNextFallbackLocale " 
										+ "method for test giving language-only locale (%2$s) when ultimage " 
										+ "fall-back locale has been set to %3$s for different language.",
										languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
										languageLocale.toString(), 
										ultimateFallbackLocale.toString()),
								ultimateFallbackLocale, 
								languagePolicyLocaleFallbackHandler.getNextFallbackLocale( languageLocale));
						differentLanaugeLocaleCount++;
					}
				}
				
				{
					for( List<Locale> variantLocaleList : variantLocalesMap.values()) {
						for( Locale variantLocale : variantLocaleList) {
							if ( language.equals( variantLocale.getLanguage())) continue; // for
							
							String nextLanguage = language;
								boolean hasLoopedOnce = false;
								do {
									SortedMap<String, Locale> sortedMap 
									= languageTreeLocalesMap.subMap( 
											nextLanguage.concat( ",,,"), nextLanguage.concat( "Z,,,"));
									String lastKey = sortedMap.lastKey();
									String nextKey = languageTreeLocalesMap.higherKey( lastKey);
										if ( nextKey == null) {
											Assert.assertFalse( 
													String.format(
															"Test data error: could not find locales of " 
															+ "what language is neither %1$s nor %2$s among " 
															+ "system provided locales: %3$s.",
															language, 
															variantLocale.getLanguage(), 
															Arrays.toString( Locale.getAvailableLocales())),
													hasLoopedOnce);
											nextKey = languageTreeLocalesMap.firstKey();
											hasLoopedOnce = true;
										}
									nextLanguage = nextKey.substring( 0, nextKey.indexOf( ","));
								} while( 
										nextLanguage.equals( language) 
										|| nextLanguage.equals( variantLocale.getLanguage()));
							
							SortedMap<String, Locale> sortedMap 
							= languageTreeLocalesMap.subMap( 
									nextLanguage.concat( ",,,"), nextLanguage.concat( "Z,,,"));
							languagePolicyLocaleFallbackHandler
							.setFallbackLocalesList( 
									new ArrayList<Locale>( sortedMap.values()));
							Assert.assertEquals(
									String.format(
											"Unexpected fall-back locale returned by %1$s." 
											+ "getNextFallbackLocale method for test giving locale with " 
											+ "variant (%2$s) when ultimage fall-back locale has been set " 
											+ "to %3$s for different language , and fall-back locale list " 
											+ "does not hold any locale of same language with %2$s: %4$s",
											languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
											variantLocale.toString(), 
											ultimateFallbackLocale.toString(), 
											languagePolicyLocaleFallbackHandler
											.getFallbackLocalesList().toString()),
									ultimateFallbackLocale, 
									languagePolicyLocaleFallbackHandler
									.getNextFallbackLocale( variantLocale));
							variantLocaleCount++;
						} // for
					} // for
				}
			} // for
		} // for
		Assert.assertTrue(
				String.format(
						"Test data error: could not find %1$s locale and language-only locale " 
						+ "for same language among system-provided locales. Here are system provided " 
						+ "locales: %2$s.",
						localeType.getLocaleTypeStr(),
						Arrays.asList( Locale.getAvailableLocales())),
				( languageOnlyLocaleCount > 0));
		Assert.assertTrue(
				String.format(
						"Test data error: could not find %1$s locale for one language and " 
						+ "language-only locale for other language among system-provided locales. Here " 
						+ "are system provided locales: %2$s.",
						localeType.getLocaleTypeStr(),
						Arrays.asList( Locale.getAvailableLocales())),
				( differentLanaugeLocaleCount > 0));
		Assert.assertTrue(
				String.format(
						"Test data error: could not find %1$s locale for one language, " 
						+ "locale with variant for other language, and locale list for different language " 
						+ "from system-provided locales. Here are system provided locales: %2$s.",
						localeType.getLocaleTypeStr(),
						Arrays.asList( Locale.getAvailableLocales())),
				( variantLocaleCount > 0));
	}
	
	/**
	 * Test case for <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> 
	 * method when language+country locale has been set as ultimate fall-back locale. <br />
	 * Test confirms whether ultimate fall-back locale is returned by 
	 * <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> method.
	 */
	@Test
	public void test_getNextFallbackLocale_with_LanguageCountry_ultimate_fallback_locale() {
		do_test_getNextFallbackLocale_by_giving_locales_but_LanguageCountryScript_locale(
				LocaleType.LanguageCountry, getCountryWithMultiLocales());
	}
	
	@Test
	public void test_getNextFallbackLocale_with_LanguageCountryScript_ultimate_fallback_locale() {
		do_test_getNextFallbackLocale_by_giving_locales_but_LanguageCountryScript_locale(
				LocaleType.LanguageCountryScriptVariant, getScriptWithMultiLocales());
	}
	
	static enum LocaleListOrderType {
		Random, Reverse;
	}
	
	static enum LocaleListLanguage {
		SameLanguage, DifferetLanguage
	}
	
	void do_test_getNextFallbackLocale_by_feeding_locale( 
			final Map<String, List<Locale>> localesMap, 
			final LocaleListOrderType localeListOrderType, 
			final LocaleListLanguage localeListLanguage) {
		
		Logger logger = getLogger();
		Locale ultimateFallbackLocale = null;
		LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler
		= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
		
		TreeMap<String, Locale> languageTreeLocalesMap = getLanguageTreeLocalesMap();
		
		int testedLocaleCount = 0;
		for( List<Locale> localeList : localesMap.values()) {
			for( Locale localeInput : localeList) {
				String language = localeInput.getLanguage();
				int loopCount = 0;
				List<Locale> languageLocaleList = null;
				Locale fallbackLocale = null;
				int localeTypeCheck = 4;
				do {
					if ( LocaleListLanguage.DifferetLanguage.equals( localeListLanguage)) {
						String key 
						= languageTreeLocalesMap.higherKey( language.concat( "Z,,,"));
							if ( key == null) {
								key = languageTreeLocalesMap.firstKey();
								if ( ++loopCount > 1) {
									localeTypeCheck = 4;
									break; // do-while;
								}
							}
						language = key.substring( 0, key.indexOf( ","));
							if ( language.equals( localeInput.getLanguage())) {
								continue; // do-while
							}
					}
					SortedMap<String, Locale> sortedMap
					= languageTreeLocalesMap.subMap( language.concat( ",,,"), language.concat( "Z,,,"));
					languageLocaleList = new ArrayList<Locale>( sortedMap.values());
					ListIterator<Locale> localeListIterator = languageLocaleList.listIterator();
					LocaleType fallbackLocaleType = null;
					localeTypeCheck = 4;
					while( localeListIterator.hasNext()) {
						Locale locale = localeListIterator.next();
						if ( LocaleListLanguage.SameLanguage.equals( localeListLanguage) 
								&& ( !localeInput.getCountry().equals( locale.getCountry()))
								&& ( !"".equals( locale.getCountry()))) {
							continue; 
						}
							// localeInput object and locale object have same country and not language-only locale
						if ( !"".equals( locale.getVariant())) {
							if ( localeInput.getVariant().equals( locale.getVariant())) {
								if (( fallbackLocaleType == null) 
										|| (fallbackLocaleType.getLocaleTypeIndex() 
												< LocaleType.LanguageCountryScriptVariant.getLocaleTypeIndex())) {
									if ( !localeInput.equals( locale)) {
										fallbackLocaleType = LocaleType.LanguageCountryScriptVariant;
										fallbackLocale = locale;
									}
								}
								localeTypeCheck--;
									// Note: locale with same variant may not be available in languageLocaleList
							}
						}
						else if ( !"".equals( locale.getScript())) {
							if ( localeInput.getScript().equals( locale.getScript())) {
								if (( fallbackLocaleType == null) 
										|| (fallbackLocaleType.getLocaleTypeIndex() 
												< LocaleType.LanguageCountryScript.getLocaleTypeIndex())) {
									if ( !localeInput.equals( locale)) {
										fallbackLocaleType = LocaleType.LanguageCountryScript;
										fallbackLocale = locale;
									}
								}
								localeTypeCheck--;
									// Note: locale with same script may not be available in languageLocaleList
							}
						}
						else if ( !"".equals( locale.getCountry())) {
							if ( LocaleListLanguage.SameLanguage.equals( localeListLanguage)) {
								if ( localeInput.getCountry().equals( locale.getCountry())) {
									if (( fallbackLocaleType == null) 
											|| (fallbackLocaleType.getLocaleTypeIndex() < LocaleType.LanguageCountry.getLocaleTypeIndex())) {
										if ( !localeInput.equals( locale)) {
											fallbackLocaleType = LocaleType.LanguageCountry;
											fallbackLocale = locale;
										}
									}
									localeTypeCheck--;
								}
							}
							else {
								localeTypeCheck--;
							}
						}
						else if ( !"".equals( locale.getLanguage())) {
							if ( LocaleListLanguage.SameLanguage.equals( localeListLanguage)) {
								if ( localeInput.getLanguage().equals( locale.getLanguage())) {
									if (( fallbackLocaleType == null) 
											|| (fallbackLocaleType.getLocaleTypeIndex() < LocaleType.LanguageOnly.getLocaleTypeIndex())) {
										fallbackLocaleType = LocaleType.LanguageOnly;
										if ( !localeInput.equals( locale)) {
											fallbackLocale = locale;
										}
										else {
											fallbackLocale = ultimateFallbackLocale;
										}
									}
									localeTypeCheck--;
								}
							}
							else {
								localeTypeCheck--;
							}
						}
					} // while
				} while( 
						(localeTypeCheck > 1) 
						&& LocaleListLanguage.DifferetLanguage.equals( localeListLanguage));
				
				if ( localeTypeCheck > 1) {
					if ( logger.isDebugEnabled()) {
						switch( localeListLanguage) {
						case SameLanguage:
							logger.debug(
									String.format(
											"Skipping test with %1$s because could not find most likely either " 
											+ "language+country+variant or language+country+script locale of " 
											+ "same language among system provided locales: %2$s.",
											localeInput.toString(),
											languageLocaleList.toString())
									);
							break;
						case DifferetLanguage:
							logger.debug(
									String.format(
											"Skipping test with %1$s because could not find most likely either " 
											+ "language+country+variant or language+country+script locale of " 
											+ "different language among system provided locales: %2$s.",
											localeInput.toString(),
											languageLocaleList.toString())
									);
							break;
						} // switch
					}
					continue; //for
				}
				
				if ( LocaleListLanguage.DifferetLanguage.equals( localeListLanguage)) {
					fallbackLocale = ultimateFallbackLocale;
				}
				
				switch( localeListOrderType) {
				case Random:
					Collections.shuffle( languageLocaleList);
					break;
				case Reverse:
					Collections.reverse( languageLocaleList);
					break;
				} // switch
				languagePolicyLocaleFallbackHandler.setFallbackLocalesList( languageLocaleList);
				
				Assert.assertEquals(
						String.format(
								"Unexpected fall-back locale returned by %1$s." 
								+ "getNextFallbackLocale method for test giving %2$s as the argument " 
								+ "and the fall-back locale list holds locales of same language: %3$s",
								languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
								localeInput.toString(), 
								languagePolicyLocaleFallbackHandler
								.getFallbackLocalesList().toString()),
						fallbackLocale,
						languagePolicyLocaleFallbackHandler.getNextFallbackLocale( localeInput)
						);
				testedLocaleCount++;
			} // for
		} // for
		
		Assert.assertTrue( 
				String.format(
						"Test data error: could not find locale with language, language+country, " 
						+ "language+country+script, and/or language+country+variant for same language and " 
						+ "country among system-provided locales. Here are system provided locales: %1$s.",
						languageTreeLocalesMap.values().toString()),
				(testedLocaleCount > 0));
	}
	
	/**
	 * Test case for <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> 
	 * method when locale with variant is given as argument. 
	 */
	@Test
	public void test_getNextFallbackLocale_with_LanguageCountryScriptVariant_locale() {
		do_test_getNextFallbackLocale_by_feeding_locale( 
				getVariantLocales(), 
				LocaleListOrderType.Reverse, 
				LocaleListLanguage.SameLanguage);
	}
	
	/**
	 * Test case for <code>{@link LanguagePolicyLocaleFallbackHandler#getNextFallbackLocale(Locale)}</code> 
	 * method when locale with script is given as argument. 
	 */
	@Test
	public void test_getNextFallbackLocale_with_LanguageCountryScript_locale() {
		do_test_getNextFallbackLocale_by_feeding_locale( 
				getScriptWithMultiLocales(), 
				LocaleListOrderType.Random, 
				LocaleListLanguage.DifferetLanguage);
	}
	
	@Test
	public void test_getNextFallbackLocale_with_LanguageCountry_locale() {
		TreeMap<String, Locale> languageTreeLocalesMap = getLanguageTreeLocalesMap();
			NavigableSet<String> navigableKeySet = languageTreeLocalesMap.navigableKeySet();
			Iterator<String> iterator = navigableKeySet.iterator();
			while( iterator.hasNext()) {
				String key = iterator.next();
				String scriptVariantStr 
				= key.substring( key.indexOf( ",", key.indexOf( ",") + 1) + 1);
				if ( ",".equals( scriptVariantStr)) continue; // for
				iterator.remove();
			} // while
			
		Locale ultimateFallbackLocale = null;
		int testedLocaleInputCount = 0;
		iterator = navigableKeySet.iterator();
		while( iterator.hasNext()) {
			String key = iterator.next();
				String country = key.substring( key.indexOf( ",") + 1);
					if ( ",,".equals( country)) continue;
			Locale inputLocale = languageTreeLocalesMap.get( key);
			
			String language = key.substring( 0, key.indexOf( ","));
			SortedMap<String, Locale> sortedMap 
			= languageTreeLocalesMap.subMap( language.concat( ",,,"), language.concat( "Z,,,"));
			
			TreeMap<String, Locale> workTreeMap 
			= (TreeMap<String, Locale>)(languageTreeLocalesMap.clone());
				workTreeMap.navigableKeySet().removeAll( sortedMap.keySet());
			List<Locale> localeList = new ArrayList<Locale>( workTreeMap.values());
				Collections.shuffle( localeList);
			
			LanguagePolicyLocaleFallbackHandler languagePolicyLocaleFallbackHandler
			= new LanguagePolicyLocaleFallbackHandler( ultimateFallbackLocale);
				languagePolicyLocaleFallbackHandler.setFallbackLocalesList(localeList);
				Assert.assertEquals(
						String.format(
								"Unexpected fall-back locale returned by %1$s." 
								+ "getNextFallbackLocale method for test giving %2$s as the argument " 
								+ "and the fall-back locale list holds language-only and language+country " 
								+ "locales of different languages: %3$s",
								languagePolicyLocaleFallbackHandler.getClass().getSimpleName(),
								inputLocale.toString(), 
								languagePolicyLocaleFallbackHandler
								.getFallbackLocalesList().toString()),
						ultimateFallbackLocale, 
						languagePolicyLocaleFallbackHandler.getNextFallbackLocale( inputLocale)
						);
			
			testedLocaleInputCount++;	
		} // while
		
		Assert.assertTrue( 
				String.format(
						"Test data error: could not find enough number of locales with language-only " 
						+ "and/or language+country for different languages among system-provided locales. " 
						+ "Here are system provided locales: %1$s.",
						languageTreeLocalesMap.values().toString()),
				(testedLocaleInputCount > 0));
	}
	
}
