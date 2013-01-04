package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2013-01-04 01:04:02")
/** */
public final class AuthorMeta extends org.slim3.datastore.ModelMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author> {

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author> name = new org.slim3.datastore.StringAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author>(this, "name", "name");

    private static final AuthorMeta slim3_singleton = new AuthorMeta();

    /**
     * @return the singleton
     */
    public static AuthorMeta get() {
       return slim3_singleton;
    }

    /** */
    public AuthorMeta() {
        super("Author", com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author.class);
    }

    @Override
    public com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author model = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author();
        model.setKey(entity.getKey());
        model.setName((java.lang.String) entity.getProperty("name"));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        entity.setProperty("name", m.getName());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author) is not defined.");
    }

    @Override
    protected void assignKeyToModelRefIfNecessary(com.google.appengine.api.datastore.AsyncDatastoreService ds, java.lang.Object model) {
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
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author) model;
        writer.beginObject();
        org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
        if(m.getKey() != null){
            writer.setNextPropertyName("key");
            encoder0.encode(writer, m.getKey());
        }
        if(m.getName() != null){
            writer.setNextPropertyName("name");
            encoder0.encode(writer, m.getName());
        }
        if(m.getAuthorBookListRef() != null){
            writer.setNextPropertyName("authorBookListRef");
            encoder0.encode(writer, m.getAuthorBookListRef());
        }
        if(m.getAuthorChapterListRef() != null){
            writer.setNextPropertyName("authorChapterListRef");
            encoder0.encode(writer, m.getAuthorChapterListRef());
        }
        writer.endObject();
    }

    @Override
    protected com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author m = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.Author();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("name");
        m.setName(decoder0.decode(reader, m.getName()));
        reader = rootReader.newObjectReader("authorBookListRef");
        reader = rootReader.newObjectReader("authorChapterListRef");
        return m;
    }
}