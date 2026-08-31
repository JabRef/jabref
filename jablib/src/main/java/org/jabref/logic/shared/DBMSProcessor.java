package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.logic.shared.notifications.NotificationListener;
import org.jabref.logic.shared.notifications.Notifier;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.metadata.MetaData;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import io.github.thibaultmeyer.cuid.CUID;
import org.jspecify.annotations.NonNull;
import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Processes all incoming or outgoing bib data to external SQL Database and manages its structure.
public class DBMSProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DBMSProcessor.class);

    protected final Connection connection;

    protected DatabaseConnectionProperties connectionProperties;

    // Identifies this processor among all clients connected to the same database.
    // Deliberately per instance, not static: two synchronizers in the same JVM (two open shared
    // libraries) must not mistake each other's notifications for their own.
    private final String processorId = CUID.randomCUID2(8).toString();

    private final DatabaseConnection dbmsConnection;

    private NotificationListener listener;

    // The listener needs its own connection: polling for notifications blocks the connection it runs on
    private Connection listenerConnection;

    private int VERSION_DB_STRUCT_DEFAULT = -1;

    private int CURRENT_VERSION_DB_STRUCT = 2;

    protected DBMSProcessor(DatabaseConnection dbmsConnection) {
        this.dbmsConnection = dbmsConnection;
        this.connection = dbmsConnection.getConnection();
        this.connectionProperties = dbmsConnection.getProperties();
    }

    public String getProcessorId() {
        return processorId;
    }

    /// Scans the database for required tables.
    ///
    /// @return <code>true</code> if the structure matches the requirements, <code>false</code> if not.
    /// @throws SQLException in case of error
    public boolean checkBaseIntegrity() throws SQLException {
        boolean databasePassesIntegrityCheck = false;
        Map<String, String> metadata = getSharedMetaData();
        String metadataVersion = metadata.get(MetaData.VERSION_DB_STRUCT);
        if (metadataVersion != null) {
            int VERSION_DB_STRUCT = Integer.parseInt(metadata.getOrDefault(MetaData.VERSION_DB_STRUCT, "").replace(";", ""));
            if (VERSION_DB_STRUCT == getCURRENT_VERSION_DB_STRUCT()) {
                databasePassesIntegrityCheck = true;
            }
        }
        return databasePassesIntegrityCheck;
    }

    /// Determines whether the database is using an pre-3.6 structure.
    ///
    /// @return <code>true</code> if the structure is old, else <code>false</code>.
    public boolean databaseIsAtMostJabRef35() throws SQLException {
        return checkTableAvailability(
                "ENTRIES",
                "ENTRY_GROUP",
                "ENTRY_TYPES",
                "GROUPS",
                "GROUP_TYPES",
                "JABREF_DATABASE",
                "STRINGS"); // old tables
    }

    /// Checks whether all given table names (<b>case insensitive</b>) exist in database.
    ///
    /// @param tableNames Table names to be checked
    /// @return <code>true</code> if <b>all</b> given tables are present, else <code>false</code>.
    protected boolean checkTableAvailability(String... tableNames) throws SQLException {
        List<String> requiredTables = new ArrayList<>();
        for (String name : tableNames) {
            requiredTables.add(name.toUpperCase(Locale.ENGLISH));
        }

        DatabaseMetaData databaseMetaData = connection.getMetaData();
        // ...getTables(null, ...): no restrictions
        try (ResultSet databaseMetaDataResultSet = databaseMetaData.getTables(null, null, null, null)) {
            while (databaseMetaDataResultSet.next()) {
                String tableName = databaseMetaDataResultSet.getString("TABLE_NAME").toUpperCase(Locale.ROOT);
                requiredTables.remove(tableName); // Remove matching tables to check requiredTables for emptiness
            }
            return requiredTables.isEmpty();
        }
    }

    /// Creates and sets up the needed tables and columns according to the database type and performs a check whether the
    /// needed tables are present.
    ///
    /// @throws SQLException in case of error
    public void setupSharedDatabase() throws SQLException {
        setUp();

        if (!checkBaseIntegrity()) {
            // can only happen with users direct intervention on shared database
            LOGGER.error("Corrupt_shared_database_structure.");
        }
    }

    /// Creates and sets up the needed tables and columns according to the database type.
    ///
    /// @throws SQLException in case of error
    public void setUp() throws SQLException {
        // TODO: Think of using Flyway or Liquibase instead of manual migration
        //       Liquibase:
        //         - https://contribute.liquibase.com/extensions-integrations/directory/integration-docs/gradle
        //         - https://forum.liquibase.org/t/adding-liquibase-to-an-existing-project/6076
        // If the schema name is changed, also adjust [TestManager#clearTables]
        // The old (structure version <= 1) quoted upper-case tables coexist in the same schema
        connection.createStatement().executeUpdate("CREATE SCHEMA IF NOT EXISTS jabref");
        connection.createStatement().executeUpdate("SET search_path TO jabref");

        // TODO: entrytype should be moved to table "field" (org.jabref.model.entry.field.InternalField.TYPE_HEADER)
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
        connection.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_field_entry_shared_id ON FIELD (ENTRY_SHARED_ID);");
        connection.createStatement().executeUpdate("CREATE INDEX IF NOT EXISTS idx_field_name ON FIELD (NAME);");

        connection.createStatement().executeUpdate("""
                    CREATE TABLE IF NOT EXISTS metadata (
                        key VARCHAR,
                        value TEXT
                    )
                """);
        connection.createStatement().executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_metadata_key ON METADATA (key);");

        migrateFromOldStructure();

        Map<String, String> metadata = getSharedMetaData();

        if (metadata.get(MetaData.VERSION_DB_STRUCT) != null) {
            try {
                // replace semicolon so we can parse it
                VERSION_DB_STRUCT_DEFAULT = Integer.parseInt(metadata.get(MetaData.VERSION_DB_STRUCT).replace(";", ""));
            } catch (NumberFormatException e) {
                LOGGER.warn("[VERSION_DB_STRUCT_DEFAULT] is not an Integer.");
            }
        } else {
            LOGGER.warn("[VERSION_DB_STRUCT_DEFAULT] does not exist.");
        }

        // Parameters must not be named key/value: inside the function body they would be
        // ambiguous with the equally named columns.
        // CREATE OR REPLACE cannot rename parameters, hence the preceding DROP.
        String upsertMetadata = """
                DROP FUNCTION IF EXISTS upsert_metadata(TEXT, TEXT);
                CREATE FUNCTION upsert_metadata(metadata_key TEXT, metadata_value TEXT) RETURNS VOID AS $$
                DECLARE
                    existing_value TEXT;
                BEGIN
                    -- Check if the key already exists and get its current value
                    SELECT VALUE INTO existing_value FROM METADATA WHERE KEY = metadata_key;

                    -- Perform the upsert
                    INSERT INTO METADATA (KEY, VALUE)
                    VALUES (metadata_key, metadata_value)
                    ON CONFLICT (KEY)
                    DO UPDATE SET VALUE = EXCLUDED.VALUE;

                    -- Notify only if the value has changed
                    IF existing_value IS DISTINCT FROM metadata_value THEN
                        PERFORM pg_notify('%s', json_build_object('key', metadata_key, 'value', metadata_value)::TEXT);
                    END IF;
                END;
                $$ LANGUAGE plpgsql;
                """.formatted(Notifier.METADATA_CHANNEL);
        connection.createStatement().executeUpdate(upsertMetadata);

        if (VERSION_DB_STRUCT_DEFAULT < CURRENT_VERSION_DB_STRUCT) {
            metadata.put(MetaData.VERSION_DB_STRUCT, String.valueOf(CURRENT_VERSION_DB_STRUCT));
            setSharedMetaData(metadata);
        }
    }

    /// Copies the data of a shared database created by earlier JabRef versions into the current
    /// table structure: quoted upper-case tables in the schema `jabref` (structure version 1,
    /// JabRef 5.x/6.0-alpha) or in the default schema (structure version 0, JabRef < 5).
    /// `"TYPE"` became `entrytype`, all other names only changed case.
    ///
    /// The old tables are kept untouched, so older JabRef versions can still work with them.
    // [impl->req~shared-database.migration~1]
    private void migrateFromOldStructure() throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT EXISTS (SELECT 1 FROM entry)")) {
            resultSet.next();
            if (resultSet.getBoolean(1)) {
                // Only a freshly created database is migrated into - never one that is already used
                return;
            }
        }

        Optional<String> oldSchema = findOldSchema();
        if (oldSchema.isEmpty()) {
            return;
        }

        LOGGER.info("Migrating shared database from old structure in schema \"{}\"", oldSchema.get());
        connection.createStatement().executeUpdate(
                "INSERT INTO entry (shared_id, entrytype, version) SELECT \"SHARED_ID\", \"TYPE\", \"VERSION\" FROM %s.\"ENTRY\"".formatted(oldSchema.get()));
        connection.createStatement().executeUpdate(
                "INSERT INTO field (entry_shared_id, name, value) SELECT \"ENTRY_SHARED_ID\", \"NAME\", \"VALUE\" FROM %s.\"FIELD\"".formatted(oldSchema.get()));
        connection.createStatement().executeUpdate(
                "INSERT INTO metadata (key, value) SELECT \"KEY\", \"VALUE\" FROM %s.\"METADATA\"".formatted(oldSchema.get()));
        // The serial has to continue after the copied ids
        connection.createStatement().execute(
                "SELECT setval(pg_get_serial_sequence('entry', 'shared_id'), COALESCE((SELECT MAX(shared_id) FROM entry), 0) + 1, false)");
    }

    private Optional<String> findOldSchema() throws SQLException {
        for (String schema : List.of("jabref", "public")) {
            try (ResultSet resultSet = connection.createStatement().executeQuery(
                    "SELECT to_regclass('%s.\"ENTRY\"')".formatted(schema))) {
                if (resultSet.next() && (resultSet.getString(1) != null)) {
                    return Optional.of(schema);
                }
            }
        }
        return Optional.empty();
    }

    int getCURRENT_VERSION_DB_STRUCT() {
        return CURRENT_VERSION_DB_STRUCT;
    }

    /// For use in test only. Inserts the BibEntry into the shared database.
    ///
    /// @param bibEntry [BibEntry] to be inserted.
    @VisibleForTesting
    public void insertEntry(BibEntry bibEntry) {
        insertEntries(List.of(bibEntry));
    }

    public void insertEntries(List<BibEntry> bibEntries) {
        List<BibEntry> notYetExistingEntries = getNotYetExistingEntries(bibEntries);
        insertIntoEntryTable(notYetExistingEntries);
        insertIntoFieldTable(notYetExistingEntries);
    }

    /// Filters a list of BibEntry to those which do not yet exist in the database
    private List<BibEntry> getNotYetExistingEntries(List<BibEntry> bibEntries) {
        List<Integer> localIds = bibEntries.stream()
                                           .map(entry -> entry.getSharedBibEntryData().getSharedIdAsInt())
                                           .filter(id -> id != -1)
                                           .toList();
        if (localIds.isEmpty()) {
            return bibEntries;
        }

        List<Integer> remoteIds = new ArrayList<>();
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT shared_id FROM entry")) {
            while (resultSet.next()) {
                remoteIds.add(resultSet.getInt("shared_id"));
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
        return bibEntries.stream()
                         .filter(entry -> !remoteIds.contains(entry.getSharedBibEntryData().getSharedIdAsInt()))
                         .toList();
    }

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
                    bibEntry.getSharedBibEntryData().setSharedId(generatedKeys.getInt(1));
                }
                if (generatedKeys.next()) {
                    LOGGER.error("Some shared IDs left unassigned");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error during entry insertion", e);
        }
    }

    /// Inserts the given list of BibEntry into FIELD table.
    /// These entries do not yet exist in the remote database.
    ///
    /// @param bibEntries [BibEntry] to be inserted
    protected void insertIntoFieldTable(List<BibEntry> bibEntries) {
        if (bibEntries.isEmpty()) {
            return;
        }

        try {
            // Inserting into FIELD table
            // Coerce to ArrayList in order to use List.get()
            List<List<Field>> fields = bibEntries.stream()
                                                 .map(bibEntry -> new ArrayList<>(bibEntry.getFields()))
                                                 .collect(Collectors.toList());

            StringBuilder insertFieldQuery = new StringBuilder()
                    .append("INSERT INTO FIELD (ENTRY_SHARED_ID, NAME, VALUE) VALUES(?, ?, ?)");
            int numFields = 0;
            for (List<Field> entryFields : fields) {
                numFields += entryFields.size();
            }

            if (numFields == 0) {
                // Nothing to insert
                return;
            }

            // Number of commas is fields.size() - 1
            insertFieldQuery.append(", (?, ?, ?)".repeat(numFields - 1));
            try (PreparedStatement preparedFieldStatement = connection.prepareStatement(insertFieldQuery.toString())) {
                int fieldsCompleted = 0;
                for (int entryIndex = 0; entryIndex < fields.size(); entryIndex++) {
                    for (int entryFieldsIndex = 0; entryFieldsIndex < fields.get(entryIndex).size(); entryFieldsIndex++) {
                        // columnIndex starts with 1
                        preparedFieldStatement.setInt((3 * fieldsCompleted) + 1, bibEntries.get(entryIndex).getSharedBibEntryData().getSharedIdAsInt());
                        preparedFieldStatement.setString((3 * fieldsCompleted) + 2, fields.get(entryIndex).get(entryFieldsIndex).getName());
                        preparedFieldStatement.setString((3 * fieldsCompleted) + 3, bibEntries.get(entryIndex).getField(fields.get(entryIndex).get(entryFieldsIndex)).get());
                        fieldsCompleted += 1;
                    }
                }
                // TODO: This could grow too large for a single query
                preparedFieldStatement.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
    }

    /// Updates the whole [BibEntry] on shared database.
    ///
    /// @param localBibEntry [BibEntry] affected by changes
    /// @throws SQLException in case of error
    public void updateEntry(BibEntry localBibEntry) throws OfflineLockException, SQLException {
        // FIXME: either two connections (one with auto commit and one without) or better auto commit state - this line here can lead to issues if autocommit is required in a parallel thread
        connection.setAutoCommit(false); // disable auto commit due to transaction

        try {
            Optional<BibEntry> sharedEntryOptional = getSharedEntry(localBibEntry.getSharedBibEntryData().getSharedIdAsInt());

            if (sharedEntryOptional.isEmpty()) {
                return;
            }

            BibEntry sharedBibEntry = sharedEntryOptional.get();

            // remove shared fields which do not exist locally
            removeSharedFieldsByDifference(localBibEntry, sharedBibEntry);

            // update only if local version is higher or the entries are equal
            if ((localBibEntry.getSharedBibEntryData().getVersion() >= sharedBibEntry.getSharedBibEntryData()
                                                                                     .getVersion()) || localBibEntry.equals(sharedBibEntry)) {
                insertOrUpdateFields(localBibEntry);

                String updateEntryTypeQuery = """
                            UPDATE entry
                            SET entrytype = ?,
                                version = version + 1
                            WHERE shared_id = ?
                        """;

                try (PreparedStatement preparedUpdateEntryTypeStatement = connection.prepareStatement(updateEntryTypeQuery)) {
                    preparedUpdateEntryTypeStatement.setString(1, localBibEntry.getType().getName());
                    preparedUpdateEntryTypeStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedIdAsInt());
                    preparedUpdateEntryTypeStatement.executeUpdate();
                }

                connection.commit(); // apply all changes in current transaction
            } else {
                throw new OfflineLockException(localBibEntry, sharedBibEntry);
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
            connection.rollback(); // undo changes made in current transaction
        } finally {
            connection.setAutoCommit(true); // enable auto commit mode again
        }
    }

    /// Helping method. Removes shared fields which do not exist locally
    private void removeSharedFieldsByDifference(BibEntry localBibEntry, BibEntry sharedBibEntry) throws SQLException {
        Set<Field> nullFields = new HashSet<>(sharedBibEntry.getFields());
        nullFields.removeAll(localBibEntry.getFields());
        for (Field nullField : nullFields) {
            String deleteFieldQuery = """
                        DELETE FROM FIELD
                        WHERE NAME = ? AND ENTRY_SHARED_ID = ?
                    """;

            try (PreparedStatement preparedDeleteFieldStatement = connection
                    .prepareStatement(deleteFieldQuery)) {
                preparedDeleteFieldStatement.setString(1, nullField.getName());
                preparedDeleteFieldStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedIdAsInt());
                preparedDeleteFieldStatement.executeUpdate();
            }
        }
    }

    /// Helping method. Inserts a key-value pair into FIELD table for every field if not existing. Otherwise only an
    /// update is performed.
    private void insertOrUpdateFields(BibEntry localBibEntry) throws SQLException {
        for (Field field : localBibEntry.getFields()) {
            // avoiding to use deprecated BibEntry.getField() method. null values are accepted by PreparedStatement!
            Optional<String> valueOptional = localBibEntry.getField(field);
            String value = null;
            if (valueOptional.isPresent()) {
                value = valueOptional.get();
            }

            String selectFieldQuery = """
                        SELECT name FROM FIELD
                        WHERE NAME = ? AND ENTRY_SHARED_ID = ?
                    """;

            try (PreparedStatement preparedSelectFieldStatement = connection
                    .prepareStatement(selectFieldQuery)) {
                preparedSelectFieldStatement.setString(1, field.getName());
                preparedSelectFieldStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedIdAsInt());

                try (ResultSet selectFieldResultSet = preparedSelectFieldStatement.executeQuery()) {
                    if (selectFieldResultSet.next()) { // check if field already exists
                        String updateFieldQuery = """
                                    UPDATE FIELD
                                    SET VALUE = ?
                                    WHERE NAME = ? AND ENTRY_SHARED_ID = ?
                                """;

                        try (PreparedStatement preparedUpdateFieldStatement = connection
                                .prepareStatement(updateFieldQuery)) {
                            preparedUpdateFieldStatement.setString(1, value);
                            preparedUpdateFieldStatement.setString(2, field.getName());
                            preparedUpdateFieldStatement.setInt(3, localBibEntry.getSharedBibEntryData().getSharedIdAsInt());
                            preparedUpdateFieldStatement.executeUpdate();
                        }
                    } else {
                        String insertFieldQuery = """
                                    INSERT INTO FIELD (ENTRY_SHARED_ID, NAME, VALUE)
                                    VALUES (?, ?, ?)
                                """;

                        try (PreparedStatement preparedFieldStatement = connection
                                .prepareStatement(insertFieldQuery)) {
                            preparedFieldStatement.setInt(1, localBibEntry.getSharedBibEntryData().getSharedIdAsInt());
                            preparedFieldStatement.setString(2, field.getName());
                            preparedFieldStatement.setString(3, value);
                            preparedFieldStatement.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    /// Removes the shared bibEntry.
    ///
    /// @param bibEntries [BibEntry] to be deleted
    public void removeEntries(@NonNull List<BibEntry> bibEntries) {
        if (bibEntries.isEmpty()) {
            return;
        }
        String query = "DELETE FROM ENTRY WHERE SHARED_ID IN (" +
                "?, ".repeat(bibEntries.size() - 1) +
                "?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            for (int j = 0; j < bibEntries.size(); j++) {
                preparedStatement.setInt(j + 1, bibEntries.get(j).getSharedBibEntryData().getSharedIdAsInt());
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    /// @param sharedID Entry ID
    /// @return instance of [BibEntry]
    public Optional<BibEntry> getSharedEntry(int sharedID) {
        List<BibEntry> sharedEntries = getSharedEntries(List.of(sharedID));
        if (sharedEntries.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(sharedEntries.getFirst());
        }
    }

    /// Queries the database for shared entries in 500 element batches.
    /// Optionally, they are filtered by the given list of sharedIds
    ///
    /// @param sharedIDs the list of Ids to filter. If list is empty, then no filter is applied
    public List<BibEntry> partitionAndGetSharedEntries(List<Integer> sharedIDs) {
        List<List<Integer>> partitions = Lists.partition(sharedIDs, 500);
        List<BibEntry> result = new ArrayList<>();

        for (List<Integer> sublist : partitions) {
            result.addAll(getSharedEntries(sublist));
        }
        return result;
    }

    /// Queries the database for shared entries. Optionally, they are filtered by the given list of sharedIds
    ///
    /// @param sharedIDs the list of Ids to filter. If list is empty, then no filter is applied
    public List<BibEntry> getSharedEntries(@NonNull List<Integer> sharedIDs) {
        List<BibEntry> sharedEntries = new ArrayList<>();

        StringBuilder query = new StringBuilder()
                .append("SELECT entry.shared_id, entry.version, entry.entrytype, ")
                .append("F.entry_shared_id, F.name, F.value ")
                .append("FROM entry ")
                .append("LEFT OUTER JOIN field F ON entry.shared_id = F.entry_shared_id");

        if (!sharedIDs.isEmpty()) {
            query.append(" WHERE entry.shared_id IN (")
                 .append("?, ".repeat(sharedIDs.size() - 1))
                 .append("?)");
        }

        query.append(" ORDER BY shared_id");

        try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            for (int i = 0; i < sharedIDs.size(); i++) {
                preparedStatement.setInt(i + 1, sharedIDs.get(i));
            }

            try (ResultSet selectEntryResultSet = preparedStatement.executeQuery()) {
                BibEntry bibEntry = null;
                int lastId = -1;
                while (selectEntryResultSet.next()) {
                    // We get a list of field values of bib entries "grouped" by bib entries
                    // Thus, the first change in the shared id leads to a new BibEntry
                    if (selectEntryResultSet.getInt("SHARED_ID") > lastId) {
                        int sharedId = selectEntryResultSet.getInt("shared_id");
                        int version = selectEntryResultSet.getInt("version");
                        EntryType entrytype = EntryTypeFactory.parse(selectEntryResultSet.getString("entrytype"));

                        bibEntry = new BibEntry(entrytype);
                        bibEntry.getSharedBibEntryData().setSharedId(sharedId);
                        bibEntry.getSharedBibEntryData().setVersion(version);

                        sharedEntries.add(bibEntry);
                        lastId = sharedId;
                    }

                    // In all cases, we set the field value of the newly created BibEntry object
                    String value = selectEntryResultSet.getString("VALUE");
                    if (value != null && bibEntry != null) {
                        bibEntry.setField(FieldFactory.parseField(selectEntryResultSet.getString("NAME")), value, EntriesEventSource.SHARED);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Executed >{}< and got an error", query, e);
            return List.of();
        }

        return sharedEntries;
    }

    public List<BibEntry> getSharedEntries() {
        return getSharedEntries(List.of());
    }

    /// Retrieves a mapping between the columns SHARED_ID and VERSION.
    public Map<Integer, Integer> getSharedIDVersionMapping() {
        Map<Integer, Integer> sharedIDVersionMapping = new HashMap<>();
        String selectEntryQuery = "SELECT shared_id, version FROM entry";
        try (ResultSet selectEntryResultSet = connection.createStatement().executeQuery(selectEntryQuery)) {
            while (selectEntryResultSet.next()) {
                sharedIDVersionMapping.put(
                        selectEntryResultSet.getInt("shared_id"),
                        selectEntryResultSet.getInt("version"));
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }

        return sharedIDVersionMapping;
    }

    /// Fetches and returns all shared meta data.
    public Map<String, String> getSharedMetaData() {
        Map<String, String> data = new HashMap<>();

        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM METADATA")) {
            while (resultSet.next()) {
                data.put(resultSet.getString("KEY"), resultSet.getString("VALUE"));
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }

        return data;
    }

    /// Clears and sets all shared meta data.
    ///
    /// @param data JabRef meta data as map
    public void setSharedMetaData(Map<String, String> data) throws SQLException {
        // The function upserts and notifies other clients about actually changed values (see setUp)
        try (PreparedStatement statement = connection.prepareStatement("SELECT upsert_metadata(?, ?)")) {
            for (Map.Entry<String, String> metaEntry : data.entrySet()) {
                statement.setString(1, metaEntry.getKey());
                statement.setString(2, metaEntry.getValue());
                statement.execute();
            }
        }
    }

    public DatabaseConnectionProperties getDBMSConnectionProperties() {
        return this.connectionProperties;
    }

    /// Listens for notifications from DBMS. Needs to be implemented if LiveUpdate is supported by the DBMS
    ///
    /// @param dbmsSynchronizer [DBMSSynchronizer] which handles the notification.
    public void startNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        try {
            listenerConnection = dbmsConnection.openNewConnection();
            listenerConnection.createStatement().execute("LISTEN \"" + Notifier.CHANNEL + "\"");
            listenerConnection.createStatement().execute("LISTEN \"" + Notifier.METADATA_CHANNEL + "\"");
            listener = new NotificationListener(dbmsSynchronizer, listenerConnection.unwrap(PGConnection.class), processorId);
            HeadlessExecutorService.INSTANCE.execute(listener);
        } catch (SQLException e) {
            LOGGER.error("SQL Error during starting the notification listener", e);
        }
    }

    /// Terminates the notification listener. Needs to be implemented if LiveUpdate is supported by the DBMS
    public void stopNotificationListener() {
        if (listener == null) {
            return;
        }
        listener.stop();
        try {
            // Also interrupts a pending getNotifications poll on the listener thread
            listenerConnection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error during stopping the notification listener", e);
        }
    }
}

