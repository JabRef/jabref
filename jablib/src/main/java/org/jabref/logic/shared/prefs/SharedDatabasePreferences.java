package org.jabref.logic.shared.prefs;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.logic.shared.DBMSConnectionUrl;
import org.jabref.logic.shared.DatabaseConnectionProperties;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.strings.StringUtil;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedDatabasePreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(SharedDatabasePreferences.class);

    private static final String DEFAULT_NODE = "default";
    private static final String PREFERENCES_PATH_NAME = "/org/jabref-shared";

    private static final String SHARED_DATABASE_TYPE = "sharedDatabaseType";
    private static final String SHARED_DATABASE_HOST = "sharedDatabaseHost";
    private static final String SHARED_DATABASE_PORT = "sharedDatabasePort";
    private static final String SHARED_DATABASE_NAME = "sharedDatabaseName";
    private static final String SHARED_DATABASE_USER = "sharedDatabaseUser";
    /// Legacy key: the password used to be stored AES-encrypted in these preferences; it now lives in the system keyring
    private static final String SHARED_DATABASE_PASSWORD = "sharedDatabasePassword";
    private static final String KEYRING_SERVICE = "org.jabref.shareddatabase";
    private static final String SHARED_DATABASE_FOLDER = "sharedDatabaseFolder";
    private static final String SHARED_DATABASE_AUTOSAVE = "sharedDatabaseAutosave";
    private static final String SHARED_DATABASE_REMEMBER_PASSWORD = "sharedDatabaseRememberPassword";
    private static final String SHARED_DATABASE_USE_SSL = "sharedDatabaseUseSSL";
    private static final String SHARED_DATABASE_EXPERT_MODE = "sharedDatabaseExpertMode";
    private static final String SHARED_DATABASE_JDBC_URL = "sharedDatabaseJdbcUrl";

    // This {@link Preferences} is used only for things which should not appear in real JabRefPreferences due to security reasons.
    private final Preferences connectionsRoot;
    private final Preferences internalPrefs;
    private final String keyringAccount;
    /// Opens the system keyring; a `Callable` because opening it throws when no backend is available
    private final Callable<Keyring> keyringFactory;

    /// Opens the connection used last; reach the other stored connections through [#forConnection(String)].
    public SharedDatabasePreferences() {
        this(Preferences.userRoot().node(PREFERENCES_PATH_NAME), DEFAULT_NODE, Keyring::create);
    }

    /// @param connectionsRoot the node holding one child node per stored connection
    /// @param keyringFactory  opens the keyring the passwords are stored in
    ///
    /// Tests pass their own root and keyring to keep away from the user's stored connections and passwords.
    SharedDatabasePreferences(Preferences connectionsRoot, String sharedDatabaseID, Callable<Keyring> keyringFactory) {
        this.connectionsRoot = connectionsRoot;
        internalPrefs = connectionsRoot.node(sharedDatabaseID);
        keyringAccount = sharedDatabaseID;
        this.keyringFactory = keyringFactory;
    }

    public Optional<String> getType() {
        return getOptionalValue(SHARED_DATABASE_TYPE);
    }

    public Optional<String> getHost() {
        return getOptionalValue(SHARED_DATABASE_HOST);
    }

    public Optional<String> getPort() {
        return getOptionalValue(SHARED_DATABASE_PORT);
    }

    public Optional<String> getName() {
        return getOptionalValue(SHARED_DATABASE_NAME);
    }

    public Optional<String> getUser() {
        return getOptionalValue(SHARED_DATABASE_USER);
    }

    /// @return the plain password from the system keyring; empty if none is stored or the keyring is unavailable
    public Optional<String> getPassword() {
        try (Keyring keyring = keyringFactory.call()) {
            return Optional.of(keyring.getPassword(KEYRING_SERVICE, keyringAccount)).filter(StringUtil::isNotBlank);
        } catch (PasswordAccessException _) {
            return migrateLegacyPassword();
        } catch (Exception e) {
            LOGGER.warn("Could not access keyring for retrieving the shared database password", e);
            return Optional.empty();
        }
    }

    private Optional<String> migrateLegacyPassword() {
        Optional<String> legacy = getOptionalValue(SHARED_DATABASE_PASSWORD);
        Optional<String> user = getUser();
        if (legacy.isEmpty() || user.isEmpty()) {
            return Optional.empty();
        }
        try {
            String password = new Password(legacy.get().toCharArray(), user.get()).decrypt();
            setPassword(password);
            return Optional.of(password);
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.error("Could not read the stored shared database password", e);
            return Optional.empty();
        }
    }

    public boolean getRememberPassword() {
        return internalPrefs.getBoolean(SHARED_DATABASE_REMEMBER_PASSWORD, false);
    }

    public Optional<String> getFolder() {
        return getOptionalValue(SHARED_DATABASE_FOLDER);
    }

    public boolean getAutosave() {
        return internalPrefs.getBoolean(SHARED_DATABASE_AUTOSAVE, false);
    }

    public boolean isUseSSL() {
        return internalPrefs.getBoolean(SHARED_DATABASE_USE_SSL, false);
    }

    public void setType(String type) {
        internalPrefs.put(SHARED_DATABASE_TYPE, type);
    }

    public void setHost(String host) {
        internalPrefs.put(SHARED_DATABASE_HOST, host);
    }

    public void setPort(String port) {
        internalPrefs.put(SHARED_DATABASE_PORT, port);
    }

    public void setName(String name) {
        internalPrefs.put(SHARED_DATABASE_NAME, name);
    }

    public void setUser(String user) {
        internalPrefs.put(SHARED_DATABASE_USER, user);
    }

    /// Stores the plain password in the system keyring; a blank password clears it.
    ///
    /// @return whether the password is stored or cleared as asked; clearing succeeds without a keyring backend, as no
    ///         password can be stored then
    public boolean setPassword(String password) {
        try (Keyring keyring = keyringFactory.call()) {
            if (StringUtil.isBlank(password)) {
                try {
                    keyring.deletePassword(KEYRING_SERVICE, keyringAccount);
                } catch (PasswordAccessException _) {
                    // nothing stored, nothing to clear
                }
            } else {
                keyring.setPassword(KEYRING_SERVICE, keyringAccount, password);
            }
            internalPrefs.remove(SHARED_DATABASE_PASSWORD);
            return true;
        } catch (BackendNotSupportedException e) {
            // Without a keyring backend no password can have been stored, so there is nothing to clear
            LOGGER.debug("No keyring backend for the shared database password", e);
            if (StringUtil.isBlank(password)) {
                internalPrefs.remove(SHARED_DATABASE_PASSWORD);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("Could not access keyring for storing the shared database password", e);
            return false;
        }
    }

    public void setRememberPassword(boolean rememberPassword) {
        internalPrefs.putBoolean(SHARED_DATABASE_REMEMBER_PASSWORD, rememberPassword);
    }

    public void setFolder(String folder) {
        internalPrefs.put(SHARED_DATABASE_FOLDER, folder);
    }

    public void setAutosave(boolean autosave) {
        internalPrefs.putBoolean(SHARED_DATABASE_AUTOSAVE, autosave);
    }

    public void setUseSSL(boolean useSSL) {
        internalPrefs.putBoolean(SHARED_DATABASE_USE_SSL, useSSL);
    }

    public void clearPassword() {
        setPassword("");
    }

    public void setExpertMode(boolean expertMode) {
        internalPrefs.putBoolean(SHARED_DATABASE_EXPERT_MODE, expertMode);
    }

    public void setJdbcUrl(String jdbcUrl) {
        internalPrefs.put(SHARED_DATABASE_JDBC_URL, jdbcUrl);
    }

    public boolean isUseExpertMode() {
        return internalPrefs.getBoolean(SHARED_DATABASE_EXPERT_MODE, false);
    }

    public Optional<String> getJdbcUrl() {
        return getOptionalValue(SHARED_DATABASE_JDBC_URL);
    }

    public void clear() throws BackingStoreException {
        clearPassword();
        internalPrefs.clear();
    }

    /// Removes this connection's stored settings from the preferences tree.
    ///
    /// The password is deleted first: should removing the settings fail afterwards, the connection is still
    /// listed, but its credential is gone rather than left behind for an entry the user asked to delete.
    ///
    /// @return whether the password was removed from the system keyring
    /// @throws BackingStoreException if the settings could not be removed
    public boolean remove() throws BackingStoreException {
        boolean passwordCleared = setPassword("");
        internalPrefs.removeNode();
        return passwordCleared;
    }

    private boolean hasConnection() {
        if (getUser().isEmpty()) {
            return false;
        }
        return isUseExpertMode()
               ? getJdbcUrl().filter(url -> !url.isBlank()).isPresent()
               : getHost().isPresent() && getName().isPresent();
    }

    /// Whether this stored connection addresses the same database as `properties`. The password is not part of
    /// the comparison, and neither are driver settings that do not identify the database (such as SSL).
    public boolean addresses(DatabaseConnectionProperties properties) {
        return addresses(properties.isUseExpertMode(),
                properties.getUser(),
                properties.getJdbcUrl(),
                properties.getHost(),
                String.valueOf(properties.getPort()),
                properties.getDatabase());
    }

    /// Whether both stored connections address the same database. Unlike [#addresses(DatabaseConnectionProperties)],
    /// this reads neither connection's password, so it does not access the system keyring.
    public boolean addressesSameDatabaseAs(SharedDatabasePreferences other) {
        return addresses(other.isUseExpertMode(),
                other.getUser().orElse(""),
                other.getJdbcUrl().orElse(""),
                other.getHost().orElse(""),
                other.getPort().orElse(""),
                other.getName().orElse(""));
    }

    private boolean addresses(boolean expertMode, String user, String jdbcUrl, String host, String port, String database) {
        if (isUseExpertMode() != expertMode) {
            return false;
        }
        if (!getUser().orElse("").equals(user)) {
            return false;
        }
        if (isUseExpertMode()) {
            return reachesSameDatabase(getJdbcUrl().orElse(""), jdbcUrl);
        }
        return getHost().orElse("").equalsIgnoreCase(host)
                && getPort().orElse("").equals(port)
                && getName().orElse("").equals(database);
    }

    /// Compares the server, port, and database a JDBC URL leads to, so that spellings such as an explicit default
    /// port count as the same database. Text that is not a PostgreSQL URL is compared as written.
    private static boolean reachesSameDatabase(String jdbcUrl, String otherJdbcUrl) {
        Optional<DBMSConnectionUrl> parsed = DBMSConnectionUrl.parse(jdbcUrl);
        Optional<DBMSConnectionUrl> otherParsed = DBMSConnectionUrl.parse(otherJdbcUrl);
        if (parsed.isEmpty() || otherParsed.isEmpty()) {
            return jdbcUrl.strip().equals(otherJdbcUrl.strip());
        }
        DBMSConnectionUrl url = parsed.get();
        DBMSConnectionUrl otherUrl = otherParsed.get();
        return url.host().equalsIgnoreCase(otherUrl.host())
                && (url.port() == otherUrl.port())
                && url.database().equals(otherUrl.database());
    }

    /// The text the connection is listed with in the login dialog. It names the user, which is part of what
    /// identifies a stored connection.
    public String getDisplayLabel() {
        if (isUseExpertMode()) {
            // Rebuilt from its parts, as a URL typed in expert mode may carry the password as a parameter
            String url = getJdbcUrl().flatMap(DBMSConnectionUrl::parse)
                                     .map(DBMSConnectionUrl::toJdbcUrl)
                                     .orElse(keyringAccount);
            return "%s (%s)".formatted(url, getUser().orElse(""));
        }
        return "%s@%s:%s/%s".formatted(
                getUser().orElse(""),
                getHost().orElse(""),
                getPort().orElse(""),
                getName().orElse(""));
    }

    /// @return the stored settings of the connection with the given identifier, in the same preferences tree
    public SharedDatabasePreferences forConnection(String sharedDatabaseID) {
        return new SharedDatabasePreferences(connectionsRoot, sharedDatabaseID, keyringFactory);
    }

    /// Finds the stored connection for a database that has been connected to before. Connecting to it again
    /// updates that connection instead of adding a second, indistinguishable one to the list.
    ///
    /// @return the identifier the connection is stored under, empty if the database is not stored yet
    // [impl->req~shared-database.saved-connections-unique~1]
    public Optional<String> findSavedId(DatabaseConnectionProperties properties) {
        return listSavedIds().stream()
                             .filter(sharedDatabaseId -> forConnection(sharedDatabaseId).addresses(properties))
                             .findFirst();
    }

    /// Before connections were reused, every save stored the same database under a new identifier. Such duplicates
    /// are listed once: the first identifier stands for all of them, also in [#findSavedId(DatabaseConnectionProperties)].
    /// They are not deleted, because a library file may still refer to any of them.
    ///
    /// Nodes without a usable connection are skipped: reading the settings of an unknown identifier (a stale
    /// reconnect entry, a library file from another computer) creates an empty node.
    ///
    /// @return the identifiers of all stored connections, without the "last used" default node
    // [impl->req~shared-database.saved-connections-offered~1]
    // [impl->req~shared-database.saved-connections-unique~1]
    public List<String> listSavedIds() {
        List<SharedDatabasePreferences> listed = new ArrayList<>();
        for (SharedDatabasePreferences stored : listStoredConnections()) {
            if (listed.stream().noneMatch(stored::addressesSameDatabaseAs)) {
                listed.add(stored);
            }
        }
        return listed.stream().map(stored -> stored.keyringAccount).toList();
    }

    /// @return the identifiers of all stored connections to the same database as this one, this one included
    public List<String> listIdsOfSameDatabase() {
        return listStoredConnections().stream()
                                      .filter(this::addressesSameDatabaseAs)
                                      .map(stored -> stored.keyringAccount)
                                      .toList();
    }

    private List<SharedDatabasePreferences> listStoredConnections() {
        try {
            return Arrays.stream(connectionsRoot.childrenNames())
                         .filter(id -> !DEFAULT_NODE.equals(id))
                         .map(this::forConnection)
                         .filter(SharedDatabasePreferences::hasConnection)
                         .toList();
        } catch (BackingStoreException e) {
            LOGGER.warn("Could not read the stored shared database connections", e);
            return List.of();
        }
    }

    private Optional<String> getOptionalValue(String key) {
        return Optional.ofNullable(internalPrefs.get(key, null));
    }

    /// Removes every stored connection, passwords included, and clears the connection used last.
    /// Used when all preferences are reset.
    ///
    /// A connection whose password cannot be deleted from the keyring is kept: removing its settings would leave the
    /// password behind under an identifier nothing refers to anymore, while a listed connection can still be removed later.
    public void clearAllConnections() throws BackingStoreException {
        for (String sharedDatabaseId : connectionsRoot.childrenNames()) {
            if (DEFAULT_NODE.equals(sharedDatabaseId)) {
                continue;
            }
            SharedDatabasePreferences stored = forConnection(sharedDatabaseId);
            if (stored.setPassword("")) {
                stored.internalPrefs.removeNode();
            } else {
                LOGGER.warn("Kept the stored shared database connection {}, as its password could not be removed from the keyring", sharedDatabaseId);
            }
        }
        forConnection(DEFAULT_NODE).clear();
    }

    /// A live connection still holds the password the user typed, which must not reach the keyring when they opted
    /// out. The choice made for this connection counts; the login dialog's last choice only applies to a connection
    /// stored for the first time, so that saving one library cannot erase another library's password.
    boolean shouldRememberPassword() {
        if (internalPrefs.get(SHARED_DATABASE_REMEMBER_PASSWORD, null) != null) {
            return getRememberPassword();
        }
        return forConnection(DEFAULT_NODE).getRememberPassword();
    }

    public void putAllDBMSConnectionProperties(DatabaseConnectionProperties properties) {
        assert (properties.isValid());

        setType(properties.getType().toString());
        setHost(properties.getHost());
        setPort(String.valueOf(properties.getPort()));
        setName(properties.getDatabase());
        setUser(properties.getUser());
        setUseSSL(properties.isUseSSL());
        setExpertMode(properties.isUseExpertMode());
        setJdbcUrl(properties.getJdbcUrl());

        boolean rememberPassword = shouldRememberPassword();
        setRememberPassword(rememberPassword);
        setPassword(rememberPassword ? properties.getPassword() : "");
    }
}
