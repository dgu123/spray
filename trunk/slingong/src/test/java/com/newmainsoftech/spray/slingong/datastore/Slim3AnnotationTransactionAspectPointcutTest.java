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

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.transaction.annotation.Transactional;

public class Slim3AnnotationTransactionAspectPointcutTest {
	@Test
	public void test_Slim3PlatformTransactionManager_class() {
		final String slim3PlatformTransactionManagerClassKnownName 
		= "com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager";
		Assert.assertEquals( 
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						Slim3AnnotationTransactionAspect.class.getName(),
						Slim3PlatformTransactionManager.class.getSimpleName(),
						slim3PlatformTransactionManagerClassKnownName,
						Slim3PlatformTransactionManager.class.getName()
						),
				slim3PlatformTransactionManagerClassKnownName, 
				Slim3PlatformTransactionManager.class.getName());
	}
	
	@SuppressWarnings( "unused")
	@Test
	public void test_GlobalTransaction_class() throws Throwable {
		final String globalTransactionClassKnownName = "org.slim3.datastore.GlobalTransaction";
		final Class<GlobalTransaction> globalTransactionClass = GlobalTransaction.class;
			Assert.assertEquals( 
					String.format(
							"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
							+ "refactoring from %3$s to %4$s.",
							Slim3AnnotationTransactionAspect.class.getName(),
							globalTransactionClass.getSimpleName(),
							globalTransactionClassKnownName,
							globalTransactionClass.getName()
							),
					globalTransactionClassKnownName, 
					globalTransactionClass.getName());
		String methodName = "commit";
			try {
				Method method = globalTransactionClass.getMethod( methodName, (Class<?>[])null);
			}
			catch( NoSuchMethodException exception) {
				Assert.fail( 
						String.format(
								"Pointcut expressions in %1$s class MAY need update because " 
								+ "%2$s method is not found among public methods of %3$s any longer.",
								Slim3AnnotationTransactionAspect.class.getName(),
								methodName, 
								globalTransactionClass.toString()
								)
						);
			}
		methodName = "rollback";
			try {
				Method method = globalTransactionClass.getMethod( methodName, (Class<?>[])null);
			}
			catch( NoSuchMethodException exception) {
				Assert.fail( 
						String.format(
								"Pointcut expressions in %1$s class MAY need update because " 
								+ "%2$s method is not found among public methods of %3$s any longer.",
								Slim3AnnotationTransactionAspect.class.getName(),
								methodName, 
								globalTransactionClass.toString()
								)
						);
			}
		int putMethodCount = 0;
		int deleteMethodCount = 0;
			Method[] methodArrays = globalTransactionClass.getMethods();
			for( Method method : methodArrays) {
				if ( "put".equals( method.getName())) {
					putMethodCount++;
				}
				else if ( method.getName().startsWith( "delete")) {
					deleteMethodCount++;
				}
			} // for
			Assert.assertTrue( 
					String.format(
							"Pointcut expressions in %1$s class MAY need update because " 
							+ "%2$s method is not found among public methods of %3$s any longer.",
							Slim3AnnotationTransactionAspect.class.getName(),
							"put", 
							globalTransactionClass.toString()
							), 
					(putMethodCount > 0));
			Assert.assertTrue( 
					String.format(
							"Pointcut expressions in %1$s class MAY need update because " 
							+ "%2$s method is not found among public methods of %3$s any longer.",
							Slim3AnnotationTransactionAspect.class.getName(),
							"delete", 
							globalTransactionClass.toString()
							), 
					(deleteMethodCount > 0));
	}
	
	@Test
	public void test_Transactional_class() {
		final String transactionalClassKnownName 
		= "org.springframework.transaction.annotation.Transactional";
		Assert.assertEquals(
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						Slim3AnnotationTransactionAspect.class.getName(),
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
						Slim3AnnotationTransactionAspect.class.getName(),
						Transactional.class.getName()
						),
				Transactional.class.isAnnotation()
				);
	}
}
