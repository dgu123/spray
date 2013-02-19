package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
public class InternationalizationConfigTest {	
	@Autowired WebApplicationContext webApplicationContext;
		WebApplicationContext getWebApplicationContext() {
			return webApplicationContext;
		}
		
	@Controller // --------------------------------------------------------------------------------
	public static class TestController {
		Logger logger = LoggerFactory.getLogger( this.getClass());
			public final Logger getLogger() {
				return logger;
			}
			
		// Test data ------------------------------------------------------------------------------
		static String convertToHtmlUnicodeCodeReference( final String input) {
			String htmlUnicodeCodeReferenceExpression = "";
			String charStr;
			for( char charTypeObj : input.toCharArray()) {
				charStr = Integer.toHexString( charTypeObj);
				if ( charStr.length() < 4) {
					String formatStr = "%1$0".concat( String.valueOf(  4 - charStr.length())).concat( "d%2$s");
					charStr = String.format( formatStr, 0, charStr);
				}
				htmlUnicodeCodeReferenceExpression 
				= htmlUnicodeCodeReferenceExpression.concat( "&#x").concat( charStr).concat( ";");
			} // for
			return htmlUnicodeCodeReferenceExpression;
		}
		
		private static final String EnglishResponse = "Hello";
			public static String getEnglishResponse() {
				return EnglishResponse;
			}
		private static String JapaneseResponse = convertToHtmlUnicodeCodeReference( "ハロー");
			/* Converted Japanese string to HTML Unicode code reference expression due to
			 * MockMvcResultMatchers.content().string matcher method cannot handle other character than ANSI code point ones. 
			 * Also, output of MockMvcResultHandlers.print method would be shown as garbage character unless 
			 * system native locale of OS has ability to handle such character code. 
			 */
			public static String getJapaneseResponse() {
				return JapaneseResponse;
			}
		private static final String NonLinquisticResponse = "86-";
			public static String getNonLinquisticResponse() {
				return NonLinquisticResponse;
			}
		// ----------------------------------------------------------------------------------------
			
/*		class TestView extends AbstractView {			
			Logger logger = LoggerFactory.getLogger( this.getClass());
				public final Logger getLogger() {
					return logger;
				}
				
			@Override
			protected void renderMergedOutputModel(
					Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
				response.setCharacterEncoding( "UTF-8");
				
				Locale locale = getLocaleResolver().resolveLocale( request);
					Logger logger = getLogger();
					if ( logger.isDebugEnabled()) {
						logger.debug( 
								String.format( "Locale of request: %1$s", locale.toString())
								);
					}
					
				PrintWriter responsePrintWriter = response.getWriter();
					responsePrintWriter.println( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\">");
					responsePrintWriter.println( "<html>");
					responsePrintWriter.println( "<head>");
					responsePrintWriter.println( "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
					responsePrintWriter.println( "</head>");
					responsePrintWriter.println( "<body>");
					if ( Locale.ENGLISH.equals( locale)) {
						responsePrintWriter.println( TestController.getEnglishResponse());
					}
					else if ( Locale.JAPANESE.equals( locale)) {
						final String japaneseResponse = TestController.getJapaneseResponse();
							if ( logger.isDebugEnabled()) {
								logger.debug( 
										String.format( "Returing Japanese response : %1$s", japaneseResponse)
										);
							}				
						responsePrintWriter.println( japaneseResponse);
					}
					else {
						responsePrintWriter.println( TestController.getNonLinquisticResponse());
					}			
					responsePrintWriter.println( "</body>");
					responsePrintWriter.println( "</html>");
			}
		}
		@RequestMapping( RequestTestUrl)
		public TestView getIndexTestView( final HttpServletRequest request, final HttpServletResponse response) {
			return new TestView();
		}
*/		
		
		@Autowired LocaleResolver localeResolver;
			public LocaleResolver getLocaleResolver() {
				return localeResolver;
			}

		public final static String RequestTestUrl = "/index";
			public static final String getRequestTestUrl() {
				return RequestTestUrl;
			}

		@RequestMapping( "/index")
		@ResponseBody
		public String getIndexView( final HttpServletRequest request, final HttpServletResponse response) {
			
			response.setCharacterEncoding( "UTF-8");
			
			Locale locale = getLocaleResolver().resolveLocale( request);
				Logger logger = getLogger();
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format( "Locale of request: %1$s", locale.toString())
							);
				}
					
			if ( Locale.ENGLISH.equals( locale)) {
				return TestController.getEnglishResponse();
			}
			else if ( Locale.JAPANESE.equals( locale)) {
				final String japaneseResponse = TestController.getJapaneseResponse();
					if ( logger.isDebugEnabled()) {
						logger.debug( 
								String.format( "Returing Japanese response : %1$s", japaneseResponse)
								);						
					}				
				return japaneseResponse;
			}
			else {
				return TestController.getNonLinquisticResponse();				
			}			
		}
	}
	// --------------------------------------------------------------------------------------------
	
	@Configuration // -----------------------------------------------------------------------------
	@Import( value={ InternationalizationConfig.class})
	public static class TestConfiguration extends WebMvcConfigurationSupport {
		@Autowired InternationalizationConfigCase internationalizationConfig;
			InternationalizationConfigCase getInternationalizationConfig() {
				return internationalizationConfig;
			}
		@Override
		protected void addInterceptors( InterceptorRegistry registry) {
			registry.addInterceptor( getInternationalizationConfig().localeChangeInterceptor());
		}


		@Bean
		public TestController testController() {
			return new TestController();
		}
	}
	// --------------------------------------------------------------------------------------------
	
/*	@Autowired TestController testController;
		TestController getTestController() {
			return testController;
		}

	@Autowired LocaleChangeInterceptor localeChangeInterceptor;
		LocaleChangeInterceptor getLocaleChangeInterceptor() {
			return localeChangeInterceptor;
		}
	@Autowired LocaleResolver localeResolver;
		LocaleResolver getLocaleResolver() {
			return localeResolver;
		}
*/	
	MockMvc mockMvc;
		MockMvc getMockMvc() {
			return mockMvc;
		}
		void setMockMvc( final MockMvc mockMvc) {
			this.mockMvc = mockMvc;
		}

	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup( getWebApplicationContext()).build();
/*		StandaloneMockMvcBuilder standaloneMockMvcBuilder = MockMvcBuilders.standaloneSetup( getTestController());
			standaloneMockMvcBuilder.addInterceptors( getLocaleChangeInterceptor());
			standaloneMockMvcBuilder.setLocaleResolver( getLocaleResolver());
		setMockMvc( standaloneMockMvcBuilder.build());
*/
	}
	
	@Test
	public void test_localeChangeInterceptor() throws Throwable {
		MockMvc mockMvcObj = getMockMvc();
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( TestController.getRequestTestUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.content().string( TestController.getEnglishResponse()));
		
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( TestController.getRequestTestUrl())
				.characterEncoding( "UTF-8")
				.locale( Locale.ENGLISH).param( "locale", "ja")
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.content().string( TestController.getJapaneseResponse()));		
		
		Cookie cookie = new Cookie( CookieLocaleResolver.DEFAULT_COOKIE_NAME, "ja");
		
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( TestController.getRequestTestUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.cookie( cookie)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.content().string( TestController.getJapaneseResponse()));
	}
}
