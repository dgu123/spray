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
