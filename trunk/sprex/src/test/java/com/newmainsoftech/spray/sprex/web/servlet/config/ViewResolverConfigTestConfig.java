package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.io.File;
import java.io.FileInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.Assert;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.InternalResourceView;

@Configuration class ViewResolverConfigTestConfig extends ViewResolverConfig {
	// For testing of resolving view for static resource ------------------------------------------
	private final static String TestUrlForResolvingStaticView = "/static/**/*";
		public static String getTestUrlForResolvingStaticView() {
			return TestUrlForResolvingStaticView;
		}
	private final static String StaticTestViewPath = "/static/image/WebCharImageAnimation1a.gif";
		public static String getTestStaticResourcePath() {
			Assert.assertNotNull( 
					String.format(
							"Failure in test preparation: %1$s class needs update due to not be able to " 
							+ "locate %2$s file as a test resouce.",
							ViewResolverConfigTestConfig.class.getName(),
							StaticTestViewPath
							),
					ViewResolverConfigTestConfig.class.getResource( StaticTestViewPath));

			return StaticTestViewPath;
		}
	public static String getStaticTestViewName() {
		return "staticResourceView";
	}
	// --------------------------------------------------------------------------------------------		
	// For testing BeanNameViewResolver -----------------------------------------------------------
	final static String TestBeanNameView = "testBeanNameView";
		public static String getTestBeanNameView() {
			return TestBeanNameView;
		}
	final static String TestUrlForBeanNameViewResolverView = "/".concat( getTestBeanNameView());
		public static String getTestUrlForBeanNameViewResolverView() {
			return ViewResolverConfigTestConfig.TestUrlForBeanNameViewResolverView;
		}
	final static String ForwardUrlByTestBeanNameView = "/for_test_BeanNameViewResolver";
		public static String getForwardUrlByTestBeanNameView() {
			return ForwardUrlByTestBeanNameView;
		}
	@Bean
	public View testBeanNameView() {
		InternalResourceView internalResourceView 
		= new InternalResourceView( ViewResolverConfigTestConfig.getForwardUrlByTestBeanNameView());
		return internalResourceView;
	}
	// --------------------------------------------------------------------------------------------		
	// For testing XmlViewResolver ----------------------------------------------------------------	
	final static String ViewXmlLocation = "/WEB-INF/views_for_XmlViewResolver_configuration_test.xml";
		public static String getViewXmlLocation() {
			return ViewResolverConfigTestConfig.ViewXmlLocation;
		}
	final static String TestUrlForXmlViewResolver = "/xmlView";
		public static String getTestUrlForXmlViewResolver() {
			return TestUrlForXmlViewResolver;
		}
	public static String getXmlViewBeanName() {
		try {
			String xmlViewBeanName = null;
				File viewXmlFile = new File( ViewResolverConfigTestConfig.class.getResource( getViewXmlLocation()).toURI());
				FileInputStream fileInputStream = new FileInputStream( viewXmlFile);
				XMLInputFactory xMLInputFactory = XMLInputFactory .newInstance();
				XMLEventReader xMLEventReader = xMLInputFactory.createXMLEventReader( fileInputStream, "UTF-8");
				EventFilter eventFilter = new EventFilter() {
					@Override
					public boolean accept( XMLEvent event) {
						if ( !event.isStartElement()) return false;
						if ( "bean".equalsIgnoreCase( event.asStartElement().getName().getLocalPart())) return true;
						return false;
					}
				};
				xMLEventReader = xMLInputFactory.createFilteredReader( xMLEventReader, eventFilter);
				while( xMLEventReader.hasNext()) {
					XMLEvent xMLEvent = xMLEventReader.nextEvent();
					StartElement startElement = xMLEvent.asStartElement();
						Attribute attribute = startElement.getAttributeByName( new QName( "abstract"));
							if ( ( attribute != null) && "true".equalsIgnoreCase( attribute.getValue())) {
								continue; // while
							}
						attribute = startElement.getAttributeByName( new QName( "id"));
							if ( attribute != null) {
								xmlViewBeanName = attribute.getValue();
								break; // while
							}
						attribute = startElement.getAttributeByName( new QName( "name"));
							if ( attribute != null) {
								xmlViewBeanName = attribute.getValue();
								break; // while
							}
				} // while
			Assert.assertNotNull( 
					String.format(
							"%1$s view xml definition file is invalid: could not find valid bean element in it.",
							getViewXmlLocation()
							), 
					xmlViewBeanName);
			return xmlViewBeanName;
		}
		catch( Exception exception) {
			if ( exception instanceof RuntimeException) throw (RuntimeException)exception;
			else throw new RuntimeException( exception);
		}
	}
	public final static String ForwardUrlByXmlView = "/WEB-INF/XmlView/testXmlViewResolver.html";
		public static String getForwardUrlByXmlView() {
			return ForwardUrlByXmlView;
		}
	// --------------------------------------------------------------------------------------------		
	// Injecting BeanNameViewResolver and XmlViewResolver -----------------------------------------		
	@Override
	@Bean
	public ViewResolverDefinitionInjectorCase viewResolverDefinitionInjector() {
		ViewResolverDefinitionInjector viewResolverDefinitionInjector = new ViewResolverDefinitionInjector();
			viewResolverDefinitionInjector.setApplicationContext( getApplicationContext());
			viewResolverDefinitionInjector.setViewXmlLocation( getViewXmlLocation());
			viewResolverDefinitionInjector.addViewResolverDefinition();
		return viewResolverDefinitionInjector;
	}
	// --------------------------------------------------------------------------------------------	
	// For testing of resolving internal resource view --------------------------------------------	
	final static String TestUrlForResolvingInternalResourceView = "/for_test_InternalResourceViewResolver";
		public static String getTestUrlForResolvingInternalResourceView() {
			return ViewResolverConfigTestConfig.TestUrlForResolvingInternalResourceView;
		}
		
	@Override
	public String getInternalResourcePrefix() {
		return super.getInternalResourcePrefix();
	}

	@Override
	public String getInternalResourceSuffix() {
		return super.getInternalResourceSuffix();
	}
	// --------------------------------------------------------------------------------------------	

	@Override
	public void addViewControllers( ViewControllerRegistry registry) {
		Logger logger = getLogger();

		// Add static cases of request mapping (what do not involve no controller logic) here like next example:
		registry
		.addViewController( ViewResolverConfigTestConfig.getTestUrlForResolvingStaticView())
		.setViewName( ViewResolverConfigTestConfig.getStaticTestViewName());
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"To test resolving view for static resource, registered a \"%1$s\" " 
								+ "controller (named as \"%2$s\") for \"%3$s\" view.",
								ParameterizableViewController.class.getSimpleName(),
								ViewResolverConfigTestConfig.getTestUrlForResolvingStaticView(), 
								ViewResolverConfigTestConfig.getStaticTestViewName()
								));
			}
		registry
		.addViewController( ViewResolverConfigTestConfig.getTestUrlForBeanNameViewResolverView())
		.setViewName( ViewResolverConfigTestConfig.getTestBeanNameView());
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"To test resolving views by BeanNameViewResolver, registered " 
								+ "a \"%1$s\" controller (named as \"%2$s\") for \"%3$s\" view.",
								ParameterizableViewController.class.getSimpleName(),
								ViewResolverConfigTestConfig.getTestUrlForBeanNameViewResolverView(), 
								ViewResolverConfigTestConfig.getTestBeanNameView()
								));
			}
		registry
		.addViewController( ViewResolverConfigTestConfig.getTestUrlForXmlViewResolver())
		.setViewName( ViewResolverConfigTestConfig.getXmlViewBeanName());
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"To test resolving views by XmlViewResolver, registered " 
								+ "a \"%1$s\" controller (named as \"%2$s\") for \"%3$s\" view.",
								ParameterizableViewController.class.getSimpleName(),
								ViewResolverConfigTestConfig.getTestUrlForXmlViewResolver(), 
								ViewResolverConfigTestConfig.getXmlViewBeanName()
								));
			}
		registry
		.addViewController( ViewResolverConfigTestConfig.getTestUrlForResolvingInternalResourceView());
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"To test resolving views by InternalResourceViewResolver, " 
								+ "registered a \"%1$s\" controller (named as \"%2$s\").",
								ParameterizableViewController.class.getSimpleName(),
								ViewResolverConfigTestConfig.getTestUrlForResolvingInternalResourceView() 
								));
			}
	}
}