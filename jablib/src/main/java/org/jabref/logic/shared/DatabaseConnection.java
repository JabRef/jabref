package org.jabref.logic.shared;

import java.sql.Connection;

public interface DatabaseConnection {

    DatabaseConnectionProperties getProperties();

    Connection getConnection();
}
