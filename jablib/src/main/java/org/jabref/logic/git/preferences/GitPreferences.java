package org.jabref.logic.git.preferences;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.util.OptionalUtil;

import org.jspecify.annotations.NonNull;

public record GitPreferences(StringProperty username, StringProperty pat, StringProperty repositoryUrl, BooleanProperty rememberPat) {
    public GitPreferences(String username,
                          String pat,
                          String repositoryUrl,
                          boolean rememberPat) {
        this(
                new SimpleStringProperty(username),
                new SimpleStringProperty(pat),
                new SimpleStringProperty(repositoryUrl),
                new SimpleBooleanProperty(rememberPat));
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

    public boolean getRememberPat() {
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
