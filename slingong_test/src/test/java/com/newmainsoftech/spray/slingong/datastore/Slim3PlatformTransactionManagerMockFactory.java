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
