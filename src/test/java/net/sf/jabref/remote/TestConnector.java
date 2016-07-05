package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnector {

    public static DBMSType currentConnectionType;


    public static Connection getTestConnection(DBMSType dbType) throws ClassNotFoundException, SQLException {
        String user = "root";
        String password = "";
        String database = "jabref";

        if (dbType == DBMSType.POSTGRESQL) {
            user = "postgres";
        } else if (dbType == DBMSType.ORACLE) {
            user = "travis";
            password = "travis";
            database = "xe";
        }
        currentConnectionType = dbType;

        return DBMSConnector.getNewConnection(dbType, "localhost", database, user, password);
    }
}
