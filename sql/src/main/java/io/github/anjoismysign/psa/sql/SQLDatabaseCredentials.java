package io.github.anjoismysign.psa.sql;

import io.github.anjoismysign.psa.crud.CrudDatabase;
import io.github.anjoismysign.psa.crud.CrudDatabaseCredentials;
import io.github.anjoismysign.psa.crud.Crudable;
import io.github.anjoismysign.psa.lehmapp.LehmappCrudable;
import io.github.anjoismysign.psa.lehmapp.LehmappSerializable;
import io.github.anjoismysign.psa.serializablemanager.SerializableManager;
import io.github.anjoismysign.psa.serializablemanager.SimpleSerializableManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;
import java.util.logging.Logger;

public record SQLDatabaseCredentials(
        @NotNull String getDatabase,
        @NotNull Identifier getIdentifier,
        @Nullable Logger getLogger,
        @Nullable String getHostname,
        int getPort,
        @Nullable String getUsername,
        @Nullable String getPassword,
        @Nullable File localDatabaseDirectory
) implements CrudDatabaseCredentials {
    public static SQLDatabaseCredentials userHome(@NotNull Identifier identifier) {
        return new SQLDatabaseCredentials("database", identifier, null, null, 0, null, null, null);
    }

    public static SQLDatabaseCredentials at(@NotNull Identifier identifier, @NotNull File directory) {
        if (!directory.exists()) {
            directory.mkdirs();
        }

        return new SQLDatabaseCredentials("database", identifier, null, null, 0, null, null, directory);
    }

    @NotNull
    public <T extends Crudable> CrudDatabase getCrudDatabaseFor(@NotNull Class<T> type) {
        return this.isLocalhost() ? SQLiteCrudDatabase.of(type, this) : MySQLCrudDatabase.of(type, this);
    }

    @NotNull
    public <T extends LehmappSerializable> SerializableManager<T> getSerializableManager(@NotNull Class<T> type, @NotNull Function<LehmappCrudable, T> function) {
        return new SimpleSerializableManager(this, function);
    }

    public static SQLDatabaseCredentials ofConnectionString(@NotNull String string) {
        try {
            URI uri = new URI(string);

            String scheme = uri.getScheme();
            Identifier identifier = Identifier.valueOf(scheme.toUpperCase());

            String database = uri.getPath();
            if (database == null || database.isBlank()) {
                throw new IllegalArgumentException("Connection string must specify a database.");
            }
            if (database.startsWith("/")) {
                database = database.substring(1);
            }

            String username = null;
            String password = null;

            if (uri.getUserInfo() != null) {
                String[] split = uri.getUserInfo().split(":", 2);
                username = split[0];
                if (split.length == 2) {
                    password = split[1];
                }
            }

            return new SQLDatabaseCredentials(
                    database,
                    identifier,
                    null,
                    uri.getHost(),
                    uri.getPort(),
                    username,
                    password,
                    null
            );
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid SQL connection string: " + string, e);
        }
    }
}
