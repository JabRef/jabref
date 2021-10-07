package org.jabref.gui.importer;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.preferences.PreferencesService;

public class GenerateEntryFromIdDialog {

    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final JabRefFrame jabRefFrame;

    @FXML
    DialogPane dialogPane;
    @FXML
    TextField idTextField;
    @FXML
    Button generateButton;

    public GenerateEntryFromIdDialog(JabRefFrame jabRefFrame, DialogService dialogService, PreferencesService preferencesService, ActionFactory factory) {
        ViewLoader.view(this).load();
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.jabRefFrame = jabRefFrame;
    }

    @FXML private void generateEntry() {
        new GenerateEntryFromIdAction(
                jabRefFrame,
                dialogService,
                preferencesService,
                idTextField.getText()
        ).execute();
    }

    public DialogPane getDialogPane() {
        return dialogPane;
    }

}
