package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.resource.DefaultServletHttpRequestHandler;

import com.newmainsoftech.spray.sprex.web.servlet.config.ViewResolverConfig.SupportingMediaType;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration( value="/src/test/webapp")
public class ViewResolverConfigTest {
	@Configuration
	@Import( value={ ViewResolverConfigTestConfig.class, ViewResolverConfig.SupportingMediaType.class})
	public static class TestConfiguration extends WebMvcConfigurationSupport {
		Logger logger = LoggerFactory.getLogger( TestConfiguration.class);
			protected Logger getLogger() {
				return logger;
			}

		// Configure default Servlet mapping ----------------------------------------------------------
			// Since SpringWebAppContextConfig setup default Servlet mapping.
		@Override
		protected void configureDefaultServletHandling( DefaultServletHandlerConfigurer configurer) {
			super.configureDefaultServletHandling(configurer);
			configurer.enable();
		}
		// --------------------------------------------------------------------------------------------
		
		// For resolving view -------------------------------------------------------------------------
		@Autowired ViewResolverConfigCase.SupportingMediaTypeCase supportingMediaType;
			protected ViewResolverConfigCase.SupportingMediaTypeCase getSupportingMediaType() {
				Assert.assertNotNull( 
						String.format(
								"%1$s bean has not been injected to supportingMediaType member field of %2$s.",
								SupportingMediaType.class.getName(),
								this.getClass().getName()
								),
						supportingMediaType);
				return supportingMediaType;
			}
	
		@Autowired ViewResolverConfigCase viewResolverConfig;
			protected ViewResolverConfigCase getViewResolverConfig() {
				Assert.assertNotNull( 
						String.format(
								"%1$s bean has not been injected to viewResolverConfig member field of %2$s.", 
								ViewResolverConfigTestConfig.class.getName(),
								this.getClass().getName()
								),
						viewResolverConfig);
				return viewResolverConfig;
			}

		@Override
		protected void addViewControllers( ViewControllerRegistry registry) {
			getViewResolverConfig().addViewControllers( registry);

			super.addViewControllers( registry);
		}
		
		@Override
		protected void configureContentNegotiation( ContentNegotiationConfigurer configurer) {
			super.configureContentNegotiation( configurer);
			
			configurer.mediaTypes( getSupportingMediaType().getMediaTypes());
			
			configurer.favorParameter( true);
				configurer.parameterName( getSupportingMediaType().getMediaTypeParameterName());
					// Must manually set parameteName when turn on favorParameter even JavaDoc of parameterName method 
					// said like 'the default parameter name is "format"'. Otherwise, will hit the next exception:
					// IllegalArgumentException: Parameter name must not be null
			configurer.ignoreAcceptHeader( true);
				// Avoid fall back to use Accept header because situation around Accept header is really chaotic. 
				// For more info, refer to http://www.gethifi.com/blog/browser-rest-http-accept-headers 
				// and http://lists.webkit.org/pipermail/webkit-dev/2010-January/011188.html
				// Extension-less URL will be always interpreted as .jsp.
		}
		// --------------------------------------------------------------------------------------------	
	}
	
	
	MockMvc mockMvc;
		MockMvc getMockMvc() {
			return mockMvc;
		}
		void setMockMvc( final MockMvc mockMvc) {
			this.mockMvc = mockMvc;
		}
	
	@Autowired WebApplicationContext webApplicationContext;
		WebApplicationContext getWebApplicationContext() {
			return webApplicationContext;
		}
			
	@Before
	public void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup( getWebApplicationContext()).build();
	}
	
	/**
	 * Test for immediate forwarding of static case when no controller involvement is necessary. 
	 * 
	 * @throws Throwable
	 */
	@Test
	public void test_addViewControllers() throws Throwable {
/*		String staticTestViewName = ViewResolverConfigTestConfig.getStaticTestViewName();
			if ( !staticTestViewName.startsWith( "/")) staticTestViewName = "/".concat( staticTestViewName);
		
		String staticTestViewFileContent = "";
			String staticTestViewPath = ViewResolverConfigTestConfig.getStaticTestViewPath();
				File viewFile = ResourceUtils.getFile( this.getClass().getResource( staticTestViewPath));
					Assert.assertTrue(
							String.format( "Could not locate %1$s file.", staticTestViewPath),
							viewFile.exists());
				FileInputStream fileInputStream = new FileInputStream( viewFile);
				InputStreamReader inputStreamReader = new InputStreamReader( fileInputStream, "UTF-8");
				BufferedReader bufferedReader = new BufferedReader( inputStreamReader);
					for( String redLine = ""; redLine != null; redLine = bufferedReader.readLine()) {
						staticTestViewFileContent = staticTestViewFileContent + redLine;
					} // for
					bufferedReader.close();
			
		getMockMvc()
		.perform( 
				MockMvcRequestBuilders.get( staticTestViewName)
				.characterEncoding( "UTF-8")
				.locale( Locale.ENGLISH).param( "locale", "ja")
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.view().name( ViewResolverConfigTestConfig.getStaticTestViewName()))
		.andExpect( MockMvcResultMatchers.content().xml( staticTestViewFileContent));		
*/		
		getMockMvc()
		.perform( 
				MockMvcRequestBuilders.get( ViewResolverConfigTestConfig.getTestStaticResourcePath())
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.handler().handlerType( ParameterizableViewController.class))
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.forwardedUrl( ViewResolverConfigTestConfig.getTestStaticResourcePath()))
		.andExpect( MockMvcResultMatchers.view().name( ViewResolverConfigTestConfig.getStaticTestViewName()));		
	}
	
	@Autowired TestConfiguration testConfiguration;
		TestConfiguration getTestConfiguration() {
			return testConfiguration;
		}
		
	/**
	 * Test of handling request for not existing page.
	 * 
	 * @throws Throwable
	 */
	@Test
	public void test_not_existing_page_case() throws Throwable {
		final String staticTestViewName = "/Not_existing_view_for_".concat( Thread.currentThread().getStackTrace()[ 1].getMethodName());
		
		getMockMvc()
		.perform( 
				MockMvcRequestBuilders.get( staticTestViewName)
				.characterEncoding( "UTF-8")
				.locale( Locale.ENGLISH).param( "locale", "ja")
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.handler().handlerType( DefaultServletHttpRequestHandler.class))
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.forwardedUrl( "default"));
	}
	
	@Test
	public void test_InternalResourceViewResolver() throws Throwable {
		String testViewName = ViewResolverConfigTestConfig.getTestUrlForResolvingInternalResourceView();
			if ( testViewName.startsWith( "/")) {
				testViewName = testViewName.substring( 1);
			}
		String internalResourcePrefix 
		= getTestConfiguration().getViewResolverConfig().getInternalResourcePrefix();
			if ( !internalResourcePrefix.endsWith( "/")) {
				internalResourcePrefix = internalResourcePrefix.concat( "/");
			}
		final String internalResourceSuffix 
		= getTestConfiguration().getViewResolverConfig().getInternalResourceSuffix();
		final String forwardedUrl 
		= internalResourcePrefix.concat( testViewName).concat( internalResourceSuffix);
		
		getMockMvc()
		.perform( 
				MockMvcRequestBuilders.get( "/".concat( testViewName))
				.characterEncoding( "UTF-8")
				.locale( Locale.ENGLISH).param( "locale", "ja")
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.forwardedUrl( forwardedUrl))
		.andExpect( MockMvcResultMatchers.view().name( testViewName));		
	}
	
	@Test
	public void test_BeanNameViewResolver_via_ViewResolverDefinitionInjector() throws Throwable {
		final String testUrlForBeanNameViewResolverView 
		= ViewResolverConfigTestConfig.getTestUrlForBeanNameViewResolverView();
		final String testViewName
		= ViewResolverConfigTestConfig.getTestBeanNameView();
		final String forwardUrlByTestBeanNameView 
		= ViewResolverConfigTestConfig.getForwardUrlByTestBeanNameView();
		
		getMockMvc()
		.perform( 
				MockMvcRequestBuilders.get( testUrlForBeanNameViewResolverView)
				.characterEncoding( "UTF-8")
				.locale( Locale.ENGLISH).param( "locale", "ja")
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.forwardedUrl( forwardUrlByTestBeanNameView))
		.andExpect( MockMvcResultMatchers.view().name( testViewName));		
	}
	@Test
	public void test_XmlViewResolver_via_ViewResolverDefinitionInjector() throws Throwable {
		final String testUrlForXmlViewResolver 
		= ViewResolverConfigTestConfig.getTestUrlForXmlViewResolver();
		final String forwardUrlByXmlView
		= ViewResolverConfigTestConfig.getForwardUrlByXmlView();
		final String testViewName
		= ViewResolverConfigTestConfig.getXmlViewBeanName();
		
		getMockMvc()
		.perform( 
				MockMvcRequestBuilders.get( testUrlForXmlViewResolver)
				.characterEncoding( "UTF-8")
				.locale( Locale.ENGLISH).param( "locale", "ja")
				.accept( MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
				)
		.andDo( MockMvcResultHandlers.print())
		.andExpect( MockMvcResultMatchers.status().isOk())
		.andExpect( MockMvcResultMatchers.forwardedUrl( forwardUrlByXmlView))
		.andExpect( MockMvcResultMatchers.view().name( testViewName));		
	}
}
