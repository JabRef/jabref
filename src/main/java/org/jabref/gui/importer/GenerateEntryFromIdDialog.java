package org.jabref.gui.importer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;

public class GenerateEntryFromIdDialog {

    @FXML DialogPane dialogPane;
    @FXML TextField idTextField;
    @FXML Button generateButton;

    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final LibraryTab libraryTab;

    public GenerateEntryFromIdDialog(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferencesService) {
        ViewLoader.view(this).load();
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.libraryTab = libraryTab;
    }

    @FXML private void generateEntry() {
        new GenerateEntryFromIdAction(
                libraryTab,
                dialogService,
                preferencesService,
                idTextField.getText()
        ).execute();
    }

    public DialogPane getDialogPane() {
        return dialogPane;
    }

}
