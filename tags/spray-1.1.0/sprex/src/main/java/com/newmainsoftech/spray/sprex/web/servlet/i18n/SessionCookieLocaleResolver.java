package com.newmainsoftech.spray.sprex.web.servlet.i18n;

import java.util.Locale;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import org.springframework.web.util.WebUtils;

/**
 * {@link LocaleResolver} consults session and cookie in resolving locale.<br />
 * Locale will be resolved by the following sequences:
 * <ol>
 *  <li>Check in session.</li>
 *  <li>Check in request attribute.
 *   <ol>
 *    <li>If locale has been set in request attribute, then set locale to session.</li>
 *   </ol>
 *  </li>
 *  <li>If locale has not been set in request attribute, then check in cookie.
 *   <ol>
 *    <li>If locale has been set in cookie, then set locale to session.</li>
 *   </ol>
 *  </li>
 *  <li>If locale has not been set in cookie, then fall back to the accept-language locale 
 *  in the request headers.
 *  </li>
 * </ol>
 * 
 * @author Arata Y.
 */
public class SessionCookieLocaleResolver extends CookieLocaleResolver {
	/**
	 * Set a default Locale that this resolver will return if no other locale found.
	 * 
	 * @param defaultLocale
	 */
	public void setDefaultLocale( final Locale defaultLocale) {
		super.setDefaultLocale( defaultLocale);
	}
		
	// Implementation of LocaleResolver interface -------------------------------------------------
	/**
	 * Getter of session attribute name for locale data. <br />
	 * Note: no setter of session attribute name for locale data in order to keep 
	 * compatibility with Spring's <code>{@link SessionLocaleResolver}</code> class.
	 * 
	 * @return value of <code>{@link org.springframework.web.servlet.i18n.SessionLocaleResolver#LOCALE_SESSION_ATTRIBUTE_NAME 
	 * SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME}</code> constant.
	 */
	public String getLocaleSessionAttributeName() {
		return SessionLocaleResolver.LOCALE_SESSION_ATTRIBUTE_NAME;
	}
	/**
	 * Getter of request attribute name for locale data. <br />
	 * Note: no setter of request attribute name for locale data in order to keep 
	 * compatibility with Spring's <code>{@link CookieLocaleResolver}</code> class.
	 * 
	 * @return value of <code>{@link org.springframework.web.servlet.i18n.CookieLocaleResolver#LOCALE_REQUEST_ATTRIBUTE_NAME 
	 * CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME}</code> constant.
	 */
	public String getLocaleRequestAttributeName() {
		return LOCALE_REQUEST_ATTRIBUTE_NAME;
	}
	
	protected void setLocaleToRequest( final HttpServletRequest request, final Locale locale) {
		WebUtils.setSessionAttribute( 
				request, getLocaleSessionAttributeName(), locale);
		request.setAttribute( getLocaleRequestAttributeName(), locale);
	}
	/**
	 * {@inheritDoc}
	 * <br />
	 * Set locale data to the following places:
	 * <ul>
	 *  <li>Request session attribute. Attribute name will be obtained by 
	 *  <code>{@link #getLocaleSessionAttributeName()}</code> method.
	 *  </li>
	 *  <li>Request attribute. Attribute name will be obtained by 
	 *  <code>{@link #getLocaleRequestAttributeName()}</code> method.
	 *  </li>
	 *  <li><u>Response</u> cookie. Cookie name will be obtained by 
	 *  <code>{@link org.springframework.web.servlet.i18n.CookieLocaleResolver#getCookieName() 
	 *  super.getCookieName}</code> method.
	 *  </li>
	 * </ul>
	 * When locale input is null, it obtains default locale by 
	 * <code>{@link org.springframework.web.servlet.i18n.CookieLocaleResolver#determineDefaultLocale(
	 * HttpServletRequest) super.determineDefaultLocale}</code> method. 
	 * Then set it to session attribute and request attribute, and remove previously set 
	 * locale value from cookie.<br />
	 * When Spring's <code>LocaleChangeInterceptor</code> has been configured and 
	 * <code>{@link SessionCookieLocaleResolver}</code> bean has been added to context, then   
	 * this will be called from Spring's <code>{@link LocaleChangeInterceptor#preHandle(HttpServletRequest, HttpServletResponse, Object) 
	 * LocaleChangeInterceptor.preHandle}</code> method.
	 */
	@Override
	public void setLocale( 
			final HttpServletRequest request, final HttpServletResponse response, final Locale locale) {
		if ( locale != null) {
			setLocaleToRequest( request, locale);
			super.addCookie( response, locale.toString());
		}
		else {
			Locale defaultLocale = super.determineDefaultLocale( request);
			setLocaleToRequest( request, defaultLocale);
			super.removeCookie( response);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * <br />
	 * Locale will be resolved by checking following places in the shown order:
	 * <ol>
	 *  <li>Session attribute. Attribute name will be obtained by 
	 *  <code>{@link #getLocaleSessionAttributeName()}</code> method.
	 *  </li>
	 *  <li>Request attribute. Attribute name will be obtained by 
	 *  <code>{@link #getLocaleRequestAttributeName()}</code> method.
	 *  </li>
	 *  <li>Request cookie. Cookie name will be obtained by 
	 *  <code>{@link org.springframework.web.servlet.i18n.CookieLocaleResolver#getCookieName() 
	 *  super.getCookieName}</code> method.
	 *  </li>
	 *  <li>Deafult locale what has been set by 
	 *  <code>{@link org.springframework.web.servlet.i18n.CookieLocaleResolver#setDefaultLocale(Locale) 
	 *  super.setDefaultLocale}</code> method.
	 *  </li>
	 *  <li>Accept-Language header. 
	 *  </li>
	 * </ol>
	 */
	@Override
	public Locale resolveLocale( final HttpServletRequest request) {
		// Check session for locale.
		Locale locale 
		= (Locale)(WebUtils.getSessionAttribute( request, getLocaleSessionAttributeName()));
		if ( locale != null) return locale;
		
		// Check request for pre-parsed or preset locale.
		locale 
		= (Locale)(request.getAttribute( getLocaleRequestAttributeName()));
		if (locale != null) {
			WebUtils.setSessionAttribute( request, getLocaleSessionAttributeName(), locale);
			return locale;
		}
		
		// Retrieve and parse cookie value.
		Cookie cookie = WebUtils.getCookie( request, getCookieName());
		if ( cookie != null) {
			locale = StringUtils.parseLocaleString( cookie.getValue());
				if (logger.isDebugEnabled()) {
					logger.debug(
							"Parsed cookie value [" + cookie.getValue() + "] into locale '" + locale + "'");
				}
			if ( locale != null) {
				setLocaleToRequest( request, locale);
				return locale;
			}
		}
		
		return super.determineDefaultLocale( request);
	}
	// --------------------------------------------------------------------------------------------
}
