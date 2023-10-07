package org.jabref.gui.preferences.git;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.PreferenceTabViewModel;
import org.jabref.preferences.PreferencesService;

public class GitTabViewModel implements PreferenceTabViewModel {

    private final StringProperty username = new SimpleStringProperty("ooo");
    private final StringProperty password = new SimpleStringProperty();
    private final PreferencesService preferences;
    private final DialogService dialogService;
    //private final GitPreferences gitPreferences;

    public GitTabViewModel(PreferencesService preferences, DialogService dialogService) {
        this.preferences = preferences;
        this.dialogService = dialogService;
        //this.gitPreferences = preferences.getGitPreferences();
    }

    @Override
    public void setValues() {
        // TEST
        username.setValue("test");
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

    public StringProperty getUsername() {
        return username;
    }
}
