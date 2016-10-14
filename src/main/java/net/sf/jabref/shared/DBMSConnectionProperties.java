package net.sf.jabref.shared;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.shared.prefs.SharedDatabasePreferences;
import net.sf.jabref.shared.security.Password;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Keeps all essential data for establishing a new connection to a DBMS using {@link DBMSConnector}.
 */
public class DBMSConnectionProperties {

    private static final Log LOGGER = LogFactory.getLog(DBMSConnectionProperties.class);

    private DBMSType type;
    private String host;
    private int port;
    private String database;
    private String user;
    private String password;


    public DBMSConnectionProperties() {
        // no data
    }

    public DBMSConnectionProperties(SharedDatabasePreferences prefs) {
        setFromPreferences(prefs);
    }

    public DBMSConnectionProperties(DBMSType type, String host, int port, String database, String user,
            String password) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
    }

    public DBMSType getType() {
        return type;
    }

    public void setType(DBMSType type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Compares all properties except the password.
     */
    public boolean equals(DBMSConnectionProperties properties) {
        return this.type.equals(properties.getType())
                && this.host.equalsIgnoreCase(properties.getHost())
                && this.port == properties.getPort()
                && this.database.equals(properties.getDatabase())
                && this.user.equals(properties.getUser());
    }

    /**
     *  Gets all required data from {@link SharedDatabasePreferences} and sets them if present.
     */
    private void setFromPreferences(SharedDatabasePreferences prefs) {
        if (prefs.getType().isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(prefs.getType().get());
            if (dbmsType.isPresent()) {
                this.type = dbmsType.get();
            }
        }

        prefs.getHost().ifPresent(theHost -> this.host = theHost);
        prefs.getPort().ifPresent(thePort -> this.port = Integer.parseInt(thePort));
        prefs.getName().ifPresent(theDatabase -> this.database = theDatabase);

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

    public boolean isValid() {
        return Objects.nonNull(type)
                && Objects.nonNull(host)
                && Objects.nonNull(port)
                && Objects.nonNull(database)
                && Objects.nonNull(user)
                && Objects.nonNull(password);
    }
}
