package io.github.anjoismysign.psa.transfer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Command line tool to back up, convert and move psa data between SQLite, MySQL and MongoDB.
 * Stop the server before using it, so nothing writes to the database meanwhile.
 */
public final class Main {
    private static final String USAGE = """
            Usage: java -jar psa-transfer.jar <command> ...
            (or psa-migrations.jar, which adds the layout conversions)

              list    <database>
                  Lists the tables and how many records each has.
              backup  <database> [--dir <directory>]
                  Copies everything stored. SQLite is copied to a .db file, MySQL and MongoDB to a .json file.
              convert <database> [--dir <directory>] [--dry-run]
                  Rewrites records saved in an older layout in place, using the LayoutConversions
                  on the classpath. Backs the database up first.
              copy    <from> <to> [--tables A,B] [--overwrite] [--convert] [--dir <directory>]
                  Copies every table (or the given ones) into another database. Backs <to> up first.
                  Refuses to write into tables that already have records unless --overwrite is given,
                  in which case records with the same identification are replaced.
              restore <backup.json> <database> [--tables A,B]
                  Writes a .json backup back. Records with the same identification are replaced.

            <database> is one of:
              sqlite:<path to database.db>
              mysql://user:password@host:3306/database   (BlobLib's 'uuid://' connection strings work too)
              mongodb://user:password@host:27017/database

            Tables are named without SQL's 'tbl' prefix, e.g. SerialPlayers.
            """;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print(USAGE);
            System.exit(1);
        }
        try {
            Options options = Options.parse(Arrays.copyOfRange(args, 1, args.length));
            switch (args[0]) {
                case "list" -> list(options.argument(0));
                case "backup" -> backup(options.argument(0), options);
                case "convert" -> convert(options.argument(0), options);
                case "copy" -> copy(options.argument(0), options.argument(1), options);
                case "restore" -> restore(Path.of(options.argument(0)), options.argument(1), options);
                default -> {
                    System.out.print(USAGE);
                    System.exit(1);
                }
            }
        } catch (Exception exception) {
            System.err.println("Failed: " + exception.getMessage());
            exception.printStackTrace();
            System.exit(2);
        }
    }

    private static void list(String uri) throws Exception {
        try (RecordStore store = RecordStore.open(uri)) {
            System.out.println(store.describe());
            for (String table : store.tables()) {
                System.out.println("  " + table + ": " + store.readAll(table).size());
            }
        }
    }

    private static void backup(String uri, Options options) throws Exception {
        try (RecordStore store = RecordStore.open(uri)) {
            Path backup = store.backup(options.directory(store));
            System.out.println("Backed up " + store.describe() + " to " + backup.toAbsolutePath());
        }
    }

    private static void convert(String uri, Options options) throws Exception {
        Map<String, LayoutConversion> conversions = conversions();
        try (RecordStore store = RecordStore.open(uri)) {
            List<String> tables = store.tables();
            List<String> convertible = tables.stream().filter(conversions::containsKey).toList();
            if (convertible.isEmpty()) {
                System.out.println("Nothing to convert: " + store.describe() + " has " + tables + ", none of them has a conversion");
                return;
            }
            Map<String, Map<String, String>> pending = new LinkedHashMap<>();
            for (String table : convertible) {
                Map<String, String> records = store.readAll(table);
                Map<String, String> converted = convertAll(records, conversions.get(table));
                System.out.println(table + ": " + converted.size() + " of " + records.size() + " records need converting");
                if (!converted.isEmpty()) {
                    pending.put(table, converted);
                }
            }
            if (pending.isEmpty()) {
                System.out.println("Nothing to convert, every record is already in the current layout.");
                return;
            }
            if (options.dryRun) {
                System.out.println("Dry run, nothing was written.");
                return;
            }
            Path backup = store.backup(options.directory(store));
            System.out.println("Backed up " + store.describe() + " to " + backup.toAbsolutePath());
            for (var entry : pending.entrySet()) {
                store.write(entry.getKey(), entry.getValue());
                verify(store, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void copy(String fromUri, String toUri, Options options) throws Exception {
        Map<String, LayoutConversion> conversions = options.convert ? conversions() : Map.of();
        try (RecordStore from = RecordStore.open(fromUri);
             RecordStore to = RecordStore.open(toUri)) {
            if (from.describe().equals(to.describe())) {
                throw new IllegalArgumentException("<from> and <to> are the same database");
            }
            List<String> tables = options.tables(from.tables());
            List<String> destinationTables = to.tables();
            List<String> occupied = new ArrayList<>();
            for (String table : tables) {
                if (destinationTables.contains(table) && !to.readAll(table).isEmpty()) {
                    occupied.add(table);
                }
            }
            if (!occupied.isEmpty() && !options.overwrite) {
                throw new IllegalStateException(to.describe() + " already has records in " + occupied + ". Use --overwrite to replace records with the same identification");
            }
            Path backup = to.backup(options.directory(to));
            System.out.println("Backed up " + to.describe() + " to " + backup.toAbsolutePath());
            System.out.println("Copying " + from.describe() + " -> " + to.describe());
            for (String table : tables) {
                Map<String, String> records = from.readAll(table);
                if (options.convert && conversions.containsKey(table)) {
                    records.putAll(convertAll(records, conversions.get(table)));
                }
                to.write(table, records);
                verify(to, table, records);
            }
        }
    }

    private static void restore(Path file, String uri, Options options) throws Exception {
        Map<String, Map<String, String>> backup = JsonBackup.read(file);
        try (RecordStore store = RecordStore.open(uri)) {
            Path current = store.backup(options.directory(store));
            System.out.println("Backed up " + store.describe() + " to " + current.toAbsolutePath());
            for (String table : options.tables(new ArrayList<>(backup.keySet()))) {
                Map<String, String> records = backup.get(table);
                store.write(table, records);
                verify(store, table, records);
            }
        }
    }

    /**
     * @throws IllegalStateException if there are none, so a conversion isn't silently skipped
     */
    private static Map<String, LayoutConversion> conversions() {
        Map<String, LayoutConversion> conversions = LayoutConversion.load();
        if (conversions.isEmpty()) {
            throw new IllegalStateException("No LayoutConversions on the classpath. Use psa-migrations.jar, or add a jar with them to the classpath");
        }
        System.out.println("Layout conversions for " + conversions.keySet());
        return conversions;
    }

    /**
     * @return only the records the conversion changed, converted
     */
    private static Map<String, String> convertAll(Map<String, String> records, LayoutConversion conversion) {
        Map<String, String> converted = new LinkedHashMap<>();
        records.forEach((id, json) -> {
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            if (conversion.convert(object)) {
                converted.put(id, object.toString());
            }
        });
        return converted;
    }

    /**
     * Reads the written records back and checks they hold the same JSON.
     */
    private static void verify(RecordStore store, String table, Map<String, String> written) throws Exception {
        Map<String, String> stored = store.readAll(table);
        List<String> mismatches = new ArrayList<>();
        written.forEach((id, json) -> {
            String read = stored.get(id);
            if (read == null || !JsonParser.parseString(read).equals(JsonParser.parseString(json))) {
                mismatches.add(id);
            }
        });
        if (!mismatches.isEmpty()) {
            throw new IllegalStateException(table + ": " + mismatches.size() + " records don't match after writing: " + mismatches);
        }
        System.out.println("  " + table + ": wrote and verified " + written.size() + " records");
    }

    private static final class Options {
        private final List<String> arguments = new ArrayList<>();
        private List<String> tables;
        private Path directory;
        private boolean overwrite;
        private boolean convert;
        private boolean dryRun;

        static Options parse(String[] args) {
            Options options = new Options();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--tables" -> options.tables = Arrays.asList(args[++i].split(","));
                    case "--dir" -> options.directory = Path.of(args[++i]);
                    case "--overwrite" -> options.overwrite = true;
                    case "--convert" -> options.convert = true;
                    case "--dry-run" -> options.dryRun = true;
                    default -> options.arguments.add(args[i]);
                }
            }
            return options;
        }

        String argument(int index) {
            if (index >= arguments.size()) {
                throw new IllegalArgumentException("Missing argument, run without arguments to see the usage");
            }
            return arguments.get(index);
        }

        Path directory(RecordStore store) {
            return directory == null ? store.defaultBackupDirectory() : directory;
        }

        List<String> tables(List<String> available) {
            if (tables == null) {
                return available;
            }
            List<String> missing = tables.stream().filter(table -> !available.contains(table)).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("No tables named " + missing + ". Available: " + available);
            }
            return tables;
        }
    }
}
