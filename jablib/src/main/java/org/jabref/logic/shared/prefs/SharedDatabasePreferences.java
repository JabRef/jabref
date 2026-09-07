package org.jabref.logic.shared.prefs;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.logic.shared.DatabaseConnectionProperties;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.strings.StringUtil;

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
    private final Preferences internalPrefs;
    private final String keyringAccount;

    public SharedDatabasePreferences() {
        this(DEFAULT_NODE);
    }

    public SharedDatabasePreferences(String sharedDatabaseID) {
        internalPrefs = Preferences.userRoot().node(PREFERENCES_PATH_NAME).node(sharedDatabaseID);
        keyringAccount = sharedDatabaseID;
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
        try (Keyring keyring = Keyring.create()) {
            return Optional.of(keyring.getPassword(KEYRING_SERVICE, keyringAccount)).filter(StringUtil::isNotBlank);
        } catch (PasswordAccessException e) {
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
    /// @return whether the keyring operation succeeded
    public boolean setPassword(String password) {
        try (Keyring keyring = Keyring.create()) {
            if (StringUtil.isBlank(password)) {
                try {
                    keyring.deletePassword(KEYRING_SERVICE, keyringAccount);
                } catch (PasswordAccessException e) {
                    // nothing stored, nothing to clear
                }
            } else {
                keyring.setPassword(KEYRING_SERVICE, keyringAccount, password);
            }
            internalPrefs.remove(SHARED_DATABASE_PASSWORD);
            return true;
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
        setExpertMode(properties.isUseExpertMode());
        setJdbcUrl(properties.getJdbcUrl());

        setPassword(properties.getPassword());
    }
}
