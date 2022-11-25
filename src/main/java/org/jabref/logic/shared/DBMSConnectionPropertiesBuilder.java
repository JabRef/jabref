package org.jabref.logic.shared;

public class DBMSConnectionPropertiesBuilder {
    private DBMSType type;
    private String host;
    private int port = -1;
    private String database;
    private String user;
    private String password;
    private boolean useSSL;
    private boolean allowPublicKeyRetrieval;
    private String serverTimezone = "";
    private String keyStore;

    public DBMSConnectionPropertiesBuilder setType(DBMSType type) {
        this.type = type;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setHost(String host) {
        this.host = host;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setDatabase(String database) {
        this.database = database;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setUser(String user) {
        this.user = user;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setAllowPublicKeyRetrieval(boolean allowPublicKeyRetrieval) {
        this.allowPublicKeyRetrieval = allowPublicKeyRetrieval;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setServerTimezone(String serverTimezone) {
        this.serverTimezone = serverTimezone;
        return this;
    }

    public DBMSConnectionPropertiesBuilder setKeyStore(String keyStore) {
        this.keyStore = keyStore;
        return this;
    }

    public DBMSConnectionProperties createDBMSConnectionProperties() {
        if (port == -1) {
            port = type.getDefaultPort();
        }
        return new DBMSConnectionProperties(type, host, port, database, user, password, useSSL, allowPublicKeyRetrieval, serverTimezone, keyStore);
    }
}
