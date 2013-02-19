package com.newmainsoftech.spray.sprex.web.servlet.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionStoreException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.springframework.web.servlet.view.XmlViewResolver;

import com.newmainsoftech.spray.sprex.web.servlet.config.ViewResolverConfig.ViewResolverOrder;
import com.newmainsoftech.spray.sprex.web.servlet.config.ViewResolverConfigCase.ViewResolverConfigCommonDefualtValue;

/**
 * Default implementation of <code>{@link ViewResolverDefinitionInjectorCase}</code> interface to 
 * adds view resolver beans to application context.<br />
 * This is developed due to that {@link BeanDefinitionRegistryPostProcessor} and {@link PostConstruct} 
 * does not work (broken) with {@link Configuration @Configuration} of Spring' JavaConfig 
 * (but {@link BeanFactoryPostProcessor}.) For more info regarding this issue, refer to 
 * <ul>
 * <li></li>
 * </ul> 
 * <br />
 * This implementation add the following view resolver beans: 
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
 * @author Arata.Y
 */
public class ViewResolverDefinitionInjector implements ApplicationContextAware, ViewResolverDefinitionInjectorCase {
	Logger logger = LoggerFactory.getLogger( this.getClass());
		protected Logger getLogger() {
			return logger;
		}
		
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
	
	protected void addBeanNameViewResolverDefinition( BeanDefinitionRegistry registry) {
		GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
			beanDefinition.setBeanClass( BeanNameViewResolver.class);
			beanDefinition.setScope( BeanDefinition.SCOPE_SINGLETON);
			beanDefinition.setAutowireCandidate( true);
			final MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
				int order = ViewResolverOrder.consumeOrder();
				mutablePropertyValues.add( "order", order);
				mutablePropertyValues.add( "applicationContext", getApplicationContext());
			beanDefinition.setPropertyValues( mutablePropertyValues);
		registry.registerBeanDefinition( BeanNameViewResolver.class.getSimpleName(), beanDefinition);
		
		Logger logger = getLogger();
			if ( logger.isInfoEnabled()) {
				logger.info(
						String.format(
								"Registered %1$s bean with the order number of %2$d.", 
								BeanNameViewResolver.class.getSimpleName(), 
								order
								)
						);
			}
	}
	
	public final static String DefaultViewXmlLocation 
	= (String)(ViewResolverConfigCommonDefualtValue.ViewXmlLocationPath.getValue());
		public static String getDefaultViewXmlLocation() {
			return DefaultViewXmlLocation;
		}
	protected String viewXmlLocation = ViewResolverDefinitionInjector.getDefaultViewXmlLocation();
		/**
		 * To retrieve class path string pointing view definition XML file to construct a {@link Resource} 
		 * object being given to <code>{@link XmlViewResolver#setLocation( org.springframework.core.io.Resource) 
		 * XmlViewResolver.setLocation}</code> method. 
		 * 
		 * @return Class path of view definition xml file being used by <code>{@link XmlViewResolver}</code> 
		 * bean. <br />
		 * Default location is &quot;{@value ViewResolverDefinitionInjector#DefaultViewXmlLocation}&quot;.
		 */
		public String getViewXmlLocation() {
			return viewXmlLocation;
		}
		/**
		 * Set class path string of view definition xml file being used by <code>{@link XmlViewResolver}</code> 
		 * bean. <br />
		 * Default location is {@value ViewResolverDefinitionInjector#DefaultViewXmlLocation}.
		 * @param viewXmlLocation
		 */
		public void setViewXmlLocation( String viewXmlLocation) {
			this.viewXmlLocation = viewXmlLocation;
		}
	
		protected boolean isViewXmlExisting() {
			final String viewXmlPath = getViewXmlLocation();
			final Resource viewXmlResource = getApplicationContext().getResource( viewXmlPath);
			if ( !viewXmlResource.exists()) return false;
			return true;
		}
	
	
	protected void addXmlViewResolverBeanDefinition( BeanDefinitionRegistry registry) {
		/* It may be preferable to use the combination of 
		 * {@link BeanFactoryPostProcessor#postProcessBeanFactory(ConfigurableListableBeanFactory)} and 
		 * applyLifecycleMethods method like one in {@link StaticViewResolver#applyLifecycleMethods(String, AbstractView)} 
		 * rather than registering bean definition via BeanDefinitionRegistry (what will result instantiating bean 
		 * via CGLIB). The reason is that registering bean definition may ends up to conflicts over multiple bean instances 
		 * of same type for the case that such multiple instances are anticipated.<br /> 
		 * But I must refresh the context manually if I use postProcessBeanFactory way, I think.
		 * 
		 */
		
		if ( isViewXmlExisting()) {	
			GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
				beanDefinition.setBeanClass( XmlViewResolver.class);
				beanDefinition.setScope( BeanDefinition.SCOPE_SINGLETON);
				beanDefinition.setAutowireCandidate( true);
				final MutablePropertyValues mutablePropertyValues = new MutablePropertyValues();
					int order = ViewResolverOrder.consumeOrder();
					mutablePropertyValues.add( "order", order);
					ClassPathResource classPathResource = new ClassPathResource( getViewXmlLocation());
					mutablePropertyValues.add( "location", classPathResource);
				beanDefinition.setPropertyValues( mutablePropertyValues);
			registry.registerBeanDefinition( XmlViewResolver.class.getSimpleName(), beanDefinition);
			
			Logger logger = getLogger();
				if ( logger.isInfoEnabled()) {
					logger.info(
							String.format(
									"Registered %1$s bean for [%2$s] view xml file with the order number of %3$d.", 
									XmlViewResolver.class.getSimpleName(), 
									getViewXmlLocation(),
									order
									)
							);
				}
		}
		else {
			final String viewXmlPath = getViewXmlLocation();			
			if ( viewXmlPath != null) {
				if ( !ViewResolverConfigCommonDefualtValue.ViewXmlLocationPath.getValue().equals( viewXmlPath)) {
					throw new XmlBeanDefinitionStoreException( 
							viewXmlPath, 
							String.format(
									"Error in creating a bean of %1$s instance: Although custom view XML file " 
									+ "(%2$s rather than default one) has been specified, that file does not exist.",
									XmlViewResolver.class.getSimpleName(), 
									viewXmlPath),
							null
							);
				}
				else {
					Logger logger = getLogger();
					if ( logger.isDebugEnabled()) {
						logger.debug(
								String.format(
										"Skiped to add a bean of %1$s instance since %2$s view XML file does not exist.",
										XmlViewResolver.class.getSimpleName(),
										viewXmlPath)
								);
					}
				}
			}
		}
	}
		
	/* (non-Javadoc)
	 * @see com.newmainsoftech.spray.sprex.web.servlet.config.ViewResolverDefinitionInjectorCase#addViewResolverDefinition(org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	public void addViewResolverDefinition( BeanDefinitionRegistry registry) {
		addBeanNameViewResolverDefinition( registry);
		addXmlViewResolverBeanDefinition( registry);
	}
	/* (non-Javadoc)
	 * @see com.newmainsoftech.spray.sprex.web.servlet.config.ViewResolverDefinitionInjectorCase#addViewResolverDefinition()
	 */
	@Override
	public void addViewResolverDefinition() {
		AutowireCapableBeanFactory autowireCapableBeanFactory = getApplicationContext().getAutowireCapableBeanFactory();
		BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry)autowireCapableBeanFactory;
		addViewResolverDefinition( beanDefinitionRegistry);
	}
}
