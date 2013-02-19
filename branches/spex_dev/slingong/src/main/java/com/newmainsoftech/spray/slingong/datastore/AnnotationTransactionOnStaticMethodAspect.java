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

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.annotation.SuppressAjWarnings;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.aspectj.AbstractTransactionAspect;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Supplemental aspect to Spring's {@link AnnotationTransactionAspect} for static method annotated 
 * with @{@link Transactional}annotation, since <code>AnnotationTransactionAspect</code> 
 * aspect does not cover static method annotated with <code>@Transactional</code> annotation. 
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@Aspect
public class AnnotationTransactionOnStaticMethodAspect extends TransactionAspectSupport {

	public AnnotationTransactionOnStaticMethodAspect() {
		setTransactionAttributeSource( new AnnotationTransactionAttributeSource( false));
	}
	
	// Copy of pointcuts from org.springframework.transaction.aspectj.AnnotationTransactionAspect -
	/**
	 * AspectJ's {@link Pointcut} for execution of public method annotated 
	 * with @{@link Transactional} annotation. <br />
	 * This is copy from Spring's {@link AnnotationTransactionAspect} aspect.
	 */
	@Pointcut( 
			"execution( public * ((@org.springframework.transaction.annotation.Transactional *)+).*(..)) ")
	protected static void pointcutAtExecutionOfPublicMethodOfTransactionalType() {}
	
	/**
	 * AspectJ's {@link Pointcut} for execution of public transactional method in class annotated 
	 * with @{@link Transactional} annotation. <br />
	 * This is copy from Spring's {@link AnnotationTransactionAspect} aspect.
	 */
	@Pointcut( 
			"pointcutAtExecutionOfPublicMethodOfTransactionalType() " +
			"&& @this( org.springframework.transaction.annotation.Transactional)"
			)
	protected static void pointcutAtExecutionOfPublicTransactionalMethodInTransactionalType() {}
	
	/**
	 * AspectJ's {@link Pointcut} for execution of method annotated with @{@link Transactional} 
	 * annotation. <br />
	 * This excludes join-point picked up by 
	 * {@link #pointcutAtExecutionOfPublicMethodOfTransactionalType()}.<br /> 
	 * This is copy from Spring's {@link AnnotationTransactionAspect} aspect.
	 */
	@Pointcut( 
			"!pointcutAtExecutionOfPublicMethodOfTransactionalType() && " +
			"execution( * *(..)) && @annotation( org.springframework.transaction.annotation.Transactional)"
			)
	protected static void pointcutAtExecutionOfTransactionalMethod() {}
	// --------------------------------------------------------------------------------------------
	
	// aspect implementation for static Transactional method --------------------------------------
	/**
	 * AspectJ's {@link Pointcut} for execution of either static method annotated 
	 * with @{@link Transactional} annotation or public static method in class annotated with 
	 * <code>@Transactional</code> annotation.
	 */
	@Pointcut( 
			"(pointcutAtExecutionOfPublicTransactionalMethodInTransactionalType() " 
			+ "|| pointcutAtExecutionOfTransactionalMethod()) " 
			+ "&& !target( Object)" 
			)
	protected static void pointcutAtExecutionOfStaticTransactionalMethod() {}
	
	/**
	 * AspectJ's {@link Before} advise to create new transaction before execution of either static 
	 * method annotated with @{@link Transactional} annotation or public static method in class 
	 * annotated with <code>@Transactional</code> annotation. <br />
	 * The body of advise is copy from before-advise of Spring's {@link AbstractTransactionAspect} aspect.
	 * @param joinPoint
	 */
	@SuppressAjWarnings( "adviceDidNotMatch")
	@Before( "pointcutAtExecutionOfStaticTransactionalMethod()")
	public void beforeAdvisedExecutionOfStaticTransactionalMethod( JoinPoint joinPoint) {
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = methodSignature.getMethod();
		TransactionInfo txInfo 
		= createTransactionIfNecessary( method, joinPoint.getStaticPart().getClass());
	}
	
	/**
	 * AspectJ's {@link AfterThrowing} advise to complete transaction after exception during execution 
	 * of either static method annotated with @{@link Transactional} annotation or public static method 
	 * in class annotated with <code>@Transactional</code> annotation. <br />
	 * The body of advise is copy from after-throwing-advise of Spring's {@link AbstractTransactionAspect} 
	 * aspect.
	 * 
	 * @param joinPoint
	 * @param throwable
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	@AfterThrowing( 
			pointcut="pointcutAtExecutionOfStaticTransactionalMethod()", 
			throwing="throwable"
			)
	public void afterThrowingAdvisedExecutionOfStaticTransactionalMethod( 
			JoinPoint joinPoint, Throwable throwable) 
	{
	    try {
	        completeTransactionAfterThrowing( TransactionAspectSupport.currentTransactionInfo(), throwable);
	      }
	      catch( Throwable throwableObj) {
	        logger.error( "Failed to close transaction after throwing in a transactional method", throwableObj);
	      }
	}
	
	/**
	 * AspectJ's {@link AfterReturning} advise to commit transaction after normal completion of 
	 * execution of either static method annotated with @{@link Transactional} annotation or public 
	 * static method in class annotated with <code>@Transactional</code> annotation. <br />
	 * The body of advise is copy from after-returning-advise of Spring's {@link AbstractTransactionAspect} 
	 * aspect.
	 * 
	 * @param joinPoint
	 * @param txObject
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	@AfterReturning(
			pointcut="pointcutAtExecutionOfStaticTransactionalMethod()", 
			returning="txObject"
			)
	public void afterReturningAdvisedExecutionOfStaticTransactionalMethod( 
			JoinPoint joinPoint, Object txObject) 
	{
		commitTransactionAfterReturning( TransactionAspectSupport.currentTransactionInfo());
	}
	
	/**
	 * AspectJ's {@link After} advise to clean up transaction after execution of either static 
	 * method annotated with @{@link Transactional} annotation or public static method in class annotated 
	 * with <code>@Transactional</code> annotation. <br />
	 * The body of advise is copy from after-advise of Spring's {@link AbstractTransactionAspect} 
	 * aspect.
	 * 
	 * @param joinPoint
	 */
	@SuppressAjWarnings("adviceDidNotMatch")
	@After( "pointcutAtExecutionOfStaticTransactionalMethod()")
	public void afterAdvisedExecutionOfStaticTransactionalMethod( JoinPoint joinPoint) {
		cleanupTransactionInfo(TransactionAspectSupport.currentTransactionInfo());
	}
	// --------------------------------------------------------------------------------------------
}
