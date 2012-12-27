/**
 * 
 */
package slingong.datastore.testmodel.book;

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