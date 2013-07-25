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
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect to handle incident of call to not-read-only method of {@link GlobalTransaction} class 
 * during read-only transaction.
 * 
 * @author <a href="mailto:artymt@gmail.com">Arata Y.</a>
 */
@Aspect
public class Slim3AnnotationTransactionAspect {
	private static Logger logger 
	= LoggerFactory.getLogger( Slim3AnnotationTransactionAspect.class.getClass());
		static Logger getLogger() {
			return logger;
		}
	public static final boolean DefaultValueOfIsToTerminate = true;
	private static boolean isToTerminate = DefaultValueOfIsToTerminate;
		/**
		 * @return boolean value to be used to control whether throwing {@link IllegalStateException} 
		 * exception when encountered call to not-read-only method of 
		 * {@link GlobalTransaction} class during read-only transaction.
		 */
		public static boolean isToTerminate() {
			return isToTerminate;
		}
		/**
		 * Set switch to throw {@link IllegalStateException} exception when encountered call to 
		 * not-read-only method of {@link GlobalTransaction} class during read-only transaction. <br />
		 * If it's set to false, then only logging of such incident of call to not-read-only method of 
		 * {@link GlobalTransaction} class during read-only transaction will be happen. 
		 * Default setting is {@value #DefaultValueOfIsToTerminate}.
		 * 
		 * @param isToTerminate To set value to control whether throwing {@link IllegalStateException} 
		 * exception.
		 */
		public static void setToTerminate( boolean isToTerminate) {
			Logger logger = getLogger();
			if ( logger.isInfoEnabled() && (isToTerminate() != isToTerminate)) {
				logger.info( 
						String.format(
								"Value of isToTerminate switch (what controls whether throw %1$s when " 
								+ "incident of call to non-read-only method of %2$s class during " 
								+ "read-only transaction) is changed from %3$b to %4$b.",
								IllegalStateException.class.getSimpleName(),
								GlobalTransaction.class.getSimpleName(),
								isToTerminate(), 
								isToTerminate
								)
						);
			}
			Slim3AnnotationTransactionAspect.isToTerminate = isToTerminate;
		}
	/**
	 * Handles incident of call to not-read-only method of {@link GlobalTransaction} class during 
	 * read-only transaction. 
	 * 
	 * @param isToTerminate When true, {@link IllegalStateException} will be thrown. When false, only 
	 * perform logging such incident.
	 * @param joinPoint
	 * @param enclosingStaticPart
	 */
	protected void handleInproperPersistenceIncident( 
			boolean isToTerminate, JoinPoint joinPoint, JoinPoint.EnclosingStaticPart enclosingStaticPart) 
	{
		String statementInfo = String.format(
				"Detected the inappropriate call to not-read-only method (%1$s) during read-only " 
				+ "trasaction. Call is made from %2$s. ", 
				joinPoint.getSignature().toString(), 
				enclosingStaticPart.getSourceLocation().toString()
				);
		if ( isToTerminate) {
			throw new IllegalStateException( statementInfo);
		}
		else {
			Logger logger = Slim3AnnotationTransactionAspect.getLogger();
			if ( logger.isWarnEnabled()) {
				logger.warn( statementInfo);
			}
		}
	} // protected void logInproperPersistenceIncident( ..)

	// pointcut expressions and advises for readOnly attribute of @Transactional annotation -------
	/* Refer to http://www.infoq.com/articles/Simplifying-Enterprise-Apps;jsessionid=C95E28A686F0184FBFE644DDCEB6F840
	 */
	/**
	 * AspectJ's pointcut at call to not-read-only methods of {@link GlobalTransaction} from other than 
	 * {@link GlobalTransaction} or {@link Slim3PlatformTransactionManager} lineage type objects. <br />
	 * Those not-read-only methods of {@link GlobalTransaction} are:
	 * <ul>
	 * <li>{@link GlobalTransaction#commit()}</li>
	 * <li>overloaded delete methods</li>
	 * <li>overloaded put methods</li>
	 * <li>{@link GlobalTransaction#rollback()}</li>
	 * </ul>
	 */
	@Pointcut( 
			"!withincode( * org.slim3.datastore.GlobalTransaction+.*( ..)) && " +
			"!withincode( * com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager+.*( ..)) && " +
			"(call( public * org.slim3.datastore.GlobalTransaction+.commit()) || " +
			"call( public * org.slim3.datastore.GlobalTransaction+.delete*( ..)) || " +
			"call( public * org.slim3.datastore.GlobalTransaction+.put( ..)) || " +
			"call( public * org.slim3.datastore.GlobalTransaction+.rollback())) "
			)
	protected static void pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction() {}
	
	/**
	 * AspectJ's pointcut at execution of public method in class what is annotated 
	 * with @{@link Transactional}. 
	 */
	@Pointcut( 
			"execution( " +
				"public * ((@org.springframework.transaction.annotation.Transactional *)+).*(..))"
			)
	protected static void pointcutAtExecutionOfPublicMethodOfTransactionalType() {}
	
	/**
	 * AspectJ's pointcut to check whether executing public method in class of 
	 * what @{@link Transactional} annotation is specified as read-only. 
	 * 
	 * @param atTx 
	 * @return readOnly attribute value of @{@link Transactional} annotation
	 */
	@Pointcut( "pointcutAtExecutionOfPublicMethodOfTransactionalType() && @this( atTx) && if()")
	public static boolean pointcutAtExecutionOfPublicMethodOfTransactionalReadOnlyType( Transactional atTx) {
		return atTx.readOnly();
	}
	
	/**
	 * AspectJ's pointcut to pick up call to not-read-only method of {@link GlobalTransaction} class 
	 * during execution of public method in class of what @{@link Transactional} annotation is specified 
	 * as read-only. <br />
	 * This excludes join-point within {@link Slim3PlatformTransactionManager} lineage class and 
	 * classes in org.slim3 package. 
	 * 
	 * @param atTx
	 */
	@Pointcut( 
			"cflow( pointcutAtExecutionOfPublicMethodOfTransactionalReadOnlyType( atTx)) " 
			+ "&& pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction() " 
			+ "&& !within( org.slim3.*) " 
			+ "&& !within( com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager+)"
			)
	protected static void pointcutForInproperPersistenceInTransactionalReadOnlyType( Transactional atTx) {}
	
	/**
	 * AspectJ's {@link Before} advise method to handle incident of call to not-read-only method 
	 * of {@link GlobalTransaction} class during execution of public method in class of 
	 * what @{@link Transactional} annotation is specified as read-only. <br />
	 * Uses {@link #pointcutForInproperPersistenceInTransactionalReadOnlyType(Transactional)} pointcut to 
	 * pick up join-point. <br />
	 * This let {@link #handleInproperPersistenceIncident(boolean, JoinPoint, org.aspectj.lang.JoinPoint.EnclosingStaticPart)} 
	 * method do actual handling.
	 * 
	 * @param joinPoint
	 * @param enclosingStaticPart
	 * @param atTx
	 */
	@Before( value="pointcutForInproperPersistenceInTransactionalReadOnlyType( atTx)")
	public void beforeInproperPersistenceInTransactionalReadOnlyType( 
			JoinPoint joinPoint, 
			JoinPoint.EnclosingStaticPart enclosingStaticPart,
			Transactional atTx
			) 
	{
		handleInproperPersistenceIncident( 
				Slim3AnnotationTransactionAspect.isToTerminate(), joinPoint, enclosingStaticPart);
	}
	
	/**
	 * AspectJ's pointcut at execution of method annotated with @{@link Transactional} 
	 * annotation what has read-only attribute specified. <br />
	 * This excludes join-points picked up by 
	 * {@link #pointcutAtExecutionOfPublicMethodOfTransactionalReadOnlyType(Transactional)} pointcut. 
	 * 
	 * @param atTx
	 * @return true when atTx input is for read-only transaction.
	 */
	@Pointcut( 
			"!pointcutAtExecutionOfPublicMethodOfTransactionalType() && " +
			"execution( * *(..)) && @annotation( atTx) && if()"
			)
	public static boolean pointcutAtExecutionOfReadOnlyTransactionalMethod( Transactional atTx) {
		return atTx.readOnly();
	}
	
	/**
	 * AspectJ's pointcut to pick up call to not-read-only method of {@link GlobalTransaction} class 
	 * during execution of method annotated with @{@link Transactional} annotation what has read-only 
	 * attribute specified. <br />
	 * This excludes join-point picked up by 
	 * {@link #pointcutForInproperPersistenceInTransactionalReadOnlyType(Transactional)} pointcut, and 
	 * join-point within {@link Slim3PlatformTransactionManager} lineage class and classes in 
	 * org.slim3 package  
	 * 
	 * @param atTx
	 */
	@Pointcut( 
			"cflow( pointcutAtExecutionOfReadOnlyTransactionalMethod( atTx)) " 
			+ "&& pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction() " 
			+ "&& !within( org.slim3.*) " 
			+ "&& !within( com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager+)"
			)
	protected static void pointcutForInproperPersistenceInReadOnlyTransactionalMethod( Transactional atTx) {}
	
	/**
	 * AspectJ's {@link Before} advise method to handle incident of call to not-read-only method 
	 * of {@link GlobalTransaction} class during execution of method annotated 
	 * with @{@link Transactional} annotation what has read-only attribute specified. <br />
	 * Uses {@link #pointcutForInproperPersistenceInReadOnlyTransactionalMethod(Transactional)} pointcut to 
	 * pick up join-point. <br />
	 * This let {@link #handleInproperPersistenceIncident(boolean, JoinPoint, org.aspectj.lang.JoinPoint.EnclosingStaticPart)} 
	 * method do actual handling.
	 * 
	 * @param joinPoint
	 * @param enclosingStaticPart
	 * @param atTx
	 */
	@Before( value="pointcutForInproperPersistenceInReadOnlyTransactionalMethod( atTx)")
	public void beforeInproperPersistenceInReadOnlyTransactionalMethod( 
			JoinPoint joinPoint, 
			JoinPoint.EnclosingStaticPart enclosingStaticPart,
			Transactional atTx
			) 
	{
		handleInproperPersistenceIncident( 
				Slim3AnnotationTransactionAspect.isToTerminate(), joinPoint, enclosingStaticPart);
	}
	// --------------------------------------------------------------------------------------------
}
