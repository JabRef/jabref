package org.jabref.logic.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.jabref.logic.shared.listener.PostgresSQLNotificationListener;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.metadata.MetaData;

import org.postgresql.PGConnection;

/**
 * Processes all incoming or outgoing bib data to PostgreSQL database and manages its structure.
 */
public class PostgreSQLProcessor extends DBMSProcessor {

    private PostgresSQLNotificationListener listener;

    private int VERSION_DB_STRUCT_DEFAULT = -1;

    // TODO: We need to migrate data - or ask the user to recreate
    private final int CURRENT_VERSION_DB_STRUCT = 2;

    public PostgreSQLProcessor(DatabaseConnection connection) {
        super(connection);
    }

    /**
     * Creates and sets up the needed tables and columns according to the database type.
     *
     * @throws SQLException in case of error
     */
    @Override
    public void setUp() throws SQLException {
        if (CURRENT_VERSION_DB_STRUCT == 1 && checkTableAvailability("ENTRY", "FIELD", "METADATA")) {
            // checkTableAvailability does not distinguish if same table name exists in different schemas
            // VERSION_DB_STRUCT_DEFAULT must be forced
            VERSION_DB_STRUCT_DEFAULT = 0;
        }

        // TODO: Before a release, fix the names (and migrate data to the new names)
        //       Think of using Flyway or Liquibase instead of manual migration
        // If changed, also adjust {@link org.jabref.logic.shared.TestManager.clearTables}
        connection.createStatement().executeUpdate("CREATE SCHEMA IF NOT EXISTS \"jabref-alpha\"");
        connection.createStatement().executeUpdate("SET search_path TO \"jabref-alpha\"");

        connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS entry (
                        shared_id SERIAL PRIMARY KEY,
                        entrytype VARCHAR,
                        version INTEGER DEFAULT 1
                    )
                """);

        connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS field (
                        entry_shared_id INTEGER REFERENCES entry(shared_id) ON DELETE CASCADE,
                        name VARCHAR,
                        value TEXT
                    )
                """);
        connection.createStatement().executeUpdate("CREATE INDEX idx_field_entry_shared_id ON FIELD (ENTRY_SHARED_ID);");
        connection.createStatement().executeUpdate("CREATE INDEX idx_field_name ON FIELD (NAME);");

        connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS metadata (
                        key VARCHAR,
                        value TEXT
                    )
                """);
        connection.createStatement().executeUpdate("CREATE UNIQUE INDEX idx_metadata_key ON METADATA (key);");

        Map<String, String> metadata = getSharedMetaData();

        if (metadata.get(MetaData.VERSION_DB_STRUCT) != null) {
            try {
                // replace semicolon so we can parse it
                VERSION_DB_STRUCT_DEFAULT = Integer.parseInt(metadata.get(MetaData.VERSION_DB_STRUCT).replace(";", ""));
            } catch (Exception e) {
                LOGGER.warn("[VERSION_DB_STRUCT_DEFAULT] is not an Integer.");
            }
        } else {
            LOGGER.warn("[VERSION_DB_STRUCT_DEFAULT] does not exist.");
        }

        String upsertMetadata = """
                CREATE OR REPLACE FUNCTION upsert_metadata(key TEXT, value TEXT) RETURNS VOID AS $$
                DECLARE
                    existing_value TEXT;
                BEGIN
                    -- Check if the key already exists and get its current value
                    SELECT VALUE INTO existing_value FROM METADATA WHERE KEY = key;

                    -- Perform the upsert
                    INSERT INTO METADATA (KEY, VALUE)
                    VALUES (key, value)
                    ON CONFLICT (KEY)
                    DO UPDATE SET VALUE = EXCLUDED.VALUE;

                    -- Notify only if the value has changed
                    IF existing_value IS DISTINCT FROM value THEN
                        PERFORM pg_notify('metadata_update', json_build_object('key', key, 'value', value)::TEXT);
                    END IF;
                END;
                $$ LANGUAGE plpgsql;
                """;
        connection.createStatement().executeUpdate(upsertMetadata);

        if (VERSION_DB_STRUCT_DEFAULT < CURRENT_VERSION_DB_STRUCT) {
            // We can migrate data from old tables in new table
            if (VERSION_DB_STRUCT_DEFAULT == 0 && CURRENT_VERSION_DB_STRUCT == 1) {
                LOGGER.info("Migrating from VersionDBStructure == 0");
                connection.createStatement().executeUpdate("INSERT INTO ENTRY SELECT * FROM \"ENTRY\"");
                connection.createStatement().executeUpdate("INSERT INTO FIELD SELECT * FROM \"FIELD\"");
                connection.createStatement().executeUpdate("INSERT INTO METADATA SELECT * FROM \"METADATA\"");
                connection.createStatement().execute("SELECT setval(\'\"ENTRY_SHARED_ID_seq\"\', (select max(\"SHARED_ID\") from \"ENTRY\"))");
                metadata = getSharedMetaData();
            }

            metadata.put(MetaData.VERSION_DB_STRUCT, String.valueOf(CURRENT_VERSION_DB_STRUCT));
            setSharedMetaData(metadata);
        }

        // TODO: implement migration of changes from version 1 to 2
        // - "TYPE" is now called entrytype (to be consistent with org.jabref.model.entry.field.InternalField.TYPE_HEADER)
        // - table names and field names now lower case
    }

    @Override
    protected void insertIntoEntryTable(List<BibEntry> bibEntries) {
        if (bibEntries.isEmpty()) {
            return;
        }

        StringJoiner insertIntoEntryQuery = new StringJoiner(", ", "INSERT INTO entry (entrytype) values ", ";");
        for (int i = 0; i < bibEntries.size(); i++) {
            insertIntoEntryQuery.add("(?)");
        }
        try (PreparedStatement preparedEntryStatement = connection.prepareStatement(
                insertIntoEntryQuery.toString(),
                Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < bibEntries.size(); i++) {
                preparedEntryStatement.setString(i + 1, bibEntries.get(i).getType().getName());
            }
            preparedEntryStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedEntryStatement.getGeneratedKeys()) {
                // The following assumes that we get the generated keys in the order the entries were inserted
                // This should be the case
                for (BibEntry bibEntry : bibEntries) {
                    generatedKeys.next();
                    bibEntry.getSharedBibEntryData().setSharedID(generatedKeys.getInt(1));
                }
                if (generatedKeys.next()) {
                    LOGGER.error("Some shared IDs left unassigned");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error during entry insertion", e);
        }
    }

    @Override
    Integer getCURRENT_VERSION_DB_STRUCT() {
        return CURRENT_VERSION_DB_STRUCT;
    }

    @Override
    public void startNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        // Disable cleanup output of ThreadedHousekeeper
        // Logger.getLogger(ThreadedHousekeeper.class.getName()).setLevel(Level.SEVERE);
        try {
            connection.createStatement().execute("LISTEN jabrefLiveUpdate");
            // Do not use `new PostgresSQLNotificationListener(...)` as the object has to exist continuously!
            // Otherwise, the listener is going to be deleted by Java's garbage collector.
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            listener = new PostgresSQLNotificationListener(dbmsSynchronizer, pgConnection);
            HeadlessExecutorService.INSTANCE.execute(listener);
        } catch (SQLException e) {
            LOGGER.error("SQL Error during starting the notification listener", e);
        }
    }

    @Override
    public void stopNotificationListener() {
        try {
            listener.stop();
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error during stopping the notification listener", e);
        }
    }

    @Override
    public void notifyClients() {
        try {
            connection.createStatement().execute("NOTIFY jabrefLiveUpdate, '" + PROCESSOR_ID + "';");
        } catch (SQLException e) {
            LOGGER.error("SQL Error during client notification", e);
        }
    }
}
