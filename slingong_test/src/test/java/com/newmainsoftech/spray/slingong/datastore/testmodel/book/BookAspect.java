package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.Key;

@Aspect
public class BookAspect {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	// To validate arguments for setAuthorPriority method of Book class ---------------------------
	// Used join point to get necessary argument instead of using args for pointcut 
	// because method signature may change.
	@Pointcut( 
			"execution( public void com.newmainsoftech.spray.slingong.datastore.testmodel.book.Book.setAuthorPriority( ..))")
	public void setAuthorPriorityPointcut() {}
	
	/* Doesn't this need to be around advice instead of before advice, in order to terminate thread by 
	 * throwing RuntimeException?
	 */
	@Before( "setAuthorPriorityPointcut()")
	public void beforeSetAuthorPriority( JoinPoint joinPoint) {
		String errorMessge;

		Object[] argArray = joinPoint.getArgs();
		Key authorKey;
		int priority;
		try {
			authorKey = (Key)argArray[ 0];
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
			
			errorMessge = 
				String.format( 
						"Before-advise failure on setAuthorPriority method of Book class. %n" +
						"Could be caused by unaware method signature change on its arguments. %n" +
						"The passed number of arguments: %1$d, the passed arguments: %2$s", 
						argArray.length, argStr
						);
			if ( logger.isErrorEnabled()) {
				logger.error( errorMessge, throwable);
			}
			throw new RuntimeException( errorMessge, throwable);
		} // catch( Throwable throwable)
		
		if ( authorKey == null) {
			errorMessge = "Key type argument of setAuthorPriority method of Book class cannot be null.";
			if ( logger.isErrorEnabled()) {
				logger.error( errorMessge);
			}
			throw new RuntimeException( errorMessge); 
		}
		if ( priority < 0) {
			errorMessge = 
				"priority integer argument of setAuthorPriority method of Book class must be bigger than -1.";
			if ( logger.isErrorEnabled()) {
				logger.error( errorMessge);
			}
			throw new RuntimeException( errorMessge); 
		}
	} // public void beforeSetAuthorPriority( JoinPoint joinPoint)
	// --------------------------------------------------------------------------------------------
	
	
	// To validate arguments for addNewChapter method of Book class -------------------------------
	// Used join point to get necessary argument instead of using args for pointcut 
	// because method signature may change.
	@Pointcut( 
			"execution( public void com.newmainsoftech.spray.slingong.datastore.testmodel.book.Book.addNewChapter( ..))")
	public static void addNewChapterPointcut() {}
		
	@Before( "addNewChapterPointcut()")
	public void beforeAddNewChapter( JoinPoint joinPoint) {
		String errorMessge;

		Object[] argArray = joinPoint.getArgs();
		String title;
		int pages;
		try {
			title = (String)argArray[ 0];
			pages = (Integer)argArray[ 1];
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
			
			errorMessge = 
				String.format( 
						"Before-advise failure on addNewChapter method of Book class. %n" +
						"Could be caused by unaware method signature change on its arguments. %n" +
						"The passed number of arguments: %1$d, the passed arguments: %2$s", 
						argArray.length, argStr
						);
			if ( logger.isErrorEnabled()) {
				logger.error( errorMessge, throwable);
			}
			throw new RuntimeException( errorMessge, throwable);
		} // catch( Throwable throwable)
		
		if ( ( title == null) || "".equals( title)) {
			errorMessge = 
				"title String type argument of addNewChapter method of Book class can " +
				"be neither null nor empty string.";
			if ( logger.isErrorEnabled()) {
				logger.error( errorMessge);
			}
			throw new RuntimeException( errorMessge); 
		}
		if ( pages < 1) {
			errorMessge = 
				"pages integer argument of addNewChapter method of Book class must be bigger than 0.";
			if ( logger.isErrorEnabled()) {
				logger.error( errorMessge);
			}
			throw new RuntimeException( errorMessge); 
		}
		
		// About AuthorComparatorUnit authorComparatorUnit argument, its validity will be checked by 
		// AuthorComparatorFactoryAspect aspect.
		
	} // public void beforeAddNewChapter( JoinPoint joinPoint)
	// --------------------------------------------------------------------------------------------
	
}
