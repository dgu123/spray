package slingong.datastore;

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
import org.springframework.transaction.aspectj.AbstractTransactionAspect;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Aspect
public abstract class ExtendedAnnotationTransactionAspect extends AbstractTransactionAspect {

	public ExtendedAnnotationTransactionAspect() {
		super( new AnnotationTransactionAttributeSource(false));
	}

	
	// Implementation from org.springframework.transaction.aspectj.AnnotationTransactionAspect ----
	@Pointcut( 
			"execution( public * ((@org.springframework.transaction.annotation.Transactional *)+).*(..)) ")
	protected static void executionAtTransactionalType() {}
	
	@Pointcut( 
			"executionAtTransactionalType() " +
			"&& @this( org.springframework.transaction.annotation.Transactional)"
			)
	protected static void executionOfAnyPublicMethodInAtTransactionalType() {}
	
	@Pointcut( 
			"!executionAtTransactionalType() && " +
			"execution( * *(..)) && @annotation( org.springframework.transaction.annotation.Transactional)"
			)
	protected static void executionOfTransactionalMethod() {}
	
	@Pointcut( 
			"(executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod()) " +
			"&& this( txObject)"
			)
	protected void transactionalMethodExecution( Object txObject) {
		/* before-advice and a few after-advice for transactionalMethodExecution point-cut are 
		 * defined in org.springframework.transaction.aspectj.AbstractTransactionAspect
		 */
	}
	// --------------------------------------------------------------------------------------------
	
	// Transactional aspect implementation for static method --------------------------------------
	@Pointcut( 
			"(executionOfAnyPublicMethodInAtTransactionalType() || executionOfTransactionalMethod()) " +
			"&& !target( Object)" 
			)
	protected static void transactionalStaticMethodExecution() {}
	
	@SuppressAjWarnings( "adviceDidNotMatch")
	@Before( "transactionalStaticMethodExecution()")
	public void beforeTransactionalStaticMethodExecution( JoinPoint joinPoint) {
		MethodSignature methodSignature = (MethodSignature)joinPoint.getSignature();
		Method method = methodSignature.getMethod();
		TransactionInfo txInfo 
		= createTransactionIfNecessary( method, joinPoint.getStaticPart().getClass());
	}
	
	@SuppressAjWarnings("adviceDidNotMatch")
	@AfterThrowing( 
			pointcut="transactionalStaticMethodExecution()", 
			throwing="throwable"
			)
	public void throwingAfterTransactionalStaticMethodExecution( JoinPoint joinPoint, Throwable throwable) {
	    try {
	        completeTransactionAfterThrowing( TransactionAspectSupport.currentTransactionInfo(), throwable);
	      }
	      catch( Throwable throwableObj) {
	        logger.error( "Failed to close transaction after throwing in a transactional method", throwableObj);
	      }
	}
	
	@SuppressAjWarnings("adviceDidNotMatch")
	@AfterReturning(
			pointcut="transactionalStaticMethodExecution()", 
			returning="txObject"
			)
	public void returningAfterTransactionalStaticMethodExecution( JoinPoint joinPoint, Object txObject) {
		commitTransactionAfterReturning( TransactionAspectSupport.currentTransactionInfo());
	}
	
	@SuppressAjWarnings("adviceDidNotMatch")
	@After( "transactionalStaticMethodExecution()")
	public void afterTransactionalStaticMethodExecution( JoinPoint joinPoint) {
		cleanupTransactionInfo(TransactionAspectSupport.currentTransactionInfo());
	}
	// --------------------------------------------------------------------------------------------
}
