package org.jabref.logic.shared;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jabref.JabRefExecutorService;
import org.jabref.logic.shared.listener.PostgresSQLNotificationListener;
import org.jabref.model.database.shared.DatabaseConnection;
import org.jabref.model.entry.BibEntry;

import org.postgresql.PGConnection;

/**
 * Processes all incoming or outgoing bib data to PostgreSQL database and manages its structure.
 */
public class PostgreSQLProcessor extends DBMSProcessor {

    private PostgresSQLNotificationListener listener;

    public PostgreSQLProcessor(DatabaseConnection connection) {
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
    String escape(String expression) {
        return "\"" + expression + "\"";
    }

    @Override
    public void startNotificationListener(DBMSSynchronizer dbmsSynchronizer) {
        // Disable cleanup output of ThreadedHousekeeper
        //Logger.getLogger(ThreadedHousekeeper.class.getName()).setLevel(Level.SEVERE);
        try {
            connection.createStatement().execute("LISTEN jabrefLiveUpdate");
            // Do not use `new PostgresSQLNotificationListener(...)` as the object has to exist continuously!
            // Otherwise the listener is going to be deleted by GC.
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            listener = new PostgresSQLNotificationListener(dbmsSynchronizer, pgConnection);
            JabRefExecutorService.INSTANCE.execute(listener);

        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void stopNotificationListener() {
        try {
            listener.stop();
            connection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }

    @Override
    public void notifyClients() {
        try {
            connection.createStatement().execute("NOTIFY jabrefLiveUpdate, '" + PROCESSOR_ID + "';");
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
        }
    }
}
