package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
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
                return new DBMSConnectionPropertiesBuilder().setType(dbmsType).setHost("127.0.0.1").setPort(3800).setDatabase("jabref").setUser("root").setPassword("root").setUseSSL(false).setAllowPublicKeyRetrieval(true).createDBMSConnectionProperties();
            case POSTGRESQL:
                return new DBMSConnectionPropertiesBuilder().setType(dbmsType).setHost("localhost").setPort(dbmsType.getDefaultPort()).setDatabase("postgres").setUser("postgres").setPassword("postgres").setUseSSL(false).createDBMSConnectionProperties();
            case ORACLE:
                return new DBMSConnectionPropertiesBuilder().setType(dbmsType).setHost("localhost").setPort(32118).setDatabase("jabref").setUser("jabref").setPassword("jabref").setUseSSL(false).createDBMSConnectionProperties();
            default:
                return new DBMSConnectionPropertiesBuilder().createDBMSConnectionProperties();
        }
    }
}
