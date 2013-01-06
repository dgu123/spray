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

@Model
public class PublisherAuthor {
	@Attribute( primaryKey = true)
	protected Key key;
	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}

	// For Bidirectional Many-to-Many Relationships Between Publisher class entity and Author class entity
	// --------------------------------------------------------------------------------------------
	protected ModelRef<Publisher> publisherRef = new ModelRef<Publisher>( Publisher.class);
	public ModelRef<Publisher> getPublisherRef() {
		return publisherRef;
	}
	protected ModelRef<Author> authorRef = new ModelRef<Author>( Author.class);
	public ModelRef<Author> getAuthorRef() {
		return authorRef;
	}
	// --------------------------------------------------------------------------------------------
}
