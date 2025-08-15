package org.jabref.logic.git.prefs;

import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);

    private static final String PREF_PATH = "/org/jabref-git";
    private static final String GITHUB_PAT_KEY = "githubPersonalAccessToken";
    private static final String GITHUB_USERNAME_KEY = "githubUsername";
    private static final String GITHUB_REMOTE_URL_KEY = "githubRemoteUrl";
    private static final String GITHUB_REMEMBER_PAT_KEY = "githubRememberPat";

    private final Preferences preferences;

    public GitPreferences() {
        this.preferences = Preferences.userRoot().node(PREF_PATH);
    }

    public void setPersonalAccessToken(String pat) {
        Objects.requireNonNull(pat, "pat");
        preferences.put(GITHUB_PAT_KEY, pat);
    }

    public Optional<String> getPersonalAccessToken() {
        return Optional.ofNullable(preferences.get(GITHUB_PAT_KEY, null));
    }

    public void clearGitHubPersonalAccessToken() {
        preferences.remove(GITHUB_PAT_KEY);
    }

    public void setUsername(String username) {
        Objects.requireNonNull(username, "username");
        preferences.put(GITHUB_USERNAME_KEY, username);
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(preferences.get(GITHUB_USERNAME_KEY, null));
    }

    public void setPat(String encryptedToken) {
        Objects.requireNonNull(encryptedToken, "encryptedToken");
        preferences.put(GITHUB_PAT_KEY, encryptedToken);
    }

    public Optional<String> getPat() {
        return Optional.ofNullable(preferences.get(GITHUB_PAT_KEY, null));
    }

    public Optional<String> getRepositoryUrl() {
        return Optional.ofNullable(preferences.get(GITHUB_REMOTE_URL_KEY, null));
    }

    public void setRepositoryUrl(String url) {
        Objects.requireNonNull(url, "url");
        preferences.put(GITHUB_REMOTE_URL_KEY, url);
    }

    public boolean getRememberPat() {
        return preferences.getBoolean(GITHUB_REMEMBER_PAT_KEY, false);
    }

    public void setRememberPat(boolean remember) {
        preferences.putBoolean(GITHUB_REMEMBER_PAT_KEY, remember);
    }
}
