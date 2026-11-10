package io.github.anjoismysign.psa.mongo;

import com.mongodb.ConnectionString;
import io.github.anjoismysign.psa.crud.CrudDatabase;
import io.github.anjoismysign.psa.crud.CrudDatabaseCredentials;
import io.github.anjoismysign.psa.crud.Crudable;
import io.github.anjoismysign.psa.lehmapp.LehmappCrudable;
import io.github.anjoismysign.psa.lehmapp.LehmappSerializable;
import io.github.anjoismysign.psa.serializablemanager.SerializableManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.function.Function;
import java.util.logging.Logger;

public record MongoDatabaseCredentials(
        @NotNull String getConnectionString,
        @NotNull String getDatabase,
        @NotNull Identifier getIdentifier,
        @Nullable Logger getLogger
) implements CrudDatabaseCredentials {
    public static final String DEFAULT_DATABASE = "psa";

    /**
     * @param string a MongoDB connection string, such as 'mongodb://user:password@localhost:27017/database'.
     *               If it doesn't name a database, {@link #DEFAULT_DATABASE} is used
     */
    public static MongoDatabaseCredentials ofConnectionString(@NotNull String string) {
        ConnectionString connectionString = new ConnectionString(string);
        String database = connectionString.getDatabase();
        if (database == null || database.isEmpty()) {
            database = DEFAULT_DATABASE;
        }
        return new MongoDatabaseCredentials(string, database, Identifier.UUID, null);
    }

    @NotNull
    public <T extends Crudable> CrudDatabase getCrudDatabaseFor(@NotNull Class<T> type) {
        return MongoCrudDatabase.of(type, this);
    }

    @NotNull
    public <T extends LehmappSerializable> SerializableManager<T> getSerializableManager(@NotNull Class<T> type, @NotNull Function<LehmappCrudable, T> function) {
        throw new UnsupportedOperationException("Lehmapp is not supported by MongoDB");
    }

    @Nullable
    public String getHostname() {
        var hosts = new ConnectionString(getConnectionString).getHosts();
        return hosts.isEmpty() ? null : hosts.get(0);
    }

    public int getPort() {
        return 0;
    }

    @Nullable
    public String getUsername() {
        return new ConnectionString(getConnectionString).getUsername();
    }

    @Nullable
    public String getPassword() {
        char[] password = new ConnectionString(getConnectionString).getPassword();
        return password == null ? null : new String(password);
    }

    @Nullable
    public File localDatabaseDirectory() {
        return null;
    }

    @Override
    public boolean isLocalhost() {
        return false;
    }
}
