package org.jabref.logic.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

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
    private final int CURRENT_VERSION_DB_STRUCT = 1;

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

        connection.createStatement().executeUpdate("CREATE SCHEMA IF NOT EXISTS jabref");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + escape_Table("ENTRY") + " (" +
                        "\"SHARED_ID\" SERIAL PRIMARY KEY, " +
                        "\"TYPE\" VARCHAR, " +
                        "\"VERSION\" INTEGER DEFAULT 1)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + escape_Table("FIELD") + " (" +
                        "\"ENTRY_SHARED_ID\" INTEGER REFERENCES " + escape_Table("ENTRY") + "(\"SHARED_ID\") ON DELETE CASCADE, " +
                        "\"NAME\" VARCHAR, " +
                        "\"VALUE\" TEXT)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS " + escape_Table("METADATA") + " ("
                        + "\"KEY\" VARCHAR,"
                        + "\"VALUE\" TEXT)");

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
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("ENTRY") + " SELECT * FROM \"ENTRY\"");
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("FIELD") + " SELECT * FROM \"FIELD\"");
                connection.createStatement().executeUpdate("INSERT INTO " + escape_Table("METADATA") + " SELECT * FROM \"METADATA\"");
                connection.createStatement().execute("SELECT setval(\'jabref.\"ENTRY_SHARED_ID_seq\"\', (select max(\"SHARED_ID\") from jabref.\"ENTRY\"))");
                metadata = getSharedMetaData();
            }

            metadata.put(MetaData.VERSION_DB_STRUCT, String.valueOf(CURRENT_VERSION_DB_STRUCT));
            setSharedMetaData(metadata);
        }
    }

    @Override
    protected void insertIntoEntryTable(List<BibEntry> bibEntries) {
        StringBuilder insertIntoEntryQuery = new StringBuilder()
                .append("INSERT INTO ")
                .append(escape_Table("ENTRY"))
                .append("(")
                .append(escape("TYPE"))
                .append(") VALUES(?)");
        // Number of commas is bibEntries.size() - 1
        insertIntoEntryQuery.append(", (?)".repeat(Math.max(0, bibEntries.size() - 1)));
        try (PreparedStatement preparedEntryStatement = connection.prepareStatement(insertIntoEntryQuery.toString(),
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
    String escape(String expression) {
        return "\"" + expression + "\"";
    }

    @Override
    String escape_Table(String expression) {
        return "jabref." + escape(expression);
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
