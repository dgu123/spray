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
import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.google.apphosting.api.DeadlineExceededException;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorBook;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Book;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration( "file:target/test-classes/com/newmainsoftech/spray/slingong/testContext2.xml")
@TransactionConfiguration( transactionManager="txManager")
public class Slim3PlatformTransactionManagerTest2 extends TestBookModelsArranger {
/*	
	protected void setUpLoggers() {
		java.util.logging.Logger julLogger = Logger.getLogger( Logger.GLOBAL_LOGGER_NAME);
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( this.getClass().getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( Slim3PlatformTransactionManager.class.getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( AbstractPlatformTransactionManager.class.getName());
		julLogger.setLevel( Level.FINEST);
			// Even setting JUL logger level as above additionally to setting in log4j.properties file and 
			// logging.properties file, in org.springframework.transaction.support.AbstractPlatformTransactionManager,  
			// debug level log won't be shown.  
			// However, debugEnabled boolean local member field (of what value is obtained by logger.isDebugEnabled() 
			// method) in AbstractPlatformTransactionManager class was true.
			// Don't know why debug level log won't be shown. 
			// It was confirmed by getTransaction method in AbstractPlatformTransactionManager class.
		julLogger = Logger.getLogger( ExtendedAnnotationTransactionAspect.class.getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( TransactionAspectSupport.class.getName());
		julLogger.setLevel( Level.FINEST);
	}
*/
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
/*	{
		setUpLoggers();
	};
*/
	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig = new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig = new LocalMemcacheServiceTestConfig();
	// LocalTaskQueueTestConfig is for Slim3 use
	protected LocalTaskQueueTestConfig gaeTaskQueueTestConfig = new LocalTaskQueueTestConfig();
	{
		gaeTaskQueueTestConfig = 
			gaeTaskQueueTestConfig.setQueueXmlPath( "src\\test\\webapp\\WEB-INF\\queue.xml");
	};
	protected final LocalServiceTestHelper gaeTestHelper = 
		new LocalServiceTestHelper( gaeDatastoreTestConfig, gaeMemcacheTestConfig, gaeTaskQueueTestConfig);
	// --------------------------------------------------------------------------------------------
	
	@Autowired
	protected ApplicationContext applicationContext;
	
	@Autowired
	protected Slim3PlatformTransactionManager slim3PlatformTransactionManager;
	
/*	@Autowired
	protected ExtendedAnnotationTransactionAspect annotationTransactionAspect;
*/	
	@Autowired
	protected AnnotationTransactionAspect annotationTransactionAspect;

	@Before
	public void setUp() throws Throwable {
		gaeTestHelper.setUp();
		
		// Confirmations on Spring ApplicationContext status --------------------------------------
		Assert.assertNotNull( applicationContext);
		
		final String annotationBeanConfigurerAspectBeanId = "annotationBeanConfigurerAspect";
			Assert.assertTrue( applicationContext.containsBean( annotationBeanConfigurerAspectBeanId));
			Assert.assertNotNull( applicationContext.getBean( annotationBeanConfigurerAspectBeanId));
		
		final String slim3PlatformTransactionManagerBeanId = "txManager";
			Assert.assertTrue( applicationContext.containsBean( slim3PlatformTransactionManagerBeanId));
			Assert.assertEquals( 
					applicationContext.getBean( slim3PlatformTransactionManagerBeanId), 
					slim3PlatformTransactionManager
					);
			Assert.assertTrue( slim3PlatformTransactionManager instanceof Slim3PlatformTransactionManager);
			Assert.assertEquals( 
					AbstractPlatformTransactionManager.SYNCHRONIZATION_ALWAYS, 
					slim3PlatformTransactionManager.getTransactionSynchronization()
					);
			Assert.assertFalse( slim3PlatformTransactionManager.isNestedTransactionAllowed());
	
		final String annotationTransactionAspectBeanId = "annotationTransactionAspect";
			Assert.assertTrue( applicationContext.containsBean( annotationTransactionAspectBeanId));
			Assert.assertEquals( 
					applicationContext.getBean( annotationTransactionAspectBeanId), 
					annotationTransactionAspect);
			
		final String annotationTransactionOnStaticMethodAspectBeanId 
		= "annotationTransactionOnStaticMethodAspect";
			Assert.assertTrue( 
					applicationContext.containsBean( annotationTransactionOnStaticMethodAspectBeanId));
			Assert.assertNotNull( 
					applicationContext.getBean( annotationTransactionOnStaticMethodAspectBeanId));
		// ----------------------------------------------------------------------------------------
		
		logGlobalTransactionStatus();
	} // public void setUp()
	
	@After
	public void tearDown() throws Throwable {
		logGlobalTransactionStatus();

		gaeTestHelper.tearDown();
	}
	
	protected void logGlobalTransactionStatus() {
		if ( logger.isDebugEnabled()) {
			GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
			logger.debug( 
					String.format(
							"%1$cCurrent globalTransaction instance (ID:%2$s) is %3$s.",
							'\t',
							(gtx == null ? "null" : gtx.getId()), 
							(gtx == null ? "null" : (gtx.isActive() ? "active" : "inactive"))
							)
					);
			for( GlobalTransaction gtxObj : Datastore.getActiveGlobalTransactions()) {
				if ( gtxObj instanceof GlobalTransaction) {
					if ( gtxObj.getId().equals( gtx.getId())) continue;
					logger.debug(
							String.format(
									"%1$cGlobalTransaction instance (ID:%2$s) is %3$s.", 
									'\t',
									(gtxObj == null ? "null" : gtxObj.getId()), 
									(gtxObj == null ? "null" : (gtxObj.isActive() ? "active" : "inactive"))
									)
							);
				}
			} // for
		}
	} // protected void logGlobalTransactionStatus()
	


	Author author3;
	Book book3;
	AuthorBook a2book3;
	Chapter book3Chapter1;
	AuthorChapter a3b3Chapter1;
	protected void prep3rdBookModels() {
		String author3Name = "Author on propagation";
		author3 = new Author();
		author3.setName( author3Name);
		author3.setKey( Datastore.allocateId( Author.class));
		
		book3 = new Book();
		String book3Title = "Book about propagation";
		book3.setTitle( book3Title);
		book3.setKey( Datastore.allocateId( author3.getKey(), Book.class));
		
		a2book3 = new AuthorBook();
		a2book3.getAuthorRef().setModel( a2);
		a2book3.getBookRef().setModel( book3);
		
		book3Chapter1 = new Chapter();
		String b3c1Title = "Chapter about propagation";
		book3Chapter1.setTitle( b3c1Title);
		int b3c1NumPages = 31;
		book3Chapter1.setNumPages( b3c1NumPages);
		book3Chapter1.setKey( Datastore.allocateId( book3.getKey(), Chapter.class));
		
		a3b3Chapter1 = new AuthorChapter();
		a3b3Chapter1.getAuthorRef().setModel( author3);
		a3b3Chapter1.getChapterRef().setModel( book3Chapter1);
	} // protected void prep3rdBookModels()
	
	protected void verify3rdBookEntities() {
logGlobalTransactionStatus();

		List<Key> author3GroupKeyList = Datastore.query( author3.getKey()).asKeyList();
		Assert.assertTrue( author3GroupKeyList.contains( author3.getKey()));
		Assert.assertTrue( author3GroupKeyList.contains( book3.getKey()));
		
logGlobalTransactionStatus();

		Book book3Copy = Datastore.get( Book.class, book3.getKey());
		List<AuthorBook> authorBookList = book3Copy.getAuthorBookListRef().getModelList();
		Assert.assertEquals( 1, authorBookList.size());
		Assert.assertEquals( a2, authorBookList.get( 0).getAuthorRef().getModel());
		
logGlobalTransactionStatus();
		
		List<Chapter> chapterList = book3Copy.getChapterList();
		Assert.assertTrue( chapterList.contains( book3Chapter1));
		List<AuthorChapter> authorChapterList 
		= chapterList.get( chapterList.indexOf( book3Chapter1)).getAuthorChapterListRef().getModelList();
		Assert.assertEquals( 1, authorChapterList.size());
		Assert.assertEquals( author3, authorChapterList.get( 0).getAuthorRef().getModel());
		
logGlobalTransactionStatus();
	} // protected void verify3rdBookEntities()
	
	
	// Test case of transactional method with Propagation.SUPPORTS for existing transaction -------
	@Transactional( 
			propagation=Propagation.SUPPORTS, 
			noRollbackFor={ ConcurrentModificationException.class, DeadlineExceededException.class}
			)
	protected void supportPropagationOnExistingTransaction() throws Throwable {
logGlobalTransactionStatus();

		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		prep3rdBookModels(); // Note: Calling non transactional method
		
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
		
logGlobalTransactionStatus();
	} // protected void supportPropagationToExistingTransaction()
	
	@Transactional( 
			propagation=Propagation.REQUIRES_NEW, 
			noRollbackFor={ ConcurrentModificationException.class, DeadlineExceededException.class}
			)
	protected void prepTransactionForPropergation( Method calleeMethod) throws Throwable {
logGlobalTransactionStatus();

		super.prepTestModels();
		
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( a1, a2, b1, b2, a1b2, a2b1, a2b2, b1c1, b1c2, b2c1, b2c2);
		
		calleeMethod.setAccessible( true);
		calleeMethod.invoke( this);
		
logGlobalTransactionStatus();
	}
/* Somehow this makes testMandatoryPropagationOnExistingTransaction test fail.
 * It seems to leave active transaction (GlobalTransacion) behind.
 * Suspicion is that mocked slim3PlatformTransactionManager object holds GlobalTransaction object and 
 * GlobalTransaction object won't be garbage collected because of that.
 * Or something with reflection usage.
 */
	@Test
	public void testSupportPropagationOnExistingTransaction() throws Throwable {
		Method supportPropagationOnExistingTransactionMethod 
		= this.getClass().getDeclaredMethod( 
				"supportPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		prepTransactionForPropergation( supportPropagationOnExistingTransactionMethod);

logGlobalTransactionStatus();
		
		verifyPrepedEntities();
logGlobalTransactionStatus();
		verify3rdBookEntities();
logGlobalTransactionStatus();
	} // public void testSupportPropagationOnExistingTransaction() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// Test case of transactional method with Propagation.MANDATORY for existing transaction -------
	@Transactional( 
			propagation=Propagation.MANDATORY, 
			noRollbackFor={ ConcurrentModificationException.class, DeadlineExceededException.class}
			)
	protected void mandatoryPropagationOnExistingTransaction() throws Throwable {
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		Assert.assertTrue( gtx instanceof GlobalTransaction);
		
		prep3rdBookModels(); // Note: Calling non transactional method
		gtx.put( author3, book3, a2book3, book3Chapter1, a3b3Chapter1);
	} // protected void supportPropagationToExistingTransaction()
	
/* Somehow this makes testCommitInNonTransactionalMethod test fail. 
 * Error was: 
 * 	com.google.appengine.api.datastore.DatastoreFailureException: handle 9 not found
 */
	@Test
	public void testMandatoryPropagationOnExistingTransaction() throws Throwable {
		Method mandatoryPropagationOnExistingTransactionMethod 
		= this.getClass().getDeclaredMethod( 
				"mandatoryPropagationOnExistingTransaction", 
				new Class<?>[]{}
				);
		prepTransactionForPropergation( mandatoryPropagationOnExistingTransactionMethod);
		
		verifyPrepedEntities();
		verify3rdBookEntities();
	}
	// --------------------------------------------------------------------------------------------
	
}
