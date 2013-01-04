package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2013-01-04 01:04:02")
/** */
public final class ChapterMeta extends org.slim3.datastore.ModelMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter> {

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter> title = new org.slim3.datastore.StringAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter>(this, "title", "title");

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter, java.lang.Integer> numPages = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter, java.lang.Integer>(this, "numPages", "numPages", int.class);

    private static final ChapterMeta slim3_singleton = new ChapterMeta();

    /**
     * @return the singleton
     */
    public static ChapterMeta get() {
       return slim3_singleton;
    }

    /** */
    public ChapterMeta() {
        super("Chapter", com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter.class);
    }

    @Override
    public com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter model = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter();
        model.setKey(entity.getKey());
        model.setTitle((java.lang.String) entity.getProperty("title"));
        model.setNumPages(longToPrimitiveInt((java.lang.Long) entity.getProperty("numPages")));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        entity.setProperty("title", m.getTitle());
        entity.setProperty("numPages", m.getNumPages());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter) is not defined.");
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
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter) model;
        writer.beginObject();
        org.slim3.datastore.json.Default encoder0 = new org.slim3.datastore.json.Default();
        if(m.getKey() != null){
            writer.setNextPropertyName("key");
            encoder0.encode(writer, m.getKey());
        }
        if(m.getTitle() != null){
            writer.setNextPropertyName("title");
            encoder0.encode(writer, m.getTitle());
        }
        writer.setNextPropertyName("numPages");
        encoder0.encode(writer, m.getNumPages());
        if(m.getAuthorChapterListRef() != null){
            writer.setNextPropertyName("authorChapterListRef");
            encoder0.encode(writer, m.getAuthorChapterListRef());
        }
        writer.endObject();
    }

    @Override
    protected com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter m = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.Chapter();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("title");
        m.setTitle(decoder0.decode(reader, m.getTitle()));
        reader = rootReader.newObjectReader("numPages");
        m.setNumPages(decoder0.decode(reader, m.getNumPages()));
        reader = rootReader.newObjectReader("authorChapterListRef");
        return m;
    }
}