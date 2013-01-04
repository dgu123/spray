package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2013-01-04 01:04:02")
/** */
public final class PublisherAuthorMeta extends org.slim3.datastore.ModelMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor> {

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher> publisherRef = new org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher>(this, "publisherRef", "publisherRef", org.slim3.datastore.ModelRef.class, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher.class);

    /** */
    public final org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author> authorRef = new org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>(this, "authorRef", "authorRef", org.slim3.datastore.ModelRef.class, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author.class);

    private static final PublisherAuthorMeta slim3_singleton = new PublisherAuthorMeta();

    /**
     * @return the singleton
     */
    public static PublisherAuthorMeta get() {
       return slim3_singleton;
    }

    /** */
    public PublisherAuthorMeta() {
        super("PublisherAuthor", com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor.class);
    }

    @Override
    public com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor model = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor();
        model.setKey(entity.getKey());
        if (model.getPublisherRef() == null) {
            throw new NullPointerException("The property(publisherRef) is null.");
        }
        model.getPublisherRef().setKey((com.google.appengine.api.datastore.Key) entity.getProperty("publisherRef"));
        if (model.getAuthorRef() == null) {
            throw new NullPointerException("The property(authorRef) is null.");
        }
        model.getAuthorRef().setKey((com.google.appengine.api.datastore.Key) entity.getProperty("authorRef"));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        if (m.getPublisherRef() == null) {
            throw new NullPointerException("The property(publisherRef) must not be null.");
        }
        entity.setProperty("publisherRef", m.getPublisherRef().getKey());
        if (m.getAuthorRef() == null) {
            throw new NullPointerException("The property(authorRef) must not be null.");
        }
        entity.setProperty("authorRef", m.getAuthorRef().getKey());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor) is not defined.");
    }

    @Override
    protected void assignKeyToModelRefIfNecessary(com.google.appengine.api.datastore.AsyncDatastoreService ds, java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor) model;
        if (m.getPublisherRef() == null) {
            throw new NullPointerException("The property(publisherRef) must not be null.");
        }
        m.getPublisherRef().assignKeyIfNecessary(ds);
        if (m.getAuthorRef() == null) {
            throw new NullPointerException("The property(authorRef) must not be null.");
        }
        m.getAuthorRef().assignKeyIfNecessary(ds);
    }

    @Override
    protected void incrementVersion(Object model) {
    }

    @Override
    protected void prePut(Object model) {
    }

    @Override
    protected void postGet(Object model) {
    }

    @Override
    public String getSchemaVersionName() {
        return "slim3.schemaVersion";
    }

    @Override
    public String getClassHierarchyListName() {
        return "slim3.classHierarchyList";
    }

    @Override
    protected boolean isCipherProperty(String propertyName) {
        return false;
    }

    @Override
    protected void modelToJson(org.slim3.datastore.json.JsonWriter writer, java.lang.Object model, int maxDepth, int currentDepth) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor) model;
        writer.beginObject();
        org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
        if(m.getKey() != null){
            writer.setNextPropertyName("key");
            encoder0.encode(writer, m.getKey());
        }
        if(m.getPublisherRef() != null && m.getPublisherRef().getKey() != null){
            writer.setNextPropertyName("publisherRef");
            encoder0.encode(writer, m.getPublisherRef(), maxDepth, currentDepth);
        }
        if(m.getAuthorRef() != null && m.getAuthorRef().getKey() != null){
            writer.setNextPropertyName("authorRef");
            encoder0.encode(writer, m.getAuthorRef(), maxDepth, currentDepth);
        }
        writer.endObject();
    }

    @Override
    protected com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor m = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.PublisherAuthor();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("publisherRef");
        decoder0.decode(reader, m.getPublisherRef(), maxDepth, currentDepth);
        reader = rootReader.newObjectReader("authorRef");
        decoder0.decode(reader, m.getAuthorRef(), maxDepth, currentDepth);
        return m;
    }
}