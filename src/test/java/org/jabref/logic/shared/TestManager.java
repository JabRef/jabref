package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.Objects;

/**
 * This class provides helping methods for database tests. Furthermore it determines database systems which are ready to
 * be used for tests.
 */
public class TestManager {

    /**
     * Determine the DBMSType to test from the environment variable "DMBS". In case that variable is not set, use "PostgreSQL" as default
     */
    public static DBMSType getDBMSTypeTestParameter() {
        return DBMSType.fromString(System.getenv("DBMS")).orElse(DBMSType.POSTGRESQL);
    }

    public static void clearTables(DBMSConnection dbmsConnection) throws SQLException {
        Objects.requireNonNull(dbmsConnection);
        DBMSType dbmsType = dbmsConnection.getProperties().getType();

        if (dbmsType == DBMSType.MYSQL) {
            dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS `FIELD`");
            dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS `ENTRY`");
            dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS `METADATA`");
        } else if (dbmsType == DBMSType.POSTGRESQL) {
            dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS \"FIELD\"");
            dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS \"ENTRY\"");
            dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS \"METADATA\"");
        } else if (dbmsType == DBMSType.ORACLE) {
            dbmsConnection.getConnection().createStatement()
                          .executeUpdate("BEGIN\n"
                                  + "EXECUTE IMMEDIATE 'DROP TABLE \"FIELD\"';\n" + "EXCEPTION\n" + "WHEN OTHERS THEN\n"
                                  + "IF SQLCODE != -942 THEN\n" + "RAISE;\n" + "END IF;\n" + "END;\n");
            dbmsConnection.getConnection().createStatement()
                          .executeUpdate("BEGIN\n"
                                  + "EXECUTE IMMEDIATE 'DROP TABLE \"ENTRY\"';\n" + "EXCEPTION\n" + "WHEN OTHERS THEN\n"
                                  + "IF SQLCODE != -942 THEN\n" + "RAISE;\n" + "END IF;\n" + "END;\n");
            dbmsConnection.getConnection().createStatement()
                          .executeUpdate("BEGIN\n"
                                  + "EXECUTE IMMEDIATE 'DROP TABLE \"METADATA\"';\n" + "EXCEPTION\n" + "WHEN OTHERS THEN\n"
                                  + "IF SQLCODE != -942 THEN\n" + "RAISE;\n" + "END IF;\n" + "END;\n");
            dbmsConnection.getConnection().createStatement()
                          // Sequence does not exist has a different error code than table does not exist
                          .executeUpdate("BEGIN\n"
                                  + "EXECUTE IMMEDIATE 'DROP SEQUENCE \"ENTRY_SEQ\"';\n" + "EXCEPTION\n" + "WHEN OTHERS THEN\n"
                                  + "IF SQLCODE != -2289 THEN\n" + "RAISE;\n" + "END IF;\n" + "END;\n");
        }
    }
}
