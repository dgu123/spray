package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

public class BookAspectTest {
	@Test
	public void test_Book_class() {
		final String testBookClassKnownName 
		= "com.newmainsoftech.spray.slingong.datastore.testmodel.book.Book";
		Assert.assertEquals( 
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						BookAspect.class.getName(),
						Book.class.getSimpleName(),
						testBookClassKnownName,
						Book.class.getName()
						),
				testBookClassKnownName, 
				Book.class.getName());
		
		final String methodName = "setAuthorPriority";
		int methodCount = 0;
			Method[] methodArrays = Book.class.getMethods();
			for( Method method : methodArrays) {
				if ( methodName.equals( method.getName())) {
					methodCount++;
				}
			} // for
			Assert.assertTrue( 
					String.format(
							"Pointcut expressions in %1$s class MAY need update because " 
							+ "%2$s method is not found among public methods of %3$s any longer.",
							BookAspect.class.getName(),
							methodName, 
							Book.class.toString()
							), 
					(methodCount > 0));
	}
}
