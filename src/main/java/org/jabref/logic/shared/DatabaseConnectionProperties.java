package org.jabref.logic.shared;

public interface DatabaseConnectionProperties {

    DBMSType getType();

    String getDatabase();

    int getPort();

    String getHost();

    String getUser();

    String getPassword();

    boolean isValid();

    String getKeyStore();

    boolean isUseSSL();

    boolean isAllowPublicKeyRetrieval();

    String getServerTimezone();
}
