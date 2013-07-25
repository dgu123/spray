package com.newmainsoftech.spray.sprex.context.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.support.AbstractMessageSource;

public interface MessageSourceConfigCase {

	abstract String getPropertyDefaultEncoding();

	abstract String[] getMessageSourceBaseNames();

	@Bean
	AbstractMessageSource messageSource();
}