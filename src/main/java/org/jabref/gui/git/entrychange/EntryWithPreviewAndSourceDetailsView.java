package org.jabref.gui.collab.entrychange;

import javafx.scene.control.TabPane;

import org.jabref.gui.collab.GitChangeDetailsView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.model.database.GitContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

public final class EntryWithPreviewAndSourceDetailsView extends GitChangeDetailsView {

    private final PreviewWithSourceTab previewWithSourceTab = new PreviewWithSourceTab();

    public EntryWithPreviewAndSourceDetailsView(BibEntry entry, GitContext bibGitContext, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager, PreviewViewer previewViewer) {
        TabPane tabPanePreviewCode = previewWithSourceTab.getPreviewWithSourceTab(entry, bibGitContext, preferencesService, entryTypesManager, previewViewer);
        setLeftAnchor(tabPanePreviewCode, 8d);
        setTopAnchor(tabPanePreviewCode, 8d);
        setRightAnchor(tabPanePreviewCode, 8d);
        setBottomAnchor(tabPanePreviewCode, 8d);

        getChildren().setAll(tabPanePreviewCode);
    }
}
