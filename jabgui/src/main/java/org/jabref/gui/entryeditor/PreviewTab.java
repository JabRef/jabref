package org.jabref.gui.entryeditor;

import javafx.scene.control.SplitPane;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;

/// @implNote The Preview tab's visibility is driven by the "show preview as a separate tab" preference
/// (its {@link EntryEditorTabModel} visibility bit is unused); the factory wires that gate.
public class PreviewTab extends TabWithPreviewPanel {

    private final SplitPane splitPane;

    public PreviewTab(GuiPreferences preferences,
                      StateManager stateManager,
                      PreviewPanel previewPanel) {
        super(stateManager, previewPanel);

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(EntryEditorTabModel.BuiltIn.PREVIEW.displayName());

        splitPane = new SplitPane();
        setContent(splitPane);
    }

    protected void handleFocus() {
        removePreviewPanelFromOtherTabs();
        this.splitPane.getItems().clear();
        this.splitPane.getItems().add(previewPanel);
    }
}
