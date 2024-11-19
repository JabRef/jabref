package org.jabref.gui.preferences.git;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitPreferences;

public class AuthentifyDialogViewModel extends AbstractViewModel {
    private final DialogService dialogService;
    private final GitPreferences preferences;
    private SimpleStringProperty username;
    private SimpleStringProperty password;

    public AuthentifyDialogViewModel(DialogService dialogService,
                                     GitPreferences preferences) {

        this.dialogService = dialogService;
        this.preferences = preferences;
        this.username = preferences.getUsernameProperty();
        this.password = preferences.getPasswordProperty();
    }

    public GitViewModel saveCredentials() {
        if (username.get().isBlank() || password.get().isBlank()) {
            dialogService.showErrorDialogAndWait("Error", "Username and password cannot be empty.");
            return null;
        }

        preferences.setUsername(username.get());
        preferences.setPassword(password.get());
        // preferences.store(); // Uncomment if preferences need to be saved persistently

        // dialogService.showInformationDialogAndWait("Success", "Credentials saved successfully.");
        return new GitViewModel(preferences, dialogService);
    }

    // TODO: look over this
    public void setUsername(SimpleStringProperty username) {
        this.username = username;
    }

    public SimpleStringProperty getUsername() {
        return this.username;
    }

    public void setPassword(SimpleStringProperty password) {
        this.password = password;
    }

    public SimpleStringProperty getPassword() {
        return this.password;
    }
}
