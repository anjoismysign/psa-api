package io.github.anjoismysign.psa.transfer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Raw access to the records psa stores: each table (SQL) or collection (MongoDB)
 * maps a Crudable's identification to the JSON Gson wrote for it.
 * Tables are named without the SQL 'tbl' prefix, e.g. 'SerialPlayers'.
 */
public interface RecordStore extends AutoCloseable {

    static RecordStore open(String uri) throws Exception {
        if (uri.startsWith("sqlite:")) {
            return SqlRecordStore.sqlite(Path.of(uri.substring("sqlite:".length())));
        }
        if (uri.startsWith("mongodb://") || uri.startsWith("mongodb+srv://")) {
            return new MongoRecordStore(uri);
        }
        if (uri.startsWith("mysql://") || uri.startsWith("uuid://") || uri.startsWith("user_name://")) {
            return SqlRecordStore.mysql(uri);
        }
        throw new IllegalArgumentException("Unknown database '" + uri + "'. Use sqlite:<file>, mysql://user:password@host:port/database or mongodb://user:password@host:port/database");
    }

    /**
     * @return a description that doesn't include passwords
     */
    String describe();

    List<String> tables() throws Exception;

    /**
     * @return identification to JSON, in storage order
     */
    Map<String, String> readAll(String table) throws Exception;

    /**
     * Inserts the records, replacing the ones with the same identification.
     * Creates the table if it doesn't exist.
     */
    void write(String table, Map<String, String> records) throws Exception;

    /**
     * Copies everything stored into the directory.
     *
     * @return the backup file
     */
    Path backup(Path directory) throws Exception;

    /**
     * @return where backups go when no directory is given
     */
    Path defaultBackupDirectory();

    @Override
    void close() throws Exception;
}
