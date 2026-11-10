package io.github.anjoismysign.psa.transfer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite and MySQL, laid out the way psa-sql creates them:
 * 'tbl' + name, a primary key column and a JSON column.
 */
public final class SqlRecordStore implements RecordStore {
    private static final String TABLE_PREFIX = "tbl";
    private static final String PRIMARY_KEY = "UUID";

    private final Connection connection;
    private final boolean isMySQL;
    private final String description;
    private final Path backupDirectory;
    private final Path sqliteFile;

    private SqlRecordStore(Connection connection, boolean isMySQL, String description, Path backupDirectory, Path sqliteFile) {
        this.connection = connection;
        this.isMySQL = isMySQL;
        this.description = description;
        this.backupDirectory = backupDirectory;
        this.sqliteFile = sqliteFile;
    }

    public static SqlRecordStore sqlite(Path file) throws SQLException {
        Path absolute = file.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) {
            throw new IllegalArgumentException("SQLite file " + absolute + " doesn't exist");
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + absolute);
        return new SqlRecordStore(connection, false, "SQLite " + absolute, absolute.getParent().resolve("backups"), absolute);
    }

    /**
     * @param uri 'mysql://user:password@host:port/database'. BlobLib's 'uuid://' scheme is accepted too
     */
    public static SqlRecordStore mysql(String uri) throws Exception {
        URI parsed = new URI(uri);
        String database = parsed.getPath() == null ? "" : parsed.getPath().replaceFirst("^/", "");
        if (database.isEmpty()) {
            throw new IllegalArgumentException("MySQL connection string must name a database: " + uri);
        }
        int port = parsed.getPort() == -1 ? 3306 : parsed.getPort();
        String user = null;
        String password = null;
        if (parsed.getUserInfo() != null) {
            String[] split = parsed.getUserInfo().split(":", 2);
            user = split[0];
            password = split.length == 2 ? split[1] : null;
        }
        String jdbc = "jdbc:mysql://" + parsed.getHost() + ":" + port + "/" + database;
        Connection connection = DriverManager.getConnection(jdbc, user, password);
        return new SqlRecordStore(connection, true, "MySQL " + parsed.getHost() + ":" + port + "/" + database, Path.of("psa-backups"), null);
    }

    @Override
    public String describe() {
        return description;
    }

    @Override
    public List<String> tables() throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, TABLE_PREFIX + "%", new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                if (!name.startsWith(TABLE_PREFIX) || columns(name) == null) {
                    continue;
                }
                tables.add(name.substring(TABLE_PREFIX.length()));
            }
        }
        return tables;
    }

    @Override
    public Map<String, String> readAll(String table) throws SQLException {
        Map<String, String> records = new LinkedHashMap<>();
        String[] columns = columns(TABLE_PREFIX + table);
        if (columns == null) {
            return records;
        }
        String sql = "SELECT " + columns[0] + ", " + columns[1] + " FROM " + TABLE_PREFIX + table;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                String json = resultSet.getString(2);
                // psa inserts the identification alone before the first update
                if (json == null || json.isBlank()) {
                    continue;
                }
                records.put(resultSet.getString(1), json);
            }
        }
        return records;
    }

    @Override
    public void write(String table, Map<String, String> records) throws SQLException {
        String tableName = TABLE_PREFIX + table;
        String[] columns = columns(tableName);
        if (columns == null) {
            columns = new String[]{PRIMARY_KEY, dataColumn(table)};
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                        + columns[0] + " VARCHAR(128)," + columns[1] + (isMySQL ? " LONGTEXT" : " BLOB")
                        + ",PRIMARY KEY(" + columns[0] + "))");
            }
        }
        if (isMySQL) {
            // same widening psa-sql does, profile identifications don't fit VARCHAR(36)
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE " + tableName + " MODIFY " + columns[0] + " VARCHAR(128), MODIFY " + columns[1] + " LONGTEXT");
            }
        }
        String sql = isMySQL
                ? "INSERT INTO " + tableName + " (" + columns[0] + "," + columns[1] + ") VALUES (?,?) ON DUPLICATE KEY UPDATE " + columns[1] + "=VALUES(" + columns[1] + ")"
                : "INSERT INTO " + tableName + " (" + columns[0] + "," + columns[1] + ") VALUES (?,?) ON CONFLICT(" + columns[0] + ") DO UPDATE SET " + columns[1] + "=excluded." + columns[1];
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (var entry : records.entrySet()) {
                statement.setString(1, entry.getKey());
                statement.setString(2, entry.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
            connection.commit();
        } catch (SQLException exception) {
            connection.rollback();
            throw exception;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public Path backup(Path directory) throws Exception {
        if (sqliteFile == null) {
            return JsonBackup.write(this, directory);
        }
        Files.createDirectories(directory);
        Path target = directory.resolve(sqliteFile.getFileName() + ".backup-" + JsonBackup.timestamp());
        // a consistent copy, even with uncommitted WAL pages
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("VACUUM INTO '" + target.toAbsolutePath().toString().replace("'", "''") + "'");
        }
        return target;
    }

    @Override
    public Path defaultBackupDirectory() {
        return backupDirectory;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    /**
     * psa names the JSON column after the type, e.g. 'SerialPlayers' -> 'SERIALPLAYER'
     */
    private static String dataColumn(String table) {
        String type = table.endsWith("s") ? table.substring(0, table.length() - 1) : table;
        return type.toUpperCase(Locale.ROOT);
    }

    /**
     * @return the primary key and JSON column names, null if the table isn't laid out by psa
     */
    private String[] columns(String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName + " LIMIT 0")) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            if (metaData.getColumnCount() != 2) {
                return null;
            }
            return new String[]{metaData.getColumnName(1), metaData.getColumnName(2)};
        } catch (SQLException exception) {
            return null;
        }
    }
}
