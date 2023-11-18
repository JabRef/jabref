package org.jabref.gui.git.entrychange;

import javafx.scene.control.TabPane;

import org.jabref.gui.git.GitChangeDetailsView;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.model.git.BibGitContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.preferences.PreferencesService;

public final class EntryWithPreviewAndSourceDetailsView extends GitChangeDetailsView {

    private final PreviewWithSourceTab previewWithSourceTab = new PreviewWithSourceTab();

    public EntryWithPreviewAndSourceDetailsView(BibEntry entry, BibGitContext bibGitContext, PreferencesService preferencesService, BibEntryTypesManager entryTypesManager, PreviewViewer previewViewer) {
        TabPane tabPanePreviewCode = previewWithSourceTab.getPreviewWithSourceTab(entry, bibGitContext, preferencesService, entryTypesManager, previewViewer);
        setLeftAnchor(tabPanePreviewCode, 8d);
        setTopAnchor(tabPanePreviewCode, 8d);
        setRightAnchor(tabPanePreviewCode, 8d);
        setBottomAnchor(tabPanePreviewCode, 8d);
    }

    private void setLeftAnchor(TabPane tabPanePreviewCode, double d) {
    }

    private void setBottomAnchor(TabPane tabPanePreviewCode, double d) {
    }

    private Object getChildren() {
        return null;
    }

    private void setTopAnchor(TabPane tabPanePreviewCode, double d) {
    }

    private void setRightAnchor(TabPane tabPanePreviewCode, double d) {
    }
}
