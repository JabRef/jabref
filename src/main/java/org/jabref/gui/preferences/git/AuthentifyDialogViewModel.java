package org.jabref.gui.preferences.git;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.logic.git.GitPreferences;
import org.jabref.logic.shared.security.Password;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthentifyDialogViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthentifyDialogViewModel.class);
    private final DialogService dialogService;
    private final GitPreferences preferences;
    private final SimpleStringProperty username = new SimpleStringProperty("");
    private final SimpleStringProperty password = new SimpleStringProperty("");

    public AuthentifyDialogViewModel(DialogService dialogService,
                                     GitPreferences preferences) {

        this.dialogService = dialogService;
        this.preferences = preferences;
    }

    public GitViewModel saveCredentials() {
        if (username.get().isBlank() || password.get().isBlank()) {
            dialogService.showErrorDialogAndWait("Error", "Username and password cannot be empty.");
            return null;
        }

        Optional<String> passPhrase = dialogService.showPasswordDialogAndWait("Passphrase",
                "Please enter a passphrase to encrypt your password.",
                "If you wish to not encrypt your password, leave the passphrase field empty or press cancel.");
        passPhrase = passPhrase.filter(phrase -> !phrase.isBlank()); // filter out empty strings

        preferences.setUsername(username.get());
        preferences.setPasswordEncrypted(passPhrase.isPresent());
        try {
            preferences.setPassword(new Password(password.get(), passPhrase.orElse(username.get())).encrypt());
        } catch (GeneralSecurityException | UnsupportedEncodingException e) {
            LOGGER.error("Could not store the password due to encryption problems.", e);
        }
        // changes apply as soon as the user clicks save
        dialogService.notify("Credentials saved successfully.");

        return new GitViewModel(preferences, dialogService);
    }

    public SimpleStringProperty getUsername() {
        return this.username;
    }

    public SimpleStringProperty getPassword() {
        return this.password;
    }
}
