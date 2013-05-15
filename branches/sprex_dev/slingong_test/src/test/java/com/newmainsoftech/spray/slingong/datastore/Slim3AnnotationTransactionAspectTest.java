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

import java.util.List;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.aspectj.AnnotationTransactionAspect;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorBook;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Book;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter;

@RunWith( SpringJUnit4ClassRunner.class)
@ContextConfiguration( "file:target/test-classes/com/newmainsoftech/spray/slingong/testContext.xml")
@TransactionConfiguration( transactionManager="txManager")
@Aspect
public class Slim3AnnotationTransactionAspectTest extends TestBookModelsArranger {
/*	
	protected void setUpLoggers() {
		java.util.logging.Logger julLogger 
		= java.util.logging.Logger.getLogger( java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( this.getClass().getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( Slim3PlatformTransactionManager.class.getName());
		julLogger.setLevel( Level.FINEST);
		julLogger = Logger.getLogger( Slim3AnnotationTransactionAspect.class.getName());
		julLogger.setLevel( Level.FINEST);
	}
*/	
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
/*	{
		setUpLoggers();
	};
*/
	// Test preparation for GAE/J environment ----------------------------------------------------- 
	protected final LocalDatastoreServiceTestConfig gaeDatastoreTestConfig 
	= new LocalDatastoreServiceTestConfig();
	protected final LocalMemcacheServiceTestConfig gaeMemcacheTestConfig 
	= new LocalMemcacheServiceTestConfig();
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
	protected Slim3PlatformTransactionManager slim3PlatformTransactionManager;
	protected InOrder mockedSlim3TxMangerInOrder;
	
/*	@Autowired
	protected Slim3AnnotationTransactionAspect annotationTransactionAspect;
*/	
	@Autowired
	protected AnnotationTransactionAspect annotationTransactionAspect;

	
	protected static int globalTransactionNotReadOnlyMethodCallCount;
	protected static int readOnlyTransactionalMethodCallCount;
	protected static int inproperPersistenceTransactionalMethodCallCount;
	
	@Before
	public void setUp() {
		gaeTestHelper.setUp();
		
		// Confirmations on Spring ApplicationContext status --------------------------------------
		Assert.assertTrue( slim3PlatformTransactionManager instanceof Slim3PlatformTransactionManager);
		Assert.assertTrue( annotationTransactionAspect instanceof AnnotationTransactionAspect);
		// ----------------------------------------------------------------------------------------
		
		mockedSlim3TxMangerInOrder = Mockito.inOrder( slim3PlatformTransactionManager);
		
		globalTransactionNotReadOnlyMethodCallCount = 0;
		readOnlyTransactionalMethodCallCount = 0;
		inproperPersistenceTransactionalMethodCallCount = 0;
	}
	
	@After
	public void tearDown() {
		Mockito.reset( slim3PlatformTransactionManager);
		
		gaeTestHelper.tearDown();
	}
	
	@Transactional( readOnly=true)
	protected void inproperPersistenceInReadOnlyTransaction() throws Throwable {
//		prepTestModels();
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( a2, b2, a2b1, a2b2, b2c1, b2c2);
	}
	
	@org.aspectj.lang.annotation.After( 
		"Slim3AnnotationTransactionAspect.pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction()")
	public void afterCallToNotReadOnlyMethodsOfGlobalTransaction( JoinPoint joinPoint) {
		++globalTransactionNotReadOnlyMethodCallCount;
		if ( logger.isDebugEnabled()) {
			MethodSignature methodSignature = (MethodSignature)(joinPoint.getSignature());
			logger.debug(
					String.format(
							"%1$s as not-read-only method is called. Advanced value of " +
							"globalTransactionNotReadOnlyMethodCallCount counter to %2$d.",
							methodSignature.getMethod().toString(),
							globalTransactionNotReadOnlyMethodCallCount
							)
					);
		}
	}
	
	@org.aspectj.lang.annotation.After( 
		"Slim3AnnotationTransactionAspect.pointcutAtExecutionOfPublicMethodOfTransactionalReadOnlyType( atTx)")
	public void afterExecutionOfPublicMethodOfTransactionalReadOnlyType( Transactional atTx) {
		readOnlyTransactionalMethodCallCount++;
	}
	
	@org.aspectj.lang.annotation.After( 
		"Slim3AnnotationTransactionAspect.pointcutAtExecutionOfReadOnlyTransactionalMethod( atTx)")
	public void afterExecutionOfReadOnlyTransactionalMethod( Transactional atTx) {
		readOnlyTransactionalMethodCallCount++;
	}
	
	@org.aspectj.lang.annotation.After( 
			"Slim3AnnotationTransactionAspect.pointcutForInproperPersistenceInTransactionalReadOnlyType( atTx)")
	public void afterInproperPersistenceInTransactionalReadOnlyType( 
			Transactional atTx, JoinPoint.EnclosingStaticPart enclosingStaticPart) {
		inproperPersistenceTransactionalMethodCallCount++;
	}
	
	@org.aspectj.lang.annotation.After( 
			"Slim3AnnotationTransactionAspect.pointcutForInproperPersistenceInReadOnlyTransactionalMethod( atTx)")
	public void afterInproperPersistenceInReadOnlyTransactionalMethod( 
			Transactional atTx, JoinPoint.EnclosingStaticPart enclosingStaticPart) {
		inproperPersistenceTransactionalMethodCallCount++;
	}
	
	/* Do not export this to JavaDoc since this is AspectJ's advise method.
	 * This will show info level log when non-read-only methods of Slim3's GlobalTransaction class are  
	 * being executed.
	 */
	@org.aspectj.lang.annotation.Before( 
			"Slim3AnnotationTransactionAspect.pointcutAtCallToNotReadOnlyMethodsOfGlobalTransaction()")
	public void beforeCallToNotReadOnlyMethodsOfGlobalTransaction( JoinPoint joinPoint) {
		if ( logger.isDebugEnabled()) {
			logger.debug( 
					"Executing not-read-only method of Slim3 GlobalTransaction: " 
					+ joinPoint.getSignature().toString());
		}
	}
	
	@Test
	public void test_IllegalStateException_by_inproper_persistence() throws Throwable {
		prepTestModels();
		
		GlobalTransaction gtx = Datastore.beginGlobalTransaction();
			gtx.put( a1, b1, a1b2, b1c1, b1c2);
			gtx.commit();
				Assert.assertFalse( gtx.isActive());
			List<Author> authorList = Datastore.query( Author.class).asList();
				Assert.assertEquals( 1, authorList.size());
				Assert.assertEquals( a1, authorList.get( 0));
			List<Book> bookList = Datastore.query( Book.class).asList();
				Assert.assertEquals( 1, bookList.size());
				Assert.assertEquals( b1, bookList.get( 0));
			List<AuthorBook> authorBookList = Datastore.query( AuthorBook.class).asList();
				Assert.assertEquals( 1, authorBookList.size());
				Assert.assertEquals( a1b2, authorBookList.get( 0));
			List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
				Assert.assertEquals( 2, chapterList.size());
				Assert.assertTrue( chapterList.contains( b1c1));
				Assert.assertTrue( chapterList.contains( b1c2));
				
		Slim3AnnotationTransactionAspect.setToTerminate( true);
		globalTransactionNotReadOnlyMethodCallCount = 0;
		try {
			inproperPersistenceInReadOnlyTransaction();
				Assert.fail(
						String.format(
								"%1$s is not thrown during execution of " 
								+ "inproperPersistenceInReadOnlyTransaction method what calls " 
								+ "not-read-only method (put method) of %2$s class in read-only " 
								+ "transaction.",
								IllegalStateException.class.getSimpleName(),
								GlobalTransaction.class.getSimpleName()
								)
						);
		}
		catch( IllegalStateException exception) { // do nothing
		}
		
		authorList = Datastore.query( Author.class).asList();
			Assert.assertEquals( 1, authorList.size());
			Assert.assertEquals( a1, authorList.get( 0));
		bookList = Datastore.query( Book.class).asList();
			Assert.assertEquals( 1, bookList.size());
			Assert.assertEquals( b1, bookList.get( 0));
		authorBookList = Datastore.query( AuthorBook.class).asList();
			Assert.assertEquals( 1, authorBookList.size());
			Assert.assertEquals( a1b2, authorBookList.get( 0));
		chapterList = Datastore.query( Chapter.class).asList();
			Assert.assertEquals( 2, chapterList.size());
			Assert.assertTrue( chapterList.contains( b1c1));
			Assert.assertTrue( chapterList.contains( b1c2));
		
		Assert.assertEquals( 0, globalTransactionNotReadOnlyMethodCallCount);
		Assert.assertEquals( 0, inproperPersistenceTransactionalMethodCallCount);
		Assert.assertEquals( 1, readOnlyTransactionalMethodCallCount);
	}
	
	@Test
	public void test_only_logging_inproper_persistence() throws Throwable {
		prepTestModels();
		
		GlobalTransaction gtx = Datastore.beginGlobalTransaction();
			gtx.put( a1, b1, a1b2, b1c1, b1c2);
			gtx.commit();
				Assert.assertFalse( gtx.isActive());
			List<Author> authorList = Datastore.query( Author.class).asList();
				Assert.assertEquals( 1, authorList.size());
				Assert.assertEquals( a1, authorList.get( 0));
			List<Book> bookList = Datastore.query( Book.class).asList();
				Assert.assertEquals( 1, bookList.size());
				Assert.assertEquals( b1, bookList.get( 0));
			List<AuthorBook> authorBookList = Datastore.query( AuthorBook.class).asList();
				Assert.assertEquals( 1, authorBookList.size());
				Assert.assertEquals( a1b2, authorBookList.get( 0));
			List<Chapter> chapterList = Datastore.query( Chapter.class).asList();
				Assert.assertEquals( 2, chapterList.size());
				Assert.assertTrue( chapterList.contains( b1c1));
				Assert.assertTrue( chapterList.contains( b1c2));
				
		Slim3AnnotationTransactionAspect.setToTerminate( false);
		globalTransactionNotReadOnlyMethodCallCount = 0;
		inproperPersistenceInReadOnlyTransaction();
			// This should proceed execution of not-read-only method of GlobalTransaction class 
			// without throwing IllegalStateException exception.
		
		authorList = Datastore.query( Author.class).asList();
			Assert.assertEquals( 2, authorList.size());
			Assert.assertTrue( authorList.contains( a1));
			Assert.assertTrue( authorList.contains( a2));
		bookList = Datastore.query( Book.class).asList();
			Assert.assertEquals( 2, bookList.size());
			Assert.assertTrue( bookList.contains( b1));
			Assert.assertTrue( bookList.contains( b2));
		authorBookList = Datastore.query( AuthorBook.class).asList();
			Assert.assertEquals( 3, authorBookList.size());
			Assert.assertTrue( authorBookList.contains( a2b1));
			Assert.assertTrue( authorBookList.contains( a2b2));
		chapterList = Datastore.query( Chapter.class).asList();
			Assert.assertEquals( 4, chapterList.size());
			Assert.assertTrue( chapterList.contains( b1c1));
			Assert.assertTrue( chapterList.contains( b1c2));
			Assert.assertTrue( chapterList.contains( b2c1));
			Assert.assertTrue( chapterList.contains( b2c2));
		
		Assert.assertEquals( 1, globalTransactionNotReadOnlyMethodCallCount);
		Assert.assertEquals( 1, inproperPersistenceTransactionalMethodCallCount);
		Assert.assertEquals( 1, readOnlyTransactionalMethodCallCount);
	}
	
}
