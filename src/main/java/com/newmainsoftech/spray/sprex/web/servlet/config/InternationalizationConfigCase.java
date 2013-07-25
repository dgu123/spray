package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public interface InternationalizationConfigCase {
	public abstract String getLocaleParamName();
	@Bean
	public LocaleChangeInterceptor localeChangeInterceptor();

	public abstract Locale getDefaultlocale();
	@Bean
	public LocaleResolver localeResolver();

}