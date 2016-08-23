package net.sf.jabref.shared;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Processes all incoming or outgoing bib data to PostgreSQL database and manages its structure.
 */
public class PostgreSQLProcessor extends DBMSProcessor {

    /**
     * @param connection Working SQL connection
     * @param dbmsType Instance of {@link DBMSType}
     */
    public PostgreSQLProcessor(Connection connection) {
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

        connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS \"METADATA\" ("
                    + "\"KEY\" VARCHAR,"
                    + "\"VALUE\" TEXT)");
    }

    @Override
    public String escape(String expression) {
        return "\"" + expression + "\"";
    }
}
