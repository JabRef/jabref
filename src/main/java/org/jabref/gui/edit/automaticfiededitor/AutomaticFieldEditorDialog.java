package org.jabref.gui.edit.automaticfiededitor;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;

public class AutomaticFieldEditorDialog extends BaseDialog<Void> {
    @FXML
    private TabPane tabPane;

    private final BibDatabaseContext databaseContext;
    private final List<BibEntry> selectedEntries;
    private AutomaticFieldEditorViewModel viewModel;

    public AutomaticFieldEditorDialog(List<BibEntry> selectedEntries, BibDatabaseContext databaseContext) {
        this.selectedEntries = selectedEntries;
        this.databaseContext = databaseContext;

        this.setTitle(Localization.lang("Automatic Field Editor"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    public void initialize() {
        viewModel = new AutomaticFieldEditorViewModel(selectedEntries, databaseContext);

        for (AutomaticFieldEditorTab tabModel : viewModel.getFieldEditorTabs()) {
            tabPane.getTabs().add(new Tab(tabModel.getTabName(), tabModel.getContent()));
        }
    }
}
