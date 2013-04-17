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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slim3.datastore.Attribute;
import org.slim3.datastore.InverseModelListRef;
import org.slim3.datastore.Model;

import com.google.appengine.api.datastore.Key;

@Model
public class Publisher implements Serializable {
	@Attribute( primaryKey = true)
	protected Key key;	
	public Key getKey() {
		return key;
	}
	public void setKey(Key key) {
		this.key = key;
	}
	
	protected String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	// For Bidirectional Many-to-Many Relationships Between Publisher class entity and Author class entity 
	// --------------------------------------------------------------------------------------------
	protected List<Key> authorKeys = Collections.synchronizedList( new ArrayList<Key>());
	public List<Key> getAuthorKeys() {
		return authorKeys;
	}
	public void setAuthorKeys(List<Key> authorKeys) {
		this.authorKeys = authorKeys;
	}

	@Attribute( persistent = false)
	private InverseModelListRef<PublisherAuthor, Publisher> publisherAuthorListRef = 
		new InverseModelListRef<PublisherAuthor, Publisher>( PublisherAuthor.class, "publisherRef", this);
	public InverseModelListRef<PublisherAuthor, Publisher> getPublisherAuthorListRef() {
		return publisherAuthorListRef;
	}
	// --------------------------------------------------------------------------------------------
}
