package org.jabref.gui.importer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenerateEntryFromIdDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateEntryFromIdDialog.class);

    @FXML Label titleLabel;
    @FXML DialogPane dialogPane;
    @FXML TextField idTextField;
    @FXML Button generateButton;

    private final PreferencesService preferencesService;
    private final DialogService dialogService;
    private final LibraryTab libraryTab;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;

    public GenerateEntryFromIdDialog(LibraryTab libraryTab, DialogService dialogService, PreferencesService preferencesService, StateManager stateManager, TaskExecutor taskExecutor) {
        ViewLoader.view(this).load();
        this.preferencesService = preferencesService;
        this.dialogService = dialogService;
        this.libraryTab = libraryTab;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;

        this.titleLabel.setText(Localization.lang("Import new entry from ID..."));
        this.generateButton.setGraphic(IconTheme.JabRefIcons.IMPORT.getGraphicNode());

        this.generateButton.setDefaultButton(true);

    }

    @FXML private void generateEntry() {
        if (idTextField.getText().isEmpty()) {
            dialogService.notify(Localization.lang("Enter a valid ID"));
            return;
        }

        // auto shift focus back to textfield for workflow
        this.idTextField.requestFocus();

        GenerateEntryFromIdAction generateEntryFromIdAction = new GenerateEntryFromIdAction(
                libraryTab,
                dialogService,
                preferencesService,
                stateManager,
                taskExecutor,
                idTextField.getText()
        );
        generateEntryFromIdAction.execute();
    }

    public DialogPane getDialogPane() {
        return dialogPane;
    }

}
