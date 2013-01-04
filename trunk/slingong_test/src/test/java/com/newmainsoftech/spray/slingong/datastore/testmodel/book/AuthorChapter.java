package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Model;
import org.slim3.datastore.ModelRef;

import com.google.appengine.api.datastore.Key;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;

@Model
public class AuthorChapter {
	@Attribute( primaryKey = true)
	protected Key key;
	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}
	
	// Holds priority among co-authors for chapter
	protected int authorPriority = AuthorComparatorUnit.MINIMUM_PRIORITY - 1;
	public int getAuthorPriority() {
		return authorPriority;
	}
	public void setAuthorPriority(int authorPriority) {
		this.authorPriority = authorPriority;
	}

	// For Bidirectional Many-to-Many Relationships Between Author class entity and Chapter class entity
	// --------------------------------------------------------------------------------------------
	protected ModelRef<Author> authorRef = new ModelRef<Author>( Author.class);
	public ModelRef<Author> getAuthorRef() {
		return authorRef;
	}

	protected ModelRef<Chapter> chapterRef = new ModelRef<Chapter>( Chapter.class);
	public ModelRef<Chapter> getChapterRef() {
		return chapterRef;
	}
	// --------------------------------------------------------------------------------------------
}
