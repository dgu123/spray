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
