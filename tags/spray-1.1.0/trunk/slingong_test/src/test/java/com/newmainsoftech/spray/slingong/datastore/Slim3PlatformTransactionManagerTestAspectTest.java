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

public class Slim3PlatformTransactionManagerTestAspectTest {
	@Test
	public void test_Slim3PlatformTransactionManager_class() {
		final String slim3PlatformTransactionManagerClassKnownName 
		= "com.newmainsoftech.spray.slingong.datastore.Slim3PlatformTransactionManager";
		Assert.assertEquals( 
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						Slim3PlatformTransactionManagerTestAspect.class.getName(),
						Slim3PlatformTransactionManager.class.getSimpleName(),
						slim3PlatformTransactionManagerClassKnownName,
						Slim3PlatformTransactionManager.class.getName()
						),
				slim3PlatformTransactionManagerClassKnownName, 
				Slim3PlatformTransactionManager.class.getName());
	}
	@Test
	public void test_TestBookModelsArranger_class() {
		final String testBookModelsArrangerClassKnownName 
		= "com.newmainsoftech.spray.slingong.datastore.TestBookModelsArranger";
		Assert.assertEquals( 
				String.format(
						"Pointcut expressions in %1$s class MAY need update regarding %2$s class " 
						+ "refactoring from %3$s to %4$s.",
						Slim3PlatformTransactionManagerTestAspect.class.getName(),
						TestBookModelsArranger.class.getSimpleName(),
						testBookModelsArrangerClassKnownName,
						TestBookModelsArranger.class.getName()
						),
				testBookModelsArrangerClassKnownName, 
				TestBookModelsArranger.class.getName());
	}
}
