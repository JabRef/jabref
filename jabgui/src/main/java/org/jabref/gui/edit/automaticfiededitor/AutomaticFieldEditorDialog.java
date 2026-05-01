package org.jabref.gui.edit.automaticfiededitor;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticFieldEditorDialog extends BaseDialog<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticFieldEditorDialog.class);

    @FXML
    private TabPane tabPane;

    private final DialogService dialogService;
    private final UndoManager undoManager;

    private final BibDatabase database;

    private final StateManager stateManager;

    private AutomaticFieldEditorViewModel viewModel;

    public AutomaticFieldEditorDialog(StateManager stateManager,
                                      DialogService dialogService,
                                      UndoManager undoManager) {
        this.database = stateManager.getActiveDatabase().orElseThrow().getDatabase();
        this.stateManager = stateManager;
        this.dialogService = dialogService;
        this.undoManager = undoManager;

        this.setTitle(Localization.lang("Automatic field editor"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(buttonType -> {
            if (buttonType != null && buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                saveChanges();
            } else {
                cancelChanges();
            }
            return "";
        });
    }

    @FXML
    public void initialize() {
        viewModel = new AutomaticFieldEditorViewModel(database, undoManager, dialogService, stateManager);

        for (AutomaticFieldEditorTab tabModel : viewModel.getFieldEditorTabs()) {
            tabPane.getTabs().add(new Tab(tabModel.getTabName(), tabModel.getContent()));
        }
    }

    private void saveChanges() {
        viewModel.saveChanges();
    }

    private void cancelChanges() {
        try {
            viewModel.cancelChanges();
        } catch (CannotUndoException e) {
            LOGGER.info("Could not undo", e);
        }
    }
}
