package io.github.anjoismysign.psa.mongo;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import io.github.anjoismysign.psa.PostLoadable;
import io.github.anjoismysign.psa.PreUpdatable;
import io.github.anjoismysign.psa.crud.CrudManager;
import io.github.anjoismysign.psa.crud.Crudable;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Stores each Crudable as a document whose '_id' is its identification.
 * The document holds the same fields Gson writes, nested, so they can be read and queried.
 */
public class MongoCrudManager<T extends Crudable> implements CrudManager<T> {
    private static final Gson GSON = new Gson();
    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final Function<String, T> createFunction;
    private final MongoCrudDatabase<T> database;
    private final MongoCollection<Document> collection;

    public MongoCrudManager(@NotNull MongoCrudDatabase<T> database, @NotNull Function<String, T> createFunction) {
        this.createFunction = createFunction;
        this.database = database;
        this.collection = MongoClientRegistry.acquire(database.getConnectionString())
                .getDatabase(database.getDatabaseName())
                .getCollection(database.getCollectionName());
    }

    @NotNull
    public static Document toDocument(@NotNull Crudable crudable) {
        Document document = new Document("_id", crudable.getIdentification());
        document.putAll(Document.parse(GSON.toJson(crudable)));
        return document;
    }

    /**
     * @return the document as the JSON Gson wrote, without '_id'
     */
    @NotNull
    public static String toJson(@NotNull Document document) {
        Document copy = new Document(document);
        copy.remove("_id");
        return copy.toJson(RELAXED);
    }

    public String getCollectionName() {
        return database.getCollectionName();
    }

    @Override
    public void disconnect() {
        MongoClientRegistry.release(database.getConnectionString());
    }

    @Override
    public boolean exists(String id) {
        return collection.countDocuments(Filters.eq("_id", id), new CountOptions().limit(1)) > 0;
    }

    @Override
    @NotNull
    public T create(String identification) {
        T crudable = createFunction.apply(identification);
        // only inserts if nothing is stored under this identification yet
        Document setOnInsert = new Document("$setOnInsert", toDocument(crudable));
        var result = collection.updateOne(Filters.eq("_id", identification), setOnInsert, new UpdateOptions().upsert(true));
        if (result.getUpsertedId() != null) {
            log("Created new record with id " + identification + ".");
        }
        return crudable;
    }

    @Override
    @NotNull
    public T read(String id) {
        return readOrGenerate(id, () -> create(id));
    }

    @Override
    @Nullable
    public T readOrNull(String id) {
        return readOrGenerate(id, () -> null);
    }

    @Override
    public void update(T crudable) {
        if (crudable instanceof PreUpdatable preUpdatable) {
            preUpdatable.onPreUpdate();
        }
        collection.replaceOne(Filters.eq("_id", crudable.getIdentification()), toDocument(crudable), new ReplaceOptions().upsert(true));
    }

    @Override
    public void delete(String id) {
        collection.deleteOne(Filters.eq("_id", id));
        log("Deleted record with id " + id + ".");
    }

    public void forEachRecord(Consumer<T> consumer) {
        forEachRecord((crudable, version) -> consumer.accept(crudable));
    }

    public void forEachRecord(BiConsumer<T, Integer> consumer) {
        for (Document document : collection.find()) {
            T crudable = fromDocument(document);
            if (crudable != null) {
                consumer.accept(crudable, 0);
            }
        }
    }

    @Override
    @Nullable
    public Logger getLogger() {
        return database.getLogger();
    }

    @Nullable
    private T fromDocument(@NotNull Document document) {
        T crudable = GSON.fromJson(toJson(document), database.type());
        if (crudable instanceof PostLoadable postLoadable) {
            postLoadable.onPostLoad();
        }
        return crudable;
    }

    private T readOrGenerate(String id, Supplier<T> replacement) {
        @Nullable Document document = collection.find(Filters.eq("_id", id)).first();
        @Nullable T crudable = document == null ? null : fromDocument(document);
        if (crudable != null) {
            log("Read record with id " + id + " successfully.");
            return crudable;
        }
        log("Record with id " + id + " does not exist.");
        return replacement.get();
    }

    private void log(@NotNull String message) {
        Logger logger = getLogger();
        if (logger != null) {
            logger.info(message);
        }
    }
}
