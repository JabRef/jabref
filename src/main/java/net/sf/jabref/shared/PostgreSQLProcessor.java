package net.sf.jabref.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jabref.model.entry.BibEntry;

import com.impossibl.postgres.api.jdbc.PGConnection;
import com.impossibl.postgres.api.jdbc.PGNotificationListener;
import com.impossibl.postgres.jdbc.PGDataSource;
import com.impossibl.postgres.jdbc.ThreadedHousekeeper;

/**
 * Processes all incoming or outgoing bib data to PostgreSQL database and manages its structure.
 */
public class PostgreSQLProcessor extends DBMSProcessor {

    private PGConnection pgConnection;

    private PostgresSQLNotificationListener listener;


    public PostgreSQLProcessor(DBMSConnection connection) {
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
                "CREATE TABLE IF NOT EXISTS \"ENTRY\" (" +
                "\"SHARED_ID\" SERIAL PRIMARY KEY, " +
                "\"TYPE\" VARCHAR, " +
                "\"VERSION\" INTEGER DEFAULT 1)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS \"FIELD\" (" +
                "\"ENTRY_SHARED_ID\" INTEGER REFERENCES \"ENTRY\"(\"SHARED_ID\") ON DELETE CASCADE, " +
                "\"NAME\" VARCHAR, " +
                "\"VALUE\" TEXT)");

        connection.createStatement().executeUpdate(
                "CREATE TABLE IF NOT EXISTS \"METADATA\" ("
                + "\"KEY\" VARCHAR,"
                + "\"VALUE\" TEXT)");
    }

    @Override
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
                Statement.RETURN_GENERATED_KEYS)) {

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

    @Override
    public String escape(String expression) {
        return "\"" + expression + "\"";
    }


    private class PostgresSQLNotificationListener implements PGNotificationListener {

        private final DBMSSynchronizer dbmsSynchronizer;


        public PostgresSQLNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
            this.dbmsSynchronizer = dbmsSynchronizer;
        }

        @Override
        public void notification(int processId, String channel, String payload) {
            if (!payload.equals(PROCESSOR_ID)) {
                dbmsSynchronizer.pullChanges();
            }
        }

    }

    @Override
    public void startNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        // Disable cleanup output of ThreadedHousekeeper
        Logger.getLogger(ThreadedHousekeeper.class.getName()).setLevel(Level.SEVERE);

        this.listener = new PostgresSQLNotificationListener(dbmsSynchronizer);

        PGDataSource dataSource = new PGDataSource();
        dataSource.setHost(connectionProperties.getHost());
        dataSource.setPort(connectionProperties.getPort());
        dataSource.setDatabase(connectionProperties.getDatabase());
        dataSource.setUser(connectionProperties.getUser());
        dataSource.setPassword(connectionProperties.getPassword());

        try {
            pgConnection = (PGConnection) dataSource.getConnection();
            pgConnection.createStatement().execute("LISTEN jabrefLiveUpdate");
            // Do not use `new PostgresSQLNotificationListener(...)` as the object has to exist continuously!
            // Otherwise the listener is going to be deleted by GC.
            pgConnection.addNotificationListener(listener);
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void stopNotificationListener() {
        try {
            pgConnection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void notifyClients() {
        try {
            pgConnection.createStatement().execute("NOTIFY jabrefLiveUpdate, '" + PROCESSOR_ID + "';");
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }
}
