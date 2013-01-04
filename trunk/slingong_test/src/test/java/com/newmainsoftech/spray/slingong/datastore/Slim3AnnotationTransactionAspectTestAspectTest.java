package com.newmainsoftech.spray.slingong.datastore;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

public class Slim3AnnotationTransactionAspectTestAspectTest {
	boolean isPointcutFound( final String pointcutName, final Class<?>[] pointcutArgArray) {
		try {
			Slim3AnnotationTransactionAspect.class.getDeclaredMethod( pointcutName, pointcutArgArray);
			return true;
		}
		catch( NoSuchMethodException exception) {
			return false;
		}
	}
	
	@Test
	public void test_pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction() {
		final String pointcutName = "pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction";
		final Class<?>[] pointcutArgArray = {};
		Assert.assertTrue( 
				String.format( 
						"%1$s may need update because the next pointcut is missing in %2$s: " 
						+ "[pointcut name: %3$s, argument types: %4$s]",
						Slim3AnnotationTransactionAspectTest.class.getName(),
						Slim3AnnotationTransactionAspect.class.getName(),
						pointcutName,
						Arrays.toString( pointcutArgArray)
						),
				isPointcutFound( pointcutName, pointcutArgArray)
				);
	}
	@Test
	public void test_pointcutAtExecutionOfPublicMethodOfTransactionalReadOnlyType() {
		final String pointcutName = "pointcutAtExecutionOfPublicMethodOfTransactionalReadOnlyType";
		final Class<?>[] pointcutArgArray = { Transactional.class};
		Assert.assertTrue( 
				String.format( 
						"%1$s may need update because the next missing pointcut is missing in %2$s: " 
						+ "[pointcut name: %3$s, argument types: %4$s]",
						Slim3AnnotationTransactionAspectTest.class.getName(),
						Slim3AnnotationTransactionAspect.class.getSimpleName(),
						pointcutName,
						Arrays.toString( pointcutArgArray)
						),
				isPointcutFound( pointcutName, pointcutArgArray)
				);
	}
	@Test
	public void test_pointcutAtExecutionOfReadOnlyTransactionalMethod() {
		final String pointcutName = "pointcutAtExecutionOfReadOnlyTransactionalMethod";
		final Class<?>[] pointcutArgArray = { Transactional.class};
		Assert.assertTrue( 
				String.format( 
						"%1$s may need update because the next missing pointcut is missing in %2$s: " 
						+ "[pointcut name: %3$s, argument types: %4$s]",
						Slim3AnnotationTransactionAspectTest.class.getName(),
						Slim3AnnotationTransactionAspect.class.getSimpleName(),
						pointcutName,
						Arrays.toString( pointcutArgArray)
						),
				isPointcutFound( pointcutName, pointcutArgArray)
				);
	}
	@Test
	public void test_pointcutForInproperPersistenceInTransactionalReadOnlyType() {
		final String pointcutName = "pointcutForInproperPersistenceInTransactionalReadOnlyType";
		final Class<?>[] pointcutArgArray = { Transactional.class};
		Assert.assertTrue( 
				String.format( 
						"%1$s may need update because the next missing pointcut is missing in %2$s: " 
						+ "[pointcut name: %3$s, argument types: %4$s]",
						Slim3AnnotationTransactionAspectTest.class.getName(),
						Slim3AnnotationTransactionAspect.class.getSimpleName(),
						pointcutName,
						Arrays.toString( pointcutArgArray)
						),
				isPointcutFound( pointcutName, pointcutArgArray)
				);
	}
	@Test
	public void test_pointcutForInproperPersistenceInReadOnlyTransactionalMethod() {
		final String pointcutName = "pointcutForInproperPersistenceInReadOnlyTransactionalMethod";
		final Class<?>[] pointcutArgArray = { Transactional.class};
		Assert.assertTrue( 
				String.format( 
						"%1$s may need update because the next missing pointcut is missing in %2$s: " 
						+ "[pointcut name: %3$s, argument types: %4$s]",
						Slim3AnnotationTransactionAspectTest.class.getName(),
						Slim3AnnotationTransactionAspect.class.getSimpleName(),
						pointcutName,
						Arrays.toString( pointcutArgArray)
						),
				isPointcutFound( pointcutName, pointcutArgArray)
				);
	}
}
