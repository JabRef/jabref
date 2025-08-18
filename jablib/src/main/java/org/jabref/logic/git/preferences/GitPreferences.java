package org.jabref.logic.git.preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.NonNull;

public class GitPreferences {
    private final StringProperty username;
    private final StringProperty pat;
    private final StringProperty repositoryUrl;
    private final BooleanProperty rememberPat;

    public GitPreferences(String username,
                          String pat,
                          String repositoryUrl,
                          boolean rememberPat) {
        this.username = new SimpleStringProperty(username);
        this.pat = new SimpleStringProperty(pat);
        this.repositoryUrl = new SimpleStringProperty(repositoryUrl);
        this.rememberPat = new SimpleBooleanProperty(rememberPat);
    }

    public void setUsername(@NonNull String username) {
        this.username.set(username);
    }

    public String getUsername() {
        return username.get();
    }

    public void setPat(@NonNull String pat) {
        this.pat.set(pat);
    }

    public String getPat() {
        return pat.get();
    }

    public String getRepositoryUrl() {
        return repositoryUrl.get();
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
