package slingong.datastore.testmodel.book;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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

/**
 * Based on http://gae-java-persistence.blogspot.com/2009/10/creating-bidrectional-owned-one-to-many.html
 * 
 * Book class object will be the root entity 
 */
@Configurable
@Model
public class Book implements Serializable {
	@Attribute( persistent = false)
	protected static Logger logger = LoggerFactory.getLogger( Book.class); 
	
	/* Should be child key to Author parent entity.
	 * Can be root key for the case of no primary author, written only by co-authors.
	 */
	@Attribute( primaryKey = true)
	protected Key key;	
	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}

	protected String title;
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
// Can add published date and price
	
	@Attribute( persistent = false)
	List<Chapter> chapterList;	// will be initialized by AOP when new chapter is added to this book entity
	
	@Transactional( propagation=Propagation.REQUIRED)
	public List<Chapter> getChapterList() {
		if ( chapterList == null) {
			GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
			chapterList = gtx.query( Chapter.class, key).asList();
		}
		return chapterList;
	}

	public int getPages() {
		int pages = 0;
		for( Chapter chapter : getChapterList()) {
			pages = pages + chapter.getNumPages();
		} // for
		return pages;
	}
	
	// For Bidirectional Many-to-Many Relationships Between Author class entity and Book class entity
	// --------------------------------------------------------------------------------------------
	@Attribute( persistent = false)
	private InverseModelListRef<AuthorBook, Book> authorBookListRef = 
		new InverseModelListRef<AuthorBook, Book>( AuthorBook.class, "bookRef", this);
	public InverseModelListRef<AuthorBook, Book> getAuthorBookListRef() {
		return authorBookListRef;
	}
	// --------------------------------------------------------------------------------------------
	
	public List<AuthorComparatorUnit> getAuthors() {
		LinkedList<AuthorComparatorUnit> authorList = new LinkedList<AuthorComparatorUnit>();
		for( AuthorBook authorBook : getAuthorBookListRef().getModelList()) {
			AuthorComparatorUnit authorComparatorUnit = 
				new AuthorComparatorUnit( 
						authorBook.getAuthorRef().getModel(), 
						authorBook.getAuthorPriority()
						);
			authorList.add( authorComparatorUnit);
		} // for
		Collections.sort( authorList, AuthorComparatorFactory.getComparator());
		return authorList;
	}
	
	@Transactional
	public void setAuthorPriority( Key authorKey, int priority) {
		for( AuthorBook authorBook : getAuthorBookListRef().getModelList()) {
			if ( authorKey.equals( authorBook.getAuthorRef().getKey())) {
				authorBook.setAuthorPriority( priority);
				GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
				gtx.put( authorBook);
				break; // for
			}
		} // for
	}
	
	protected Set<Key> getAuthorKeySet() {
		Set<Key> authorKeySet = new HashSet<Key>();
		for( AuthorBook authorBook : getAuthorBookListRef().getModelList()) {
			authorKeySet.add( authorBook.getAuthorRef().getKey());
		} // for
		return authorKeySet;
	}
	
	@Transactional
	public void addNewChapter( String title, int pages, AuthorComparatorUnit authorComparatorUnit) {
		Future<KeyRange> chapterFutureKeys = Datastore.allocateIdsAsync( key, Chapter.class, 1);
		Chapter chapter = new Chapter();
		chapter.setTitle( title);
		chapter.setNumPages( pages);
		try {
			chapter.setKey( chapterFutureKeys.get().getStart());
		}
		catch( Throwable throwable) {
			if ( logger.isErrorEnabled()) {
				logger.error( 
						String.format( 
								"Failed to generate key asynchronously for new chapter entity.%n" +
								"Regenrate it synchronously."
								), 
						throwable
						);
			}
			chapter.setKey( Datastore.allocateId( key, Chapter.class));
		}
		GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
		gtx.put( chapter);
				
		AuthorChapter authorChapter = new AuthorChapter();
		authorChapter.getAuthorRef().setModel( authorComparatorUnit.getAuthor());
		authorChapter.setAuthorPriority( authorComparatorUnit.getPriority());
		authorChapter.getChapterRef().setModel( chapter);
		gtx.put( authorChapter);
		
		if ( !getAuthorKeySet().contains( authorComparatorUnit.getAuthor().getKey())) {
			AuthorBook authorBook = new AuthorBook();
			authorBook.getAuthorRef().setModel( authorComparatorUnit.getAuthor());
			authorBook.getBookRef().setModel( this);
			gtx.put( authorBook);
		}
	}
	
	// hashCode() and equals() methods ------------------------------------------------------------	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((chapterList == null) ? 0 : chapterList.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + authorBookListRef.hashCode();
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Book other = (Book) obj;
		if (chapterList == null) {
			if (other.chapterList != null) return false;
		} else if (!chapterList.equals(other.chapterList)) return false;
		
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key)) return false;
		
		if (title == null) {
			if (other.title != null) return false;
		} else if (!title.equals(other.title)) return false;
		
		if ( !authorBookListRef.equals( other.authorBookListRef)) return false;
		
		return true;
	}
	// --------------------------------------------------------------------------------------------
}