package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class AuthorAspect {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	// To validate arguments for createNewAuthor method of Author class ---------------------------
	// Used join point to get necessary argument instead of using args for pointcut 
	// because method signature may change.
	@Pointcut( "execution( public void com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author.createNewAuthor( ..))")
	public static void createNewAuthorPointcut() {}
	
	@Around( "createNewAuthorPointcut()")
	public void aroundCreateNewAuthor( ProceedingJoinPoint proceedingJoinPoint) {
		Object[] argsArray = proceedingJoinPoint.getArgs();
		String name = (String)argsArray[ 0];
		if ( ( name == null) || "".equals( name)) {
			String message = 
				"".equals( name) ? 
						"Value of name property of Author entity cannot be empty string.":
						"Value of name property of Author entity cannot be null.";
			if ( logger.isErrorEnabled()) {
				logger.error( message);
			}
			throw new RuntimeException( message);
		}
		
		try {
			proceedingJoinPoint.proceed();
		}
		catch( Throwable throwable) {
			Throwable cause = throwable.getCause();
			if ( cause == null) {
				cause = throwable;
			}
			if ( logger.isErrorEnabled()) {
				logger.error( "Failure at Author.createNewAuthor method", cause);
			}
			throw new RuntimeException( cause);
		}
	}
	// --------------------------------------------------------------------------------------------
}
