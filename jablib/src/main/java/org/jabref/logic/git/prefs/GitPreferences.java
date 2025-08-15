package org.jabref.logic.git.prefs;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.util.OptionalUtil;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitPreferences.class);
    private final StringProperty username;
    private final StringProperty pat;
    private final StringProperty repositoryUrl;
    private final BooleanProperty rememberPat;

    public GitPreferences(String username,
                          String pat,
                          String repositoryUrl,
                          Boolean rememberPat) {
        this.username = new SimpleStringProperty(username);
        this.pat = new SimpleStringProperty(pat);
        this.repositoryUrl = new SimpleStringProperty(repositoryUrl);
        this.rememberPat = new SimpleBooleanProperty(rememberPat);
    }

    public void setUsername(@NonNull String username) {
        this.username.set(username);
    }

    public Optional<String> getUsername() {
        return OptionalUtil.fromStringProperty(username);
    }

    public void setPat(@NonNull String pat) {
        this.pat.set(pat);
    }

    public Optional<String> getPat() {
        return OptionalUtil.fromStringProperty(pat);
    }

    public Optional<String> getRepositoryUrl() {
        return OptionalUtil.fromStringProperty(repositoryUrl);
    }

    public void setRepositoryUrl(@NonNull String repositoryUrl) {
        this.repositoryUrl.set(repositoryUrl);
    }

    public Boolean getRememberPat() {
        return this.rememberPat.get();
    }

    public void setRememberPat(boolean remember) {
        this.rememberPat.set(remember);
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public StringProperty patProperty() {
        return pat;
    }

    public StringProperty repositoryUrlProperty() {
        return repositoryUrl;
    }

    public BooleanProperty rememberPatProperty() {
        return rememberPat;
    }
}
