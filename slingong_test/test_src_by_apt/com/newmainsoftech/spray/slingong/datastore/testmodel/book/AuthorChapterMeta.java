package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2013-01-04 01:04:02")
/** */
public final class AuthorChapterMeta extends org.slim3.datastore.ModelMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter> {

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, java.lang.Integer> authorPriority = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, java.lang.Integer>(this, "authorPriority", "authorPriority", int.class);

    /** */
    public final org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author> authorRef = new org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>(this, "authorRef", "authorRef", org.slim3.datastore.ModelRef.class, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author.class);

    /** */
    public final org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter> chapterRef = new org.slim3.datastore.ModelRefAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter, org.slim3.datastore.ModelRef<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter>, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter>(this, "chapterRef", "chapterRef", org.slim3.datastore.ModelRef.class, com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter.class);

    private static final AuthorChapterMeta slim3_singleton = new AuthorChapterMeta();

    /**
     * @return the singleton
     */
    public static AuthorChapterMeta get() {
       return slim3_singleton;
    }

    /** */
    public AuthorChapterMeta() {
        super("AuthorChapter", com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter.class);
    }

    @Override
    public com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter model = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter();
        model.setKey(entity.getKey());
        model.setAuthorPriority(longToPrimitiveInt((java.lang.Long) entity.getProperty("authorPriority")));
        if (model.getAuthorRef() == null) {
            throw new NullPointerException("The property(authorRef) is null.");
        }
        model.getAuthorRef().setKey((com.google.appengine.api.datastore.Key) entity.getProperty("authorRef"));
        if (model.getChapterRef() == null) {
            throw new NullPointerException("The property(chapterRef) is null.");
        }
        model.getChapterRef().setKey((com.google.appengine.api.datastore.Key) entity.getProperty("chapterRef"));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        entity.setProperty("authorPriority", m.getAuthorPriority());
        if (m.getAuthorRef() == null) {
            throw new NullPointerException("The property(authorRef) must not be null.");
        }
        entity.setProperty("authorRef", m.getAuthorRef().getKey());
        if (m.getChapterRef() == null) {
            throw new NullPointerException("The property(chapterRef) must not be null.");
        }
        entity.setProperty("chapterRef", m.getChapterRef().getKey());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter) is not defined.");
    }

    @Override
    protected void assignKeyToModelRefIfNecessary(com.google.appengine.api.datastore.AsyncDatastoreService ds, java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter) model;
        if (m.getAuthorRef() == null) {
            throw new NullPointerException("The property(authorRef) must not be null.");
        }
        m.getAuthorRef().assignKeyIfNecessary(ds);
        if (m.getChapterRef() == null) {
            throw new NullPointerException("The property(chapterRef) must not be null.");
        }
        m.getChapterRef().assignKeyIfNecessary(ds);
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
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter) model;
        writer.beginObject();
        org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
        if(m.getKey() != null){
            writer.setNextPropertyName("key");
            encoder0.encode(writer, m.getKey());
        }
        writer.setNextPropertyName("authorPriority");
        encoder0.encode(writer, m.getAuthorPriority());
        if(m.getAuthorRef() != null && m.getAuthorRef().getKey() != null){
            writer.setNextPropertyName("authorRef");
            encoder0.encode(writer, m.getAuthorRef(), maxDepth, currentDepth);
        }
        if(m.getChapterRef() != null && m.getChapterRef().getKey() != null){
            writer.setNextPropertyName("chapterRef");
            encoder0.encode(writer, m.getChapterRef(), maxDepth, currentDepth);
        }
        writer.endObject();
    }

    @Override
    protected com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter m = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.AuthorChapter();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("authorPriority");
        m.setAuthorPriority(decoder0.decode(reader, m.getAuthorPriority()));
        reader = rootReader.newObjectReader("authorRef");
        decoder0.decode(reader, m.getAuthorRef(), maxDepth, currentDepth);
        reader = rootReader.newObjectReader("chapterRef");
        decoder0.decode(reader, m.getChapterRef(), maxDepth, currentDepth);
        return m;
    }
}