package org.jabref.gui.preferences.git;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.logic.git.GitPreferences;

public class GitTabViewModel implements PreferenceTabViewModel {
    private final StringProperty username;
    private final StringProperty password;
    private final BooleanProperty autoCommit;
    private final BooleanProperty autoSync;
    private final GitPreferences gitPreferences;

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
        this.autoCommit.setValue(this.gitPreferences.getAutoCommit() || this.gitPreferences.getAutoSync());
        this.autoSync.setValue(this.gitPreferences.getAutoSync());
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

    public BooleanProperty getAutoCommitProperty() {
        return this.autoCommit;
    }

    public BooleanProperty getAutoSyncProperty() {
        return this.autoSync;
    }
}
