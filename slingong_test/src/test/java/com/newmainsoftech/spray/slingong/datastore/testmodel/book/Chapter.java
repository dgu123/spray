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
package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.Datastore;
import org.slim3.datastore.GlobalTransaction;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Model;
import org.springframework.transaction.annotation.Transactional;

import com.google.appengine.api.datastore.Key;
import com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorComparatorFactory.AuthorComparatorUnit;


/**
 * Based on http://gae-java-persistence.blogspot.com/2009/10/creating-bidrectional-owned-one-to-many.html
 * 
 * Chapter class object will be sub-entity of Book entity group.
 */
@Model
public class Chapter implements Serializable {
	
	/* Should be child key to Book parent entity.
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
	
    protected int numPages;
	public int getNumPages() {
		return numPages;
	}
	public void setNumPages(int numPages) {
		this.numPages = numPages;
	}

	// TODO add order member field what is for order among chapters.
	
	// For Bidirectional Many-to-Many Relationships Between Author class entity and Chapter class entity
	// --------------------------------------------------------------------------------------------
	@Attribute( persistent = false)
	private InverseModelListRef<AuthorChapter, Chapter> authorChapterListRef = 
		new InverseModelListRef<AuthorChapter, Chapter>( AuthorChapter.class, "chapterRef", this);
	public InverseModelListRef<AuthorChapter, Chapter> getAuthorChapterListRef() {
		return authorChapterListRef;
	}
	// --------------------------------------------------------------------------------------------
	
	public List<AuthorComparatorUnit> getAuthors() {
		LinkedList<AuthorComparatorUnit> authorList = new LinkedList<AuthorComparatorUnit>();
		for( AuthorChapter authorChapter : getAuthorChapterListRef().getModelList()) {
			AuthorComparatorUnit authorComparatorUnit = 
				new AuthorComparatorUnit( 
						authorChapter.getAuthorRef().getModel(), 
						authorChapter.getAuthorPriority()
						);
			authorList.add( authorComparatorUnit);
		} // for
		Collections.sort( authorList, AuthorComparatorFactory.getComparator());
		return authorList;
	}
	
	@Transactional
	public void setAuthorPriority( Key authorKey, int priority) {
		for( AuthorChapter authorChapter : getAuthorChapterListRef().getModelList()) {
			if ( authorKey.equals( authorChapter.getAuthorRef().getKey())) {
				authorChapter.setAuthorPriority( priority);
				GlobalTransaction gtx = Datastore.getCurrentGlobalTransaction();
				gtx.put( authorChapter);
				break; // for
			}
		} // for
	}
	
	// hashCode() and equals() methods ------------------------------------------------------------	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + numPages;
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + getAuthorChapterListRef().hashCode();
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		Chapter other = (Chapter) obj;
		if (key == null) {
			if (other.key != null) return false;
		} else if (!key.equals(other.key)) return false;
		
		if (numPages != other.numPages) return false;
		
		if (title == null) {
			if (other.title != null) return false;
		} else if (!title.equals(other.title)) return false;
		
		if ( !getAuthorChapterListRef().equals( other.getAuthorChapterListRef())) return false;
			
		return true;
	}
	// --------------------------------------------------------------------------------------------
}