package net.sf.jabref.shared;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnector {

    public static DBMSType currentConnectionType;


    public static Connection getTestConnection(DBMSType dbmsType) throws ClassNotFoundException, SQLException {
        currentConnectionType = dbmsType;

        DBMSConnectionProperties properties = getConnectionProperties(dbmsType);

        return DBMSConnector.getNewConnection(properties);
    }

    public static DBMSConnectionProperties getConnectionProperties(DBMSType dbmsType) {

        if (dbmsType == DBMSType.MYSQL) {
            return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "jabref", "root", "");
        }

        if (dbmsType == DBMSType.POSTGRESQL) {
            return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "jabref", "postgres", "");
        }

        if (dbmsType == DBMSType.ORACLE) {
            return new DBMSConnectionProperties(dbmsType, "localhost", dbmsType.getDefaultPort(), "xe", "travis", "travis");
        }

        return new DBMSConnectionProperties();
    }
}
