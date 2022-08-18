package org.jabref.gui.collab.experimental.entrychange;

import javafx.fxml.FXML;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;

public final class EntryChangeDetailsView extends ExternalChangeDetailsView {

    private EntryChangeDetailsViewModel viewModel;
    private EntryChange entryChange;

    public EntryChangeDetailsView(EntryChange entryChange, BibDatabaseContext bibDatabaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager) {
    }

    @FXML
    private void initialize() {
        viewModel = new EntryChangeDetailsViewModel();
    }
}
