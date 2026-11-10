package io.github.anjoismysign.psa.transfer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backups of any RecordStore as one JSON file:
 * {"source": "...", "tables": {"SerialPlayers": {"<identification>": {...}}}}
 */
public final class JsonBackup {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static String timestamp() {
        return LocalDateTime.now().format(TIMESTAMP);
    }

    public static Path write(RecordStore store, Path directory) throws Exception {
        Files.createDirectories(directory);
        Path target = directory.resolve("psa-backup-" + timestamp() + ".json");
        JsonObject root = new JsonObject();
        root.addProperty("source", store.describe());
        JsonObject tables = new JsonObject();
        for (String table : store.tables()) {
            JsonObject records = new JsonObject();
            store.readAll(table).forEach((id, json) -> records.add(id, JsonParser.parseString(json)));
            tables.add(table, records);
        }
        root.add("tables", tables);
        try (Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
        }
        return target;
    }

    /**
     * @return table to (identification to JSON)
     */
    public static Map<String, Map<String, String>> read(Path file) throws Exception {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }
        Map<String, Map<String, String>> tables = new LinkedHashMap<>();
        root.getAsJsonObject("tables").entrySet().forEach(table -> {
            Map<String, String> records = new LinkedHashMap<>();
            table.getValue().getAsJsonObject().entrySet()
                    .forEach(record -> records.put(record.getKey(), record.getValue().toString()));
            tables.put(table.getKey(), records);
        });
        return tables;
    }
}
