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
