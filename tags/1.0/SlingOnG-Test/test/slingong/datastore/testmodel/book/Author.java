package slingong.datastore.testmodel.book;

import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slim3.datastore.Attribute;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Model;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.apphosting.api.DeadlineExceededException;

/**
 * Based on http://gae-java-persistence.blogspot.com/2009/10/creating-bidrectional-owned-one-to-many.html
 * 
 * Author class will be root entity of entity group.
 */
@Configurable
@Model
public class Author implements Serializable {
	@Attribute( persistent = false)
	protected static Logger logger = LoggerFactory.getLogger( Author.class);
	
	@Attribute( primaryKey = true)
	protected Key key;
	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}
	
	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	// For Bidirectional Many-to-Many Relationships Between Author class entity and Book class entity
	// --------------------------------------------------------------------------------------------
	@Attribute( persistent = false)
	private InverseModelListRef< AuthorBook, Author> authorBookListRef = 
		new InverseModelListRef<AuthorBook, Author>( AuthorBook.class, "authorRef", this);
	public InverseModelListRef<AuthorBook, Author> getAuthorBookListRef() {
		return authorBookListRef;
	}
	// --------------------------------------------------------------------------------------------
	
	/**
	 * Returned Author model object has preset key. 
	 */
	@Transactional( propagation=Propagation.REQUIRED)
	public static Author createNewAuthor( String name) {
		Future<KeyRange> futureKeys = Datastore.allocateIdsAsync( Author.class, 1);
		Author author = new Author();
		author.setName( name);
		try {
			author.setKey( futureKeys.get().getStart());
		}
		catch( Throwable throwable) {
			if ( logger.isErrorEnabled()) {
				logger.error( 
						String.format( 
								"Failed to generate key asynchronously for new Author entity.%n" +
								"Going to attempt to regenrate it synchronously."
								), 
						throwable
						);
			}
			author.setKey( Datastore.allocateId( Author.class));
		}
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( author);
		
		return author;
	} // public static void createNewAuthor( String name)

	public Set<Book> getBooksAsAuthor() {
		Set<Book> bookSet = new LinkedHashSet<Book>();
		
		for( AuthorBook authorBook : authorBookListRef.getModelList()) {
			Book book = authorBook.getBookRef().getModel();
			List<AuthorComparatorUnit> authorComparatorUnitList = book.getAuthors();
			int firstPriority = authorComparatorUnitList.get( 0).getPriority();
			for( AuthorComparatorUnit authorComparatorUnit : authorComparatorUnitList) {
				if ( authorComparatorUnit.getPriority() != firstPriority) break;
				if ( key.equals( authorComparatorUnit.getAuthor().getKey())) {
					bookSet.add( book);
					break; // for
				}
			} // for
		} // for
		
		return bookSet;
	}

	public Set<Book> getInvolvedBooks() {
		Set<Book> bookSet = new LinkedHashSet<Book>();
		
		for( AuthorBook authorBook : authorBookListRef.getModelList()) {
			bookSet.add( authorBook.getBookRef().getModel());
		} // for
		
		return bookSet;
	}
	
	// For Bidirectional Many-to-Many Relationships Between Author class entity and Chapter class entity
	// --------------------------------------------------------------------------------------------
	@Attribute( persistent = false)
	private InverseModelListRef< AuthorChapter, Author> authorChapterListRef = 
		new InverseModelListRef<AuthorChapter, Author>( AuthorChapter.class, "authorRef", this);
	public InverseModelListRef<AuthorChapter, Author> getAuthorChapterListRef() {
		return authorChapterListRef;
	}
	// --------------------------------------------------------------------------------------------
	
	// hashCode() and equals() methods ------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + authorBookListRef.hashCode();
		result = prime * result + authorChapterListRef.hashCode();
		
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Author other = (Author) obj;
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key)) return false;
		
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		
		if ( !authorBookListRef.equals( other.getAuthorBookListRef())) return false;
		if ( !authorChapterListRef.equals( other.getAuthorChapterListRef())) return false;
		
		return true;
	}
	// --------------------------------------------------------------------------------------------
}
