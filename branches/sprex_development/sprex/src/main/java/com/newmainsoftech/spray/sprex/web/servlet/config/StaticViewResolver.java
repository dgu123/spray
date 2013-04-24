package com.newmainsoftech.spray.sprex.web.servlet.config;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;
import org.springframework.web.servlet.view.AbstractView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import com.newmainsoftech.spray.sprex.web.servlet.config.ViewResolverConfig.ViewResolverOrder;

/**
 * <code>{@link ViewResolver}</code> for static view (what do not involve no controller logic.)<br />
 * By default, returns <code>{@link StaticResourceView}</code> instance as view when adequate. <br />
 * Expected to be used with <code>{@link ParameterizableViewController}</code> bean what will 
 * be automatically utilized behind scene when view controller is added in <code>addViewControllers</code> 
 * method of <code>{@link WebMvcConfigurationSupport}</code>.<br />
 * <br />
 * Precondition:
 * <ul>
 * <li>Expected that view name has not been set by calling 
 * <code>{@link ViewControllerRegistration#setViewName(String)}</code> method in the code flow 
 * from <code>addViewControllers</code> method of <code>{@link WebMvcConfigurationSupport}</code>. 
 * Thereby, the view name being give to this view resolver will be the one yield by 
 * <code>{@link RequestToViewNameTranslator}</code> (via <code>{@link ParameterizableViewController}</code>); 
 * means the relative path without leading '/' character (and without file extension.) 
 * ex. 'static/image/sample' not '/static/image/sample.jpg'. <br />
 * Override <code>{@link #createView(String, Locale) createView}</code> method if it's not desirable. 
 * </li>
 * </ul>
 * 
 * @author Arata.Y
 */
public class StaticViewResolver extends AbstractCachingViewResolver implements Ordered {
	protected Log getLogger() {
		return logger;
	}	
	
	/**
	 * View just for static resource.<br />
	 * This just forwards request to target static resource.
	 * 
	 * @author Arata.Y
	 */
	public static class StaticResourceView extends AbstractView implements InitializingBean {
		protected Log getLogger() {
			return logger;
		}

		private String resolvedViewName;
			public String getResolvedViewName() {
				return resolvedViewName;
			}
			public void setResolvedViewName( final String resolvedViewName) {
				this.resolvedViewName = resolvedViewName;
			}
		
		@Override
		public void afterPropertiesSet() throws Exception {
			if ( getResolvedViewName() == null) {
				throw new IllegalStateException( 
						"Value must have already been set to 'resolvedViewName' property.");
			}
		}
		
		@Override
		protected void renderMergedOutputModel( 
				final Map<String, Object> model, 
				final HttpServletRequest request, 
				final HttpServletResponse response)
		throws Exception {
			
			String requestURI = request.getRequestURI();
			String viewName = getResolvedViewName();
				if ( viewName.startsWith( "/") 
						? requestURI.equals( viewName) : requestURI.equals( StringUtils.applyRelativePath( requestURI, viewName))) {
					throw new ServletException( 
							String.format(
									"Circular view path [%1$s]: would dispatch back to the current handler URL [%2$s] again. " 
									+ "Check ViewResolver setup.", 
									viewName, 
									requestURI
									)
							);
				}
			
			RequestDispatcher requestDispatcher = request.getRequestDispatcher( requestURI);				
				if ( requestDispatcher == null) {
					throw new ServletException(
							String.format(
									"Could not get RequestDispatcher for [%1$s]: Check that the corresponding " 
									+ "file exists within your web application archive!", 
									requestURI
									)
							);
				}

				Log logger = getLogger();
				if ( logger.isDebugEnabled()) {
					logger.debug(
							String.format(
									"Forwarding to resource [%1$s] in '%2$s' view bean (%3$s instance)", 
									requestURI, 
									getBeanName(),
									this.getClass().getSimpleName()
									)
							);
				}
			requestDispatcher.forward( request, response);
				// Note: The forwarded resource is supposed to determine the content type itself.
		}
		
		/**
		 * Check whether the underlying resource that the configured {@link #resolvedViewName} member 
		 * field points to actually exists. <br />
		 * This default implementation <u>always returns true</u> for any locale. Override this if it is 
		 * necessary to locale dependent static resource. 
		 * 
		 * @param locale the desired Locale that we're looking for
		 * @return <code>true</code> if the resource exists (or is assumed to exist);
		 * <code>false</code> if we know that it does not exist
		 * @throws Exception if the resource exists but is invalid (e.g. could not be parsed)
		 */
		public boolean checkResource( final Locale locale) throws Exception {
			return true;
		}
	}
	
	private int order =  ViewResolverOrder.consumeOrder();		
		@Override
		public int getOrder() {
			return order;
		}
		public final void setOrder(int order) {
			this.order = order;
		}

	private String delegateViewName;
		/**
		 * Retrieve the attribute value of delegation view name being commonly used 
		 * for each static resource under the same root path being returned by 
		 * <code>{@link #getStaticResourceRootPath()}</code> method, in order to 
		 * avoid instantiation of individual bean for each static resource.
		 *  
		 * @return delegation view name being commonly used for each static resource under 
		 * the same root path.  
		 */
		public String getDelegateViewName() {
			return delegateViewName;
		}
		protected void setDelegateViewName( final String delegateViewName) {
			this.delegateViewName = delegateViewName;
		}
	private String staticResourceRootPath;
		/**
		 * @return Root path of static resources.<br />
		 * Returned path should have "/" prefix.
		 */
		public String getStaticResourceRootPath() {
			Assert.notNull( staticResourceRootPath, "Root path of static resources has not been set.");
			return staticResourceRootPath;
		}
		public void setStaticResourceRootPath( final String staticResourceRootPath) {
			if ( ( staticResourceRootPath == null) || ( staticResourceRootPath.trim().length() < 1)) {
				this.staticResourceRootPath = null;
			}
			else {
				String staticResourceRootPathCopy = staticResourceRootPath.trim();
				if ( !staticResourceRootPathCopy.startsWith( "/")) {
					this.staticResourceRootPath = "/".concat( staticResourceRootPathCopy);
				}
				else {
					this.staticResourceRootPath = staticResourceRootPathCopy;
				}
				
				String delegateViewName = staticResourceRootPathCopy;
					if ( !delegateViewName.endsWith( "/")) delegateViewName = delegateViewName.concat( "/");
					delegateViewName = delegateViewName.concat( "**/*");
				setDelegateViewName( delegateViewName);
			}
		}
	
		
		
	public final static boolean IsPathVariablesExposeByDefault = true;
		public static boolean isPathVariablesExposeByDefault() {
			return IsPathVariablesExposeByDefault;
		}
	private boolean isPathVariablesExposed = StaticViewResolver.IsPathVariablesExposeByDefault;
		public boolean isPathVariablesExposed() {
			return isPathVariablesExposed;
		}
		public void setPathVariablesExposed( boolean isPathVariablesExposed) {
			this.isPathVariablesExposed = isPathVariablesExposed;
		}
		
	private String requestContextAttribute;
		/**
		 * Set the name of the RequestContext attribute for all views.
		 * 
		 * @param requestContextAttribute name of the RequestContext attribute
		 * @see AbstractView#setRequestContextAttribute
		 */
		public void setRequestContextAttribute( final String requestContextAttribute) {
			this.requestContextAttribute = requestContextAttribute;
		}
		/**
		 * Return the name of the RequestContext attribute for all views, if any.
		 */
		protected String getRequestContextAttribute() {
			return this.requestContextAttribute;
		}
		
	/** Map of static attributes, keyed by attribute name (String) */
	private final Map<String, Object> staticAttributes = new HashMap<String, Object>();
		/**
		 * Set static attributes from a <code>java.util.Properties</code> object,
		 * for all views returned by this resolver.
		 * <p>This is the most convenient way to set static attributes. Note that
		 * static attributes can be overridden by dynamic attributes, if a value
		 * with the same name is included in the model.
		 * <p>Can be populated with a String "value" (parsed via PropertiesEditor)
		 * or a "props" element in XML bean definitions.
		 * @see org.springframework.beans.propertyeditors.PropertiesEditor
		 * @see AbstractView#setAttributes
		 */
		public void setAttributes( final Properties props) {
			CollectionUtils.mergePropertiesIntoMap(props, this.staticAttributes);
		}

		/**
		 * Set static attributes from a Map, for all views returned by this resolver.
		 * This allows to set any kind of attribute values, for example bean references.
		 * <p>Can be populated with a "map" or "props" element in XML bean definitions.
		 * @param attributes Map with name Strings as keys and attribute objects as values
		 * @see AbstractView#setAttributesMap
		 */
		public void setAttributesMap( final Map<String, ?> attributes) {
			if ( attributes != null) {
				this.staticAttributes.putAll( attributes);
			}
		}

		/**
		 * Allow Map access to the static attributes for views returned by
		 * this resolver, with the option to add or override specific entries.
		 * <p>Useful for specifying entries directly, for example via
		 * "attributesMap[myKey]". This is particularly useful for
		 * adding or overriding entries in child view definitions.
		 */
		public Map<String, Object> getAttributesMap() {
			return this.staticAttributes;
		}

		
	/**
	 * Creates a new <code>{@link StaticResourceView}</code> instance. <br />
	 * (Lookup for pre-defined View instances is not performed in this method rather is done in 
	 * <code>{@link #resolveViewName(String, Locale) resolveViewName}</code> method.) 
	 * 
	 * @param viewName View name to build. <br />
	 * It's expected to begin with prefix of static resource path what has been set by 
	 * {@link #setStaticResourceRootPath(String)} method.
	 * @return the View instance
	 * @throws Exception when encounters obstacle in instantiating view object.
	 * @see #loadView(String, java.util.Locale)
	 */
	protected StaticViewResolver.StaticResourceView buildView( final String viewName) throws Exception {
		StaticViewResolver.StaticResourceView view 
		= (StaticViewResolver.StaticResourceView) BeanUtils.instantiateClass( StaticViewResolver.StaticResourceView.class);
			view.setResolvedViewName( viewName);
			view.setRequestContextAttribute( getRequestContextAttribute());
			view.setAttributesMap( getAttributesMap());
			view.setExposePathVariables( isPathVariablesExposed());
		return view;
	}
	
	/**
	 * Resister <code>view</code> input as a Spring bean under name of <code>viewName</code> input value.<br />
	 * This calls the following Spring lifecycle methods via the generic Spring bean factory:
	 * <ul>
	 * <li><code>{@link ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext) 
	 * ApplicationContextAware.setApplicationContext}</code> method
	 * </li>
	 * <li><code>{@link InitializingBean#afterPropertiesSet() InitializingBean.afterPropertiesSet}</code> method
	 * </li>
	 * </ul>
	 * 
	 * @param viewName Name of view. 
	 * @param view View object.
	 * @return view as bean.
	 * @see org.springframework.context.ApplicationContextAware#setApplicationContext
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet
	 */
	protected View applyLifecycleMethods( final String viewName, final AbstractView view) {
		return (View)(getApplicationContext().getAutowireCapableBeanFactory().initializeBean( view, viewName));
	}
	
	/**
	 * Delegates to <code>buildView</code> for creating a new View instance.<br />
	 * The instantiated View will be registered as a Spring bean.
	 * 
	 * @param viewName the name of the view to retrieve
	 * @return the View instance
	 * @throws Exception if the view couldn't be resolved
	 * @see #buildView(String)
	 */
	@Override
	protected View loadView( final String viewName, final Locale locale) throws Exception {
		StaticViewResolver.StaticResourceView view = buildView( viewName);
		View viewBean = null;
		if ( view.checkResource( locale)) {
			viewBean = applyLifecycleMethods( viewName, view);
			
			Log looger = getLogger();
			if ( looger.isDebugEnabled()) {
				
				logger.debug(
						String.format(
								"Loaded a %1$s view for the request( viewName: %2$s, locale: %3$s)",
								StaticResourceView.class.getSimpleName(),
								viewName,
								locale
								)
						);
			}
		}
		return viewBean;
	}
	
	/**
	 * Indicates whether this <code>{@link ViewResolver}</code> can resolve a view for supplied 
	 * resource URL. <br />
	 * It checks whether <code>resourceUrl</code> input starts with path for static resources 
	 * returned by <code>{@link #getStaticResourceRootPath()}</code> method.
	 * (If not, then later it will result eventually as <code>{@link #createView(String, java.util.Locale) 
	 * createView}</code> method will return <code>null</code>.)
	 * 
	 * @param resourceUrl Path to a static resource to retrieve. <br />
	 * Expected to have '/' character as prefix.
	 * @param locale Locale to retrieve the view for. <br />
	 * It will be ignored in this implementation. If it's not desirable, then override this. 
	 * @return Indicates whether this resolver applies to the specified view.
	 */
	protected boolean canHandle( final String resourceUrl, final Locale locale) {
		if ( !resourceUrl.startsWith( getStaticResourceRootPath())) return false;
		Log logger = getLogger();
		try {
			File file = new File( this.getClass().getResource( resourceUrl).toURI());
			boolean canHandle = (file.exists() && file.isFile());
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"Returning %1$b indicating whether possible to resolve a view for " 
									+ "\"%2$s\" static resource.", 
									canHandle, 
									resourceUrl
									)
							);
				}
			return canHandle;
		}
		catch( Exception exception) {
				if ( logger.isWarnEnabled()) {
					logger.warn( 
							String.format(
									"Returing false as impossible to resolve a view for \"%1$s\" static " 
									+ "resource due to the occurence of the exception.", 
									resourceUrl
									), 
							exception
							);
				}
			return false;
		}
	}
	
	public final static boolean IsRedirectContextRelativeByDefault = true;
		public static boolean isRedirectContextRelativeByDefault() {
			return IsRedirectContextRelativeByDefault;
		}

	private boolean redirectContextRelative = StaticViewResolver.isRedirectContextRelativeByDefault();
		/**
		 * Set whether to interpret a given redirect URL that starts with a slash ("/") as relative to 
		 * the current ServletContext.<br />
		 * Default is "{@value #IsRedirectContextRelativeByDefault}". 
		 * i.e. as relative to the web application root thereby the context path will be prepended to 
		 * the URL if it's true. <br />
		 * <b>Redirect URLs are specified with the 
		 * {@value org.springframework.web.servlet.view.UrlBasedViewResolver#REDIRECT_URL_PREFIX} prefix.
		 * </b> E.g.: "{@value org.springframework.web.servlet.view.UrlBasedViewResolver#REDIRECT_URL_PREFIX}myAction.do"
		 * 
		 * @see org.springframework.web.servlet.view.RedirectView#setContextRelative
		 */
		public void setRedirectContextRelative( boolean redirectContextRelative) {
			this.redirectContextRelative = redirectContextRelative;
		}
		/**
		 * Return whether to interpret a given redirect URL that starts with a
		 * slash ("/") as relative to the current ServletContext, i.e. as
		 * relative to the web application root.
		 */
		protected boolean isRedirectContextRelative() {
			return this.redirectContextRelative;
		}
	
	protected final static boolean IsRedirectHttp10CompatibleByDefault = true;
		public static final boolean isRedirectHttp10CompatibleByDefault() {
			return IsRedirectHttp10CompatibleByDefault;
		}
	private boolean redirectHttp10Compatible = StaticViewResolver.isRedirectHttp10CompatibleByDefault();
		/**
		 * Set whether redirects should stay compatible with HTTP 1.0 clients. <br />
		 * When set to true, it will enforce HTTP status code 302 in any case, i.e. delegate to 
		 * <code>HttpServletResponse.sendRedirect</code>. Turning this off will send HTTP status 
		 * code 303, which is the correct code for HTTP 1.1 clients, but not understood by 
		 * HTTP 1.0 clients. <br />
		 * Many HTTP 1.1 clients treat 302 just like 303, not making any difference. However, some 
		 * clients depend on 303 when redirecting after a POST request; turn this flag off in such 
		 * a scenario. <br />
		 * <b>Redirect URLs are specified with the 
		 * {@value org.springframework.web.servlet.view.UrlBasedViewResolver#REDIRECT_URL_PREFIX} prefix.
		 * </b> E.g.: "{@value org.springframework.web.servlet.view.UrlBasedViewResolver#REDIRECT_URL_PREFIX}myAction.do"
		 * 
		 * @see org.springframework.web.servlet.view.RedirectView#setHttp10Compatible
		 * @see org.springframework.web.servlet.view.UrlBasedViewResolver#REDIRECT_URL_PREFIX
		 */
		public void setRedirectHttp10Compatible( boolean redirectHttp10Compatible) {
			this.redirectHttp10Compatible = redirectHttp10Compatible;
		}

		/**
		 * Return whether redirects should stay compatible with HTTP 1.0 clients.
		 */
		protected boolean isRedirectHttp10Compatible() {
			return this.redirectHttp10Compatible;
		}

	protected String getResourceUrl() {
		final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
			Assert.isInstanceOf( ServletRequestAttributes.class, requestAttributes);
		return ((ServletRequestAttributes)requestAttributes).getRequest().getRequestURI();
	}
		
	/**
	 * Create <code>{@link View}</code> when <code>viewName</code> input starts with the prefix of static resource 
	 * path what has been set by <code>{@link #setStaticResourceRootPath(String) setStaticResourceRootPath}</code> 
	 * method.<br />
	 * Overridden to process {@value org.springframework.web.servlet.view.UrlBasedViewResolver#REDIRECT_URL_PREFIX} prefix and 
	 * {@value org.springframework.web.servlet.view.UrlBasedViewResolver#FORWARD_URL_PREFIX} prefix. 
	 * 
	 * @param viewName The value is expected to have been yield by <code>{@link RequestToViewNameTranslator}</code> 
	 * (via <code>{@link ParameterizableViewController}</code>.)<br />
	 * Meaning that it is expected to be relative path without leading '/' character; ex. 'static/image/sample' not 
	 * '/static/image/sample.jpg'. 
	 *  
	 * @param locale
	 * 
	 * @see RedirectView#setContextRelative
	 * @see #loadView
	 */
	@Override
	protected View createView( final String viewName, final Locale locale) throws Exception {
		final String resourceUrl = getResourceUrl();
		
		Log logger = getLogger();
		
		// If this resolver is not supposed to handle the given view,
		// return null to pass on to the next resolver in the chain.
		if ( !canHandle( resourceUrl, locale)) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format(
								"Returning null as view for the request( view name: %1$s, resouece: %2$s, locale: %3$s).",
								viewName,
								resourceUrl, 
								locale
								)
						);
			}
			
			return null;
		}
		
		if ( viewName.startsWith( UrlBasedViewResolver.REDIRECT_URL_PREFIX)){
			AbstractView view 
			= new RedirectView( resourceUrl, isRedirectContextRelative(), isRedirectHttp10Compatible());
				if ( logger.isDebugEnabled()) {
					logger.debug( 
							String.format(
									"Returning %1$s for the request( view name: %2$s, resouece: %3$s, locale: %4$s)",
									view.toString(),
									viewName,
									resourceUrl, 
									locale
									)
							);
				}
			return applyLifecycleMethods( viewName, view);
		}
		
		View view = loadView( viewName, locale);
			if ( logger.isDebugEnabled()) {
				if ( view == null) {
					logger.debug( 
							String.format(
									"Returning null as view for the request( view name: %1$s, resouece: %2$s, locale: %3$s).",
									viewName,
									resourceUrl, 
									locale
									)
							);
				}
				else {
					logger.debug( 
							String.format(
									"Returning a %1$s for the request( view name: %2$s, resouece: %3$s, locale: %4$s).",
									view.toString(),
									viewName,
									resourceUrl, 
									locale
									)
							);
				}
			}
		return view;
			// loadView will call buildView and register created view as bean by calling applyLifecycleMethods.   
	}
}