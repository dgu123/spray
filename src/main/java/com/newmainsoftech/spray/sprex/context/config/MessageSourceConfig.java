package com.newmainsoftech.spray.sprex.context.config;

import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

@Configuration
public class MessageSourceConfig implements MessageSourceConfigCase {
	public final String PropertyDefaultEncoding = "UTF-8";
		/* (non-Javadoc)
		 * @see com.newmainsoftech.spray.sprex.context.config.MessageSourceConfigCase#getPropertyDefaultEncoding()
		 */
		@Override
		public String getPropertyDefaultEncoding() {
			return PropertyDefaultEncoding;
		}
	public final ArrayList<String> MessageSourceBaseNameList 
	= new ArrayList<String>( Arrays.asList( "/WEB-INF/i18n/application", "/WEB-INF/i18n/messages"));		
		/* (non-Javadoc)
		 * @see com.newmainsoftech.spray.sprex.context.config.MessageSourceConfigCase#getMessageSourceBaseNames()
		 */
		@Override
		public String[] getMessageSourceBaseNames() {
			return MessageSourceBaseNameList.toArray( new String[]{});
		}
	
	/* (non-Javadoc)
	 * @see com.newmainsoftech.spray.sprex.context.config.MessageSourceConfigCase#messageSource()
	 */
	@Override
	@Bean
	public AbstractMessageSource messageSource() {
		ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource = new ReloadableResourceBundleMessageSource();
			reloadableResourceBundleMessageSource.setDefaultEncoding( getPropertyDefaultEncoding());
			reloadableResourceBundleMessageSource.setBasenames( getMessageSourceBaseNames());
		return reloadableResourceBundleMessageSource;
	}
}
