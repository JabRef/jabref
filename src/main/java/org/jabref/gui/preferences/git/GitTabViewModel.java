package org.jabref.gui.preferences.git;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import org.jabref.gui.git.GitPreferences;
import org.jabref.gui.preferences.PreferenceTabViewModel;

public class GitTabViewModel implements PreferenceTabViewModel {

    private StringProperty username = new SimpleStringProperty();
    private StringProperty password = new SimpleStringProperty();
    private GitPreferences gitPreferences;
    @FXML private TextField usernameInputField;
    @FXML private PasswordField passwordInputField;

    public GitTabViewModel(GitPreferences gitPreferences, TextField usernameInputField, PasswordField passwordInputField) {
        this.gitPreferences = gitPreferences;
        this.usernameInputField = usernameInputField;
        this.passwordInputField = passwordInputField;
    }

    @Override
    public void setValues() {
        // TEST
        this.username.setValue(this.usernameInputField.getText());
        this.password.setValue(this.passwordInputField.getText());
    }

    @Override
    public void storeSettings() {
        this.gitPreferences.setUsername(this.usernameInputField.getText());
        this.gitPreferences.setPassword(this.passwordInputField.getText());
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
        return this.username;
    }

    public StringProperty getPassword() {
        return this.password;
    }
}
