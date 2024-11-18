package org.jabref.gui.entryeditor;

import javafx.scene.Parent;
import javafx.scene.control.SplitPane;

import org.jabref.gui.preview.PreviewPanel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public abstract class TabWithPreviewPanel extends EntryEditorTab {
    protected final BibDatabaseContext databaseContext;
    protected final PreviewPanel previewPanel;

    public TabWithPreviewPanel(BibDatabaseContext databaseContext, PreviewPanel previewPanel) {
        this.databaseContext = databaseContext;
        this.previewPanel = previewPanel;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        previewPanel.setDatabase(databaseContext);
        previewPanel.setEntry(entry);
    }

    protected void removePreviewPanelFromOtherTabs() {
        Parent parent = previewPanel.getParent();
        if (parent != null) {  // On first run, there is no parent container attached
            assert parent.getParent() instanceof SplitPane;
            if (parent.getParent() instanceof SplitPane splitPane && splitPane.getItems().contains(previewPanel)) {
                splitPane.getItems().remove(previewPanel);
            }
        }
    }
}
