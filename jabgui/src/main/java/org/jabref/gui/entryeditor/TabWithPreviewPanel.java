package org.jabref.gui.entryeditor;

import javafx.scene.Parent;
import javafx.scene.control.SplitPane;

import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.model.entry.BibEntry;

public abstract class TabWithPreviewPanel extends EntryEditorTab {
    protected final PreviewPanel previewPanel;
    protected final StateManager stateManager;

    public TabWithPreviewPanel(StateManager stateManager, PreviewPanel previewPanel) {
        this.stateManager = stateManager;
        this.previewPanel = previewPanel;
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        previewPanel.setDatabase(stateManager.getActiveDatabase().orElse(null));
        previewPanel.setEntry(entry);
    }

    protected void removePreviewPanelFromOtherTabs() {
        Parent parent = previewPanel.getParent();
        if (parent != null) {  // On first run, there is no parent container attached
            assert parent.getParent() instanceof SplitPane;
            if (parent.getParent() instanceof SplitPane splitPane) {
                splitPane.getItems().remove(previewPanel);
            }
        }
    }
}
