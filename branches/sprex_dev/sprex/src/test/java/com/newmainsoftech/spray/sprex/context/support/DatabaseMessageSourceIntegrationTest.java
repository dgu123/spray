package com.newmainsoftech.spray.sprex.context.support;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.newmainsoftech.spray.sprex.test.config.SpringIntegrationTestDispatcherServletConfig;

/**
 * For <code>{@link DatabaseMessageSource}</code> class, sanitize-level test of integration with 
 * spring:format of Spring's TLD.<br /> 
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
public class DatabaseMessageSourceIntegrationTest {
	Logger logger = LoggerFactory.getLogger( this.getClass());
		public Logger getLogger() {
			return logger;
		}

	public static final String TestServerPath = "http://localhost:8080";
		public static String getTestServerPath() {
			return TestServerPath;
		}
	
	@Test
	public void test_resolveCode() throws Throwable {
		final String testViewPath 
		= getTestServerPath().concat( SpringIntegrationTestDispatcherServletConfig.getTestViewName());
		final WebClient webClient = new WebClient();
			WebClientOptions webClientOptions = webClient.getOptions();
			webClientOptions.setRedirectEnabled( true);
			webClientOptions.setJavaScriptEnabled( true);
			webClientOptions.setThrowExceptionOnFailingStatusCode( true);
			webClientOptions.setThrowExceptionOnScriptError( true);
		HtmlPage htmlPage = webClient.getPage( testViewPath);
			Logger logger = getLogger();
			if ( logger.isDebugEnabled()) {
				logger.debug(
						String.format(
								"View returned for %1$s: %n%2$s",
								testViewPath,
								htmlPage.asXml())
						);
			}
			final String source = "/html/body/h1";
			List<DomNode> h1Nodes = (List<DomNode>)(htmlPage.getByXPath( source));
				Assert.assertEquals( 1, h1Nodes.size());
				Assert.assertEquals(  
						SpringIntegrationTestDispatcherServletConfig.getMessageMap().get( Locale.ENGLISH),
						h1Nodes.get( 0).getNodeValue()
						);
		
		URL testViewUrl = new URL( testViewPath);
		WebRequest webRequest = new WebRequest( testViewUrl);
			NameValuePair localeNameValuePair 
			= new NameValuePair( LocaleChangeInterceptor.DEFAULT_PARAM_NAME, Locale.JAPANESE.toString());
				// By setting this as a request parameter, Spring's LocaleChangeInterceptor gives  
				// locale to CookieLocaleResolver what then sets locale to request attribute and cookie.
			List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
				nameValuePairList.add( localeNameValuePair);
			webRequest.setRequestParameters( nameValuePairList);
		htmlPage = webClient.getPage( webRequest);
			if ( logger.isDebugEnabled()) {
				logger.debug(
						String.format(
								"View returned for %1$s, request parameter %2$s, cookies %3$s: %n%4$s",
								webRequest.toString(),
								nameValuePairList.toString(),
									// Manually dumping parameter because WebRequest's toString has bug.
								webClient.getCookieManager().getCookies().toString(),
								htmlPage.asXml())
						);
			}
			h1Nodes = (List<DomNode>)(htmlPage.getByXPath( source));
			Assert.assertEquals(  
					SpringIntegrationTestDispatcherServletConfig.getMessageMap().get( Locale.JAPANESE),
					h1Nodes.get( 0).getNodeValue()
					);
		
		webRequest = new WebRequest( testViewUrl);
			localeNameValuePair 
			= new NameValuePair( LocaleChangeInterceptor.DEFAULT_PARAM_NAME, Locale.JAPAN.toString());
				// By setting this as a request parameter, Spring's LocaleChangeInterceptor gives  
				// locale to CookieLocaleResolver what then sets locale to request attribute and cookie.
			nameValuePairList = new ArrayList<NameValuePair>();
				nameValuePairList.add( localeNameValuePair);
			webRequest.setRequestParameters( nameValuePairList);
		htmlPage = webClient.getPage( webRequest);
			if ( logger.isDebugEnabled()) {
				logger.debug(
						String.format(
								"View returned for %1$s, request parameter %2$s, cookies %3$s: %n%4$s",
								webRequest.toString(),
								nameValuePairList.toString(),
									// Manually dumping parameter because WebRequest's toString has bug.
								webClient.getCookieManager().getCookies().toString(),
								htmlPage.asXml())
						);
			}
			h1Nodes = (List<DomNode>)(htmlPage.getByXPath( source));
				Assert.assertEquals(  
						SpringIntegrationTestDispatcherServletConfig.getMessageMap().get( Locale.JAPAN),
						h1Nodes.get( 0).getNodeValue()
						);
	}
}
