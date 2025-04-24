package org.jabref.gui.collab.entrychange;

import javafx.scene.control.TabPane;

import org.jabref.gui.collab.DatabaseChangeDetailsView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

public final class EntryWithPreviewAndSourceDetailsView extends DatabaseChangeDetailsView {

    private final PreviewWithSourceTab previewWithSourceTab = new PreviewWithSourceTab();

    public EntryWithPreviewAndSourceDetailsView(BibEntry entry, BibDatabaseContext bibDatabaseContext, GuiPreferences preferences, BibEntryTypesManager entryTypesManager, PreviewViewer previewViewer) {
        TabPane tabPanePreviewCode = previewWithSourceTab.getPreviewWithSourceTab(entry, bibDatabaseContext, preferences, entryTypesManager, previewViewer);
        setLeftAnchor(tabPanePreviewCode, 8d);
        setTopAnchor(tabPanePreviewCode, 8d);
        setRightAnchor(tabPanePreviewCode, 8d);
        setBottomAnchor(tabPanePreviewCode, 8d);

        getChildren().setAll(tabPanePreviewCode);
    }
}
