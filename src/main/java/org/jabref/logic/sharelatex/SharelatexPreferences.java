package org.jabref.logic.sharelatex;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jabref.JabRefMain;

public class SharelatexPreferences {

    private static final String DEFAULT_NODE = "default";
    private static final String PARENT_NODE = "jabref-sharelatex";

    private static final String SHARELATEX_URL = "sharelatexUrl";
    private static final String SHARELATEX_USER = "sharelatexUser";
    private static final String SHARELATEX_PASSWORD = "sharelatexPassword";
    private static final String SHARELATEX_REMEMBER_PASSWORD = "sharelatexRememberPassword";
    private static final String SHARELATEX_PROJECT = "sharelatexProject";

    // This {@link Preferences} is used only for things which should not appear in real JabRefPreferences due to security reasons.
    private final Preferences internalPrefs;

    public SharelatexPreferences() {
        this(DEFAULT_NODE);

    }

    public SharelatexPreferences(String sharelatexId) {
        internalPrefs = Preferences.userNodeForPackage(JabRefMain.class).parent().node(PARENT_NODE).node(sharelatexId);
    }

    public String getSharelatexUrl() {
        return getOptionalValue(SHARELATEX_URL).orElse("https://www.sharelatex.com");
    }

    public Optional<String> getUser() {
        return getOptionalValue(SHARELATEX_USER);
    }

    public Optional<String> getPassword() {
        return getOptionalValue(SHARELATEX_PASSWORD);
    }

    public Optional<String> getDefaultProject() {
        return getOptionalValue(SHARELATEX_PROJECT);
    }

    public void setSharelatexUrl(String url) {
        internalPrefs.put(SHARELATEX_URL, url);
    }

    public void setSharelatexUser(String user) {
        internalPrefs.put(SHARELATEX_USER, user);
    }

    public void setSharelatexPassword(String pwd) {
        internalPrefs.put(SHARELATEX_PASSWORD, pwd);
    }

    public void setSharelatexProject(String project) {
        internalPrefs.put(SHARELATEX_PROJECT, project);
    }

    public void setRememberPassword(boolean rememberPassword) {
        internalPrefs.putBoolean(SHARELATEX_REMEMBER_PASSWORD, rememberPassword);
    }

    public void clearPassword() {
        internalPrefs.remove(SHARELATEX_PASSWORD);
    }

    public void clear() throws BackingStoreException {
        internalPrefs.clear();
    }

    private Optional<String> getOptionalValue(String key) {
        return Optional.ofNullable(internalPrefs.get(key, null));
    }

    public static void clearAll() throws BackingStoreException {
        Preferences.userNodeForPackage(JabRefMain.class).parent().node(PARENT_NODE).clear();
    }
}
