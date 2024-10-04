package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.Objects;

/**
 * This class provides helping methods for database tests. Furthermore, it determines database systems which are ready to
 * be used for tests.
 */
public class TestManager {
    public static void clearTables(DBMSConnection dbmsConnection) throws SQLException {
        Objects.requireNonNull(dbmsConnection);
        dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS jabref.\"FIELD\"");
        dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS jabref.\"ENTRY\"");
        dbmsConnection.getConnection().createStatement().executeUpdate("DROP TABLE IF EXISTS jabref.\"METADATA\"");
        dbmsConnection.getConnection().createStatement().executeUpdate("DROP SCHEMA IF EXISTS jabref");
    }
}
