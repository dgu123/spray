package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;

@Model
public class AuthorBook {
	@Attribute( primaryKey = true)
	protected Key key;
	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}
	
	// Holds priority among co-authors for book
	protected int authorPriority = AuthorComparatorUnit.MINIMUM_PRIORITY - 1;
	public int getAuthorPriority() {
		return authorPriority;
	}
	/* authorPriority will be validated by beforeSetAuthorPriority before-advice in AuthorBookAspect class
	 */
	public void setAuthorPriority( int authorPriority) {
		this.authorPriority = authorPriority;
	}
	
	// For Bidirectional Many-to-Many Relationships Between Author class entity and Book class entity
	// --------------------------------------------------------------------------------------------
	protected ModelRef<Author> authorRef = new ModelRef<Author>( Author.class);
	public ModelRef<Author> getAuthorRef() {
		return authorRef;
	}
	protected ModelRef<Book> bookRef = new ModelRef<Book>( Book.class);
	public ModelRef<Book> getBookRef() {
		return bookRef;
	}
	// --------------------------------------------------------------------------------------------
	
	// --------------------------------------------------------------------------------------------
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + authorPriority;
		result = prime * result
				+ ((authorRef == null) ? 0 : authorRef.hashCode());
		result = prime * result + ((bookRef == null) ? 0 : bookRef.hashCode());
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null)return false;
		if (getClass() != obj.getClass()) return false;
		
		AuthorBook other = (AuthorBook) obj;
		if (authorPriority != other.authorPriority) return false;
		if (authorRef == null) {
			if (other.authorRef != null) return false;
		} else if (!authorRef.equals(other.authorRef)) return false;
		
		if (bookRef == null) {
			if (other.bookRef != null) return false;
		} else if (!bookRef.equals(other.bookRef)) return false;
		
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key)) return false;
		
		return true;
	}
	// --------------------------------------------------------------------------------------------
	
}
