package org.jabref.gui.entryeditor;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.SplitPane;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.logic.l10n.Localization;

public class PreviewTab extends TabWithPreviewPanel implements NamedEntryEditorTab {
    public static final String NAME = "Preview";

    private final GuiPreferences preferences;
    private final SplitPane splitPane;

    private final ObservableValue<Boolean> shouldShow;

    public PreviewTab(GuiPreferences preferences,
                      StateManager stateManager,
                      PreviewPanel previewPanel) {
        super(stateManager, previewPanel);
        this.preferences = preferences;

        this.shouldShow = preferences.getPreviewPreferences().showPreviewAsExtraTabProperty();

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(Localization.lang("Preview"));

        splitPane = new SplitPane();
        setContent(splitPane);
    }

    @Override
    public ObservableValue<Boolean> shouldShow() {
        return shouldShow;
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
