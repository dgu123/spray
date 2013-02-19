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
