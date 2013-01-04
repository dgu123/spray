package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;

@Aspect
public class AuthorComparatorFactoryAspect {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	@Pointcut( "call( com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit.new(..))")
	public void authorComparatorUnitConstructorCallPointcut() {}
	
	/**
	 * Validate arguments passed to AuthorComparatorUnit constructor
	 * @param joinPoint
	 */
	@Before( "authorComparatorUnitConstructorCallPointcut()")
	public void beforeAuthorComparatorUnitConstructor( JoinPoint joinPoint) {
		Object[] argArray = joinPoint.getArgs();
		Author author;
		int priority;
		try {
			author = (Author)argArray[ 0];
			priority = (Integer)argArray[ 1];
		} // try
		catch( Throwable throwable) {
			String argStr = "[";
			for( Object obj : argArray) {
				argStr = argStr + obj.toString() + ", ";
			} // for
			if ( argStr.length() > 1) {
				argStr = argStr.substring( 0, argStr.length() - 3) + "]";
			}
			else {
				argStr = "[]";
			}
			
			throw new RuntimeException(
					String.format( 
							"Before-advise failure on AuthorComparatorUnit constructor. %n" +
							"Could be caused by unaware constructor signature change on its arguments. %n" +
							"The passed number of arguments: %1$d, the passed arguments: %2$s", 
							argArray.length, argStr
							), 
					throwable
					);
		} // catch( Throwable throwable)
		
		if ( author == null) {
			throw new RuntimeException( 
					"Author type argument of AuthorComparatorUnit constructor cannot be null.");
		}
		
		if ( priority < AuthorComparatorUnit.MINIMUM_PRIORITY) {
			// stack trace element 0 is Thread.getStackTrace method
			// stack trace element 1 is this (AuthorComparatorFactoryAspect.beforeAuthorComparatorUnitConstructor)
			StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
			String methodName = stackTraceElement.getMethodName();
			if ( methodName.equals( "getAuthors")) {
				Object thisObj = joinPoint.getThis();
				if ( ( thisObj instanceof Book) || ( thisObj instanceof Chapter)) return;
			}
			
			throw new RuntimeException( 
				"priority integer argument of AuthorComparatorUnit constructor must be bigger than -1.");
		}
	} // public void beforeAuthorComparatorUnitConstructor( JoinPoint joinPoint)
}
