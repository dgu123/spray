package com.newmainsoftech.spray.sprex.test.config;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mockito.Mockito;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import com.newmainsoftech.spray.sprex.context.support.DatabaseMessageSource;
import com.newmainsoftech.spray.sprex.context.support.MessageDaoCase;

@Configuration
public class SpringIntegrationTestDispatcherServletConfig extends WebMvcConfigurationSupport {
	// Configure default Servlet mapping ----------------------------------------------------------
	@Override
	protected void configureDefaultServletHandling( DefaultServletHandlerConfigurer configurer) {
		super.configureDefaultServletHandling( configurer);
		configurer.enable();
			/* This configures a DefaultServletHttpRequestHandler with a URL mapping of "/**" and 
			 * the lowest priority relative to other URL mappings. 
			 * DefaultServletHttpRequestHandler will forward all requests to the default Servlet. 
			 */
	}
	// --------------------------------------------------------------------------------------------
	
	// For testing DatabaseMessageSource ----------------------------------------------------------
	
		// Set up Spring's CookieLocaleResolver bean since Spring's default AcceptHeaderLocaleResolver 
		// does not allow to change locale ------------------------------------------------------------
		@Bean
		public LocaleResolver localeResolver() {
			CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
				cookieLocaleResolver.setDefaultLocale( Locale.ENGLISH);
			return cookieLocaleResolver;
		}
		
		static final String LocaleParameterName = "locale";
			public static String getLocaleParameterName() {
				return LocaleParameterName;
			}
	
		public HandlerInterceptor localeChangeInterceptor() {
			LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
				localeChangeInterceptor.setParamName( 
						SpringIntegrationTestDispatcherServletConfig.getLocaleParameterName());
			return localeChangeInterceptor;
		}
		
		@Override
		protected void addInterceptors( InterceptorRegistry registry) {
			registry.addInterceptor( localeChangeInterceptor());
		}
		// --------------------------------------------------------------------------------------------
	
	
	public static final String TestViewName = "/for_testing_DatabaseMessageSource";
		public static String getTestViewName() {
			return TestViewName;
		}

	/**
	 * Spring controller for testing <code>{@link DatabaseMessageSource}</code> class.<br />
	 * 
	 * @author Arata Y.
	 */
	@Controller
	public static class DatabaseMessageSourceTestController {
		public static final String LocaleModelCode = "oldLocaleName";
			public static String getLocaleModelCode() {
				return LocaleModelCode;
			}
		public static final String ClassNameModelCode = "localeResolverClassName";
			public static String getClassNameModelCode() {
				return ClassNameModelCode;
			}
		public static final String DateModelCode = "date";
			public static String getDateModelCode() {
				return DateModelCode;
			}
		public static final Date date = new Date();
			public static Date getDate() {
				return date;
			}
	
		@RequestMapping( value = { SpringIntegrationTestDispatcherServletConfig.TestViewName})
		protected ModelAndView getTestView(
				final HttpServletRequest request, final HttpServletResponse response)
				throws Exception {
			Map<String, Object> map = new HashMap<String, Object>();
			
			LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver( request);
				if ( localeResolver != null) {
					Locale locale = localeResolver.resolveLocale( request);
						if ( locale != null) {
							map.put( 
									DatabaseMessageSourceTestController.getLocaleModelCode(), 
									locale.toString());
						}
					String localeResolverClassName = localeResolver.getClass().getName();
					map.put( 
							DatabaseMessageSourceTestController.getClassNameModelCode(), 
							localeResolverClassName);
					
					String newLocaleName 
					= ServletRequestUtils.getStringParameter( 
							request, SpringIntegrationTestDispatcherServletConfig.getLocaleParameterName());
						if ( newLocaleName != null) {
							LocaleEditor localeEditor = new LocaleEditor();
							localeEditor.setAsText( newLocaleName);
							localeResolver.setLocale( 
									request, response, (Locale)localeEditor.getValue());
						}
				}
			map.put( DatabaseMessageSourceTestController.getDateModelCode(), getDate());
			
			ModelAndView modelAndView 
			= new ModelAndView( 
					SpringIntegrationTestDispatcherServletConfig.getTestViewName(), map);
			
			return modelAndView;
		}
	}

	public static final String H1MessageCode = "DatabaseMessageSourceIntegrationTest.h1";
		public static String getH1MessageCode() {
			return H1MessageCode;
		}
		
	public static final Map<Locale, String> messageMap = new HashMap<Locale, String>();
		static {
			messageMap.put( Locale.ENGLISH, "English");
			messageMap.put( Locale.US, "US");
			messageMap.put( Locale.JAPANESE, "Japanese(日本語)");
			messageMap.put( Locale.JAPAN, "Japan(日本)");
		}
		public static Map<Locale, String> getMessageMap() {
			return messageMap;
		}

	MessageDaoCase messageDao = Mockito.mock( MessageDaoCase.class);
		{
			Mockito
			.when( messageDao.getMessages( SpringIntegrationTestDispatcherServletConfig.getH1MessageCode()))
			.thenReturn( SpringIntegrationTestDispatcherServletConfig.getMessageMap());
		}
		@Bean
		public MessageDaoCase messageDao() {
			return this.messageDao;
		}
	@Bean
	public AbstractMessageSource messageSource() {
		DatabaseMessageSource databaseMessageSource = new DatabaseMessageSource();
			databaseMessageSource.setMessageDao( messageDao());
		return databaseMessageSource;
	}
	
/* Strangely with configuration by Spring's WebApplicationInitializer implementation class 
 * SpringIntegrationTestWebAppInitializer (on jetty), having this method caused exception of in bean 
 * instantiation of DatabaseMessageSourceTestController class as duplicate instantiation. 
 * If I don't commented out this method and don't give SpringIntegrationTestDispatcherServletConfig 
 * class as the argument of register method of AnnotationConfigWebApplicationContext object in 
 * SpringIntegrationTestWebAppInitializer's onStartup method, then DatabaseMessageSourceTestController 
 * bean is not instantiated. In other words, AnnotationConfigWebApplicationContext is not going to scan 
 * automatically @Controller annotated class and instantiate as a bean. 
 * However, once @Configuration annotated class is given to AnnotationConfigWebApplicationContext's 
 * register method, AnnotationConfigWebApplicationContext will instantiate a bean for @Controller 
 * annotated class found via the given @Configuration annotated class. 
 * 	@Bean
	public DatabaseMessageSourceTestController databaseMessageSourceTestController() {
		return new DatabaseMessageSourceTestController();
	}
*/	
	public static final String InternalResourceViewPrefix = "/WEB-INF/jsp";
		public static String getInternalResourceViewPrefix() {
			return InternalResourceViewPrefix;
		}
	public static final String InternalResourceViewSuffix = ".jsp";
		public static String getInternalResourceViewSuffix() {
			return InternalResourceViewSuffix;
		}
	
	@Bean
	public ViewResolver viewResolver() {
		InternalResourceViewResolver internalResourceViewResolver 
		= new InternalResourceViewResolver();
			internalResourceViewResolver.setPrefix( 
					SpringIntegrationTestDispatcherServletConfig.getInternalResourceViewPrefix());
			internalResourceViewResolver.setSuffix( 
					SpringIntegrationTestDispatcherServletConfig.getInternalResourceViewSuffix());
			internalResourceViewResolver.setOrder( 0);
		return internalResourceViewResolver;
	}
	// --------------------------------------------------------------------------------------------
	
}
