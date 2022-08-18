package org.jabref.gui.collab.experimental.entrychange;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public final class EntryChangeDetailsView extends ExternalChangeDetailsView {

    private EntryChangeDetailsViewModel viewModel;
    private final EntryChange entryChange;
    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final ThemeManager themeManager;

    private PreferencesService preferencesService;

    public EntryChangeDetailsView(EntryChange entryChange, BibDatabaseContext bibDatabaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService) {
        this.entryChange = entryChange;
        this.databaseContext = bibDatabaseContext;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.themeManager = themeManager;

        viewModel = new EntryChangeDetailsViewModel();

        PreviewViewer previewViewer = new PreviewViewer(databaseContext, dialogService, stateManager, themeManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(entryChange.getNewEntry());

        setLeftAnchor(previewViewer, 8d);
        setTopAnchor(previewViewer, 8d);
        setRightAnchor(previewViewer, 8d);
        setBottomAnchor(previewViewer, 8d);

        getChildren().setAll(previewViewer);
    }
}
