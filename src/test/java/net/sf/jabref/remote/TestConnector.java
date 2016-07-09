package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnector {

    public static DBMSType currentConnectionType;


    public static Connection getTestConnection(DBMSType dbmsType) throws ClassNotFoundException, SQLException {
        currentConnectionType = dbmsType;

        TestConnectionData connectionData = getTestConnectionData(dbmsType);

        return DBMSConnector.getNewConnection(dbmsType, connectionData.getHost(), connectionData.getDatabase(),
                connectionData.getUser(), connectionData.getPassord());
    }

    public static TestConnectionData getTestConnectionData(DBMSType dbmsType) {

        if (dbmsType == DBMSType.MYSQL) {
            return new TestConnectionData("localhost", "jabref", "root", "");
        }

        if (dbmsType == DBMSType.POSTGRESQL) {
            return new TestConnectionData("localhost", "jabref", "postgres", "");
        }

        if (dbmsType == DBMSType.ORACLE) {
            return new TestConnectionData("localhost", "xe", "travis", "travis");
        }

        return new TestConnectionData();
    }
}
