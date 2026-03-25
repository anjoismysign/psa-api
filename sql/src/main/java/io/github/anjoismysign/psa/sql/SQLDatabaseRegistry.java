package io.github.anjoismysign.psa.sql;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SQLDatabaseRegistry {
    private record Entry(SQLDatabase database, AtomicInteger count) {}
    
    private static final Map<String, Entry> registry = new ConcurrentHashMap<>();

    public static SQLDatabase acquire(String key, Supplier<SQLDatabase> factory) {
        Entry entry = registry.computeIfAbsent(key,
                k -> new Entry(factory.get(), new AtomicInteger(0)));
        entry.count().incrementAndGet();
        return entry.database();
    }

    public static void release(String key) {
        registry.computeIfPresent(key, (k, entry) -> {
            if (entry.count().decrementAndGet() == 0) {
                entry.database().disconnect();
                return null;
            }
            return entry;
        });
    }

    public static String keyFor(File directory, String name) {
        String dbName = name.endsWith(".db") ? name : name + ".db";
        return new File(directory, dbName).getAbsolutePath();
    }

    public static String keyFor(String host, int port, String database, String user) {
        return user + "@" + host + ":" + port + "/" + database;
    }
}