package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.Globals;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;

public class AutomaticFieldEditorDialog extends BaseDialog<Void> {
    @FXML public ButtonType saveButton;
    @FXML public ButtonType cancelButton;
    @FXML
    private TabPane tabPane;

    private final UndoManager undoManager;

    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> selectedEntries;
    private AutomaticFieldEditorViewModel viewModel;

    public AutomaticFieldEditorDialog(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;
        this.undoManager = Globals.undoManager;

        this.setTitle(Localization.lang("Automatic field editor"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(saveButton, getDialogPane(), event -> saveChangesAndCloseDialog());
        ControlHelper.setAction(cancelButton, getDialogPane(), event -> cancelChangesAndCloseDialog());

        // This will prevent all dialog buttons from having the same size
        // Read more: https://stackoverflow.com/questions/45866249/javafx-8-alert-different-button-sizes
        getDialogPane().getButtonTypes().stream()
            .map(getDialogPane()::lookupButton)
            .forEach(btn-> ButtonBar.setButtonUniformSize(btn, false));
    }

    @FXML
    public void initialize() {
        viewModel = new AutomaticFieldEditorViewModel(selectedEntries, databaseContext, undoManager);

        for (AutomaticFieldEditorTab tabModel : viewModel.getFieldEditorTabs()) {
            tabPane.getTabs().add(new Tab(tabModel.getTabName(), tabModel.getContent()));
        }
    }

    private void saveChangesAndCloseDialog() {
        viewModel.saveChanges();
        close();
    }

    private void cancelChangesAndCloseDialog() {
        viewModel.cancelChanges();
        close();
    }
}
