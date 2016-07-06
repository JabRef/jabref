package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnector {

    public static DBMSType currentConnectionType;


    public static Connection getTestConnection(DBMSType dbmsType) throws ClassNotFoundException, SQLException {
        String user = "root";
        String password = "";
        String database = "jabref";

        if (dbmsType == DBMSType.POSTGRESQL) {
            user = "postgres";
        } else if (dbmsType == DBMSType.ORACLE) {
            user = "travis";
            password = "travis";
            database = "xe";
        }
        currentConnectionType = dbmsType;

        return DBMSConnector.getNewConnection(dbmsType, "localhost", database, user, password);
    }
}
