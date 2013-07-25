package com.newmainsoftech.spray.sprex.test.config;

import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;

public class SpringIntegrationTestWebAppInitializer implements WebApplicationInitializer {
	@Override
	public void onStartup( ServletContext servletContext) throws ServletException {
		// If there is any filter or listener to add, then add those to servletContext here
		servletContext.addListener( RequestContextListener.class);
		FilterRegistration.Dynamic characterEncodingFilter
		= servletContext.addFilter( 
				CharacterEncodingFilter.class.getSimpleName(), CharacterEncodingFilter.class);
			characterEncodingFilter.setInitParameter( "encoding", "UTF-8");
			
		// If some mapping needs to be configured, then add those to servletContext here
		
		// Configure Spring's DispatcherServlet 
			// AnnotationConfigWebApplicationContext allows for seamlessly bootstrapping @Configuration classes
			final AnnotationConfigWebApplicationContext dispatcherServletContext 
			= new AnnotationConfigWebApplicationContext();
				dispatcherServletContext.setServletContext( servletContext);
				dispatcherServletContext.register( SpringIntegrationTestDispatcherServletConfig.class);
				dispatcherServletContext.refresh();
			
	        // Load Spring DispatcherServlet (just like before) with passing the just built
	        // application context. 
	        final ServletRegistration.Dynamic dispatcherServlet 
	        = servletContext.addServlet( "spring", new DispatcherServlet( dispatcherServletContext));
	        	dispatcherServlet.setLoadOnStartup(1);
	        	dispatcherServlet.addMapping( "/");  // Make sure this is NOT "/*"!
	}
}
