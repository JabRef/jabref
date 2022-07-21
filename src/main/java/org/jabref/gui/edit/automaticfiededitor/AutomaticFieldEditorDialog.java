package org.jabref.gui.edit.automaticfiededitor;

import java.util.ArrayList;
import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.Globals;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;
import com.google.common.eventbus.Subscribe;

public class AutomaticFieldEditorDialog extends BaseDialog<String> {
    @FXML
    private TabPane tabPane;

    private final UndoManager undoManager;

    private final BibDatabase database;
    private final List<BibEntry> selectedEntries;
    private AutomaticFieldEditorViewModel viewModel;

    private List<NotificationPaneAdapter> notificationPanes = new ArrayList<>();

    public AutomaticFieldEditorDialog(List<BibEntry> selectedEntries, BibDatabase database) {
        this.selectedEntries = selectedEntries;
        this.database = database;
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
            .forEach(btn-> ButtonBar.setButtonUniformSize(btn, false));
    }

    @FXML
    public void initialize() {
        viewModel = new AutomaticFieldEditorViewModel(selectedEntries, database, undoManager);

        for (AutomaticFieldEditorTab tabModel : viewModel.getFieldEditorTabs()) {
            NotificationPaneAdapter notificationPane = new NotificationPaneAdapter(tabModel.getContent());
            notificationPanes.add(notificationPane);
            tabModel.registerListener(this);
            tabPane.getTabs().add(new Tab(tabModel.getTabName(), notificationPane));
        }
    }

    @Subscribe
    private void onEntriesUpdated(AutomaticFieldEditorEvent event) {
        assert event.tabIndex() < notificationPanes.size() : "The tab index is not associated with any of the automatic field editor tabs.";
        assert event.numberOfAffectedEntries() <= selectedEntries.size() : "The number of affected entries cannot exceed the number of selected entries.";

        notificationPanes.get(event.tabIndex())
                         .notify(event.numberOfAffectedEntries(), selectedEntries.size());
    }

    private void saveChanges() {
        viewModel.saveChanges();
    }

    private void cancelChanges() {
        viewModel.cancelChanges();
    }
}
