package com.newmainsoftech.spray.slingong.datastore;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;

public class AnnotationTransactionOnStaticMethodAspectTest {
	@Test
	public void test_Transactional_class() {
		final String transactionalClassKnownName 
		= "org.springframework.transaction.annotation.Transactional";
		Assert.assertEquals(
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						AnnotationTransactionOnStaticMethodAspect.class.getName(),
						Transactional.class.getSimpleName(),
						transactionalClassKnownName,
						Transactional.class.getName()
						),
				transactionalClassKnownName, 
				Transactional.class.getName());
		Assert.assertTrue( 
				String.format(
						"Pointcut expressions in %1$s class MAY need update because %2$s class " 
						+ "is no longer annotation type.",
						AnnotationTransactionOnStaticMethodAspect.class.getName(),
						Transactional.class.getName()
						),
				Transactional.class.isAnnotation()
				);
	}
}
