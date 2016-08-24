package net.sf.jabref.shared;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Processes all incoming or outgoing bib data to Oracle database and manages its structure.
 */
public class OracleProcessor extends DBMSProcessor {

    /**
     * @param connection Working SQL connection
     * @param dbmsType Instance of {@link DBMSType}
     */
    public OracleProcessor(Connection connection) {
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
    public String escape(String expression) {
        return "\"" + expression + "\"";
    }
}
