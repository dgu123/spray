/*
 * Copyright (C) 2011-2013 NewMain Softech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.newmainsoftech.spray.slingong.datastore;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class Slim3PlatformTransactionManagerTestAspect {
	private static Logger logger = LoggerFactory.getLogger( Slim3PlatformTransactionManagerTestAspect.class);
	
	@Pointcut( 
			"execution( * com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager+.*(..)) " 
			+ "&& within( com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager+)"
			)
	public static void slim3PlatformTransactionManagerMethodExecutionPointcut() {}
	
//	@Pointcut( "execution( * com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManagerTest.*(..))")
	@Pointcut( "execution( * com.newmainsoftech.spray.slingong.datastore.TestBookModelsArranger+.*(..))")
	public static void slim3PlatformTransactionManagerTestExecutionPointcut() {}
	
	@Before( 
			"slim3PlatformTransactionManagerMethodExecutionPointcut() " +
			"|| slim3PlatformTransactionManagerTestExecutionPointcut()"
			)
	public void beforeAbstractPlatformTransactionManagerMethodExecution( JoinPoint joinPoint) {
		if ( logger.isDebugEnabled()) {
			logger.debug(
					String.format( 
							"Executing %1$s", 
							joinPoint.getSignature().toString()
							)
					);
		}
	}
	
	@After( 
			"slim3PlatformTransactionManagerMethodExecutionPointcut() " +
			"|| slim3PlatformTransactionManagerTestExecutionPointcut()"
			)
	public void afterAbstractPlatformTransactionManagerMethodExecution( JoinPoint joinPoint) {
		if ( logger.isDebugEnabled()) {
			logger.debug(
					String.format( 
							"Exited %1$s", 
							joinPoint.getSignature().toString()
							)
					);
		}
	}
}
