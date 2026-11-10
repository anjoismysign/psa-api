package io.github.anjoismysign.psa.mongo;

import io.github.anjoismysign.aesthetic.NamingConventions;
import io.github.anjoismysign.psa.crud.CrudDatabase;
import io.github.anjoismysign.psa.crud.CrudManager;
import io.github.anjoismysign.psa.crud.Crudable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.logging.Logger;

public record MongoCrudDatabase<T extends Crudable>(
        @NotNull String getConnectionString,
        @NotNull String getDatabaseName,
        @NotNull String getCollectionName,
        @Nullable Logger getLogger,
        @NotNull Class<T> type
) implements CrudDatabase<T> {

    /**
     * Collections are named after the type, the same way SQL tables are without the 'tbl' prefix.
     * For example, 'SerialPlayer' is stored in the 'SerialPlayers' collection (SQL: 'tblSerialPlayers').
     */
    public static <T extends Crudable> MongoCrudDatabase<T> of(@NotNull Class<T> type, @NotNull MongoDatabaseCredentials credentials) {
        String collectionName = NamingConventions.toPascalCase(type.getSimpleName()) + "s";
        return new MongoCrudDatabase<>(
                credentials.getConnectionString(),
                credentials.getDatabase(),
                collectionName,
                credentials.getLogger(),
                type
        );
    }

    @NotNull
    public CrudManager<T> crudManagerOf(@NotNull Function<String, T> createFunction) {
        return new MongoCrudManager<>(this, createFunction);
    }
}
