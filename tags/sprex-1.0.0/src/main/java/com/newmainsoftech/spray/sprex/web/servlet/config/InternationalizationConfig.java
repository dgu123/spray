package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.newmainsoftech.spray.sprex.web.servlet.i18n.SessionCookieLocaleResolver;

@Configuration
public class InternationalizationConfig implements InternationalizationConfigCase {
	// For resolving locale -----------------------------------------------------------------------
	public static final String LocaleParamName = "locale";
		/* (non-Javadoc)
		 * @see com.newmainsoftech.spray.sprex.web.servlet.config.InternationalizationConfigCase#getLocaleParamName()
		 */
		@Override
		public String getLocaleParamName() {
			return InternationalizationConfig.LocaleParamName;
		}

	/* (non-Javadoc)
	 * @see com.newmainsoftech.spray.sprex.web.servlet.config.InternationalizationConfigCase#localeChangeInterceptor()
	 */
	@Override
	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor() {
		/* The LocaleChangeInterceptor is configured to look for the parameter name 'locale' to indicate a change of the user's locale. 
		 * For example, adding 'locale=es' to a URL would change the locale to Spanish.
		 * Referred at http://www.springbyexample.org/examples/basic-webapp-internationalization-spring-config.html
		 * So, the view handler method in controller needs just to consult to LocaleResolver's resolveLocale method to get locale of request.  
		 */
		LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
			localeChangeInterceptor.setParamName( getLocaleParamName());
		return localeChangeInterceptor;
	}
	
	
	public static final Locale DefaultLocale = Locale.ENGLISH;
		/* (non-Javadoc)
		 * @see com.newmainsoftech.spray.sprex.web.servlet.config.InternationalizationConfigCase#getDefaultlocale()
		 */
		@Override
		public Locale getDefaultlocale() {
			return InternationalizationConfig.DefaultLocale;
		}

	/* (non-Javadoc)
	 * @see com.newmainsoftech.spray.sprex.web.servlet.config.InternationalizationConfigCase#cookieLocaleResolver()
	 */
	@Override
	@Bean
	public LocaleResolver localeResolver() {		
		SessionCookieLocaleResolver localeResolver = new SessionCookieLocaleResolver();
			localeResolver.setDefaultLocale( getDefaultlocale());
		return localeResolver;
	}	
	// --------------------------------------------------------------------------------------------
}
