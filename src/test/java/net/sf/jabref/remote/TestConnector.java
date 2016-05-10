package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.SQLException;

public class TestConnector {

    public static DBType currentConnectionType;


    public static Connection getTestConnection(DBType dbType) throws ClassNotFoundException, SQLException {
        String user = "root";
        String password = "";
        String database = "jabref";

        if (dbType == DBType.POSTGRESQL) {
            user = "postgres";
        } else if (dbType == DBType.ORACLE) {
            user = "travis";
            password = "travis";
            database = "xe";
        }
        currentConnectionType = dbType;

        return DBConnector.getNewConnection(dbType, "localhost", database, user, password);
    }
}
