package org.jabref.logic.git;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class GitPreferences {
    private StringProperty username;
    private StringProperty password;
    private BooleanProperty autoCommit;

    private BooleanProperty autoSync;

    public GitPreferences(String username, String password, Boolean autoCommit, Boolean autoSync) {
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
        this.autoCommit = new SimpleBooleanProperty(autoCommit);
        this.autoSync = new SimpleBooleanProperty(autoSync);
    }

    public GitPreferences(StringProperty username, StringProperty password, BooleanProperty autoCommit, BooleanProperty autoSync) {
        this.username = username;
        this.password = password;
        this.autoCommit = autoCommit;
        this.autoSync = autoSync;
    }

    public StringProperty getUsernameProperty() {
        return this.username;
    }

    public Boolean getAutoCommit() {
        return this.autoCommit.get();
    }

    public BooleanProperty getAutoCommitProperty() {
        return this.autoCommit;
    }

    public Boolean getAutoSync() {
        return this.autoSync.get();
    }

    public BooleanProperty getAutoSyncProperty() {
        return this.autoSync;
    }

    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = new SimpleBooleanProperty(autoCommit);
    }

    public void setAutoCommitProperty(BooleanProperty autoCommit) {
        this.autoCommit = autoCommit;
    }

    public void setAutoSync(Boolean autoSync) {
        this.autoSync = new SimpleBooleanProperty(autoSync);
    }

    public void setAutoSyncProperty(BooleanProperty autoSync) {
        this.autoSync = autoSync;
    }

    public String getUsername() {
        return this.username.get();
    }

    public StringProperty getPasswordProperty() {
        return this.password;
    }

    public String getPassword() {
        return this.password.get();
    }

    public void setPassword(StringProperty password) {
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = new SimpleStringProperty(password);
    }

    public void setUsername(StringProperty username) {
        this.username = username;
    }

    public void setUsername(String username) {
        this.username = new SimpleStringProperty(username);
    }
}
