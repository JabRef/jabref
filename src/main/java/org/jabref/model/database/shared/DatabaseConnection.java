package org.jabref.model.database.shared;

import java.sql.Connection;

public interface DatabaseConnection {

    DatabaseConnectionProperties getProperties();

    Connection getConnection();
}
