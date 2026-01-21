package io.github.anjoismysign.psa.sql;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class SQLDatabase {
    protected final Logger logger;
    protected final String database;
    protected final String hostName;
    protected final int port;
    protected final String user;
    protected final String password;
    protected final HikariDataSource dataSource;

    protected SQLDatabase(String hostName, int port, String database, String user, String password, Logger logger) {
        this.logger = logger;
        this.dataSource = new HikariDataSource();
        this.database = database;
        this.hostName = hostName;
        this.port = port;
        this.user = user;
        this.password = password;
        this.dataSource.setUsername(user);
        this.dataSource.setPassword(password);
        this.dataSource.setMaximumPoolSize(200);
        this.dataSource.setMinimumIdle(5);
        this.dataSource.setLeakDetectionThreshold(15000L);
        this.dataSource.setConnectionTimeout(1000L);
    }

    public abstract Connection getConnection();

    public final void disconnect() {
        try {
            this.dataSource.close();
        } catch (Exception var2) {
            var2.printStackTrace();
        }
    }

    public final String getDatabase() {
        return this.database;
    }

    public void selectRowByPrimaryKey(String keyType, String key, String table, Consumer<ResultSet> action) {
        String sql = "SELECT * FROM " + table + " WHERE " + keyType + "=?";
        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    action.accept(resultSet);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PreparedStatement updateDataSet(String keyType, String table, String values) {
        try {
            Connection connection = this.getConnection();
            return connection.prepareStatement("UPDATE " + table + " SET " + values + " WHERE " + keyType + "=?");
        } catch (SQLException var6) {
            var6.printStackTrace();
            return null;
        }
    }

    public PreparedStatement delete(String table, String keyType) {
        try {
            Connection connection = this.getConnection();
            return connection.prepareStatement("DELETE FROM " + table + " WHERE " + keyType + "=?");
        } catch (SQLException var5) {
            var5.printStackTrace();
            return null;
        }
    }

    public boolean createTable(String table, String columns, String primaryKey) {
        try (Connection connection = this.getConnection()) {
            if (connection == null) {
                return false;
            }
            String sql = "CREATE TABLE IF NOT EXISTS " + table + " (" + columns + ",PRIMARY KEY(" + primaryKey + "))";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exists(String table, String keyType, String key) {
        String sql = "SELECT * FROM " + table + " WHERE " + keyType + "=?";
        try (Connection connection = this.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, key);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void selectAllFromDatabase(String table, Consumer<ResultSet> forEach) {
        String sql = "select * from " + table;
        try (Connection connection = this.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                forEach.accept(resultSet);
            }

        } catch (SQLException var5) {
            var5.printStackTrace();
        }
    }

    public Logger getLogger() {
        return this.logger;
    }
}
