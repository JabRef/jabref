package org.jabref.gui.importer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;
import org.controlsfx.control.PopOver;

public class GenerateEntryFromIdDialog {

    @FXML DialogPane dialogPane;
    @FXML TextField idTextField;
    @FXML Button generateButton;

    @Inject private FileUpdateMonitor fileUpdateMonitor;

    private final GuiPreferences preferences;
    private final DialogService dialogService;
    private final LibraryTab libraryTab;
    private final TaskExecutor taskExecutor;
    private final StateManager stateManager;

    private PopOver entryFromIdPopOver;

    public GenerateEntryFromIdDialog(LibraryTab libraryTab, DialogService dialogService, GuiPreferences preferences, TaskExecutor taskExecutor, StateManager stateManager) {
        ViewLoader.view(this).load();
        this.preferences = preferences;
        this.dialogService = dialogService;
        this.libraryTab = libraryTab;
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;
        this.generateButton.setGraphic(IconTheme.JabRefIcons.IMPORT.getGraphicNode());
        this.generateButton.setDefaultButton(true);
    }

    @FXML private void generateEntry() {
        if (idTextField.getText().isEmpty()) {
            dialogService.notify(Localization.lang("Enter a valid ID"));
            return;
        }

        this.idTextField.requestFocus();

        GenerateEntryFromIdAction generateEntryFromIdAction = new GenerateEntryFromIdAction(
                libraryTab,
                dialogService,
                preferences,
                taskExecutor,
                entryFromIdPopOver,
                idTextField.getText(),
                stateManager,
                fileUpdateMonitor
        );
        generateEntryFromIdAction.execute();
    }

    public void setEntryFromIdPopOver(PopOver entryFromIdPopOver) {
        this.entryFromIdPopOver = entryFromIdPopOver;
    }

    public DialogPane getDialogPane() {
        return dialogPane;
    }
}
