package org.jabref.gui.collab.experimental.entryadd;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.collab.experimental.ExternalChangeDetailsView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.preferences.PreferencesService;

public final class EntryAddDetailsView extends ExternalChangeDetailsView {

    private PreferencesService preferencesService;

    public EntryAddDetailsView(EntryAdd entryAdd, BibDatabaseContext bibDatabaseContext, DialogService dialogService, StateManager stateManager, ThemeManager themeManager, PreferencesService preferencesService) {

        PreviewViewer previewViewer = new PreviewViewer(bibDatabaseContext, dialogService, stateManager, themeManager);
        previewViewer.setLayout(preferencesService.getPreviewPreferences().getSelectedPreviewLayout());
        previewViewer.setEntry(entryAdd.getAddedEntry());

        setLeftAnchor(previewViewer, 8d);
        setTopAnchor(previewViewer, 8d);
        setRightAnchor(previewViewer, 8d);
        setBottomAnchor(previewViewer, 8d);

        getChildren().setAll(previewViewer);
    }
}
