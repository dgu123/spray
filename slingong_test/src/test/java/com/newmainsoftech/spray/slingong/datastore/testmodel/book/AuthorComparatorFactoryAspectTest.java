package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;

public class AuthorComparatorFactoryAspectTest {
	@Test
	public void test_AuthorComparatorUnit_class() {
		final String testAuthorComparatorUnitClassKnownName 
		= "com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit";
		Assert.assertEquals( 
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						AuthorComparatorFactoryAspect.class.getName(),
						AuthorComparatorUnit.class.getSimpleName(),
						testAuthorComparatorUnitClassKnownName,
						AuthorComparatorUnit.class.getName().replace( "$", ".")
						),
				testAuthorComparatorUnitClassKnownName, 
				AuthorComparatorUnit.class.getName().replace( "$", "."));
	}
}
