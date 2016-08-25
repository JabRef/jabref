package net.sf.jabref.shared.prefs;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.shared.OpenSharedDatabaseDialog;

/**
 * Stores and reads persistent data for {@link OpenSharedDatabaseDialog}.
 */
public class SharedDatabasePreferences {

    private static final String SHARED_DATABASE_TYPE = "sharedDatabaseType";
    private static final String SHARED_DATABASE_HOST = "sharedDatabaseHost";
    private static final String SHARED_DATABASE_PORT = "sharedDatabasePort";
    private static final String SHARED_DATABASE_NAME = "sharedDatabaseName";
    private static final String SHARED_DATABASE_USER = "sharedDatabaseUser";
    private static final String SHARED_DATABASE_PASSWORD = "sharedDatabasePassword";
    private static final String SHARED_DATABASE_REMEMBER_PASSWORD = "sharedDatabaseRememberPassword";

    // This {@link Preferences} is used only for things which should not appear in real JabRefPreferences due to security reasons.
    private final Preferences internalPrefs = Preferences.userNodeForPackage(OpenSharedDatabaseDialog.class);

    public Optional<String> getType() {
        return Globals.prefs.getAsOptional(SHARED_DATABASE_TYPE);
    }

    public Optional<String> getHost() {
        return Globals.prefs.getAsOptional(SHARED_DATABASE_HOST);
    }

    public Optional<String> getPort() {
        return Globals.prefs.getAsOptional(SHARED_DATABASE_PORT);
    }

    public Optional<String> getName() {
        return Globals.prefs.getAsOptional(SHARED_DATABASE_NAME);
    }

    public Optional<String> getUser() {
        return Globals.prefs.getAsOptional(SHARED_DATABASE_USER);
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(internalPrefs.get(SHARED_DATABASE_PASSWORD, null));
    }

    public boolean getRememberPassword() {
        return Globals.prefs.getBoolean(SHARED_DATABASE_REMEMBER_PASSWORD, false);
    }

    public void setType(String type) {
        Globals.prefs.put(SHARED_DATABASE_TYPE, type);
    }

    public void setHost(String host) {
        Globals.prefs.put(SHARED_DATABASE_HOST, host);
    }

    public void setPort(String port) {
        Globals.prefs.put(SHARED_DATABASE_PORT, port);
    }

    public void setName(String name) {
        Globals.prefs.put(SHARED_DATABASE_NAME, name);
    }

    public void setUser(String user) {
        Globals.prefs.put(SHARED_DATABASE_USER, user);
    }

    public void setPassword(String password) {
        internalPrefs.put(SHARED_DATABASE_PASSWORD, password);
    }

    public void setRememberPassword(boolean rememberPassword) {
        Globals.prefs.putBoolean(SHARED_DATABASE_REMEMBER_PASSWORD, rememberPassword);
    }

    public void clearPassword() {
        internalPrefs.remove(SHARED_DATABASE_PASSWORD);
    }

    public void clear() throws BackingStoreException {
        internalPrefs.clear();
    }

}
