package org.jabref.gui.entryeditor;

import javafx.scene.control.SplitPane;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.logic.l10n.Localization;

/// @implNote The Preview tab's visibility is driven by the "show preview as a separate tab" preference
/// (its {@link EntryEditorTabModel} visibility bit is unused); the factory wires that gate.
public class PreviewTab extends TabWithPreviewPanel implements NamedEntryEditorTab {
    public static final String NAME = "Preview";

    private final SplitPane splitPane;

    public PreviewTab(GuiPreferences preferences,
                      StateManager stateManager,
                      PreviewPanel previewPanel) {
        super(stateManager, previewPanel);

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(Localization.lang("Preview"));

        splitPane = new SplitPane();
        setContent(splitPane);
    }

    @Override
    public String getName() {
        return NAME;
    }

    protected void handleFocus() {
        removePreviewPanelFromOtherTabs();
        this.splitPane.getItems().clear();
        this.splitPane.getItems().add(previewPanel);
    }
}
