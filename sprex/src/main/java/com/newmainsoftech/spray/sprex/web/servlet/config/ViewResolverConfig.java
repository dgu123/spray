package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.ContentNegotiatingViewResolver;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.XmlViewResolver;

/**
 * <code>{@link Configuration @Configuration} implementation for view resolver configuration.</code> <br />
 * This is for configuring the following view resolvers:
 * <ul>
 * <li><code>{@link ContentNegotiatingViewResolver}</code>
 * </li>
 * <li><code>{@link StaticViewResolver}</code>
 * </li>
 * <li><code>{@link XmlViewResolver}</code>
 * </li>
 * <li><code>{@link InternalResourceViewResolver}</code><br />
 * (Please note that you can have only one <code>InternalResourceViewResolver</code> bean per application.) 
 * </li>
 * </ul>
 * 
 * @author Arata.Y
 */
@Configuration
public class ViewResolverConfig implements ViewResolverConfigCase, ApplicationContextAware {
	Logger logger = LoggerFactory.getLogger( this.getClass());
		protected Logger getLogger() {
			return logger;
		}

	public static enum ViewResolverOrder {
		ContentNegotiatingViewResolverOrder( ContentNegotiatingViewResolver.HIGHEST_PRECEDENCE);
		
		private int order;
			public int getOrder() {
				return order;
			}

		private ViewResolverOrder( int order) {
			this.order = order;
		}
		
		/**
		 * For order attribute value among view beans. <br />
		 * Higher the value, later will be the position of the vew resolver in chain. 
		 */
		private static int lastOrder = -1;
			/**
			 * @return Next order number what ViewResolver can use without conflicting with other ViewResolver.
			 */
			public synchronized static int consumeOrder() {
				if ( lastOrder < 0) {
					lastOrder = ViewResolverOrder.values().length + ContentNegotiatingViewResolver.HIGHEST_PRECEDENCE;
				}
				return lastOrder++;
			}
	}
	
	// For resolving view for static resources ----------------------------------------------------
	protected final static String StaticResourceRootPath 
	= (String)(ViewResolverConfigCommonDefualtValue.StaticResourceRootPath.getValue());
		/**
		 * Override this to specify path to root folder of static resources other than default one 
		 * what is {@value #StaticResourceRootPath}. 
		 * 
		 * @return Path to root folder of static resources what will be used in {@link StaticViewResolver} 
		 * bean to identify and yield view for static resource request.
		 */
		@Override
		public String getStaticResourceRootPath() {
			return ViewResolverConfig.StaticResourceRootPath;
		}
	
	/**
	 * @return <code>{@link StaticViewResolver}</code> bean what will be used to resolve the view for 
	 * static resources under the path being returned by <code>{@link getStaticResourceRootPath()}</code>.
	 */
	@Bean
	public StaticViewResolver staticViewResolver() {
		final StaticViewResolver staticViewResolver = new StaticViewResolver();
			staticViewResolver.setOrder( ViewResolverOrder.consumeOrder());
			staticViewResolver.setStaticResourceRootPath( getStaticResourceRootPath());
		return staticViewResolver;
	}
		
	/**
	 * Registers request mapping for static case (what do not involve no controller logic). <br />
	 * In this default implementation, register a view controller for static resources under the path 
	 * being returned by <code>{@link #getStaticResourceRootPath()}</code> method with using 
	 * the value of <code>{@link ViewResolverConfigCommonDefualtValue.StaticResourceViewName}</code>  
	 * as the view name.<br />
	 * If that is not preferable, then override this.
	 * 
	 * @param registry
	 */
	@Override
	public void addViewControllers( ViewControllerRegistry registry) {
		String staticResourceRootPath = getStaticResourceRootPath();
		if ( ( staticResourceRootPath != null) && ( staticResourceRootPath.length() > 0)) {
			if ( !staticResourceRootPath.endsWith( "*")) {
				if ( staticResourceRootPath.lastIndexOf( "/") >= staticResourceRootPath.lastIndexOf( ".")) {
					if ( !staticResourceRootPath.endsWith( "/")) {
						staticResourceRootPath = staticResourceRootPath.concat( "/");
					}
					staticResourceRootPath = staticResourceRootPath.concat( "**/*");
				}
			}
			registry
			.addViewController( staticResourceRootPath)
			.setViewName( 
					ViewResolverConfigCommonDefualtValue.StaticResourceViewName.getValue().toString());
			
			Logger logger = getLogger();
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"To resolve view for static resource, registered a %1$s " 
								+ "controller (named as \"%2$s\") for \"%3$s\" view.",
								ParameterizableViewController.class.getSimpleName(),
								staticResourceRootPath, 
								ViewResolverConfigCommonDefualtValue
								.StaticResourceViewName.getValue().toString()
								));
			}
		}
	}
	// --------------------------------------------------------------------------------------------	
	
	@Autowired ApplicationContext applicationContext;
		@Override
		public void setApplicationContext( ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}
		protected ApplicationContext getApplicationContext() {
			Assert.notNull( 
					applicationContext, 
					"Application context object has not been injected (autowired.)");
			return applicationContext;
		}
		protected WebApplicationContext getWebApplicationContext() {
			ApplicationContext applicationContext = getApplicationContext();
			Assert.isTrue(
					(applicationContext instanceof WebApplicationContext),
					String.format(
							"Injected application context is not %1$s type.",
							WebApplicationContext.class.getSimpleName())
					);
			return (WebApplicationContext)applicationContext;
		}		
	
	// For other view resolvers -------------------------------------------------------------------
	/**
	 * Instantiating <code>{@link ViewResolverDefinitionInjectorCase}</code> bean adds other view resolvers to 
	 * application context.<br />
	 * The <code>{@link ViewResolverDefinitionInjector}</code> (default implementation of 
	 * <code>ViewResolverDefinitionInjectorCase</code> interface) will add the following view resolver beans: 
	 * <ul>
	 * <li><code>{@link BeanNameViewResolver}</code> bean<br />
	 * Adding <code>BeanNameViewResolver</code> bean by default because, as nature of JavaConfig, 
	 * <code>BeanNameViewResolver</code> became to make more sense than XML config era. <br />
	 * Although, please be advised that <code>BeanNameViewResolver</code> itself is not aware of i18n 
	 * support - thereby, it must be done by view level or controller level. 
	 * </li>
	 * <li><code>{@link XmlViewResolver}</code> bean<br />
	 * Adds XmlViewResolver bean only when view XML file exists. For more info, refer to  
	 * <code>{@link ViewResolverDefinitionInjector}</code> class.  
	 * </li> 
	 * </ul>
	 * If those do not fit to your preference, then override this method.   
	 * 
	 * @return Dummy bean instantiated for purpose of adding other view resolver beans to 
	 * application context.
	 */
	@Bean
	public ViewResolverDefinitionInjectorCase viewResolverDefinitionInjector() {
		ViewResolverDefinitionInjector viewResolverDefinitionInjector = new ViewResolverDefinitionInjector();
			viewResolverDefinitionInjector.setApplicationContext( getApplicationContext());
			viewResolverDefinitionInjector.addViewResolverDefinition();
		return viewResolverDefinitionInjector;
	}
	// --------------------------------------------------------------------------------------------	
		
	// For InternalResourceViewResolver -----------------------------------------------------------
	protected final static String InternalResourcePrefix 
	= (String)(ViewResolverConfigCommonDefualtValue.InternalResourcePrefix.getValue());
		public static String staticGetInternalResourcePrefix() {
			return InternalResourcePrefix;
		}
		@Override
		public String getInternalResourcePrefix() {
			return ViewResolverConfig.staticGetInternalResourcePrefix();
		}
	protected final static String InternalResourceSuffix 
	= (String)(ViewResolverConfigCommonDefualtValue.InternalResourceSuffix.getValue());
		public static String staticGetInternalResourceSuffix() {
			return InternalResourceSuffix;
		}
		@Override
		public String getInternalResourceSuffix() {
			return ViewResolverConfig.staticGetInternalResourceSuffix();
		}
		
	@Bean
	public static InternalResourceViewResolver internalResourceViewResolver() {
		// InternalResourceViewResolver will attempt to generate view no matter wheter the view exists. 
		// Always important to configure a generic page to handle type of error of 404 error.
		InternalResourceViewResolver internalResourceViewResolver = new InternalResourceViewResolver();
//			internalResourceViewResolver.setOrder( ContentNegotiatingViewResolver.LOWEST_PRECEDENCE);
				// No need to explicitly set lowest order value because InternalResourceViewResolver always 
				// automatically positioned as the latest resolver in the chain.
			internalResourceViewResolver.setPrefix( staticGetInternalResourcePrefix());
			internalResourceViewResolver.setSuffix( staticGetInternalResourceSuffix());
			internalResourceViewResolver.setExposeContextBeansAsAttributes( true);
				//TODO: I need to evaluate security precaution for turning on context bean exposure as attribute.
		return internalResourceViewResolver;
	}
	// --------------------------------------------------------------------------------------------
	
	// For ContentNegotiatingViewResolver ---------------------------------------------------------	
	/**
	 * Holder of Media types what <code>{@link ViewResolverConfig}</code> supports. <br />
	 * This class is just for avoiding failure in instantiating <code>ViewResolverConfig</code bean by 
	 * circular reference with <code>{@link ViewResolverConfig#mvcContentNegotiationManager}</code> in 
	 * app's configuring {@link ContentNegotiationManager} object. <br />
	 * The autowiring order of <code>ViewResolverConfig</code> and <code>SupportingMediaType</code> 
	 * somewhat important: <code>SupportingMediaType</code> must be autowired ahead in your code of 
	 * <code>{@link Configuration @Configuration}</code> implementation class. Otherwise, 
	 * <code>NullPointerException</code> may be thrown in autowiring 
	 * <code>ViewResolverConfig.mvcContentNegotiationManager</code>.
	 * 
	 * @author Arata.Y
	 */
	@Configuration
	public static class SupportingMediaType implements SupportingMediaTypeCase {
		@Override
		public Map<String, MediaType> getMediaTypes() {
			// Add app's supporting media types here
			return null;
		}

		@Override
		public String getMediaTypeParameterName() {
			return SupportingMediaTypeCommonDefualtValue.MediaTypeParameterName.getValue();
		}
	}
	
//	@Autowired ContentNegotiationManager mvcContentNegotiationManager;
		/* ContentNegotiationManager instance bean cannot be injected to mvcContentNegotiationManager 
		 * member field but contentNegotiationManager member field. Although it is strange because  
		 * the name of the ContentNegotiationManager instance bean should be mvcContentNegotiationManager 
		 * as looking at the source code of WebMvcConfigurationSupport class.
		 */
	@Autowired ContentNegotiationManager contentNegotiationManager;
		ContentNegotiationManager getMvcContentNegotiationManager() {
//			return mvcContentNegotiationManager;
/*			
			ContentNegotiationManager contentNegotiationManager
			= (ContentNegotiationManager)getApplicationContext().getAutowireCapableBeanFactory()
//				.getBean( ContentNegotiationManager.class);
				.getBean( "mvcContentNegotiationManager");
getLogger().info( 
		String.format(
				"Type of mvcContentNegotiationManager = %1$s",
				getApplicationContext().getAutowireCapableBeanFactory()
				.getBean( "mvcContentNegotiationManager").getClass().getName()
				)
		);
*/
			return contentNegotiationManager;
		}
		/* Instead of injecting mvcContentNegotiationManager bean, it may be better to inject 
		 * requestMappingHandlerMapping bean and get ContentNegotiationManager instance via  
		 * RequestMappingHandlerMapping.getContentNegotiationManager method.
		 */

	@Bean
	public ContentNegotiatingViewResolver contentNegotiatingViewResolver() {
		ContentNegotiatingViewResolver contentNegotiatingViewResolver 
		= new ContentNegotiatingViewResolver();
			contentNegotiatingViewResolver
			.setOrder( ViewResolverOrder.ContentNegotiatingViewResolverOrder.getOrder());
			contentNegotiatingViewResolver
			.setContentNegotiationManager( getMvcContentNegotiationManager());
			// If contentNegotiatingViewResolver's list of ViewResolver is not configured explicitly, 
			// it automatically uses any ViewResolvers defined in the application context.
		return contentNegotiatingViewResolver;
	}
	// --------------------------------------------------------------------------------------------	
}
