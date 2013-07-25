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
/**
 * 
 */
package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

import java.util.Comparator;

public class AuthorComparatorFactory {
	public static class AuthorComparatorUnit {
		protected Author author;
		public Author getAuthor() {
			return author;
		}

		public static int MINIMUM_PRIORITY = 1;
		protected int priority;
		public int getPriority() {
			return priority;
		}

		public AuthorComparatorUnit( Author author, int priority) {
			this.author = author;
			this.priority = priority;
		}
	}
	
	protected static Comparator<AuthorComparatorUnit> comparator = 
		new Comparator<AuthorComparatorUnit>() {		
			@Override
			public int compare( AuthorComparatorUnit o1, AuthorComparatorUnit o2) {
				int priority1 = o1.getPriority();
				int priority2 = o2.getPriority();
				
				if ( priority1 == priority2) {
					return o1.getAuthor().getName().compareTo( o2.getAuthor().getName());
				}
				else if ( priority1 < AuthorComparatorUnit.MINIMUM_PRIORITY) return 1;
				else if ( priority2 < AuthorComparatorUnit.MINIMUM_PRIORITY) return -1;
				return (priority1 < priority2) ? -1 : 1;
			}
		};
	

	public static Comparator<AuthorComparatorUnit> getComparator() {
		return comparator;
	}
}