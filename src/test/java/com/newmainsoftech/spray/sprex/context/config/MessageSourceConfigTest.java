package com.newmainsoftech.spray.sprex.context.config;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class MessageSourceConfigTest {
	@Configuration
	static class MessageSourceTestConfig extends MessageSourceConfig {
		
		static final String DefaultMessageSourceFileName = "messageSourceConfigTest.xml";
			public static String getDefaultMessageSourceFileName() {
				return DefaultMessageSourceFileName;
			}
			public static URL getMessageSourceUrl( final String messageSourceFileName) {
				final URL url = MessageSourceTestConfig.class.getResource( messageSourceFileName);
					Assert.assertNotNull( 
							String.format( "Could not locate %1$s.", messageSourceFileName),
							url);
				return url;
			}
			public static String getMessageSourceFilePath( final URL messageSourceUrl) {
				try {
					File messageSourceFile = new File( messageSourceUrl.toURI());
					return messageSourceFile.getCanonicalPath();
				}
				catch( Exception exception) {
					throw new RuntimeException( exception);
				}
			}
		public static String getTestMessageSourceBaseName() {
			String messageSourceBaseName 
			= MessageSourceTestConfig.getMessageSourceFilePath( 
					MessageSourceTestConfig.getMessageSourceUrl( 
							MessageSourceTestConfig.getDefaultMessageSourceFileName())
					);
				messageSourceBaseName 
				= messageSourceBaseName.substring( 0, messageSourceBaseName.lastIndexOf( "."));
				
				int packageIndex 
				= messageSourceBaseName.lastIndexOf( 
						File.separator.concat( MessageSourceTestConfig.class.getPackage().getName().replace( ".", File.separator))
						);
					Assert.assertTrue(
							String.format(
									"Test configuration problem: %1$s file is expected under %2$s at test setup.",
									MessageSourceTestConfig.getMessageSourceFilePath(
											MessageSourceTestConfig.getMessageSourceUrl( 
													MessageSourceTestConfig.getDefaultMessageSourceFileName())
											),
									MessageSourceTestConfig.class.getPackage().getName().replace( ".", File.separator)
									),
							(packageIndex > -1)
							);
				
				messageSourceBaseName
				= messageSourceBaseName.substring( packageIndex);
			return messageSourceBaseName;
		}
			
		@Override
		public String[] getMessageSourceBaseNames() {
			return new String[]{ MessageSourceTestConfig.getTestMessageSourceBaseName()};
		}
	}
	
	@Autowired MessageSource messageSource;
	
	@Test
	public void test_setBasenames() throws Throwable {
		Assert.assertTrue( messageSource instanceof ReloadableResourceBundleMessageSource);
		
		final String basenamesFieldName = "basenames";
		final Field basenamesArrayField = ReloadableResourceBundleMessageSource.class.getDeclaredField( basenamesFieldName);
			basenamesArrayField.setAccessible( true);
			Assert.assertEquals( (new String[]{}).getClass(), basenamesArrayField.getType());
		String[] basenamesArray = (String[])(basenamesArrayField.get( (ReloadableResourceBundleMessageSource)messageSource));
			Assert.assertEquals( 
					String.format(
							"Expected that %1$s field of %2$s holds just [%3$s] but actually %4$s.",
							basenamesFieldName, 
							ReloadableResourceBundleMessageSource.class.getSimpleName(),
							MessageSourceTestConfig.getTestMessageSourceBaseName(),
							Arrays.toString( basenamesArray)
							),
					1, 
					basenamesArray.length);
			Assert.assertEquals( 
					String.format(
							"Expected that %1$s field of %2$s holds just [%3$s] but actually %4$s.",
							basenamesFieldName, 
							ReloadableResourceBundleMessageSource.class.getSimpleName(),
							MessageSourceTestConfig.getTestMessageSourceBaseName(),
							Arrays.toString( basenamesArray)
							),
					MessageSourceTestConfig.getTestMessageSourceBaseName(), 
					basenamesArray[ 0]);
	}
	
	@Test
	public void test_messageSource() throws Throwable {
		final String testMessageKey = "hello";
		
		// Test on default locale
		final Properties defaultProperties = new Properties();
			final File defaultMessageSourceFile 
			= new File(
					MessageSourceTestConfig.getMessageSourceUrl( 
							MessageSourceTestConfig.getDefaultMessageSourceFileName()
							).toURI()					
					);
			final FileInputStream defaultMessageSourceInputStream = new FileInputStream( defaultMessageSourceFile);
			defaultProperties.loadFromXML( defaultMessageSourceInputStream);
				Assert.assertNotNull( 
						String.format( 
								"Could not find %1$s property in %2$s",
								testMessageKey, 
								defaultMessageSourceFile.getCanonicalPath()
								),
						defaultProperties.getProperty( testMessageKey));			
		Assert.assertEquals( defaultProperties.getProperty( testMessageKey), messageSource.getMessage( testMessageKey, null, null));
		
		// Test on Japanese locale
		final Properties japaneseProperties = new Properties();
			String japaneseMessageSourceFileName = MessageSourceTestConfig.getDefaultMessageSourceFileName();
				japaneseMessageSourceFileName 
				= japaneseMessageSourceFileName.substring( 0, japaneseMessageSourceFileName.lastIndexOf( ".")).concat( "_ja.xml");
			final File japaneseMessageSourceFile 
			= new File(
					MessageSourceTestConfig.getMessageSourceUrl( japaneseMessageSourceFileName)
					.toURI()
					);
			final FileInputStream japaneseMessageSourceInputStream = new FileInputStream( japaneseMessageSourceFile);
			japaneseProperties.loadFromXML( japaneseMessageSourceInputStream);
				Assert.assertNotNull( 
						String.format( 
								"Could not find %1$s property in %2$s",
								testMessageKey, 
								japaneseMessageSourceFile.getCanonicalPath()
								),
						japaneseProperties.getProperty( testMessageKey));		
		Assert.assertEquals( japaneseProperties.getProperty( testMessageKey), messageSource.getMessage( testMessageKey, null, Locale.JAPANESE));
	}
}
