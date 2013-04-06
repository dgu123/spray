package com.newmainsoftech.spray.sprex.context.support;

import java.util.List;
import java.util.Locale;

public interface LocaleFallbackHandlerCase {
	/**
	 * @return ultimate fall-back locale having set by <code>{@link #setUltimateLocale(Locale)}</code> method.
	 */
	Locale getUltimateLocale();
	/**
	 * @param ultimateLocale Ultimate fall-back locale.
	 */
	void setUltimateLocale( Locale ultimateLocale);
	/**
	 * @param locale
	 * @return true when <code>locale</code> input is ultimate fall-back locale.
	 */
	boolean isUltimateLocale( final Locale locale);
	
	/**
	 * @return <code>{@link List}</code> of fall-back <code>{@link Locale}</code> objects 
	 */
	List<Locale> getFallbackLocalesList();
	/**
	 * @param fallbackLocalesList from where the next fall-back <code>{@link Locale}</code> 
	 * object will be picked up. 
	 */
	void setFallbackLocalesList( List<Locale> fallbackLocalesList);
	
	/**
	 * @param locale {@link Locale} to find next locale in the fall-back locale ladder.
	 * @return Next fall-back locale (or ultimate fall-back locale).
	 */
	Locale getNextFallbackLocale( Locale locale);
}