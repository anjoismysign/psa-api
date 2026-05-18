package io.github.anjoismysign.psa.crud;

import com.google.gson.Gson;
import io.github.anjoismysign.psa.PostLoadable;
import io.github.anjoismysign.psa.PreUpdatable;
import io.github.anjoismysign.psa.UpdatableSerializable;
import io.github.anjoismysign.psa.sql.MySQLCrudDatabase;
import io.github.anjoismysign.psa.sql.SQLContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class MySQLCrudManager<T extends Crudable> implements SQLCrudManager<T> {
    private final Function<String, T> createFunction;
    private final MySQLCrudDatabase<T> database;
    private SQLContainer container;

    private static final Gson GSON = new Gson();

    public MySQLCrudManager(@NotNull MySQLCrudDatabase<T> database, @NotNull Function<String, T> createFunction) {
        this.createFunction = createFunction;
        this.database = database;
        this.load();
    }

    public void load() {
        this.container = this.database.generateContainer();
        boolean isNewTable = this.container
                .getDatabase()
                .createTable(
                        this.getTableName(),
                        this.getPrimaryKeyName() + " VARCHAR(" + this.getPrimaryKeyLength() + ")," + this.getCrudableKeyTypeName() + " BLOB",
                        this.getPrimaryKeyName()
                );
        if (isNewTable) {
            this.log(
                    "Create table "
                            + this.getTableName()
                            + " with primary key "
                            + this.getPrimaryKeyName()
                            + " and type "
                            + this.getCrudableKeyTypeName()
                            + " was executed successfully."
            );
        }
    }

    public String getCrudableKeyTypeName() {
        return this.database.getCrudableKeyTypeName();
    }

    public String getTableName() {
        return this.database.getTableName();
    }

    public String getPrimaryKeyName() {
        return this.database.getPrimaryKeyName();
    }

    public int getPrimaryKeyLength() {
        return this.database.getPrimaryKeyLength();
    }

    public Connection getConnection() {
        return this.container.getDatabase().getConnection();
    }

    @Override
    public void disconnect() {
        this.container.disconnect();
    }

    public boolean exists(String id) {
        boolean exists = this.container.getDatabase().exists(this.getTableName(), this.getPrimaryKeyName(), id);
        if (exists) {
            this.log("Record with id " + id + " exists.");
        } else {
            this.log("Record with id " + id + " does not exist.");
        }

        return exists;
    }

    public void update(T crudable) {
        if (crudable instanceof PreUpdatable preUpdatable) {
            preUpdatable.onPreUpdate();
        }
        String jsonString = GSON.toJson(crudable);
        String id = crudable.getIdentification();
        String sql = "UPDATE " + getTableName()
                + " SET " + crudableKeyTypePrepareStatement()
                + " WHERE " + getPrimaryKeyName() + "=?";
        try (Connection connection = container.getDatabase().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, jsonString);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void forEachRecord(BiConsumer<T, Integer> biConsumer) {
        container.getDatabase().selectAllFromDatabase(getTableName(), resultSet -> {
            try {
                String jsonString = resultSet.getString(getCrudableKeyTypeName());
                T crudable = GSON.fromJson(jsonString, database.type());
                if (crudable instanceof PostLoadable postLoadable) postLoadable.onPostLoad();
                log("Read record with id " + crudable.getIdentification() + ".");
                biConsumer.accept(crudable, 0);
            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        });
    }

    public void reload() {
        this.log("Reloading database...");
        this.container.disconnect();
        this.load();
    }

    public T create(String identification) {
        T crudable = createFunction.apply(identification);
        String sql = "INSERT IGNORE INTO " + getTableName()
                + " (" + getPrimaryKeyName() + ") VALUES (?)";
        try (Connection connection = container.getDatabase().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, identification);
            int rows = preparedStatement.executeUpdate();
            if (rows > 0) log("Created new record with id " + identification + ".");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return crudable;
    }

    @NotNull
    public T read(String id) {
        return this.readOrGenerate(id, () -> this.create(id));
    }

    @Nullable
    public T readOrNull(String id) {
        return this.readOrGenerate(id, () -> null);
    }

    public void delete(String id) {
        String sql = "DELETE FROM " + getTableName()
                + " WHERE " + getPrimaryKeyName() + "=?";
        try (Connection connection = container.getDatabase().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.executeUpdate();
            log("Deleted record with id " + id + ".");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    @Nullable
    public Logger getLogger() {
        return this.database.getLogger();
    }

    private T readOrGenerate(String id, Supplier<T> replacement) {
        AtomicReference<T> result = new AtomicReference<>();
        this.container.getDatabase().selectRowByPrimaryKey(this.getPrimaryKeyName(), id, this.getTableName(), resultSet -> {
            try {
                String jsonString = resultSet.getString(this.getCrudableKeyTypeName());
                T crudable = GSON.fromJson(jsonString, database.type());
                if (crudable instanceof PostLoadable postLoadable) {
                    postLoadable.onPostLoad();
                }
                this.log("Read record with id " + id + " successfully.");
                result.set(crudable);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        if (result.get() != null) {
            return result.get();
        }
        this.log("Record with id " + id + " does not exist (or error occurred).");
        return replacement.get();
    }

    private void log(@NotNull String message) {
        Logger logger = this.getLogger();
        if (logger != null) {
            logger.info(message);
        }
    }
}
