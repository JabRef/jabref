package org.jabref.logic.shared.prefs;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.logic.shared.DatabaseConnectionProperties;
import org.jabref.logic.shared.security.Password;

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
    private static final String SHARED_DATABASE_PASSWORD = "sharedDatabasePassword";
    private static final String SHARED_DATABASE_REMEMBER_PASSWORD = "sharedDatabaseRememberPassword";
    private static final String SHARED_DATABASE_USE_SSL = "sharedDatabaseUseSSL";
    private static final String SHARED_DATABASE_KEYSTORE_FILE = "sharedDatabaseKeyStoreFile";
    private static final String SHARED_DATABASE_SERVER_TIMEZONE = "sharedDatabaseServerTimezone";

    // This {@link Preferences} is used only for things which should not appear in real JabRefPreferences due to security reasons.
    private final Preferences internalPrefs;

    public SharedDatabasePreferences() {
        this(DEFAULT_NODE);
    }

    public SharedDatabasePreferences(String sharedDatabaseID) {
        internalPrefs = Preferences.userRoot().node(PREFERENCES_PATH_NAME).node(sharedDatabaseID);
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

    public Optional<String> getPassword() {
        return getOptionalValue(SHARED_DATABASE_PASSWORD);
    }

    public Optional<String> getKeyStoreFile() {
        return getOptionalValue(SHARED_DATABASE_KEYSTORE_FILE);
    }

    public Optional<String> getServerTimezone() {
        return getOptionalValue(SHARED_DATABASE_SERVER_TIMEZONE);
    }

    public boolean getRememberPassword() {
        return internalPrefs.getBoolean(SHARED_DATABASE_REMEMBER_PASSWORD, false);
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

    public void setPassword(String password) {
        internalPrefs.put(SHARED_DATABASE_PASSWORD, password);
    }

    public void setRememberPassword(boolean rememberPassword) {
        internalPrefs.putBoolean(SHARED_DATABASE_REMEMBER_PASSWORD, rememberPassword);
    }

    public void setUseSSL(boolean useSSL) {
        internalPrefs.putBoolean(SHARED_DATABASE_USE_SSL, useSSL);
    }

    public void setKeystoreFile(String keystoreFile) {
        internalPrefs.put(SHARED_DATABASE_KEYSTORE_FILE, keystoreFile);
    }

    public void setServerTimezone(String serverTimezone) {
        internalPrefs.put(SHARED_DATABASE_SERVER_TIMEZONE, serverTimezone);
    }

    public void clearPassword() {
        internalPrefs.remove(SHARED_DATABASE_PASSWORD);
    }

    public void clear() throws BackingStoreException {
        internalPrefs.clear();
    }

    private Optional<String> getOptionalValue(String key) {
        return Optional.ofNullable(internalPrefs.get(key, null));
    }

    public static void clearAll() throws BackingStoreException {
        Preferences.userRoot().node(PREFERENCES_PATH_NAME).clear();
    }

    public void putAllDBMSConnectionProperties(DatabaseConnectionProperties properties) {
        assert (properties.isValid());

        setType(properties.getType().toString());
        setHost(properties.getHost());
        setPort(String.valueOf(properties.getPort()));
        setName(properties.getDatabase());
        setUser(properties.getUser());
        setUseSSL(properties.isUseSSL());
        setKeystoreFile(properties.getKeyStore());
        setServerTimezone(properties.getServerTimezone());

        try {
            setPassword(new Password(properties.getPassword().toCharArray(), properties.getUser()).encrypt());
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.error("Could not store the password due to encryption problems.", e);
        }
    }
}
