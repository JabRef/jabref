package org.jabref.logic.shared;

import java.sql.SQLException;

/**
 * This class provides helping methods for database tests. Furthermore, it determines database systems which are ready to
 * be used for tests.
 */
public class TestManager {
    public static void clearTables(DBMSConnection dbmsConnection) throws SQLException {
        dbmsConnection.getConnection().createStatement().executeUpdate("DROP SCHEMA IF EXISTS \"jabref-alpha\" CASCADE");
    }
}
