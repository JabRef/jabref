package org.jabref.gui.preferences.git;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;

import org.jabref.logic.git.GitPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;

public class GitTabViewModel implements PreferenceTabViewModel {

    private StringProperty username = new SimpleStringProperty();
    private StringProperty password = new SimpleStringProperty();
    private BooleanProperty autoCommit = new SimpleBooleanProperty();

    private BooleanProperty autoSync = new SimpleBooleanProperty();
    private GitPreferences gitPreferences;

    public GitTabViewModel(GitPreferences gitPreferences) {
        this.gitPreferences = gitPreferences;
        this.autoCommit = gitPreferences.getAutoCommitProperty();
        this.autoSync = gitPreferences.getAutoSyncProperty();
        this.username = gitPreferences.getUsernameProperty();
        this.password = gitPreferences.getPasswordProperty();
    }

    @Override
    public void setValues() {
        this.username.setValue(this.gitPreferences.getUsername());
        this.password.setValue(this.gitPreferences.getPassword());
        this.autoCommit.setValue(this.gitPreferences.getAutoCommit());
        this.autoCommit.setValue(this.gitPreferences.getAutoSync());
    }

    @Override
    public void storeSettings() {
    }

    @Override
    public boolean validateSettings() {
        return PreferenceTabViewModel.super.validateSettings();
    }

    @Override
    public List<String> getRestartWarnings() {
        return PreferenceTabViewModel.super.getRestartWarnings();
    }

    public String getUsername() {
        return this.username.get();
    }

    public StringProperty getUsernameProperty() {
        return this.username;
    }

    public String getPassword() {
        return this.password.get();
    }

    public StringProperty getPasswordProperty() {
        return this.password;
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
}
