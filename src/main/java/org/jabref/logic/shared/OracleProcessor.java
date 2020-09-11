package org.jabref.logic.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.jabref.logic.shared.listener.OracleNotificationListener;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleStatement;
import oracle.jdbc.dcn.DatabaseChangeRegistration;

/**
 * Processes all incoming or outgoing bib data to Oracle database and manages its structure.
 */
public class OracleProcessor extends DBMSProcessor {

    private OracleConnection oracleConnection;

    private OracleNotificationListener listener;

    private DatabaseChangeRegistration databaseChangeRegistration;

    public OracleProcessor(DatabaseConnection connection) {
        super(connection);
    }

    /**
     * Creates and sets up the needed tables and columns according to the database type.
     *
     * @throws SQLException
     */
    @Override
    public void setUp() throws SQLException {
        connection.createStatement().executeUpdate(
                "CREATE TABLE \"ENTRY\" (" +
                        "\"SHARED_ID\" NUMBER NOT NULL, " +
                        "\"TYPE\" VARCHAR2(255) NULL, " +
                        "\"VERSION\" NUMBER DEFAULT 1, " +
                        "CONSTRAINT \"ENTRY_PK\" PRIMARY KEY (\"SHARED_ID\"))");

        connection.createStatement().executeUpdate("CREATE SEQUENCE \"ENTRY_SEQ\"");

        connection.createStatement().executeUpdate("CREATE TRIGGER \"ENTRY_T\" BEFORE INSERT ON \"ENTRY\" " +
                "FOR EACH ROW BEGIN SELECT \"ENTRY_SEQ\".NEXTVAL INTO :NEW.shared_id FROM DUAL; END;");

        connection.createStatement().executeUpdate(
                "CREATE TABLE \"FIELD\" (" +
                        "\"ENTRY_SHARED_ID\" NUMBER NOT NULL, " +
                        "\"NAME\" VARCHAR2(255) NOT NULL, " +
                        "\"VALUE\" CLOB NULL, " +
                        "CONSTRAINT \"ENTRY_SHARED_ID_FK\" FOREIGN KEY (\"ENTRY_SHARED_ID\") " +
                        "REFERENCES \"ENTRY\"(\"SHARED_ID\") ON DELETE CASCADE)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE \"METADATA\" (" +
                        "\"KEY\"  VARCHAR2(255) NULL," +
                        "\"VALUE\"  CLOB NOT NULL)");
    }

    @Override
    String escape(String expression) {
        return expression;
    }

    @Override
    public void startNotificationListener(DBMSSynchronizer dbmsSynchronizer) {

        this.listener = new OracleNotificationListener(dbmsSynchronizer);

        try {
            oracleConnection = (OracleConnection) connection;

            Properties properties = new Properties();
            properties.setProperty(OracleConnection.DCN_NOTIFY_ROWIDS, "true");
            properties.setProperty(OracleConnection.DCN_QUERY_CHANGE_NOTIFICATION, "true");

            databaseChangeRegistration = oracleConnection.registerDatabaseChangeNotification(properties);
            databaseChangeRegistration.addListener(listener);

            try (Statement statement = oracleConnection.createStatement()) {
                ((OracleStatement) statement).setDatabaseChangeRegistration(databaseChangeRegistration);
                StringBuilder selectQuery = new StringBuilder()
                        .append("SELECT 1 FROM ")
                        .append(escape("ENTRY"))
                        .append(", ")
                        .append(escape("METADATA"));
                // this execution registers all tables mentioned in selectQuery
                statement.executeQuery(selectQuery.toString());
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    protected void insertIntoEntryTable(List<BibEntry> entries) {
        try {
            for (BibEntry entry : entries) {
                String insertIntoEntryQuery =
                        "INSERT INTO " +
                                escape("ENTRY") +
                                "(" +
                                escape("TYPE") +
                                ") VALUES(?)";

                try (PreparedStatement preparedEntryStatement = connection.prepareStatement(insertIntoEntryQuery,
                        new String[] {"SHARED_ID"})) {

                    preparedEntryStatement.setString(1, entry.getType().getName());
                    preparedEntryStatement.executeUpdate();

                    try (ResultSet generatedKeys = preparedEntryStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            entry.getSharedBibEntryData().setSharedID(generatedKeys.getInt(1)); // set generated ID locally
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    protected void insertIntoFieldTable(List<BibEntry> bibEntries) {
        try {
            // Inserting into FIELD table
            // Coerce to ArrayList in order to use List.get()
            List<List<Field>> fields = bibEntries.stream().map(entry -> new ArrayList<>(entry.getFields()))
                                                 .collect(Collectors.toList());
            StringBuilder insertFieldQuery = new StringBuilder()
                    .append("INSERT ALL");
            int numFields = 0;
            for (List<Field> entryFields : fields) {
                numFields += entryFields.size();
            }
            for (int i = 0; i < numFields; i++) {
                insertFieldQuery.append(" INTO ")
                                .append(escape("FIELD"))
                                .append(" (")
                                .append(escape("ENTRY_SHARED_ID"))
                                .append(", ")
                                .append(escape("NAME"))
                                .append(", ")
                                .append(escape("VALUE"))
                                .append(") VALUES (?, ?, ?)");
            }
            insertFieldQuery.append(" SELECT * FROM DUAL");
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
                preparedFieldStatement.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void stopNotificationListener() {
        try {
            oracleConnection.unregisterDatabaseChangeNotification(databaseChangeRegistration);
            oracleConnection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void notifyClients() {
        // Do nothing because Oracle triggers notifications automatically.
    }
}
