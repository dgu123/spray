package com.newmainsoftech.spray.sprex.context.support;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseMessageSourceTest {
	Logger logger = LoggerFactory.getLogger( this.getClass());
		public Logger getLogger() {
			return logger;
		}

	@Mock MessageDaoCase messageDao;
	@InjectMocks DatabaseMessageSource databaseMessageSource;
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks( this);
	}
	
	@Test
	public void test_getMessages() {
		final String messageCode = "testMessageCode";
		final Map<Locale, String> messageMap = new HashMap<Locale, String>();
			messageMap.put( Locale.ENGLISH, "English");
			messageMap.put( Locale.US, "US");
			messageMap.put( Locale.JAPANESE, "Japanese");
			messageMap.put( Locale.JAPAN, "Japan");
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		Assert.assertEquals( messageMap, databaseMessageSource.getMessages( messageCode));
		Mockito.verify( messageDao).getMessages( messageCode);
	}
	
	@Test
	public void test_getMessage_with_variant_locale() {
		final String messageCode = "testMessageCode";
		
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			Map<String, List<Locale>> variantWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getVariantLocales();
			final String messageSuffix = " message";
			for( Entry<String, List<Locale>> entry : variantWithMultiLocales.entrySet()) {
				for( Locale locale : entry.getValue()) {
					messageMap.put( 
							locale, 
							locale.toString().concat( messageSuffix));
				} // for
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		int testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : variantWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages : %3$s.",
								messageCode, 
								locale.toString(), 
								messageMap.toString()),
						locale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, locale));
				testedLocaleCount++;
			} // for
		} // for		
		Assert.assertTrue( 
				String.format( 
						"Test data error: not sufficient number of language+country+variant locale is found " 
						+ "among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
	@Test
	public void test_getMessage_with_script_locale() {
		final String messageCode = "testMessageCode";
		
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			Map<String, List<Locale>> scriptWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getScriptWithMultiLocales();
			final String messageSuffix = " message";
			for( Entry<String, List<Locale>> entry : scriptWithMultiLocales.entrySet()) {
				for( Locale locale : entry.getValue()) {
					messageMap.put( 
							locale, 
							locale.toString().concat( messageSuffix));
				} // for
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		int testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : scriptWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages : %3$s.",
								messageCode, 
								locale.toString(), 
								messageMap.toString()),
						locale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, locale));
				testedLocaleCount++;
			} // for
		} // for		
		Assert.assertTrue( 
				String.format( 
						"Test data error: not sufficient number of language+country+script locale is found " 
						+ "among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
	@Test
	public void test_getMessage_with_country_locale() {
		final String messageCode = "testMessageCode";
		
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			Map<String, List<Locale>> scriptWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getCountryWithMultiLocales();
			TreeMap<String, Locale> languageTreeLocalesMap 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageTreeLocalesMap();
			final String messageSuffix = " message";
			for( Entry<String, List<Locale>> entry : scriptWithMultiLocales.entrySet()) {
				for( Locale locale : entry.getValue()) {
					String language = locale.getLanguage();
					String country = locale.getCountry();
					Locale countryLocale 
					= languageTreeLocalesMap.get( language.concat( ",").concat( country).concat( ",,"));
						if ( countryLocale == null) continue; // for
					messageMap.put( 
							countryLocale, 
							countryLocale.toString().concat( messageSuffix));
				} // for
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		int testedLocaleCount = 0;
		for( Locale locale : messageMap.keySet()) {
			Assert.assertEquals( 
					String.format(
							"%1$s was given as message key code and %2$s was given as locale. " 
							+ "And here is prepared group of locale unique messages : %3$s.",
							messageCode, 
							locale.toString(), 
							messageMap.toString()),
					locale.toString().concat( messageSuffix), 
					databaseMessageSource.getMessage( messageCode, locale));
			testedLocaleCount++;
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: not sufficient number of language+country locale is found " 
						+ "among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
	@Test
	public void test_getMessage_with_language_locale() {
		final String messageCode = "testMessageCode";
		
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			Map<String, List<Locale>> languageWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageWithMultiLocales();
			TreeMap<String, Locale> languageTreeLocalesMap 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageTreeLocalesMap();
			final String messageSuffix = " message";
			for( String language : languageWithMultiLocales.keySet()) {
				Locale languageOnlyLocale 
				= languageTreeLocalesMap.get( language.concat( ",,,"));
					if ( languageOnlyLocale == null) continue; // for
				messageMap.put( 
						languageOnlyLocale, 
						languageOnlyLocale.toString().concat( messageSuffix));
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		int testedLocaleCount = 0;
		for( Locale locale : messageMap.keySet()) {
			Assert.assertEquals( 
					String.format(
							"%1$s was given as message key code and %2$s was given as locale. " 
							+ "And here is prepared group of locale unique messages : %3$s.",
							messageCode, 
							locale.toString(), 
							messageMap.toString()),
					locale.toString().concat( messageSuffix), 
					databaseMessageSource.getMessage( messageCode, locale));
			testedLocaleCount++;
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: not sufficient number of language-only locale is found " 
						+ "among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
	@Test
	public void test_getMessage_with_fallback_to_country_locale() {
		final String messageCode = "testMessageCode";
		
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			Map<String, List<Locale>> countryWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getCountryWithMultiLocales();
			TreeMap<String, Locale> languageTreeLocalesMap 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageTreeLocalesMap();
			final String messageSuffix = " message";
			for( Entry<String, List<Locale>> entry : countryWithMultiLocales.entrySet()) {
				for( Locale locale : entry.getValue()) {
					String language = locale.getLanguage();
					String country = locale.getCountry();
					Locale countryLocale 
					= languageTreeLocalesMap.get( language.concat( ",").concat( country).concat( ",,"));
						if ( countryLocale == null) continue; // for
					messageMap.put( 
							countryLocale, 
							countryLocale.toString().concat( messageSuffix));
				} // for
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		Map<String, List<Locale>> variantWithMultiLocales 
		= languagePolicyLocaleFallbackHandlerTest.getVariantLocales();
		int testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : variantWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				String language = locale.getLanguage();
				String country = locale.getCountry();
					if ( "".equals( country)) continue; // for
				Locale countryLocale 
				= languageTreeLocalesMap.get( language.concat( ",").concat( country).concat( ",,"));
					if ( countryLocale == null) continue; // for
				
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages : %3$s.",
								messageCode, 
								locale.toString(), 
								messageMap.toString()),
						countryLocale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, locale));
				testedLocaleCount++;
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: lagnauge+country+variant locale and lagnauge+country locale " 
						+ "are not found for same language among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
		
		Map<String, List<Locale>> scriptWithMultiLocales 
		= languagePolicyLocaleFallbackHandlerTest.getScriptWithMultiLocales();
		testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : scriptWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				String language = locale.getLanguage();
				String country = locale.getCountry();
					if ( "".equals( country)) continue; // for
				Locale countryLocale 
				= languageTreeLocalesMap.get( language.concat( ",").concat( country).concat( ",,"));
					if ( countryLocale == null) continue; // for
				
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages : %3$s.",
								messageCode, 
								locale.toString(), 
								messageMap.toString()),
						countryLocale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, locale));
				testedLocaleCount++;
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: lagnauge+country+script locale and lagnauge+country locale " 
						+ "are not found for same language among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}

	@Test
	public void test_getMessage_with_fallback_to_language_locale() {
		final String messageCode = "testMessageCode";
		
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			
			Map<String, List<Locale>> languageWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageWithMultiLocales();
			TreeMap<String, Locale> languageTreeLocalesMap 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageTreeLocalesMap();
			final String messageSuffix = " message";
			for( String language : languageWithMultiLocales.keySet()) {
				Locale locale 
				= languageTreeLocalesMap.get( language.concat( ",,,"));
					if ( locale == null) continue; // for
				messageMap.put( locale, locale.toString().concat( messageSuffix));
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		Map<String, List<Locale>> countryWithMultiLocales 
		= languagePolicyLocaleFallbackHandlerTest.getCountryWithMultiLocales();
		int testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : countryWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				String language = locale.getLanguage();
				String country = locale.getCountry();
				Locale countryLocale 
				= languageTreeLocalesMap.get( language.concat( ",").concat( country).concat( ",,"));
					if ( countryLocale == null) continue; // for
				Locale languageLocale
				= languageTreeLocalesMap.get( language.concat( ",,,"));
					if ( languageLocale == null) continue; // for
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages: %3$s.",
								messageCode, 
								countryLocale.toString(), 
								messageMap.toString()),
						languageLocale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, countryLocale));
				testedLocaleCount++;
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: lagnauge+country locale and lagnauge-only locale " 
						+ "are not found for same language among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
		
		Map<String, List<Locale>> scriptWithMultiLocales 
		= languagePolicyLocaleFallbackHandlerTest.getScriptWithMultiLocales();
		testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : scriptWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				String language = locale.getLanguage();
				Locale languageLocale
				= languageTreeLocalesMap.get( language.concat( ",,,"));
					if ( languageLocale == null) continue; // for
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages : %3$s.",
								messageCode, 
								locale.toString(), 
								messageMap.toString()),
						languageLocale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, locale));
				testedLocaleCount++;
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: lagnauge+country+script locale and lagnauge-only locale " 
						+ "are not found for same language among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
	@Test
	public void test_getMessage_with_fallback_to_ultimate_fallback_locale() {
		final String messageCode = "testMessageCode";
		
		final String messageSuffix = " message";
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			Locale ultimateFallbackLocale = DatabaseMessageSource.getDefaultUltimateFallbackLocale();
			messageMap.put( 
					ultimateFallbackLocale, 
					ultimateFallbackLocale.toString().concat( messageSuffix));
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
		= new LanguagePolicyLocaleFallbackHandlerTest();
		Map<String, List<Locale>> countryWithMultiLocales 
		= languagePolicyLocaleFallbackHandlerTest.getCountryWithMultiLocales();
		TreeMap<String, Locale> languageTreeLocalesMap
		= languagePolicyLocaleFallbackHandlerTest.getLanguageTreeLocalesMap();
		int testedLocaleCount = 0;
		for( Entry<String, List<Locale>> entry : countryWithMultiLocales.entrySet()) {
			for( Locale locale : entry.getValue()) {
				String language = locale.getLanguage();
				String country = locale.getCountry();
				Locale countryLocale 
				= languageTreeLocalesMap.get( language.concat( ",").concat( country).concat( ",,"));
					if ( countryLocale == null) continue; // for
				Assert.assertEquals( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages: %3$s.",
								messageCode, 
								countryLocale.toString(), 
								messageMap.toString()),
						ultimateFallbackLocale.toString().concat( messageSuffix), 
						databaseMessageSource.getMessage( messageCode, countryLocale));
				testedLocaleCount++;
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: not sufficient number of lagnauge+country locale among " 
						+ "system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
	@Test
	public void test_getMessage_for_null_return() {
		final String messageCode = "testMessageCode";
		
		Locale ultimateFallbackLocale = DatabaseMessageSource.getDefaultUltimateFallbackLocale();
		final String messageSuffix = " message";
		Map<Locale, String> messageMap = new HashMap<Locale, String>();
			final LanguagePolicyLocaleFallbackHandlerTest languagePolicyLocaleFallbackHandlerTest
			= new LanguagePolicyLocaleFallbackHandlerTest();
			
			Map<String, List<Locale>> languageWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getLanguageWithMultiLocales();
			Map<String, List<Locale>> scriptWithMultiLocales 
			= languagePolicyLocaleFallbackHandlerTest.getScriptWithMultiLocales();
			
			for( String language : languageWithMultiLocales.keySet()) {
				if ( language.equals( ultimateFallbackLocale.getLanguage())) continue; // for
				boolean isScriptLocaleAvailable = false;
				for( List<Locale> scriptLocaleList : scriptWithMultiLocales.values()) {
					for( Locale scriptLocale : scriptLocaleList) {
						if ( language.equals( scriptLocale.getLanguage())) {
							isScriptLocaleAvailable = true;
							break; // for
						}
					} // for
				} // for
				if ( isScriptLocaleAvailable) continue; // for
				
				for( Locale locale : languageWithMultiLocales.get( language)) {
					messageMap.put( locale, locale.toString().concat( messageSuffix));
				} // for
				
			} // for
		Mockito.when( messageDao.getMessages( messageCode)).thenReturn( messageMap);
		
		int testedLocaleCount = 0;
		for( List<Locale> scriptLocaleList : scriptWithMultiLocales.values()) {
			for( Locale scriptLocale : scriptLocaleList) {
				Assert.assertNull( 
						String.format(
								"%1$s was given as message key code and %2$s was given as locale. " 
								+ "And here is prepared group of locale unique messages: %3$s.",
								messageCode, 
								scriptLocale.toString(), 
								messageMap.toString()),
						databaseMessageSource.getMessage( messageCode, scriptLocale));
				testedLocaleCount++;
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: could not find language for what locale with script data exists, " 
						+ "and language for what locale with script data does not exist among system " 
						+ "provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
		
		final String invalidMessageCode = "invalidMessageCode";
		TreeMap<String, Locale> languageTreeLocalesMap
		= languagePolicyLocaleFallbackHandlerTest.getLanguageTreeLocalesMap();
		testedLocaleCount = 0;
		for( List<Locale> scriptLocaleList : scriptWithMultiLocales.values()) {
			for( Locale scriptLocale : scriptLocaleList) {
				String language = scriptLocale.getLanguage();
				SortedMap<String, Locale> sortedMap
				= languageTreeLocalesMap.subMap( language.concat( ",,,"), language.concat( "Z,,,"));
				if ( sortedMap == null) continue; // for
				for( Locale locale : sortedMap.values()) {
					Assert.assertNull( 
							String.format(
									"%1$s was given as message key code and %2$s was given as locale. " 
									+ "And here is prepared group of locale unique messages: %3$s.",
									invalidMessageCode, 
									locale.toString(), 
									messageMap.toString()),
							databaseMessageSource.getMessage( invalidMessageCode, locale));
					testedLocaleCount++;
				}
			} // for
		} // for
		for( List<Locale> variantLocaleList 
				: languagePolicyLocaleFallbackHandlerTest.getVariantLocales().values()) {
			for( Locale variantLocale : variantLocaleList) {
				String language = variantLocale.getLanguage();
				SortedMap<String, Locale> sortedMap
				= languageTreeLocalesMap.subMap( language.concat( ",,,"), language.concat( "Z,,,"));
				if ( sortedMap == null) continue; // for
				for( Locale locale : sortedMap.values()) {
					Assert.assertNull( 
							String.format(
									"%1$s was given as message key code and %2$s was given as locale. " 
									+ "And here is prepared group of locale unique messages: %3$s.",
									invalidMessageCode, 
									locale.toString(), 
									messageMap.toString()),
							databaseMessageSource.getMessage( invalidMessageCode, locale));
					testedLocaleCount++;
				}
			} // for
		} // for
		Assert.assertTrue( 
				String.format( 
						"Test data error: could not find language+country+variant locale and " 
						+ "language+country+script locale among system provided locales: %1$s.", 
						languagePolicyLocaleFallbackHandlerTest
						.getLanguageWithMultiLocales().values().toString()),
				testedLocaleCount > 0);
	}
	
}
