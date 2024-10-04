package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.SharedBibEntryData;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.metadata.MetaData;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes all incoming or outgoing bib data to external SQL Database and manages its structure.
 */
public abstract class DBMSProcessor {

    public static final String PROCESSOR_ID = UUID.randomUUID().toString();

    protected static final Logger LOGGER = LoggerFactory.getLogger(DBMSProcessor.class);

    protected final Connection connection;

    protected DatabaseConnectionProperties connectionProperties;

    protected DBMSProcessor(DatabaseConnection dbmsConnection) {
        this.connection = dbmsConnection.getConnection();
        this.connectionProperties = dbmsConnection.getProperties();
    }

    /**
     * Scans the database for required tables.
     *
     * @return <code>true</code> if the structure matches the requirements, <code>false</code> if not.
     * @throws SQLException in case of error
     */
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

    /**
     * Determines whether the database is using an pre-3.6 structure.
     *
     * @return <code>true</code> if the structure is old, else <code>false</code>.
     */
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

    /**
     * Checks whether all given table names (<b>case insensitive</b>) exist in database.
     *
     * @param tableNames Table names to be checked
     * @return <code>true</code> if <b>all</b> given tables are present, else <code>false</code>.
     */
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

    /**
     * Creates and sets up the needed tables and columns according to the database type and performs a check whether the
     * needed tables are present.
     *
     * @throws SQLException in case of error
     */
    public void setupSharedDatabase() throws SQLException {
        setUp();

        if (!checkBaseIntegrity()) {
            // can only happen with users direct intervention on shared database
            LOGGER.error("Corrupt_shared_database_structure.");
        }
    }

    /**
     * Creates and sets up the needed tables and columns according to the database type.
     *
     * @throws SQLException in case of error
     */
    protected abstract void setUp() throws SQLException;

    abstract Integer getCURRENT_VERSION_DB_STRUCT();

    /**
     * For use in test only. Inserts the BibEntry into the shared database.
     *
     * @param bibEntry {@link BibEntry} to be inserted.
     */
    public void insertEntry(BibEntry bibEntry) {
        insertEntries(Collections.singletonList(bibEntry));
    }

    /**
     * Inserts the List of BibEntry into the shared database.
     *
     * @param bibEntries List of {@link BibEntry} to be inserted
     */
    public void insertEntries(List<BibEntry> bibEntries) {
        List<BibEntry> notYetExistingEntries = getNotYetExistingEntries(bibEntries);
        if (notYetExistingEntries.isEmpty()) {
            return;
        }
        insertIntoEntryTable(notYetExistingEntries);
        insertIntoFieldTable(notYetExistingEntries);
    }

    /**
     * Inserts the given List of BibEntry into the ENTRY table.
     *
     * @param bibEntries List of {@link BibEntry} to be inserted
     */
    protected void insertIntoEntryTable(List<BibEntry> bibEntries) {
        if (bibEntries.isEmpty()) {
            return;
        }

        StringBuilder insertIntoEntryQuery = new StringBuilder().append("INSERT INTO entry (entrytype) VALUES (?)");
        // Number of commas is bibEntries.size() - 1
        insertIntoEntryQuery.append(", (?)".repeat(Math.max(0, (bibEntries.size() - 1))));

        try (PreparedStatement preparedEntryStatement = connection.prepareStatement(insertIntoEntryQuery.toString(),
                new String[]{"SHARED_ID"})) {
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
                    LOGGER.error("Error: Some shared IDs left unassigned");
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
    }

    /**
     * Filters a list of BibEntry to and returns those which do not exist in the database
     *
     * @param bibEntries {@link BibEntry} to be checked
     * @return <code>true</code> if existent, else <code>false</code>
     */
    private List<BibEntry> getNotYetExistingEntries(List<BibEntry> bibEntries) {
        List<Integer> remoteIds = new ArrayList<>();
        List<Integer> localIds = bibEntries.stream()
                                           .map(BibEntry::getSharedBibEntryData)
                                           .map(SharedBibEntryData::getSharedID)
                                           .filter(id -> id != -1)
                                           .toList();
        if (localIds.isEmpty()) {
            return bibEntries;
        }
        try {
            String selectQuery = "SELECT * FROM ENTRY";

            try (ResultSet resultSet = connection.createStatement().executeQuery(selectQuery)) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("SHARED_ID");
                    remoteIds.add(id);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
        return bibEntries.stream().filter(entry ->
                !remoteIds.contains(entry.getSharedBibEntryData().getSharedID()))
                         .collect(Collectors.toList());
    }

    /**
     * Inserts the given list of BibEntry into FIELD table.
     *
     * @param bibEntries {@link BibEntry} to be inserted
     */
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
                return; // Prevent SQL Exception
            }

            // Number of commas is fields.size() - 1
            insertFieldQuery.append(", (?, ?, ?)".repeat(Math.max(0, (numFields - 1))));
            try (PreparedStatement preparedFieldStatement = connection.prepareStatement(insertFieldQuery.toString())) {
                int fieldsCompleted = 0;
                for (int entryIndex = 0; entryIndex < fields.size(); entryIndex++) {
                    for (int entryFieldsIndex = 0; entryFieldsIndex < fields.get(entryIndex).size(); entryFieldsIndex++) {
                        // columnIndex starts with 1
                        preparedFieldStatement.setInt((3 * fieldsCompleted) + 1, bibEntries.get(entryIndex).getSharedBibEntryData().getSharedID());
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

    /**
     * Updates the whole {@link BibEntry} on shared database.
     *
     * @param localBibEntry {@link BibEntry} affected by changes
     * @throws SQLException in case of error
     */
    public void updateEntry(BibEntry localBibEntry) throws OfflineLockException, SQLException {
        // FIXME: either two connections (one with auto commit and one without) or better auto commit state - this line here can lead to issues if autocommit is required in a parallel thread
        connection.setAutoCommit(false); // disable auto commit due to transaction

        try {
            Optional<BibEntry> sharedEntryOptional = getSharedEntry(localBibEntry.getSharedBibEntryData().getSharedID());

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
                    preparedUpdateEntryTypeStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedID());
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

    /**
     * Helping method. Removes shared fields which do not exist locally
     */
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
                preparedDeleteFieldStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedID());
                preparedDeleteFieldStatement.executeUpdate();
            }
        }
    }

    /**
     * Helping method. Inserts a key-value pair into FIELD table for every field if not existing. Otherwise only an
     * update is performed.
     */
    private void insertOrUpdateFields(BibEntry localBibEntry) throws SQLException {
        for (Field field : localBibEntry.getFields()) {
            // avoiding to use deprecated BibEntry.getField() method. null values are accepted by PreparedStatement!
            Optional<String> valueOptional = localBibEntry.getField(field);
            String value = null;
            if (valueOptional.isPresent()) {
                value = valueOptional.get();
            }

            String selectFieldQuery = """
                        SELECT * FROM FIELD
                        WHERE NAME = ? AND ENTRY_SHARED_ID = ?
                    """;

            try (PreparedStatement preparedSelectFieldStatement = connection
                    .prepareStatement(selectFieldQuery)) {
                preparedSelectFieldStatement.setString(1, field.getName());
                preparedSelectFieldStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedID());

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
                            preparedUpdateFieldStatement.setInt(3, localBibEntry.getSharedBibEntryData().getSharedID());
                            preparedUpdateFieldStatement.executeUpdate();
                        }
                    } else {
                        String insertFieldQuery = """
                                    INSERT INTO FIELD (ENTRY_SHARED_ID, NAME, VALUE)
                                    VALUES (?, ?, ?)
                                """;

                        try (PreparedStatement preparedFieldStatement = connection
                                .prepareStatement(insertFieldQuery)) {
                            preparedFieldStatement.setInt(1, localBibEntry.getSharedBibEntryData().getSharedID());
                            preparedFieldStatement.setString(2, field.getName());
                            preparedFieldStatement.setString(3, value);
                            preparedFieldStatement.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    /**
     * Removes the shared bibEntry.
     *
     * @param bibEntries {@link BibEntry} to be deleted
     */
    public void removeEntries(List<BibEntry> bibEntries) {
        Objects.requireNonNull(bibEntries);
        if (bibEntries.isEmpty()) {
            return;
        }
        StringBuilder query = new StringBuilder().append("DELETE FROM ENTRY WHERE SHARED_ID IN (");
        query.append("?, ".repeat(bibEntries.size() - 1));
        query.append("?)");

        try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            for (int j = 0; j < bibEntries.size(); j++) {
                preparedStatement.setInt(j + 1, bibEntries.get(j).getSharedBibEntryData().getSharedID());
            }
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    /**
     * @param sharedID Entry ID
     * @return instance of {@link BibEntry}
     */
    public Optional<BibEntry> getSharedEntry(int sharedID) {
        List<BibEntry> sharedEntries = getSharedEntries(Collections.singletonList(sharedID));
        if (sharedEntries.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(sharedEntries.getFirst());
        }
    }

    /**
     * Queries the database for shared entries in 500 element batches.
     * Optionally, they are filtered by the given list of sharedIds
     *
     * @param sharedIDs the list of Ids to filter. If list is empty, then no filter is applied
     */
    public List<BibEntry> partitionAndGetSharedEntries(List<Integer> sharedIDs) {
        List<List<Integer>> partitions = Lists.partition(sharedIDs, 500);
        List<BibEntry> result = new ArrayList<>();

        for (List<Integer> sublist : partitions) {
            result.addAll(getSharedEntries(sublist));
        }
        return result;
    }

    /**
     * Queries the database for shared entries. Optionally, they are filtered by the given list of sharedIds
     *
     * @param sharedIDs the list of Ids to filter. If list is empty, then no filter is applied
     */
    public List<BibEntry> getSharedEntries(List<Integer> sharedIDs) {
        Objects.requireNonNull(sharedIDs);

        List<BibEntry> sharedEntries = new ArrayList<>();

        StringBuilder query = new StringBuilder()
                .append("SELECT entry.shared_id, entry.entrytype, entry.version, ")
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
                        bibEntry.getSharedBibEntryData().setSharedID(sharedId);
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
            return Collections.emptyList();
        }

        return sharedEntries;
    }

    public List<BibEntry> getSharedEntries() {
        return getSharedEntries(Collections.emptyList());
    }

    /**
     * Retrieves a mapping between the columns SHARED_ID and VERSION.
     */
    public Map<Integer, Integer> getSharedIDVersionMapping() {
        Map<Integer, Integer> sharedIDVersionMapping = new HashMap<>();
        String selectEntryQuery = """
                    SELECT * FROM ENTRY
                    ORDER BY SHARED_ID
                """;

        try (ResultSet selectEntryResultSet = connection.createStatement().executeQuery(selectEntryQuery)) {
            while (selectEntryResultSet.next()) {
                sharedIDVersionMapping.put(selectEntryResultSet.getInt("SHARED_ID"), selectEntryResultSet.getInt("VERSION"));
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }

        return sharedIDVersionMapping;
    }

    /**
     * Fetches and returns all shared meta data.
     */
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

    /**
     * Clears and sets all shared meta data.
     *
     * @param data JabRef meta data as map
     */
    public void setSharedMetaData(Map<String, String> data) throws SQLException {
        String insertOrUpdateQuery = """
                    INSERT INTO METADATA (KEY, VALUE)
                    VALUES (?, ?)
                    ON CONFLICT (KEY) DO UPDATE
                    SET VALUE = EXCLUDED.VALUE
                """;

        try (PreparedStatement statement = connection.prepareStatement(insertOrUpdateQuery)) {
            for (Map.Entry<String, String> metaEntry : data.entrySet()) {
                statement.setString(1, metaEntry.getKey());
                statement.setString(2, metaEntry.getValue());
                statement.executeUpdate();
            }
        }
    }

    /**
     * Returns a new instance of the abstract type {@link DBMSProcessor}
     */
    public static DBMSProcessor getProcessorInstance(DatabaseConnection connection) {
        return new PostgreSQLProcessor(connection);
    }

    public DatabaseConnectionProperties getDBMSConnectionProperties() {
        return this.connectionProperties;
    }

    /**
     * Listens for notifications from DBMS. Needs to be implemented if LiveUpdate is supported by the DBMS
     *
     * @param dbmsSynchronizer {@link DBMSSynchronizer} which handles the notification.
     */
    public void startNotificationListener(@SuppressWarnings("unused") DBMSSynchronizer dbmsSynchronizer) {
        // nothing to do
    }

    /**
     * Terminates the notification listener. Needs to be implemented if LiveUpdate is supported by the DBMS
     */
    public void stopNotificationListener() {
        // nothing to do
    }

    /**
     * Notifies all clients ({@link DBMSSynchronizer}) which are connected to the same DBMS. Needs to be implemented if
     * LiveUpdate is supported by the DBMS
     */
    public void notifyClients() {
        // nothing to do
    }
}
