package slingong.datastore;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class Slim3PlatformTransactionManagerTestAspect {
	private static Logger logger = LoggerFactory.getLogger( Slim3PlatformTransactionManagerTestAspect.class);
	
	@Pointcut( 
			"execution( * slingong.datastore.Slim3PlatformTransactionManager+.*(..)) " 
			+ "&& within( slingong.datastore.Slim3PlatformTransactionManager+)"
			)
	public static void slim3PlatformTransactionManagerMethodExecutionPointcut() {}
	
//	@Pointcut( "execution( * slingong.datastore.Slim3PlatformTransactionManagerTest.*(..))")
	@Pointcut( "execution( * slingong.datastore.TestBookModelsArranger+.*(..))")
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
