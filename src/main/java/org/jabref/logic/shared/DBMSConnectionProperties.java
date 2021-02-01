package org.jabref.logic.shared;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps all essential data for establishing a new connection to a DBMS using {@link DBMSConnection}.
 */
public class DBMSConnectionProperties implements DatabaseConnectionProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBMSConnectionProperties.class);

    private DBMSType type;
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;
    private boolean allowPublicKeyRetrieval;
    private boolean useSSL;
    private String serverTimezone = "";

    // Not needed for connection, but stored for future login
    private String keyStore;

    /**
     * Gets all required data from {@link SharedDatabasePreferences} and sets them if present.
     */
    public DBMSConnectionProperties(SharedDatabasePreferences prefs) {
        if (prefs.getType().isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(prefs.getType().get());
            if (dbmsType.isPresent()) {
                this.type = dbmsType.get();
            }
        }

        prefs.getHost().ifPresent(theHost -> this.host = theHost);
        prefs.getPort().ifPresent(thePort -> this.port = Integer.parseInt(thePort));
        prefs.getName().ifPresent(theDatabase -> this.database = theDatabase);
        prefs.getKeyStoreFile().ifPresent(theKeystore -> this.keyStore = theKeystore);
        prefs.getServerTimezone().ifPresent(theServerTimezone -> this.serverTimezone = theServerTimezone);
        this.useSSL = prefs.isUseSSL();

        if (prefs.getUser().isPresent()) {
            this.user = prefs.getUser().get();
            if (prefs.getPassword().isPresent()) {
                try {
                    this.password = new Password(prefs.getPassword().get().toCharArray(), prefs.getUser().get()).decrypt();
                } catch (UnsupportedEncodingException | GeneralSecurityException e) {
                    LOGGER.error("Could not decrypt password", e);
                }
            }
        }

        if (!prefs.getPassword().isPresent()) {
            // Some DBMS require a non-null value as a password (in case of using an empty string).
            this.password = "";
        }
    }

    DBMSConnectionProperties(DBMSType type, String host, int port, String database, String user,
                             String password, boolean useSSL, boolean allowPublicKeyRetrieval, String serverTimezone, String keyStore) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.useSSL = useSSL;
        this.allowPublicKeyRetrieval = allowPublicKeyRetrieval;
        this.serverTimezone = serverTimezone;
        this.keyStore = keyStore;
    }

    @Override
    public DBMSType getType() {
        return type;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getDatabase() {
        return database;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isUseSSL() {
        return useSSL;
    }

    @Override
    public boolean isAllowPublicKeyRetrieval() {
        return allowPublicKeyRetrieval;
    }

    @Override
    public String getServerTimezone() {
        return serverTimezone;
    }

    public String getUrl() {
        String url = type.getUrl(host, port, database);
        return url;
    }

    /**
     * Returns username, password and ssl as Properties Object
     *
     * @return Properties with values for user, password and ssl
     */
    public Properties asProperties() {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("serverTimezone", serverTimezone);
        if (useSSL) {
            props.setProperty("ssl", Boolean.toString(useSSL));
            props.setProperty("useSSL", Boolean.toString(useSSL));
        }
        if (allowPublicKeyRetrieval) {
            props.setProperty("allowPublicKeyRetrieval", Boolean.toString(allowPublicKeyRetrieval));
        }
        return props;
    }

    @Override
    public String getKeyStore() {
        return keyStore;
    }

    /**
     * Compares all properties except the password.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DBMSConnectionProperties)) {
            return false;
        }
        DBMSConnectionProperties properties = (DBMSConnectionProperties) obj;
        return Objects.equals(type, properties.getType())
                && this.host.equalsIgnoreCase(properties.getHost())
                && Objects.equals(port, properties.getPort())
                && Objects.equals(database, properties.getDatabase())
                && Objects.equals(user, properties.getUser())
                && Objects.equals(useSSL, properties.isUseSSL())
                && Objects.equals(allowPublicKeyRetrieval, properties.isAllowPublicKeyRetrieval())
                && Objects.equals(serverTimezone, properties.getServerTimezone());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, host, port, database, user, useSSL);
    }

    @Override
    public boolean isValid() {
        return Objects.nonNull(type)
                && Objects.nonNull(host)
                && Objects.nonNull(port)
                && Objects.nonNull(database)
                && Objects.nonNull(user)
                && Objects.nonNull(password);
    }
}
