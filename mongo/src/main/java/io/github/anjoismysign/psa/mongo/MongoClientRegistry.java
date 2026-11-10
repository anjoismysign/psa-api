package io.github.anjoismysign.psa.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shares one MongoClient per connection string, closing it once nothing uses it.
 */
public final class MongoClientRegistry {
    private record Entry(MongoClient client, AtomicInteger count) {}

    private static final Map<String, Entry> registry = new ConcurrentHashMap<>();

    public static MongoClient acquire(String connectionString) {
        return registry.compute(connectionString, (k, existing) -> {
            if (existing == null) {
                return new Entry(MongoClients.create(connectionString), new AtomicInteger(1));
            }
            existing.count().incrementAndGet();
            return existing;
        }).client();
    }

    public static void release(String connectionString) {
        registry.computeIfPresent(connectionString, (k, entry) -> {
            if (entry.count().decrementAndGet() == 0) {
                entry.client().close();
                return null;
            }
            return entry;
        });
    }
}
