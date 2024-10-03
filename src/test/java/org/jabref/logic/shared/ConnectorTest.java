package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.testutils.category.DatabaseTest;

/**
 * Stores the credentials for the test systems
 */
@DatabaseTest
public class ConnectorTest {

    public static DBMSConnection getTestDBMSConnection(DBMSType dbmsType) throws SQLException, InvalidDBMSConnectionPropertiesException {
        DBMSConnectionProperties properties = getTestConnectionProperties(dbmsType);
        return new DBMSConnection(properties);
    }

    public static DBMSConnectionProperties getTestConnectionProperties(DBMSType dbmsType) {
        return new DBMSConnectionPropertiesBuilder().setType(dbmsType).setHost("localhost").setPort(dbmsType.getDefaultPort()).setDatabase("postgres").setUser("postgres").setPassword("postgres").setUseSSL(false).createDBMSConnectionProperties();
    }
}
