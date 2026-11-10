package io.github.anjoismysign.psa.transfer;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MongoDB, laid out the way psa-mongo stores it: one collection per type,
 * '_id' is the identification and the rest of the document is Gson's JSON.
 */
public final class MongoRecordStore implements RecordStore {
    // same default as psa-mongo's MongoDatabaseCredentials
    private static final String DEFAULT_DATABASE = "psa";
    private static final JsonWriterSettings RELAXED = JsonWriterSettings.builder().outputMode(JsonMode.RELAXED).build();

    private final MongoClient client;
    private final MongoDatabase database;
    private final String description;

    public MongoRecordStore(String uri) {
        ConnectionString connectionString = new ConnectionString(uri);
        String databaseName = connectionString.getDatabase() == null || connectionString.getDatabase().isEmpty()
                ? DEFAULT_DATABASE
                : connectionString.getDatabase();
        this.client = MongoClients.create(connectionString);
        this.database = client.getDatabase(databaseName);
        this.description = "MongoDB " + String.join(",", connectionString.getHosts()) + "/" + databaseName;
    }

    @Override
    public String describe() {
        return description;
    }

    @Override
    public List<String> tables() {
        List<String> tables = new ArrayList<>();
        for (String name : database.listCollectionNames()) {
            if (!name.startsWith("system.")) {
                tables.add(name);
            }
        }
        return tables;
    }

    @Override
    public Map<String, String> readAll(String table) {
        Map<String, String> records = new LinkedHashMap<>();
        for (Document document : database.getCollection(table).find()) {
            Object id = document.remove("_id");
            records.put(String.valueOf(id), document.toJson(RELAXED));
        }
        return records;
    }

    @Override
    public void write(String table, Map<String, String> records) {
        if (records.isEmpty()) {
            return;
        }
        MongoCollection<Document> collection = database.getCollection(table);
        List<WriteModel<Document>> writes = new ArrayList<>();
        ReplaceOptions upsert = new ReplaceOptions().upsert(true);
        records.forEach((id, json) -> {
            Document document = new Document("_id", id);
            document.putAll(Document.parse(json));
            writes.add(new ReplaceOneModel<>(Filters.eq("_id", id), document, upsert));
        });
        collection.bulkWrite(writes);
    }

    @Override
    public Path backup(Path directory) throws Exception {
        return JsonBackup.write(this, directory);
    }

    @Override
    public Path defaultBackupDirectory() {
        return Path.of("psa-backups");
    }

    @Override
    public void close() {
        client.close();
    }
}
