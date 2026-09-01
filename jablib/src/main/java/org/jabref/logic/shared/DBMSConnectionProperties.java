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

/// Keeps all essential data for establishing a new connection to a DBMS using [DBMSConnection].
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
    private String jdbcUrl = "";
    private boolean expertMode;

    /// Gets all required data from [SharedDatabasePreferences] and sets them if present.
    public DBMSConnectionProperties(SharedDatabasePreferences prefs) {
        if (prefs.getType().isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(prefs.getType().get());
            dbmsType.ifPresent(value -> this.type = value);
        }

        prefs.getHost().ifPresent(theHost -> this.host = theHost);
        prefs.getPort().ifPresent(thePort -> this.port = Integer.parseInt(thePort));
        prefs.getName().ifPresent(theDatabase -> this.database = theDatabase);
        prefs.getServerTimezone().ifPresent(theServerTimezone -> this.serverTimezone = theServerTimezone);
        prefs.getJdbcUrl().ifPresent(theJdbcUrl -> this.jdbcUrl = theJdbcUrl);

        this.expertMode = prefs.isUseExpertMode();
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

        if (prefs.getPassword().isEmpty()) {
            // Some DBMS require a non-null value as a password (in case of using an empty string).
            this.password = "";
        }
    }

    DBMSConnectionProperties(DBMSType type, String host, int port, String database, String user,
                             String password, boolean useSSL, boolean allowPublicKeyRetrieval,
                             String serverTimezone, String jdbcUrl, boolean expertMode) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        this.useSSL = useSSL;
        this.allowPublicKeyRetrieval = allowPublicKeyRetrieval;
        this.serverTimezone = serverTimezone;
        this.jdbcUrl = jdbcUrl;
        this.expertMode = expertMode;
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

    @Override
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    @Override
    public boolean isUseExpertMode() {
        return expertMode;
    }

    public String getUrl() {
        return type.getUrl(host, port, database);
    }

    /// Returns username, password and ssl as Properties Object
    ///
    /// @return Properties with values for user, password and ssl
    public Properties asProperties() {
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("serverTimezone", serverTimezone);
        // Without keepalives, NAT/firewall timeouts silently kill idle connections
        // (issue #11211: connection lost after ~2h)
        props.setProperty("tcpKeepAlive", Boolean.toString(true));
        // Fail fast on half-dead connections instead of blocking a thread indefinitely.
        // The socket timeout has to stay above the notification listener's 12 s poll.
        props.setProperty("connectTimeout", "10");
        props.setProperty("socketTimeout", "30");
        if (useSSL) {
            // Encrypt without authenticating the server - the same default as psql/libpq.
            // Managed PostgreSQL providers use private CAs, which strict validation would
            // reject out of the box. For strict validation, use the expert-mode JDBC URL with
            // sslmode=verify-full, or sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory to
            // validate against the certificates configured in JabRef's preferences.
            props.setProperty("sslmode", "require");
        }
        if (allowPublicKeyRetrieval) {
            props.setProperty("allowPublicKeyRetrieval", Boolean.toString(true));
        }
        return props;
    }

    /// Compares all properties except the password.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof DBMSConnectionProperties properties)) {
            return false;
        }
        return Objects.equals(type, properties.getType())
                && this.host.equalsIgnoreCase(properties.getHost())
                && Objects.equals(port, properties.getPort())
                && Objects.equals(database, properties.getDatabase())
                && Objects.equals(user, properties.getUser())
                && Objects.equals(useSSL, properties.isUseSSL())
                && Objects.equals(allowPublicKeyRetrieval, properties.isAllowPublicKeyRetrieval())
                && Objects.equals(serverTimezone, properties.getServerTimezone())
                && Objects.equals(jdbcUrl, properties.getJdbcUrl())
                && Objects.equals(expertMode, properties.isUseExpertMode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, host, port, database, user, useSSL, allowPublicKeyRetrieval, serverTimezone, jdbcUrl, expertMode);
    }

    @Override
    public boolean isValid() {
        return type != null
                && host != null
                && port > 0
                && database != null
                && user != null
                && password != null;
    }
}
