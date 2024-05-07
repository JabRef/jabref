package org.jabref.gui.edit.automaticfiededitor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutomaticFieldEditorDialog extends BaseDialog<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticFieldEditorDialog.class);

    @FXML
    private TabPane tabPane;

    private final UndoManager undoManager;

    private final BibDatabase database;
    private final List<BibEntry> selectedEntries;

    private final StateManager stateManager;

    private AutomaticFieldEditorViewModel viewModel;

    private List<NotificationPaneAdapter> notificationPanes = new ArrayList<>();

    public AutomaticFieldEditorDialog(StateManager stateManager) {
        this.selectedEntries = stateManager.getSelectedEntries();
        this.database = stateManager.getActiveDatabase().orElseThrow().getDatabase();
        this.stateManager = stateManager;
        this.undoManager = Globals.undoManager;

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

        // This will prevent all dialog buttons from having the same size
        // Read more: https://stackoverflow.com/questions/45866249/javafx-8-alert-different-button-sizes
        getDialogPane().getButtonTypes().stream()
            .map(getDialogPane()::lookupButton)
            .forEach(btn -> ButtonBar.setButtonUniformSize(btn, false));
    }

    @FXML
    public void initialize() {
        viewModel = new AutomaticFieldEditorViewModel(selectedEntries, database, undoManager, stateManager);

        for (AutomaticFieldEditorTab tabModel : viewModel.getFieldEditorTabs()) {
            NotificationPaneAdapter notificationPane = new NotificationPaneAdapter(tabModel.getContent());
            notificationPanes.add(notificationPane);
            tabPane.getTabs().add(new Tab(tabModel.getTabName(), notificationPane));
        }

        EasyBind.listen(stateManager.lastAutomaticFieldEditorEditProperty(), (obs, old, lastEdit) -> {
            viewModel.getDialogEdits().addEdit(lastEdit.getEdit());
            notificationPanes.get(lastEdit.getTabIndex())
                             .notify(lastEdit.getAffectedEntries(), selectedEntries.size());
        });
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
