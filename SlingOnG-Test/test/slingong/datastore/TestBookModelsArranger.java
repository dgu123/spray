package slingong.datastore;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;

import slingong.datastore.testmodel.book.Author;
import slingong.datastore.testmodel.book.AuthorBook;
import slingong.datastore.testmodel.book.Book;
import slingong.datastore.testmodel.book.Chapter;

/**
 * Prepare book models and the relations between them for test environment.
 * This will not store those models to datastore. Storing them as entities to datastore should be performed 
 * by subclass for the sake of transactional and non-transactional tests.
 * 
 * @author ay
 */
public class TestBookModelsArranger {
	protected Logger logger = LoggerFactory.getLogger( this.getClass());
	
	protected void setKey( Key parentKey, Object targetToSetKey, String keyName) 
	throws IllegalArgumentException, NoSuchMethodException, InvocationTargetException, IllegalAccessException
	{
		/* Caution: 
		 *  - setter method must have name consisted of "set" + name of primary key member field 
		 *  of which the first character is capitalized, and it has single argument of what the same 
		 *  class type with primary key member field.
		 * 	- Outcome will be uncertain when target object has multiple primary key member fields. 
		 * 	Although I'm not sure whether existence of multiple primary key member fields is possible.
		 */
		
		if ( targetToSetKey == null) {
			throw new IllegalArgumentException( "Target object cannot be null.");
		}
		Class<?> classObj = targetToSetKey.getClass();
		if ( classObj.getAnnotation( Model.class) == null) {
			throw new IllegalArgumentException( 
					String.format( "%1$s is not Slim3's Model type object.", targetToSetKey.toString())
					);
		}
		List<Field> fieldList = Arrays.asList( classObj.getFields());
		boolean keyProvided = false;
		for( Field field : fieldList) {
			field.setAccessible( true);
			Attribute attribute = field.getAnnotation( Attribute.class);
			if ( attribute == null) continue; // for
			if ( attribute.primaryKey()) {
				String setterMethodName = 
					"set" + field.getName().substring( 0, 1).toUpperCase() + field.getName().substring( 1);
				Method setterMethod = classObj.getMethod( setterMethodName, field.getClass());
				if ( setterMethod == null) {
					throw new IllegalArgumentException( 
							String.format( 
									"Could not find setter method of primary key member field " +
									"(\"%1$s\") in target object: %2$s.",
									field.getName(), 
									targetToSetKey.toString()
									)
							);
				}
				else {
					// Set key on target
					Key key;
					if ( parentKey == null) {
						if ( ( null == keyName) || "".equals( keyName)) {
							key = Datastore.allocateId( classObj);
						}
						else {
							key = Datastore.createKey( classObj, keyName);
						}
					}
					else {
						if ( ( null == keyName) || "".equals( keyName)) {
							key = Datastore.allocateId( parentKey, classObj);
						}
						else {
							key = Datastore.createKey( parentKey, classObj, keyName);
						}
					}
					setterMethod.invoke( targetToSetKey, key);
					keyProvided = true;
				}
				break;
			}
		} // for
		
		if ( !keyProvided) {
			throw new IllegalArgumentException( 
					String.format( 
							"Could not find primary key member field in target object: %1$s.",
							targetToSetKey.toString()
							)
					);
		}
	} // protected void setKey( Key parentKey, Object targetToSetKey, String keyName)
	
	protected void setKeyWithRunTimeException( Key parentKey, Object targetToSetKey, String keyName) {
		/* Wrapper of setKey method to wrap checked exception by runtime excetpion
		 */
		
		try {
			setKey( parentKey, targetToSetKey, keyName);
		}
		catch( Throwable throwable) {
			throw new RuntimeException( throwable);
		}
	}
	
	// About Author models ------------------------------------------------------------------------
	protected Author a1;
	protected String a1OriginalName = "Author1";
	protected Author a2;
	protected String a2OriginalName = "Author2";
	
	protected void prepAuthorModels() throws Throwable {
		// Experiment on Datastore's allocateId methods
		Future<KeyRange> authorKeyRangeFuture = 
			Datastore.allocateIdsAsync( Author.class, 2L);
		a1 = new Author();	// Will be root entity
		a1.setName( a1OriginalName);
		
		a2 = new Author();	// Will be root entity
		a2.setName( a2OriginalName);
		
		// Set keys
		try {
			KeyRange authorKeyRange = authorKeyRangeFuture.get();
			a1.setKey( authorKeyRange.getStart());
			a2.setKey( authorKeyRange.getEnd());
		}
		catch( InterruptedException exception) {
			Thread.currentThread().interrupt();
			if ( !( a1.getKey() instanceof Key)) setKeyWithRunTimeException( null, a1, null);
			if ( !( a2.getKey() instanceof Key)) setKeyWithRunTimeException( null, a2, null);
		}
		catch( ExecutionException exception) {
			throw exception;
		}
	} // protected void prepAuthorModels() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	// About Book models --------------------------------------------------------------------------
	protected Book b1;
	protected String b1OriginalTitle = "The 1st book";
	protected Book b2;
	protected String b2OriginalTitle = "The 2nd book";

	protected void prepBookModels() {
		b1 = new Book();
		b1.setTitle( b1OriginalTitle);
		// Set a1 as primary author (parent entity)
		b1.setKey( Datastore.createKey( a1.getKey(), Book.class, b1.getTitle()));

		b2 = new Book();	// Will be root entity (no parent entity)
		b2.setTitle( b2OriginalTitle);
		b2.setKey( Datastore.createKey( Book.class, b2.getTitle()));
	}
	// --------------------------------------------------------------------------------------------
	
	// About co-author on book --------------------------------------------------------------------
	protected AuthorBook a1b2;
	protected AuthorBook a2b1;
	protected AuthorBook a2b2;
	
	protected void prepAuthorBookModels() {
		// Set author a1 as co-author of book b2
		a1b2 = new AuthorBook();
		a1b2.getAuthorRef().setModel( a1);
		a1b2.getBookRef().setModel( b2);
		
		// Set author a2 as co-author of book b1
		a2b1 = new AuthorBook();
		a2b1.getAuthorRef().setModel( a2);
		a2b1.getBookRef().setModel( b1);
		
		// Set author a2 as co-author of book b2
		a2b2 = new AuthorBook();
		a2b2.getAuthorRef().setModel( a2);
		a2b2.getBookRef().setModel( b2);
	}
	// --------------------------------------------------------------------------------------------
	
	// About Chapter models -----------------------------------------------------------------------
	protected Chapter b1c1;
	protected String b1c1OriginalTitle = "The 1st chapter of book1";
	protected int b1c1OriginalPages = 11;
	protected Chapter b1c2;
	protected String b1c2OriginalTitle = "The 2nd chapter of book1";
	protected int b1c2OriginalPages = 12;
	
	protected Chapter b2c1;
	protected String b2c1OriginalTitle = "The 1st chapter of book2";
	protected int b2c1OriginalPages = 21;
	protected Chapter b2c2;
	protected String b2c2OriginalTitle = "The 2nd chapter of book2";
	protected int b2c2OriginalPages = 22;
	
	protected void prepChapterModels() throws Throwable {
		Future<KeyRange> b1cKeyRangeFuture = Datastore.allocateIdsAsync( b1.getKey(), Chapter.class, 2);
		Future<KeyRange> b2cKeyRangeFuture = Datastore.allocateIdsAsync( b2.getKey(), Chapter.class, 2);
		
		// About chapters of b1 book
		b1c1 = new Chapter();
		b1c1.setTitle( b1c1OriginalTitle);
		b1c1.setNumPages( b1c1OriginalPages);
		
		b1c2 = new Chapter();
		b1c2.setTitle( b1c2OriginalTitle);
		b1c2.setNumPages( b1c2OriginalPages);
		
		// About chapters of b2 book
		b2c1 = new Chapter();
		b2c1.setTitle( b2c1OriginalTitle);
		b2c1.setNumPages( b2c1OriginalPages);
		
		b2c2 = new Chapter();
		b2c2.setTitle( b2c2OriginalTitle);
		b2c2.setNumPages( b2c2OriginalPages);
		
		// Set keys
		try {
			KeyRange b1cKeyRange = b1cKeyRangeFuture.get();
			b1c1.setKey( b1cKeyRange.getStart());
			b1c2.setKey( b1cKeyRange.getEnd());
		}
		catch( InterruptedException exception) {
			Thread.currentThread().interrupt();
			if ( !( b1c1.getKey() instanceof Key)) setKeyWithRunTimeException( null, b1c1, null);
			if ( !( b1c2.getKey() instanceof Key)) setKeyWithRunTimeException( null, b1c2, null);
		}
		catch( ExecutionException exception) {
			throw exception;
		}
		try {
			KeyRange b2cKeyRange = b2cKeyRangeFuture.get();
			b2c1.setKey( b2cKeyRange.getStart());
			b2c2.setKey( b2cKeyRange.getEnd());
		}
		catch( InterruptedException exception) {
			Thread.currentThread().interrupt();
			if ( !( b2c1.getKey() instanceof Key)) setKeyWithRunTimeException( null, b2c1, null);
			if ( !( b2c2.getKey() instanceof Key)) setKeyWithRunTimeException( null, b2c2, null);
		}
		catch( ExecutionException exception) {
			throw exception;
		}
	} // protected void prepChapterModels() throws Throwable
	// --------------------------------------------------------------------------------------------
	
	protected void prepTestModels() throws Throwable {
		prepAuthorModels();
		prepBookModels();
		prepAuthorBookModels();
		prepChapterModels();
	} // protected void prepTestEntities()
	
	
	/* To check the relation between Author entity and Book entity.
	 * Note: This checkAuthorBookRelation method uses Datastore.get method to obtain entities.
	 */
	protected boolean checkAuthorBookRelation( Key authorKey, Key bookKey) {
		List<AuthorBook> authorBookList = 
			Arrays.asList( 
					Datastore.get( Author.class, authorKey)
					.getAuthorBookListRef().getModelList().toArray( new AuthorBook[]{})
					);
		boolean authorBookRelationConfirmed = false;
		for( AuthorBook authorBook : authorBookList) {
			if ( bookKey.equals( authorBook.getBookRef().getKey())) {
				authorBookRelationConfirmed = true;
				break;
			}
		} // for
		if ( !authorBookRelationConfirmed) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format( 
								"Missing the association from " +
								"Author entity (key: %1$s) to Book entity (key:%2$s)", 
								authorKey.toString(), bookKey.toString()
								)
						);
			}
			return false;
		}
		
		authorBookList = 
			Arrays.asList( 
					Datastore.get( Book.class, bookKey)
					.getAuthorBookListRef().getModelList().toArray( new AuthorBook[]{})
					);
		authorBookRelationConfirmed = false;
		for( AuthorBook authorBook : authorBookList) {
			if ( authorKey.equals( authorBook.getAuthorRef().getKey())) {
				authorBookRelationConfirmed = true;
				break;
			}
		} // for
		if ( !authorBookRelationConfirmed) {
			if ( logger.isDebugEnabled()) {
				logger.debug( 
						String.format( 
								"Missing the association from " +
								"Book entity (key: %1$s) to Author entity (key:%2$s)", 
								bookKey.toString(), authorKey.toString()
								)
						);
			}
		}
		return authorBookRelationConfirmed;
	} // protected boolean checkAuthorBookRelation( Author author, Book book)
	
	/**
	 * For test models prepared by prepTestModels method, use this verifyPrepedEntities method after 
	 * persisting those to datastore to verify the states of test entities in datastore. 
	 * The usage of this verifyPrepedEntities method is to be called from JUnit test (or JUnit setup method) 
	 * in the subclass.
	 */
	protected void verifyPrepedEntities() {
		// About Author entity
		Assert.assertEquals( a1, Datastore.get( Author.class, a1.getKey()));
			// Checking whether instantiated author class object and author entity can be verified as 
			// the same object since InverseModelListRef class (extending AbstractInverseModelRef class) 
			// has not implemented hashCode and equals methods. 
			Assert.assertNull( Datastore.get( a1.getKey()).getParent());
		Assert.assertEquals( a2, Datastore.get( Author.class, a2.getKey()));
			Assert.assertNull( Datastore.get( a2.getKey()).getParent());

		// About Book entity
		Assert.assertEquals( b1, Datastore.get( Book.class, b1.getKey()));
			// About entity group between a1 and b1
			Assert.assertEquals( a1.getKey(), Datastore.get( b1.getKey()).getParent());
			// About relation between a2 and b1
			Assert.assertTrue( checkAuthorBookRelation( a2.getKey(), b1.getKey()));
			
		Assert.assertEquals( b2, Datastore.get( Book.class, b2.getKey()));
			Assert.assertNull( Datastore.get( b2.getKey()).getParent());
			// About relation between a1 and b2
			Assert.assertTrue( checkAuthorBookRelation( a1.getKey(), b2.getKey()));
			// About relation between a2 and b2
			Assert.assertTrue( checkAuthorBookRelation( a2.getKey(), b2.getKey()));
		
		// About Chapter entity
		Assert.assertEquals( b1c1, Datastore.get( Chapter.class, b1c1.getKey()));
			// About entity group between b1 and c1
			Assert.assertEquals( b1.getKey(), Datastore.get( b1c1.getKey()).getParent());
		Assert.assertEquals( b1c2, Datastore.get( Chapter.class, b1c2.getKey()));
			// About entity group between b1 and c2
			Assert.assertEquals( b1.getKey(), Datastore.get( b1c2.getKey()).getParent());
		Assert.assertEquals( b2c1, Datastore.get( Chapter.class, b2c1.getKey()));
			// About entity group between b2 and c1
			Assert.assertEquals( b2.getKey(), Datastore.get( b2c1.getKey()).getParent());
		Assert.assertEquals( b2c2, Datastore.get( Chapter.class, b2c2.getKey()));
			// About entity group between b2 and c2
			Assert.assertEquals( b2.getKey(), Datastore.get( b2c2.getKey()).getParent());
	}
}
