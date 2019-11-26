package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.testutils.category.DatabaseTest;

/**
 * Stores the credentials for the test systems
 */
@DatabaseTest
public class TestConnector {

    public static DBMSConnection getTestDBMSConnection(DBMSType dbmsType) throws SQLException, InvalidDBMSConnectionPropertiesException {
        DBMSConnectionProperties properties = getTestConnectionProperties(dbmsType);
        return new DBMSConnection(properties);
    }

    public static DBMSConnectionProperties getTestConnectionProperties(DBMSType dbmsType) {
        switch (dbmsType) {
            case MYSQL:
                return new DBMSConnectionProperties(dbmsType, "127.0.0.1", 3800, "jabref", "root", "root", false, "");
            case POSTGRESQL:
                return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "postgres", "postgres", "postgres", false, "");
            case ORACLE:
                return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "xe", "travis", "travis", false, "");
            default:
                return new DBMSConnectionProperties();
        }
    }
}
