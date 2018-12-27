package org.jabref.logic.shared;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.jabref.logic.shared.listener.OracleNotificationListener;
import org.jabref.model.database.shared.DatabaseConnection;

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
        return "\"" + expression + "\"";
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
