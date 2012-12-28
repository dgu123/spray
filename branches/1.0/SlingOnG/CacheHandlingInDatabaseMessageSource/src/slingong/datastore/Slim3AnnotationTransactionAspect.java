package slingong.datastore;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.transaction.annotation.Transactional;


@Aspect
public class Slim3AnnotationTransactionAspect extends ExtendedAnnotationTransactionAspect {
	// advises for readOnly attribute of @Transactional annotation --------------------------------
	/* Refer to http://www.infoq.com/articles/Simplifying-Enterprise-Apps;jsessionid=C95E28A686F0184FBFE644DDCEB6F840
	 */
	protected void logInproperPersistenceIncident( 
			JoinPoint joinPoint, JoinPoint.EnclosingStaticPart enclosingStaticPart) 
	{
		if ( logger.isWarnEnabled()) {
			String statementInfo = String.format(
					"It is not appropriate to have the call to %1$s from %2$s in transaction annotated as " +
					"read-only although Slim3 Datastore does not support read-only access.", 
					joinPoint.getSignature().toString(), 
					enclosingStaticPart.getSourceLocation().toString()
					);
			logger.warn( statementInfo);
		}
	} // protected void logInproperPersistenceIncident( ..)
	
	@Pointcut( 
			"!withincode( * org.slim3.datastore.GlobalTransaction+.*( ..)) && " +
			"!withincode( * slingong.datastore.Slim3PlatformTransactionManager+.*( ..)) && " +
			"(call( public * org.slim3.datastore.GlobalTransaction+.commit()) || " +
			"call( public * org.slim3.datastore.GlobalTransaction+.delete*( ..)) || " +
			"call( public * org.slim3.datastore.GlobalTransaction+.put( ..)) || " +
			"call( public * org.slim3.datastore.GlobalTransaction+.rollback())) "
			)
	protected static void callingGlobalTransactionNotReadOnlyMethods() {}
	
	@Pointcut( 
			"execution( " +
				"public * ((@org.springframework.transaction.annotation.Transactional *)+).*(..))"
			)
	protected static void executingAtTransactionalReadOnlyType() {}
	
	@Pointcut( "executingAtTransactionalReadOnlyType() && @this( atTx) && if()")
	public static boolean executingMethodInAtTransactionalReadOnlyType( Transactional atTx) {
		return atTx.readOnly();
	}
	
	@Pointcut( 
			"cflow( executingMethodInAtTransactionalReadOnlyType( atTx)) && " +
			"callingGlobalTransactionNotReadOnlyMethods() && " +
			"!within( org.slim3.*) && !within( slingong.datastore.Slim3PlatformTransactionManager+)"
			)
	protected static void inproperPersistenceAtTransactionalReadOnlyType( Transactional atTx) {}
	
	@AfterReturning( 
			pointcut="inproperPersistenceAtTransactionalReadOnlyType( atTx)", 
			returning="obj"
			)
	public void afterInproperPersistenceAtTransactionalReadOnlyType( 
			JoinPoint joinPoint, 
			JoinPoint.EnclosingStaticPart enclosingStaticPart,
			Transactional atTx, 
			Object obj
			) 
	{
		logInproperPersistenceIncident( joinPoint, enclosingStaticPart);
	}
	
	@Pointcut( 
			"!executingAtTransactionalReadOnlyType() && " +
			"execution( * *(..)) && @annotation( atTx) && if()"
			)
	public static boolean executingReadOnlyTransactionalMethod( Transactional atTx) {
		return atTx.readOnly();
	}
	
	@Pointcut( 
			"cflow( " +
			"executingReadOnlyTransactionalMethod( atTx)" +
			") && callingGlobalTransactionNotReadOnlyMethods() && " +
			"!within( org.slim3.*) && !within( slingong.datastore.Slim3PlatformTransactionManager+)"
			)
	protected static void inproperPersistenceAtReadOnlyTransactionalMethod( Transactional atTx) {}
	
	@AfterReturning( 
			pointcut="inproperPersistenceAtReadOnlyTransactionalMethod( atTx)", 
			returning="obj"
			)
	public void afterInproperPersistenceAtReadOnlyTransactionalMethod( 
			JoinPoint joinPoint, 
			JoinPoint.EnclosingStaticPart enclosingStaticPart,
			Transactional atTx, 
			Object obj
			) 
	{
		logInproperPersistenceIncident( joinPoint, enclosingStaticPart);
	}
	// --------------------------------------------------------------------------------------------
}
