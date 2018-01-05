package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.model.database.shared.DBMSType;

/**
 * This class provides helping methods for database tests.
 * Furthermore it determines database systems which are ready to be used for tests.
 */
public class TestManager {

    public static Collection<DBMSType> getDBMSTypeTestParameter() {

        Set<DBMSType> dbmsTypes = new HashSet<>();
        for (DBMSType dbmsType : DBMSType.values()) {
            try {
                TestConnector.getTestDBMSConnection(dbmsType);
                dbmsTypes.add(dbmsType);
            } catch (SQLException | InvalidDBMSConnectionPropertiesException e) {
                // skip parameter
            }
        }
        return dbmsTypes;
    }

    public static void clearTables(DBMSConnection dbmsConnection) throws SQLException {
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
                    .executeUpdate("BEGIN\n" + "EXECUTE IMMEDIATE 'DROP TABLE \"FIELD\"';\n"
                        + "EXECUTE IMMEDIATE 'DROP TABLE \"ENTRY\"';\n"
                        + "EXECUTE IMMEDIATE 'DROP TABLE \"METADATA\"';\n"
                        + "EXECUTE IMMEDIATE 'DROP SEQUENCE \"ENTRY_SEQ\"';\n" + "EXCEPTION\n" + "WHEN OTHERS THEN\n"
                        + "IF SQLCODE != -942 THEN\n" + "RAISE;\n" + "END IF;\n" + "END;");
        }
    }
}
