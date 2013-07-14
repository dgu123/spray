package com.newmainsoftech.spray.sprex.web.servlet.config;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

public interface ViewResolverDefinitionInjectorCase {

	void addViewResolverDefinition( BeanDefinitionRegistry registry);

	void addViewResolverDefinition();

}