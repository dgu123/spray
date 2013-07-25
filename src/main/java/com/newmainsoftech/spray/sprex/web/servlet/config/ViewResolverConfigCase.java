package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.XmlViewResolver;

public interface ViewResolverConfigCase {
	public static enum ViewResolverConfigCommonDefualtValue {
		InternalResourcePrefix( "/WEB-INF/jsp/"),
		InternalResourceSuffix( ".jsp"),
		StaticResourceRootPath( "/static"),
		StaticResourceViewName( "staticResourceView"),
		ViewXmlLocationPath( XmlViewResolver.DEFAULT_LOCATION);
		
		private final Object value;
			public Object getValue() {
				return value;
			}
		
		private ViewResolverConfigCommonDefualtValue( final Object value) {
			this.value = value;
		}
	}
	
	/**
	 * @return Path from app's root to static resource folder. Will be give to  
	 */
	abstract String getStaticResourceRootPath();
	
	/**
	 * This method is to let the <code>{@link Configuration @Configuration}</code> implementation with this interface  
	 * have ability to add static cases of request mapping (what do not involve no controller logic). <br />
	 * So that, this may help to concentrate view related configuration to the <code>@Configuration</code> 
	 * implementation with this interface.<br />
	 * In order to let the <code>@Configuration</code> implementation with this interface have the ability, 
	 * such <code>@Configuration</code> implementation needs to be imported to sub-class of 
	 * <code>{@link WebMvcConfigurationSupport}</code>, and this method must be called (directly or indirectly) at 
	 * inside of overridden addViewControllers method of that sub-class.  
	 * 
	 * @param registry
	 */
	public void addViewControllers( ViewControllerRegistry registry);
	
	// For InternalResourceViewResolver -----------------------------------------------------------
	/**
	 * @return Prefix string being given to <code>{@link InternalResourceViewResolver#setPrefix(String)}</code> method.
	 */
	abstract String getInternalResourcePrefix();
	/**
	 * @return Suffix string being given to <code>{@link InternalResourceViewResolver#setSuffix(String)}</code> method.
	 */
	abstract String getInternalResourceSuffix();
	// --------------------------------------------------------------------------------------------
	
	// For ContentNegotiatingViewResolver ---------------------------------------------------------
	/**
	 * Interface for holder of Media types what <code>{@link ViewResolverConfigCase}</code> implementing class supports. <br />
	 * The autowiring order of <code>ViewResolverConfigCase</code> implementation class and <code>SupportingMediaTypeCase</code> 
	 * implementation class in {@link Configuration @Configuration} implementation class somewhat important: 
	 * <code>SupportingMediaTypeCase</code> implementation class must be autowired ahead in your code of 
	 * <code>&amp;Configuration</code> implementation class. Otherwise, <code>NullPointerException</code>  
	 * may be thrown in autowiring <code>{@link ContentNegotiationManager}</code> bean. In other words, if your 
	 * <code>ViewResolverConfigCase</code> implementation class configure <code>{@link ContentNegotiatingViewResolver}</code> 
	 * bean, then <code>SupportingMediaTypeCase</code> bean must complete autowired and becomes available before autowiring 
	 * <code>ViewResolverConfigCase</code> bean in your <code>&amp;Configuration</code> implementation class.
	 * 
	 * @author Arata.Y
	 */
	public static interface SupportingMediaTypeCase {
		public static enum SupportingMediaTypeCommonDefualtValue {
			MediaTypeParameterName( "format");
			
			private final String value;
				public String getValue() {
					return value;
				}
			private SupportingMediaTypeCommonDefualtValue( final String value) {
				this.value = value;
			}			
		}
		
		/**
		 * This method is to let the <code>{@link Configuration @Configuration}</code> 
		 * implementation with this interface have ability to add supporting media types to 
		 * <code>{@link ContentNegotiationConfigurer}</code>. <br />
		 * So that, this may help to concentrate view related configuration to 
		 * the <code>@Configuration</code> implementation with this interface.<br />
		 * In order to let the <code>@Configuration</code> implementation with this interface 
		 * has the ability, such <code>@Configuration</code> implementation needs to be imported 
		 * to sub-class of <code>{@link WebMvcConfigurationSupport}</code>, and this method must be 
		 * call (directly or indirectly) at inside of overridden configureContentNegotiation method of 
		 * that sub-class.
		 * 
		 * @return map object for the pair of file extension as key and media type as value.
		 */
		public Map<String, MediaType> getMediaTypes();
		
		/**
		 * @return Parameter name being given to 
		 * <code>{@link ContentNegotiationConfigurer#parameterName(String)}</code> method.
		 */
		abstract String getMediaTypeParameterName();		
	}
	// --------------------------------------------------------------------------------------------	
}
