package org.jabref.logic.git.prefs;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.jabref.logic.shared.security.Password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);

    private static final String PREF_PATH = "/org/jabref-git";
    private static final String GITHUB_PAT_KEY = "githubPersonalAccessToken";
    private static final String GITHUB_USERNAME_KEY = "githubUsername";
    private static final String GITHUB_REMOTE_URL_KEY = "githubRemoteUrl";
    private static final String GITHUB_REMEMBER_PAT_KEY = "githubRememberPat";

    private final Preferences prefs;

    public GitPreferences() {
        this.prefs = Preferences.userRoot().node(PREF_PATH);
    }

    public void savePersonalAccessToken(String pat, String username) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(pat, "pat");
        char[] patChars = pat != null ? pat.toCharArray() : new char[0];
        final String encrypted;
        try {
            encrypted = new Password(patChars, username).encrypt();
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.error("Failed to encrypt PAT", e);
            return;
        } finally {
            Arrays.fill(patChars, '\0');
        }
        setUsername(username);
        setPat(encrypted);
    }

    public Optional<String> getPersonalAccessToken() {
        Optional<String> encrypted = getPat();
        Optional<String> username = getUsername();

        if (encrypted.isEmpty() || username.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(new Password(encrypted.get().toCharArray(), username.get()).decrypt());
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.error("Failed to decrypt GitHub PAT", e);
            return Optional.empty();
        }
    }

    public void clearGitHubPersonalAccessToken() {
        prefs.remove(GITHUB_PAT_KEY);
    }

    public void setUsername(String username) {
        Objects.requireNonNull(username, "username");
        prefs.put(GITHUB_USERNAME_KEY, username);
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(prefs.get(GITHUB_USERNAME_KEY, null));
    }

    public void setPat(String encryptedToken) {
        Objects.requireNonNull(encryptedToken, "encryptedToken");
        prefs.put(GITHUB_PAT_KEY, encryptedToken);
    }

    public Optional<String> getPat() {
        return Optional.ofNullable(prefs.get(GITHUB_PAT_KEY, null));
    }

    public Optional<String> getRepositoryUrl() {
        return Optional.ofNullable(prefs.get(GITHUB_REMOTE_URL_KEY, null));
    }

    public void setRepositoryUrl(String url) {
        Objects.requireNonNull(url, "url");
        prefs.put(GITHUB_REMOTE_URL_KEY, url);
    }

    public boolean getRememberPat() {
        return prefs.getBoolean(GITHUB_REMEMBER_PAT_KEY, false);
    }

    public void setRememberPat(boolean remember) {
        prefs.putBoolean(GITHUB_REMEMBER_PAT_KEY, remember);
    }
}
