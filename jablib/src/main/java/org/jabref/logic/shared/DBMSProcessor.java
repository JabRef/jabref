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
import org.jabref.logic.shared.exception.SharedEntryNotPresentException;
import org.jabref.logic.shared.notifications.NotificationListener;
import org.jabref.logic.shared.notifications.Notifier;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Processes all incoming or outgoing bib data to external SQL Database and manages its structure.
public class DBMSProcessor {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DBMSProcessor.class);

    private static final Set<String> GROUP_TREE_METADATA_KEYS = Set.of(MetaData.GROUPSTREE, MetaData.GROUPSTREE_LEGACY);

    // Arbitrary application-wide id serializing the one-time data migration across clients
    private static final long MIGRATION_ADVISORY_LOCK_ID = 11_879L;

    protected final Connection connection;

    protected DatabaseConnectionProperties connectionProperties;

    // Identifies this processor among all clients connected to the same database.
    // Deliberately per instance, not static: two synchronizers in the same JVM (two open shared
    // libraries) must not mistake each other's notifications for their own.
    private final String processorId = CUID.randomCUID2(8).toString();

    private final DatabaseConnection dbmsConnection;

    private NotificationListener listener;

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
    /// @return `true` if the structure matches the requirements, `false` if not.
    /// @throws SQLException in case of error
    public boolean checkBaseIntegrity() throws SQLException {
        if (!tableExists("jabref.metadata")) {
            return false;
        }
        String metadataVersion = getSharedMetaData().get(MetaData.VERSION_DB_STRUCT);
        return (metadataVersion != null) && (Integer.parseInt(metadataVersion.replace(";", "")) == getCURRENT_VERSION_DB_STRUCT());
    }

    private boolean tableExists(String qualifiedName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT to_regclass(?)")) {
            statement.setString(1, qualifiedName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && (resultSet.getString(1) != null);
            }
        }
    }

    /// Determines whether the database is using an pre-3.6 structure.
    ///
    /// @return `true` if the structure is old, else `false`.
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

    /// Checks whether all given table names (**case insensitive**) exist in database.
    ///
    /// @param tableNames Table names to be checked
    /// @return `true` if **all** given tables are present, else `false`.
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
                        -- Only the key: values (e.g. serialized groups) can exceed the 8000 byte payload limit,
                        -- and receivers re-read the metadata anyway
                        PERFORM pg_notify('%s', metadata_key);
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
        if (!isEntryTableEmpty()) {
            // Only a freshly created database is migrated into - never one that is already used
            return;
        }

        Optional<String> oldSchema = findOldSchema();
        if (oldSchema.isEmpty()) {
            return;
        }

        LOGGER.info("Migrating shared database from old structure in schema \"{}\"", oldSchema.get());
        // One transaction: a mid-migration failure must not leave a partially copied library behind,
        // which would never be retried (the entry table would no longer be empty)
        inTransaction(() -> {
            // Serializes concurrent first connections; the lock is released at commit/rollback
            connection.createStatement().execute("SELECT pg_advisory_xact_lock(" + MIGRATION_ADVISORY_LOCK_ID + ")");
            if (!isEntryTableEmpty()) {
                // Another client migrated while we waited for the lock
                return;
            }
            connection.createStatement().executeUpdate(
                    "INSERT INTO entry (shared_id, entrytype, version) SELECT \"SHARED_ID\", \"TYPE\", \"VERSION\" FROM %s.\"ENTRY\"".formatted(oldSchema.get()));
            connection.createStatement().executeUpdate(
                    "INSERT INTO field (entry_shared_id, name, value) SELECT \"ENTRY_SHARED_ID\", \"NAME\", \"VALUE\" FROM %s.\"FIELD\"".formatted(oldSchema.get()));
            connection.createStatement().executeUpdate(
                    "INSERT INTO metadata (key, value) SELECT \"KEY\", \"VALUE\" FROM %s.\"METADATA\"".formatted(oldSchema.get()));
            // The serial has to continue after the copied ids
            connection.createStatement().execute(
                    "SELECT setval(pg_get_serial_sequence('entry', 'shared_id'), COALESCE((SELECT MAX(shared_id) FROM entry), 0) + 1, false)");
        });
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run() throws SQLException;
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private void inTransaction(SqlAction work) throws SQLException {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    /// Runs the work as one transaction on the (otherwise auto-committing) connection.
    /// Any exception rolls back.
    private <T> T inTransaction(SqlWork<T> work) throws SQLException {
        connection.setAutoCommit(false);
        try {
            T result = work.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            // Switching auto-commit back on would commit a still-open transaction, hence the rollback above
            connection.setAutoCommit(true);
        }
    }

    private boolean isEntryTableEmpty() throws SQLException {
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT EXISTS (SELECT 1 FROM entry)")) {
            resultSet.next();
            return !resultSet.getBoolean(1);
        }
    }

    private Optional<String> findOldSchema() throws SQLException {
        for (String schema : List.of("jabref", "public")) {
            if (tableExists(schema + ".\"ENTRY\"")) {
                return Optional.of(schema);
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
    public void insertEntry(BibEntry bibEntry) throws SQLException {
        insertEntries(List.of(bibEntry));
    }

    public void insertEntries(List<BibEntry> bibEntries) throws SQLException {
        List<BibEntry> notYetExistingEntries = getNotYetExistingEntries(bibEntries);
        // pgjdbc caps bind parameters at 65535 per statement; with three parameters per field,
        // 500 entries stay below that up to an average of 43 fields per entry
        for (List<BibEntry> chunk : Lists.partition(notYetExistingEntries, 500)) {
            // Entry rows without their fields would be visible to other clients as empty entries
            inTransaction(() -> {
                insertIntoEntryTable(chunk);
                insertIntoFieldTable(chunk);
            });
        }
    }

    /// Filters a list of BibEntry to those which do not yet exist in the database
    private List<BibEntry> getNotYetExistingEntries(List<BibEntry> bibEntries) throws SQLException {
        List<Integer> localIds = bibEntries.stream()
                                           .map(entry -> entry.getSharedBibEntryData().getSharedIdAsInt())
                                           .filter(id -> id != -1)
                                           .toList();
        if (localIds.isEmpty()) {
            return bibEntries;
        }

        Set<Integer> remoteIds = new HashSet<>();
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT shared_id FROM entry")) {
            while (resultSet.next()) {
                remoteIds.add(resultSet.getInt("shared_id"));
            }
        }
        return bibEntries.stream()
                         .filter(entry -> !remoteIds.contains(entry.getSharedBibEntryData().getSharedIdAsInt()))
                         .toList();
    }

    protected void insertIntoEntryTable(List<BibEntry> bibEntries) throws SQLException {
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
        }
    }

    /// Inserts the given list of BibEntry into FIELD table.
    /// These entries do not yet exist in the remote database.
    ///
    /// @param bibEntries [BibEntry] to be inserted
    protected void insertIntoFieldTable(List<BibEntry> bibEntries) throws SQLException {
        if (bibEntries.isEmpty()) {
            return;
        }

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
            preparedFieldStatement.executeUpdate();
        }
    }

    /// Replaces the whole [BibEntry] on the shared database - if the local entry is based on the
    /// shared entry's current version (optimistic offline lock).
    ///
    /// The version check and the increment are one statement, whose row lock serializes
    /// concurrent updates of the same entry: the second writer sees the incremented version and
    /// is refused instead of overwriting the first writer's fields with its stale copy.
    ///
    /// @param localBibEntry [BibEntry] affected by changes
    /// @throws OfflineLockException           if the shared entry has a newer version than the local one
    /// @throws SharedEntryNotPresentException if the entry does not exist on the shared side
    // [impl->req~shared-database.concurrent-edit-detection~1]
    public void updateEntry(BibEntry localBibEntry) throws OfflineLockException, SharedEntryNotPresentException, SQLException {
        int sharedId = localBibEntry.getSharedBibEntryData().getSharedIdAsInt();
        boolean written = inTransaction(() -> {
            String updateEntryQuery = """
                        UPDATE entry
                        SET entrytype = ?,
                            version = version + 1
                        WHERE shared_id = ? AND version = ?
                        RETURNING version
                    """;
            try (PreparedStatement statement = connection.prepareStatement(updateEntryQuery)) {
                statement.setString(1, localBibEntry.getType().getName());
                statement.setInt(2, sharedId);
                statement.setInt(3, localBibEntry.getSharedBibEntryData().getVersion());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return false;
                    }
                    // The fresh version travels in the change notification, so receivers
                    // do not need a pull to stay consistent
                    localBibEntry.getSharedBibEntryData().setVersion(resultSet.getInt("version"));
                }
            }

            // Replacing all fields costs two round trips regardless of the entry size; a
            // per-field diff would cost two per field
            try (PreparedStatement deleteFields = connection.prepareStatement("DELETE FROM field WHERE entry_shared_id = ?")) {
                deleteFields.setInt(1, sharedId);
                deleteFields.executeUpdate();
            }
            insertIntoFieldTable(List.of(localBibEntry));
            return true;
        });
        if (written) {
            return;
        }

        // Nothing was written - find out why
        BibEntry sharedBibEntry = getSharedEntry(sharedId).orElseThrow(() -> new SharedEntryNotPresentException(localBibEntry));
        if (localBibEntry.equals(sharedBibEntry)) {
            // Only the version lags behind
            localBibEntry.getSharedBibEntryData().setVersion(sharedBibEntry.getSharedBibEntryData().getVersion());
            return;
        }
        throw new OfflineLockException(localBibEntry, sharedBibEntry);
    }

    /// Removes the shared bibEntry.
    ///
    /// @param bibEntries [BibEntry] to be deleted
    public void removeEntries(@NonNull List<BibEntry> bibEntries) throws SQLException {
        // Chunked for the same reason as insertEntries: at most 65535 bind parameters per statement
        for (List<BibEntry> chunk : Lists.partition(bibEntries, 500)) {
            String query = "DELETE FROM ENTRY WHERE SHARED_ID IN (" +
                    "?, ".repeat(chunk.size() - 1) +
                    "?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                for (int j = 0; j < chunk.size(); j++) {
                    preparedStatement.setInt(j + 1, chunk.get(j).getSharedBibEntryData().getSharedIdAsInt());
                }
                preparedStatement.executeUpdate();
            }
        }
    }

    /// @param sharedID Entry ID
    /// @return instance of [BibEntry]
    public Optional<BibEntry> getSharedEntry(int sharedID) throws SQLException {
        return getSharedEntries(List.of(sharedID)).stream().findFirst();
    }

    /// Queries the database for the given entries in 500 element batches.
    /// Returns nothing for an empty id list.
    public List<BibEntry> partitionAndGetSharedEntries(List<Integer> sharedIDs) throws SQLException {
        List<BibEntry> result = new ArrayList<>();
        for (List<Integer> sublist : Lists.partition(sharedIDs, 500)) {
            result.addAll(getSharedEntries(sublist));
        }
        return result;
    }

    /// Queries the database for shared entries. Optionally, they are filtered by the given list of sharedIds
    ///
    /// @param sharedIDs the list of Ids to filter. If list is empty, then no filter is applied
    public List<BibEntry> getSharedEntries(@NonNull List<Integer> sharedIDs) throws SQLException {
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
        }

        return sharedEntries;
    }

    public List<BibEntry> getSharedEntries() throws SQLException {
        return getSharedEntries(List.of());
    }

    /// Retrieves a mapping between the columns SHARED_ID and VERSION.
    public Map<Integer, Integer> getSharedIDVersionMapping() throws SQLException {
        Map<Integer, Integer> sharedIDVersionMapping = new HashMap<>();
        String selectEntryQuery = "SELECT shared_id, version FROM entry";
        try (ResultSet selectEntryResultSet = connection.createStatement().executeQuery(selectEntryQuery)) {
            while (selectEntryResultSet.next()) {
                sharedIDVersionMapping.put(
                        selectEntryResultSet.getInt("shared_id"),
                        selectEntryResultSet.getInt("version"));
            }
        }
        return sharedIDVersionMapping;
    }

    /// Fetches and returns all shared meta data.
    public Map<String, String> getSharedMetaData() throws SQLException {
        Map<String, String> data = new HashMap<>();
        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM METADATA")) {
            while (resultSet.next()) {
                data.put(resultSet.getString("KEY"), resultSet.getString("VALUE"));
            }
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
        removeObsoleteGroupTreeMetaData(data.keySet());
    }

    /// Removes group tree formats which are no longer present after a group update.
    private void removeObsoleteGroupTreeMetaData(Set<String> currentMetaDataKeys) throws SQLException {
        try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM metadata WHERE key = ?");
             PreparedStatement notifyStatement = connection.prepareStatement("SELECT pg_notify(?, ?)")) {
            for (String groupTreeKey : GROUP_TREE_METADATA_KEYS) {
                if (!currentMetaDataKeys.contains(groupTreeKey)) {
                    deleteStatement.setString(1, groupTreeKey);
                    if (deleteStatement.executeUpdate() > 0) {
                        notifyStatement.setString(1, Notifier.METADATA_CHANNEL);
                        notifyStatement.setString(2, groupTreeKey);
                        notifyStatement.execute();
                    }
                }
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
        NotificationListener newListener = new NotificationListener(dbmsSynchronizer, dbmsConnection, processorId);
        try {
            // Register the subscription synchronously: notifications sent after this method
            // returns must not be missed
            newListener.start();
        } catch (SQLException e) {
            LOGGER.error("SQL Error during starting the notification listener", e);
            newListener.stop();
            return;
        }
        listener = newListener;
        // A virtual thread: the listener spends its life blocked in getNotifications
        Thread.ofVirtual().name("JabRef - shared database notification listener").start(newListener);
    }

    /// Terminates the notification listener. Needs to be implemented if LiveUpdate is supported by the DBMS
    public void stopNotificationListener() {
        if (listener != null) {
            listener.stop();
        }
    }
}
