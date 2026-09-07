package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseConnection {

    DatabaseConnectionProperties getProperties();

    Connection getConnection();

    /// Opens an additional connection to the same database. Long-running blocking operations
    /// (such as the notification listener's polling) need their own connection, because a
    /// connection serializes all operations running on it.
    Connection openNewConnection() throws SQLException;
}
