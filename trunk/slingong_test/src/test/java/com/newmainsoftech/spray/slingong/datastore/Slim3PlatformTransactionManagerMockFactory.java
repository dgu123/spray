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

import org.mockito.Mockito;

/* Created this conventional factory class to provide mocked Slim3PlatformTransactionManager object 
 * for testing purpose, since, by the various reasons, I couldn't swap Slim3PlatformTransactionManager 
 * with mocked one by AspectJ. 
 */
public class Slim3PlatformTransactionManagerMockFactory {
	public static Slim3PlatformTransactionManager mockedManagerFactory() {
		Slim3PlatformTransactionManager txMgr = new Slim3PlatformTransactionManager();
		return Mockito.spy( txMgr);
	}
}
