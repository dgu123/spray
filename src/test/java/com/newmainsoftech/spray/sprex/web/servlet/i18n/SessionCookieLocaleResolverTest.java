package com.newmainsoftech.spray.sprex.web.servlet.i18n;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.SubstringMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@RunWith( SpringJUnit4ClassRunner.class)
// @ContextConfiguration( loader = AnnotationConfigWebContextLoader.class)
@ContextConfiguration
@WebAppConfiguration( value="src/test/webapp")
public class SessionCookieLocaleResolverTest {
	@Autowired WebApplicationContext webApplicationContext;
		WebApplicationContext getWebApplicationContext() {
			Assert.isInstanceOf( 
					WebApplicationContext.class, 
					webApplicationContext, 
					String.format(
							"webApplicationContext bean has not been autowired correctly; " +
							"it's %1$s, not a WebApplicationContext instance.",
							(webApplicationContext == null) ? "null" : webApplicationContext.toString()
							)
					);
			return webApplicationContext;
		}

	@Controller // --------------------------------------------------------------------------------
	public static class SessionCookieLocaleResolverTestController {
		@Autowired LocaleResolver localeResolver;
			public LocaleResolver getSessionCookieLocaleResolver() {
				Assert.isInstanceOf( 
						SessionCookieLocaleResolver.class, 
						localeResolver, 
						String.format(
								"%1$s bean has not been autowired to %2$s.",
								SessionCookieLocaleResolver.class.getSimpleName(),
								this.getClass().getSimpleName()
								)
						);
				return localeResolver;
			}
			public void setSessionCookieLocaleResolver( LocaleResolver sessionCookieLocaleResolver) {
				this.localeResolver = sessionCookieLocaleResolver;
			}
		
		public final static String RespondMessageFormat 
		= "Locale data in %1$s [Name: %2$s, Value: %3$s]."; 
			public static String getRespondMessageFormat() {
				return RespondMessageFormat;
			}
		public final static String RequestParameterMessage = "request parameter";
			public static String getRequestParameterMessage() {
				return RequestParameterMessage;
			}
		public final static String RequestAttributeMessage = "request attribute";
			public static String getRequestAttributeMessage() {
				return RequestAttributeMessage;
			}
		public final static String SessionAttributeMessage = "session attribute";
			public static String getSessionAttributeMessage() {
				return SessionAttributeMessage;
			}
		public final static String RequestCookieMessage = "request cookie";
			public static String getRequestCookieMessage() {
				return RequestCookieMessage;
			}
		public final static String ResponseCookieMessage = "response cookie";
			public static String getResponseCookieMessage() {
				return ResponseCookieMessage;
			}
		
		String dumpLocaleDataInRequest( 
				final HttpServletRequest request, final HttpServletResponse response) {
			SessionCookieLocaleResolver sessionCookieLocaleResolver 
			= (SessionCookieLocaleResolver)getSessionCookieLocaleResolver();
			
			StringBuilder stringBuilder = new StringBuilder();
				String localeParam
				= request.getParameter( LocaleChangeInterceptor.DEFAULT_PARAM_NAME);
				stringBuilder.append( 
						String.format( 
								SessionCookieLocaleResolverTestController.getRespondMessageFormat(),
								SessionCookieLocaleResolverTestController.getRequestParameterMessage(),
								LocaleChangeInterceptor.DEFAULT_PARAM_NAME, 
								((localeParam == null) ? "null" : localeParam)
								)
						);
				Object requestLocale 
				= request.getAttribute( 
						sessionCookieLocaleResolver.getLocaleRequestAttributeName());
				stringBuilder.append( 
						String.format( 
								"%n".concat( 
										SessionCookieLocaleResolverTestController.getRespondMessageFormat()),
								SessionCookieLocaleResolverTestController.getRequestAttributeMessage(),
								sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
								((requestLocale == null) ? "null" : requestLocale.toString())
								)
						);
				Object sessionLocale 
				= request.getSession().getAttribute( 
						sessionCookieLocaleResolver.getLocaleSessionAttributeName());
				stringBuilder.append( 
						String.format( 
								"%n".concat( 
										SessionCookieLocaleResolverTestController.getRespondMessageFormat()),
								SessionCookieLocaleResolverTestController.getSessionAttributeMessage(),
								sessionCookieLocaleResolver.getLocaleSessionAttributeName(),
								((sessionLocale == null) ? "null" : sessionLocale.toString())
								)
						);
				Cookie cookie = null;
				List<Cookie> cookieList = Arrays.asList( request.getCookies());
					for( Cookie cookieObj : cookieList) {
						if ( sessionCookieLocaleResolver.getCookieName().equals( cookieObj.getName())) {
							cookie = cookieObj;
							break; // for
						}
					} // for
				stringBuilder.append( 
						String.format( 
								"%n".concat( 
										SessionCookieLocaleResolverTestController
										.getRespondMessageFormat()),
								SessionCookieLocaleResolverTestController.getRequestCookieMessage(),
								sessionCookieLocaleResolver.getCookieName(),
								((cookie == null) ? "null" : cookie.getValue())
								)
						);
				MockHttpServletResponse mockHttpServletResponse = (MockHttpServletResponse)response;
				cookie 
				= mockHttpServletResponse.getCookie( sessionCookieLocaleResolver.getCookieName());
				stringBuilder.append( 
						String.format( 
								"%n".concat( 
										SessionCookieLocaleResolverTestController
										.getRespondMessageFormat()),
								SessionCookieLocaleResolverTestController.getResponseCookieMessage(),
								sessionCookieLocaleResolver.getCookieName(),
								((cookie == null) ? "null" : cookie.getValue())
								)
						);
			return stringBuilder.toString();
		}
		
		public final static String TestWithLocaleChangeInterceptorUrl 
		= "/localeResolverTestWithLocaleChangeInterceptor";
			public static final String getTestWithLocaleChangeInterceptorUrl() {
				return TestWithLocaleChangeInterceptorUrl;
			}
		/**
		 * Generate response body string for testing <code>{@link SessionCookieLocaleResolver}</code> 
		 * in use via <code>{@link LocaleChangeInterceptor}</code>. 
		 * 
		 * @param request 
		 * @param response
		 * @return Response body string as dump of locale data from request parameter, request attribute, 
		 * request session attribute, and cookie.
		 */
		@RequestMapping( SessionCookieLocaleResolverTestController.TestWithLocaleChangeInterceptorUrl)
		@ResponseBody
		public String getViewForTestWithLocaleChangeInterceptorUrl( 
				final HttpServletRequest request, final HttpServletResponse response) {
			response.setCharacterEncoding( "UTF-8");
			
			return dumpLocaleDataInRequest( request, response);
		}
		
		public final static String TestResolveLocaleUrl 
		= "/resolveLocaleTest";
			public static String getTestResolveLocaleUrl() {
				return TestResolveLocaleUrl;
			}
		public final static String ResolvedLocaleMessageFormat = "Resolved locale = %1$s.";
			public static String getResolvedLocaleMessageFormat() {
				return ResolvedLocaleMessageFormat;
			}
		/**
		 * Generate response body string for testing 
		 * <code>{@link SessionCookieLocaleResolver#resolveLocale(HttpServletRequest)}</code> method. 
		 * 
		 * @param request
		 * @param response
		 * @return Response body string will contain resolved locale out of request, and dump of 
		 * locale data from request parameter, request attribute, request session attribute, 
		 * and cookie after manually calling 
		 * <code>{@link SessionCookieLocaleResolver#resolveLocale(HttpServletRequest)}</code> method.
		 */
		@RequestMapping( SessionCookieLocaleResolverTestController.TestResolveLocaleUrl)
		@ResponseBody
		public String getViewForTestResolveLocaleUrl( 
				final HttpServletRequest request, final HttpServletResponse response) {
			
			SessionCookieLocaleResolver sessionCookieLocaleResolver 
			= (SessionCookieLocaleResolver)getSessionCookieLocaleResolver();
				Locale locale = sessionCookieLocaleResolver.resolveLocale( request);
				if ( locale == null) {
					locale = StringUtils.parseLocaleString( request.getHeader( "Accept-Language"));
				}
					
			String responseBody 
			= String.format( 
					SessionCookieLocaleResolverTestController
					.getResolvedLocaleMessageFormat().concat( "%n"),
					((locale.toString() == null) 
							?  "null" : locale.toString())
					);
				
			return responseBody.concat( dumpLocaleDataInRequest( request, response));
		}
	}
	
	// --------------------------------------------------------------------------------------------
	
	@Configuration // -----------------------------------------------------------------------------
	public static class SessionCookieLocaleResolverTestConfiguration extends WebMvcConfigurationSupport {
			@Bean
			public LocaleResolver localeResolver() {
				return new SessionCookieLocaleResolver();
			}

		@Override
		protected void addInterceptors( InterceptorRegistry registry) {
			registry.addInterceptor( new LocaleChangeInterceptor());
		}
		
			@Bean
			public SessionCookieLocaleResolverTestController sessionCookieLocaleResolverTestController() {
				return new SessionCookieLocaleResolverTestController();
			}
	}
	// --------------------------------------------------------------------------------------------
	
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
	}

	public static class IncludeSubstringMatcher extends SubstringMatcher {
		public IncludeSubstringMatcher( final String substring) {
			super( substring);
		}
		
		@Override
		protected boolean evalSubstringOf( String arg) {
			return arg.contains( this.substring);
		}

		@Override
		protected String relationship() {
			return "contains";
		}
		
		@Factory 
		public static Matcher<String> include( String substring) {
			return new IncludeSubstringMatcher( substring);
		}
	}

	@Autowired LocaleResolver localeResolver;
		public LocaleResolver getLocaleResolver() {
			Assert.isInstanceOf( 
					SessionCookieLocaleResolver.class, 
					localeResolver, 
					String.format(
							"localeResolver bean has not been autowired correctly; " +
							"it's %1$s, not a WebApplicationContext instance.",
							(localeResolver == null) ? "null" : localeResolver.toString()
							)
					);
			return localeResolver;
		}
		
	/**
	 * Tests <code>{@link SessionCookieLocaleResolver#resolveLocale(HttpServletRequest)}</code> method.
	 */
	@Test
	public void test_resolveLocale() throws Throwable {
		final SessionCookieLocaleResolver sessionCookieLocaleResolver
		= (SessionCookieLocaleResolver)getLocaleResolver();
		
		final String handlerMethodName = "getViewForTestResolveLocaleUrl";
		
		MockMvc mockMvcObj = getMockMvc();
		
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.locale( Locale.ITALY)
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.ITALY.toString()
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.sessionAttr(  
						sessionCookieLocaleResolver.getLocaleSessionAttributeName(),
						Locale.FRANCE)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.FRANCE.toString()
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.FRANCE
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.requestAttr( 
						sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
						Locale.JAPAN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.JAPAN.toString()
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		Cookie cookie 
		= new Cookie( sessionCookieLocaleResolver.getCookieName(), Locale.JAPANESE.toString());
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.cookie( cookie)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.JAPANESE.toString()
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.JAPANESE
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.JAPANESE
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												Locale.JAPANESE
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		
		cookie 
		= new Cookie( sessionCookieLocaleResolver.getCookieName(), Locale.GERMANY.toString());
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.locale( Locale.ITALY)
				.sessionAttr(  
						sessionCookieLocaleResolver.getLocaleSessionAttributeName(),
						Locale.FRANCE)
				.requestAttr( 
						sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
						Locale.JAPAN)
				.cookie( cookie)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.FRANCE
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.FRANCE
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												Locale.GERMANY
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		
		cookie 
		= new Cookie( sessionCookieLocaleResolver.getCookieName(), Locale.GERMANY.toString());
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.locale( Locale.ITALY)
				.requestAttr( 
						sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
						Locale.JAPAN)
				.cookie( cookie)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												Locale.GERMANY
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		
		cookie 
		= new Cookie( sessionCookieLocaleResolver.getCookieName(), Locale.GERMANY.toString());
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestResolveLocaleUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.locale( Locale.ITALY)
				.cookie( cookie)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getResolvedLocaleMessageFormat(),
												Locale.GERMANY
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.GERMANY
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.GERMANY
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												Locale.GERMANY
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
	}
	
	/**
	 * Tests <code>{@link SessionCookieLocaleResolver}</code> in use via 
	 * <code>{@link LocaleChangeInterceptor}</code>, meaning testing by setting locale to 
	 * request parameter. 
	 * 
	 * @throws Throwable
	 */
	@Test
	public void test_setLocaleCalledByLocaleChangeInterceptor() throws Throwable {
		final SessionCookieLocaleResolver sessionCookieLocaleResolver
		= (SessionCookieLocaleResolver)getLocaleResolver();
		
		final String handlerMethodName = "getViewForTestWithLocaleChangeInterceptorUrl";
		
		MockMvc mockMvcObj = getMockMvc();
		
		// test integrated functionality with LocaleChangeInterceptor intercepter -----------------
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestWithLocaleChangeInterceptorUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.locale( Locale.ITALY)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestParameterMessage(),
												LocaleChangeInterceptor.DEFAULT_PARAM_NAME, 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												"null"
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												"null"
												)
										)
								)
						)
				);
		
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestWithLocaleChangeInterceptorUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.locale( Locale.ITALY)
					// I do not know where locale is set in request; not parameter, not attribute, 
					// not session, and I did not see at (Accept-Language) header.
				.param( LocaleChangeInterceptor.DEFAULT_PARAM_NAME, Locale.US.toString())
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestParameterMessage(),
												LocaleChangeInterceptor.DEFAULT_PARAM_NAME, 
												Locale.US
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.US
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.US
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												Locale.US
												)
										)
								)
						)
				);
		Cookie cookie 
		= new Cookie( sessionCookieLocaleResolver.getCookieName(), Locale.GERMANY.toString());
		mockMvcObj
		.perform( 
				MockMvcRequestBuilders.get( 
						SessionCookieLocaleResolverTestController.getTestWithLocaleChangeInterceptorUrl())
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				.param( LocaleChangeInterceptor.DEFAULT_PARAM_NAME, Locale.JAPAN.toString())
				.sessionAttr(  
						sessionCookieLocaleResolver.getLocaleSessionAttributeName(),
						Locale.FRANCE)
				.requestAttr( 
						sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
						Locale.KOREA)
				.cookie( cookie)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.handler()
				.handlerType( SessionCookieLocaleResolverTestController.class))
		.andExpect( MockMvcResultMatchers.handler().methodName( handlerMethodName))
		.andExpect( 
				MockMvcResultMatchers
				.content().string( 
						Matchers.allOf( 
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestParameterMessage(),
												LocaleChangeInterceptor.DEFAULT_PARAM_NAME, 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getRequestAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleRequestAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getSessionAttributeMessage(),
												sessionCookieLocaleResolver.getLocaleSessionAttributeName(), 
												Locale.JAPAN
												)
										),
								IncludeSubstringMatcher.include( 
										String.format(
												SessionCookieLocaleResolverTestController
												.getRespondMessageFormat(),
												SessionCookieLocaleResolverTestController
												.getResponseCookieMessage(),
												sessionCookieLocaleResolver.getCookieName(), 
												Locale.JAPAN
												)
										)
								)
						)
				);
		// ----------------------------------------------------------------------------------------
	}
}
