package com.newmainsoftech.spray.slingong.datastore.testmodel.book;

//@javax.annotation.Generated(value = { "slim3-gen", "@VERSION@" }, date = "2013-01-04 01:04:02")
/** */
public final class PublisherMeta extends org.slim3.datastore.ModelMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher> {

    /** */
    public final org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher, com.google.appengine.api.datastore.Key> key = new org.slim3.datastore.CoreAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher, com.google.appengine.api.datastore.Key>(this, "__key__", "key", com.google.appengine.api.datastore.Key.class);

    /** */
    public final org.slim3.datastore.StringAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher> name = new org.slim3.datastore.StringAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher>(this, "name", "name");

    /** */
    public final org.slim3.datastore.CollectionAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher, java.util.List<com.google.appengine.api.datastore.Key>, com.google.appengine.api.datastore.Key> authorKeys = new org.slim3.datastore.CollectionAttributeMeta<com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher, java.util.List<com.google.appengine.api.datastore.Key>, com.google.appengine.api.datastore.Key>(this, "authorKeys", "authorKeys", java.util.List.class);

    private static final PublisherMeta slim3_singleton = new PublisherMeta();

    /**
     * @return the singleton
     */
    public static PublisherMeta get() {
       return slim3_singleton;
    }

    /** */
    public PublisherMeta() {
        super("Publisher", com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher.class);
    }

    @Override
    public com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher entityToModel(com.google.appengine.api.datastore.Entity entity) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher model = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher();
        model.setKey(entity.getKey());
        model.setName((java.lang.String) entity.getProperty("name"));
        model.setAuthorKeys(toList(com.google.appengine.api.datastore.Key.class, entity.getProperty("authorKeys")));
        return model;
    }

    @Override
    public com.google.appengine.api.datastore.Entity modelToEntity(java.lang.Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher) model;
        com.google.appengine.api.datastore.Entity entity = null;
        if (m.getKey() != null) {
            entity = new com.google.appengine.api.datastore.Entity(m.getKey());
        } else {
            entity = new com.google.appengine.api.datastore.Entity(kind);
        }
        entity.setProperty("name", m.getName());
        entity.setProperty("authorKeys", m.getAuthorKeys());
        return entity;
    }

    @Override
    protected com.google.appengine.api.datastore.Key getKey(Object model) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher) model;
        return m.getKey();
    }

    @Override
    protected void setKey(Object model, com.google.appengine.api.datastore.Key key) {
        validateKey(key);
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher) model;
        m.setKey(key);
    }

    @Override
    protected long getVersion(Object model) {
        throw new IllegalStateException("The version property of the model(com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher) is not defined.");
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
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher m = (com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher) model;
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
        if(m.getAuthorKeys() != null){
            writer.setNextPropertyName("authorKeys");
            writer.beginArray();
            for(com.google.appengine.api.datastore.Key v : m.getAuthorKeys()){
                encoder0.encode(writer, v);
            }
            writer.endArray();
        }
        if(m.getPublisherAuthorListRef() != null){
            writer.setNextPropertyName("publisherAuthorListRef");
            encoder0.encode(writer, m.getPublisherAuthorListRef());
        }
        writer.endObject();
    }

    @Override
    protected com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher jsonToModel(org.slim3.datastore.json.JsonRootReader rootReader, int maxDepth, int currentDepth) {
        com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher m = new com.newmainsoftech.spray.slingong.datastore.testmodel.book.Publisher();
        org.slim3.datastore.json.JsonReader reader = null;
        org.slim3.datastore.json.Default decoder0 = new org.slim3.datastore.json.Default();
        reader = rootReader.newObjectReader("key");
        m.setKey(decoder0.decode(reader, m.getKey()));
        reader = rootReader.newObjectReader("name");
        m.setName(decoder0.decode(reader, m.getName()));
        reader = rootReader.newObjectReader("authorKeys");
        {
            java.util.ArrayList<com.google.appengine.api.datastore.Key> elements = new java.util.ArrayList<com.google.appengine.api.datastore.Key>();
            org.slim3.datastore.json.JsonArrayReader r = rootReader.newArrayReader("authorKeys");
            if(r != null){
                reader = r;
                int n = r.length();
                for(int i = 0; i < n; i++){
                    r.setIndex(i);
                    com.google.appengine.api.datastore.Key v = decoder0.decode(reader, (com.google.appengine.api.datastore.Key)null)                    ;
                    if(v != null){
                        elements.add(v);
                    }
                }
                m.setAuthorKeys(elements);
            }
        }
        reader = rootReader.newObjectReader("publisherAuthorListRef");
        return m;
    }
}