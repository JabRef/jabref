package org.jabref.gui.collab.entrychange;

import javafx.scene.control.TabPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

public final class EntryWithPreviewAndSourceDetailsView extends DatabaseChangeDetailsView {

    private final PreviewWithSourceTab previewWithSourceTab = new PreviewWithSourceTab();

    public EntryWithPreviewAndSourceDetailsView(BibEntry entry, BibDatabaseContext bibDatabaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager) {

        TabPane tabPanePreviewCode = previewWithSourceTab.getPreviewWithSourceTab(entry, bibDatabaseContext, dialogService, stateManager, themeManager, preferencesService, entryTypesManager);
        setLeftAnchor(tabPanePreviewCode, 8d);
        setTopAnchor(tabPanePreviewCode, 8d);
        setRightAnchor(tabPanePreviewCode, 8d);
        setBottomAnchor(tabPanePreviewCode, 8d);

        getChildren().setAll(tabPanePreviewCode);
    }
}
