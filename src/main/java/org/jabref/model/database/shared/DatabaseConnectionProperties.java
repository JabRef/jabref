package org.jabref.model.database.shared;

public interface DatabaseConnectionProperties {

    DBMSType getType();

    String getDatabase();

    int getPort();

    String getHost();

    String getUser();

    String getPassword();

    boolean isValid();
}
