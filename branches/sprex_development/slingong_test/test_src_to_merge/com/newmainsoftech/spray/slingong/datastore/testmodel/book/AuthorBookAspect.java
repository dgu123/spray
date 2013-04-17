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
package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;

@Aspect
public class AuthorBookAspect {
	@Pointcut( "call( public * AuthorBook.setAuthorPriority( int)) && args( authorPriority)")
	public static void setAuthorPriorityCallPointcut( int authorPriority) {}
	
	@Before( "setAuthorPriorityCallPointcut( authorPriority)")
	public void beforeSetAuthorPriority( JoinPoint joinPoint, int authorPriority) 
		throws IllegalArgumentException, NoSuchMethodException
	{
		if ( joinPoint.getThis() instanceof com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorBookMeta) {
			StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[ 2];
			String methodName = stackTraceElement.getMethodName();
			
			if ( methodName.equals( "modelToEntity")) return;
			if ( methodName.equals( "entityToModel")) return;
		}
		
		if ( authorPriority < AuthorComparatorUnit.MINIMUM_PRIORITY) {
			throw new IllegalArgumentException( 
					String.format( 
							"authorPriority value (%1$d) must be bigger than %2$d.", 
							authorPriority, AuthorComparatorUnit.MINIMUM_PRIORITY
							) 
					);
		}
	} // public void beforeSetAuthorPriority( JoinPoint joinPoint, int authorPriority)
	
}
