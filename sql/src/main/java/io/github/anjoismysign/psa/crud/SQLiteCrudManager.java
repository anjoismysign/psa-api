package io.github.anjoismysign.psa.crud;

import com.google.gson.Gson;
import io.github.anjoismysign.psa.PostLoadable;
import io.github.anjoismysign.psa.UpdatableSerializable;
import io.github.anjoismysign.psa.sql.SQLContainer;
import io.github.anjoismysign.psa.sql.SQLiteCrudDatabase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class SQLiteCrudManager<T extends Crudable> implements SQLCrudManager<T> {
    private final Function<String, T> createFunction;
    private final SQLiteCrudDatabase<T> database;
    private SQLContainer container;

    public SQLiteCrudManager(@NotNull SQLiteCrudDatabase<T> database, @NotNull Function<String, T> createFunction) {
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
        Gson gson = new Gson();
        String jsonString = gson.toJson(crudable);
        String id = crudable.getIdentification();
        PreparedStatement statement = this.container
                .getDatabase()
                .updateDataSet(this.getPrimaryKeyName(), this.getTableName(), this.crudableKeyTypePrepareStatement());

        try {
            statement.setString(1, jsonString);
            statement.setString(2, id);
            statement.executeUpdate();
        } catch (SQLException var15) {
            var15.printStackTrace();
        } finally {
            try {
                statement.close();
                statement.getConnection().close();
            } catch (SQLException var14) {
                var14.printStackTrace();
            }
        }
    }

    public void forEachRecord(BiConsumer<T, Integer> biConsumer) {
        this.container.getDatabase().selectAllFromDatabase(this.getTableName(), resultSet -> {
            try {
                byte[] bytes = resultSet.getBytes(this.getCrudableKeyTypeName());
                UpdatableSerializable<T> updatableSerializable = UpdatableSerializable.deserialize(bytes);
                T crudable = updatableSerializable.getValue();
                this.log("Read record with id " + crudable.getIdentification() + " successfully.");
                biConsumer.accept(crudable, updatableSerializable.getVersion());
            } catch (SQLException var6) {
                var6.printStackTrace();
            }
        });
    }

    public void reload() {
        this.log("Reloading database...");
        this.container.disconnect();
        this.load();
    }

    public T create(String identification) {
        T crudable = this.createFunction.apply(identification);

        String sql = "INSERT OR IGNORE INTO " + this.getTableName();
        try (Connection connection = this.container.getDatabase().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(sql + " (" + this.getPrimaryKeyName() + ") VALUES (?)");
            if (!this.exists(identification)) {
                preparedStatement.setString(1, identification);
                preparedStatement.executeUpdate();
                this.log("Created new record with id " + identification + ".");
            }

            if (preparedStatement != null) {
                preparedStatement.close();
            }
        } catch (SQLException var14) {
            var14.printStackTrace();
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
        PreparedStatement preparedStatement = this.container.getDatabase().delete(this.getTableName(), this.getPrimaryKeyName());

        try {
            preparedStatement.setString(1, id);
            preparedStatement.executeUpdate();
            this.log("Deleted record with id " + id + ".");
        } catch (SQLException var12) {
            var12.printStackTrace();
        } finally {
            try {
                preparedStatement.close();
                preparedStatement.getConnection().close();
            } catch (SQLException var11) {
                var11.printStackTrace();
            }
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
                Gson gson = new Gson();
                T crudable = gson.fromJson(jsonString, database.type());
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
