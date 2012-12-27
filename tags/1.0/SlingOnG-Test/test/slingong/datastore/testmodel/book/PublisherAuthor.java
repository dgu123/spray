package slingong.datastore.testmodel.book;

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
