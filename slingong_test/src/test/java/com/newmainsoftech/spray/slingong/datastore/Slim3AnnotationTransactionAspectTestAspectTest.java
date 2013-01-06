/*
 * Copyright (C) 2011-2013 NewMain Softech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
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
