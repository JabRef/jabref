package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.database.shared.DatabaseConnection;
import org.jabref.model.database.shared.DatabaseConnectionProperties;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEventSource;

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
     * @throws SQLException
     */
    public boolean checkBaseIntegrity() throws SQLException {
        return checkTableAvailibility("ENTRY", "FIELD", "METADATA");
    }

    /**
     * Determines whether the database is using an pre-3.6 structure.
     *
     * @return <code>true</code> if the structure is old, else <code>false</code>.
     */
    public boolean checkForPre3Dot6Intergrity() throws SQLException {
        return checkTableAvailibility(
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
    private boolean checkTableAvailibility(String... tableNames) throws SQLException {
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
     * Creates and sets up the needed tables and columns according to the database type and
     * performs a check whether the needed tables are present.
     *
     * @throws SQLException
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
     * @throws SQLException
     */
    protected abstract void setUp() throws SQLException;

    /**
     * Escapes parts of SQL expressions like table or field name to match the conventions
     * of the database system using the current dbmsType.
     *
     * This method is package private, because of DBMSProcessorTest
     *
     * @param expression Table or field name
     * @return Correctly escaped expression
     */
    abstract String escape(String expression);


    /**
     * Inserts the given bibEntry into shared database.
     *
     * @param bibEntry {@link BibEntry} to be inserted
     */
    public void insertEntry(BibEntry bibEntry) {
        if (!checkForBibEntryExistence(bibEntry)) {
            insertIntoEntryTable(bibEntry);
            insertIntoFieldTable(bibEntry);
        }

    }

    /**
     * Inserts the given bibEntry into ENTRY table.
     *
     * @param bibEntry {@link BibEntry} to be inserted
     */
    protected void insertIntoEntryTable(BibEntry bibEntry) {
        // Inserting into ENTRY table
        StringBuilder insertIntoEntryQuery = new StringBuilder()
                .append("INSERT INTO ")
                .append(escape("ENTRY"))
                .append("(")
                .append(escape("TYPE"))
                .append(") VALUES(?)");

        // This is the only method to get generated keys which is accepted by MySQL, PostgreSQL and Oracle.
        try (PreparedStatement preparedEntryStatement = connection.prepareStatement(insertIntoEntryQuery.toString(),
                new String[] {"SHARED_ID"})) {

            preparedEntryStatement.setString(1, bibEntry.getType());
            preparedEntryStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedEntryStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bibEntry.getSharedBibEntryData().setSharedID(generatedKeys.getInt(1)); // set generated ID locally
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    /**
     * Checks whether the given bibEntry already exists on shared database.
     * @param bibEntry {@link BibEntry} to be checked
     * @return <code>true</code> if existent, else <code>false</code>
     */
    private boolean checkForBibEntryExistence(BibEntry bibEntry) {
        try {
            // Check if already exists
            int sharedID = bibEntry.getSharedBibEntryData().getSharedID();
            if (sharedID != -1) {
                StringBuilder selectQuery = new StringBuilder()
                        .append("SELECT * FROM ")
                        .append(escape("ENTRY"))
                        .append(" WHERE ")
                        .append(escape("SHARED_ID"))
                        .append(" = ?");

                try (PreparedStatement preparedSelectStatement = connection.prepareStatement(selectQuery.toString())) {
                    preparedSelectStatement.setInt(1, sharedID);
                    try (ResultSet resultSet = preparedSelectStatement.executeQuery()) {
                        if (resultSet.next()) {
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
        return false;
    }

    /**
     * Inserts the given bibEntry into FIELD table.
     *
     * @param bibEntry {@link BibEntry} to be inserted
     */
    private void insertIntoFieldTable(BibEntry bibEntry) {
        try {
            // Inserting into FIELD table
            for (String fieldName : bibEntry.getFieldNames()) {
                StringBuilder insertFieldQuery = new StringBuilder()
                    .append("INSERT INTO ")
                    .append(escape("FIELD"))
                    .append("(")
                    .append(escape("ENTRY_SHARED_ID"))
                    .append(", ")
                    .append(escape("NAME"))
                    .append(", ")
                    .append(escape("VALUE"))
                    .append(") VALUES(?, ?, ?)");

                try (PreparedStatement preparedFieldStatement = connection.prepareStatement(insertFieldQuery.toString())) {
                    // columnIndex starts with 1
                    preparedFieldStatement.setInt(1, bibEntry.getSharedBibEntryData().getSharedID());
                    preparedFieldStatement.setString(2, fieldName);
                    preparedFieldStatement.setString(3, bibEntry.getField(fieldName).get());
                    preparedFieldStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    /**
     * Updates the whole {@link BibEntry} on shared database.
     *
     * @param localBibEntry {@link BibEntry} affected by changes
     * @throws SQLException
     */
    public void updateEntry(BibEntry localBibEntry) throws OfflineLockException, SQLException {
        connection.setAutoCommit(false); // disable auto commit due to transaction

        try {
            Optional<BibEntry> sharedEntryOptional = getSharedEntry(localBibEntry.getSharedBibEntryData().getSharedID());

            if (!sharedEntryOptional.isPresent()) {
                return;
            }

            BibEntry sharedBibEntry = sharedEntryOptional.get();

            // remove shared fields which do not exist locally
            removeSharedFieldsByDifference(localBibEntry, sharedBibEntry);

            // update only if local version is higher or the entries are equal
            if ((localBibEntry.getSharedBibEntryData().getVersion() >= sharedBibEntry.getSharedBibEntryData()
                    .getVersion()) || localBibEntry.equals(sharedBibEntry)) {

                insertOrUpdateFields(localBibEntry);

                // updating entry type
                StringBuilder updateEntryTypeQuery = new StringBuilder()
                    .append("UPDATE ")
                    .append(escape("ENTRY"))
                    .append(" SET ")
                    .append(escape("TYPE"))
                    .append(" = ?, ")
                    .append(escape("VERSION"))
                    .append(" = ")
                    .append(escape("VERSION"))
                    .append(" + 1 WHERE ")
                    .append(escape("SHARED_ID"))
                    .append(" = ?");

                try (PreparedStatement preparedUpdateEntryTypeStatement = connection.prepareStatement(updateEntryTypeQuery.toString())) {
                    preparedUpdateEntryTypeStatement.setString(1, localBibEntry.getType());
                    preparedUpdateEntryTypeStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedID());
                    preparedUpdateEntryTypeStatement.executeUpdate();
                }

                connection.commit(); // apply all changes in current transaction

            } else {
                throw new OfflineLockException(localBibEntry, sharedBibEntry);
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
            connection.rollback(); // undo changes made in current transaction
        } finally {
            connection.setAutoCommit(true); // enable auto commit mode again
        }
    }

    /**
     * Helping method. Removes shared fields which do not exist locally
     */
    private void removeSharedFieldsByDifference(BibEntry localBibEntry, BibEntry sharedBibEntry) throws SQLException {
        Set<String> nullFields = new HashSet<>(sharedBibEntry.getFieldNames());
        nullFields.removeAll(localBibEntry.getFieldNames());
        for (String nullField : nullFields) {
            StringBuilder deleteFieldQuery = new StringBuilder()
                .append("DELETE FROM ")
                .append(escape("FIELD"))
                .append(" WHERE ")
                .append(escape("NAME"))
                .append(" = ? AND ")
                .append(escape("ENTRY_SHARED_ID"))
                .append(" = ?");

            try (PreparedStatement preparedDeleteFieldStatement = connection
                    .prepareStatement(deleteFieldQuery.toString())) {
                preparedDeleteFieldStatement.setString(1, nullField);
                preparedDeleteFieldStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedID());
                preparedDeleteFieldStatement.executeUpdate();
            }
        }
    }

    /**
     * Helping method. Inserts a key-value pair into FIELD table for every field if not existing. Otherwise only an update is performed.
     */
    private void insertOrUpdateFields(BibEntry localBibEntry) throws SQLException {
        for (String fieldName : localBibEntry.getFieldNames()) {
            // avoiding to use deprecated BibEntry.getField() method. null values are accepted by PreparedStatement!
            Optional<String> valueOptional = localBibEntry.getField(fieldName);
            String value = null;
            if (valueOptional.isPresent()) {
                value = valueOptional.get();
            }

            StringBuilder selectFieldQuery = new StringBuilder()
                    .append("SELECT * FROM ")
                    .append(escape("FIELD"))
                    .append(" WHERE ")
                    .append(escape("NAME"))
                    .append(" = ? AND ")
                    .append(escape("ENTRY_SHARED_ID"))
                    .append(" = ?");

            try (PreparedStatement preparedSelectFieldStatement = connection
                    .prepareStatement(selectFieldQuery.toString())) {
                preparedSelectFieldStatement.setString(1, fieldName);
                preparedSelectFieldStatement.setInt(2, localBibEntry.getSharedBibEntryData().getSharedID());

                try (ResultSet selectFieldResultSet = preparedSelectFieldStatement.executeQuery()) {
                    if (selectFieldResultSet.next()) { // check if field already exists
                        StringBuilder updateFieldQuery = new StringBuilder()
                                .append("UPDATE ")
                                .append(escape("FIELD"))
                                .append(" SET ")
                                .append(escape("VALUE"))
                                .append(" = ? WHERE ")
                                .append(escape("NAME"))
                                .append(" = ? AND ")
                                .append(escape("ENTRY_SHARED_ID"))
                                .append(" = ?");

                        try (PreparedStatement preparedUpdateFieldStatement = connection
                                .prepareStatement(updateFieldQuery.toString())) {
                            preparedUpdateFieldStatement.setString(1, value);
                            preparedUpdateFieldStatement.setString(2, fieldName);
                            preparedUpdateFieldStatement.setInt(3, localBibEntry.getSharedBibEntryData().getSharedID());
                            preparedUpdateFieldStatement.executeUpdate();
                        }
                    } else {
                        StringBuilder insertFieldQuery = new StringBuilder()
                                .append("INSERT INTO ")
                                .append(escape("FIELD"))
                                .append("(")
                                .append(escape("ENTRY_SHARED_ID"))
                                .append(", ")
                                .append(escape("NAME"))
                                .append(", ")
                                .append(escape("VALUE"))
                                .append(") VALUES(?, ?, ?)");

                        try (PreparedStatement preparedFieldStatement = connection
                                .prepareStatement(insertFieldQuery.toString())) {
                            preparedFieldStatement.setInt(1, localBibEntry.getSharedBibEntryData().getSharedID());
                            preparedFieldStatement.setString(2, fieldName);
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
     * @param bibEntry {@link BibEntry} to be deleted
     */
    public void removeEntry(BibEntry bibEntry) {
        StringBuilder query = new StringBuilder()
                .append("DELETE FROM ")
                .append(escape("ENTRY"))
                .append(" WHERE ")
                .append(escape("SHARED_ID"))
                .append(" = ?");

        try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            preparedStatement.setInt(1, bibEntry.getSharedBibEntryData().getSharedID());
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
        List<BibEntry> sharedEntries = getSharedEntryList(sharedID);
        if (!sharedEntries.isEmpty()) {
            return Optional.of(sharedEntries.get(0));
        }
        return Optional.empty();
    }

    public List<BibEntry> getSharedEntries() {
        return getSharedEntryList(0);
    }

    /**
     * @param sharedID Entry ID. If 0, all entries are going to be fetched.
     * @return List of {@link BibEntry} instances
     */
    private List<BibEntry> getSharedEntryList(int sharedID) {
        List<BibEntry> sharedEntries = new ArrayList<>();

        StringBuilder selectEntryQuery = new StringBuilder();
        selectEntryQuery.append("SELECT * FROM ");
        selectEntryQuery.append(escape("ENTRY"));

        if (sharedID != 0) {
            selectEntryQuery.append(" WHERE ");
            selectEntryQuery.append(escape("SHARED_ID"));
            selectEntryQuery.append(" = ");
            selectEntryQuery.append(sharedID);
        }

        selectEntryQuery.append(" ORDER BY ");
        selectEntryQuery.append(escape("SHARED_ID"));

        try (ResultSet selectEntryResultSet = connection.createStatement().executeQuery(selectEntryQuery.toString())) {
            while (selectEntryResultSet.next()) {
                BibEntry bibEntry = new BibEntry();
                // setting the base attributes once
                bibEntry.getSharedBibEntryData().setSharedID(selectEntryResultSet.getInt("SHARED_ID"));
                bibEntry.setType(selectEntryResultSet.getString("TYPE"));
                bibEntry.getSharedBibEntryData().setVersion(selectEntryResultSet.getInt("VERSION"));

                StringBuilder selectFieldQuery = new StringBuilder()
                    .append("SELECT * FROM ")
                    .append(escape("FIELD"))
                    .append(" WHERE ")
                    .append(escape("ENTRY_SHARED_ID"))
                    .append(" = ?");

                try (PreparedStatement preparedSelectFieldStatement = connection.prepareStatement(selectFieldQuery.toString())) {
                    preparedSelectFieldStatement.setInt(1, selectEntryResultSet.getInt("SHARED_ID"));
                    try (ResultSet selectFieldResultSet = preparedSelectFieldStatement.executeQuery()) {
                        while (selectFieldResultSet.next()) {
                            bibEntry.setField(selectFieldResultSet.getString("NAME"),
                                    Optional.ofNullable(selectFieldResultSet.getString("VALUE")), EntryEventSource.SHARED);
                        }
                    }
                }
                sharedEntries.add(bibEntry);
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }

        return sharedEntries;
    }

    /**
     * Retrieves a mapping between the columns SHARED_ID and VERSION.
     */
    public Map<Integer, Integer> getSharedIDVersionMapping() {
        Map<Integer, Integer> sharedIDVersionMapping = new HashMap<>();
        StringBuilder selectEntryQuery = new StringBuilder()
            .append("SELECT * FROM ")
            .append(escape("ENTRY"))
            .append(" ORDER BY ")
            .append(escape("SHARED_ID"));

        try (ResultSet selectEntryResultSet = connection.createStatement().executeQuery(selectEntryQuery.toString())) {
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

        try (ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM " + escape("METADATA"))) {
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
        StringBuilder updateQuery = new StringBuilder()
                    .append("UPDATE ")
                    .append(escape("METADATA"))
                    .append(" SET ")
                    .append(escape("VALUE"))
                    .append(" = ? ")
                    .append(" WHERE ")
                    .append(escape("KEY"))
                    .append(" = ?");

        StringBuilder insertQuery = new StringBuilder()
                .append("INSERT INTO ")
                .append(escape("METADATA"))
                .append("(")
                .append(escape("KEY"))
                .append(", ")
                .append(escape("VALUE"))
                .append(") VALUES(?, ?)");

        for (Map.Entry<String, String> metaEntry : data.entrySet()) {
            try (PreparedStatement updateStatement = connection.prepareStatement(updateQuery.toString())) {
                updateStatement.setString(2, metaEntry.getKey());
                updateStatement.setString(1, metaEntry.getValue());
                if (updateStatement.executeUpdate() == 0) {
                    // No rows updated -> insert data
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery.toString())) {
                        insertStatement.setString(1, metaEntry.getKey());
                        insertStatement.setString(2, metaEntry.getValue());
                        insertStatement.executeUpdate();
                    } catch (SQLException e) {
                        LOGGER.error("SQL Error: ", e);
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("SQL Error: ", e);
            }
        }
    }

    /**
     *  Returns a new instance of the abstract type {@link DBMSProcessor}
     */
    public static DBMSProcessor getProcessorInstance(DatabaseConnection connection) {
        DBMSType type = connection.getProperties().getType();
        if (type == DBMSType.MYSQL) {
            return new MySQLProcessor(connection);
        } else if (type == DBMSType.POSTGRESQL) {
            return new PostgreSQLProcessor(connection);
        } else if (type == DBMSType.ORACLE) {
            return new OracleProcessor(connection);
        }
        return null; // can never happen except new types were added without updating this method.
    }

    public DatabaseConnectionProperties getDBMSConnectionProperties() {
        return this.connectionProperties;
    }

    /**
     * Listens for notifications from DBMS.
     * Needs to be implemented if LiveUpdate is supported by the DBMS
     *
     * @param dbmsSynchronizer {@link DBMSSynchronizer} which handles the notification.
     */
    public void startNotificationListener(@SuppressWarnings("unused") DBMSSynchronizer dbmsSynchronizer) {
        // nothing to do
    }

    /**
     * Terminates the notification listener.
     * Needs to be implemented if LiveUpdate is supported by the DBMS
     */
    public void stopNotificationListener() {
        // nothing to do
    }

    /**
     * Notifies all clients ({@link DBMSSynchronizer}) which are connected to the same DBMS.
     * Needs to be implemented if LiveUpdate is supported by the DBMS
     */
    public void notifyClients() {
        // nothing to do
    }
}
