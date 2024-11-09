package org.jabref.gui.entryeditor;

import javafx.scene.control.SplitPane;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class PreviewTab extends TabWithPreviewPanel {
    public static final String NAME = "Preview";

    private final GuiPreferences preferences;
    private final SplitPane splitPane;

    public PreviewTab(BibDatabaseContext databaseContext,
                      GuiPreferences preferences,
                      PreviewPanel previewPanel) {
        super(databaseContext, previewPanel);
        this.preferences = preferences;

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(Localization.lang("Preview"));

        splitPane = new SplitPane();
        setContent(splitPane);
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.getPreviewPreferences().shouldShowPreviewAsExtraTab();
    }

    protected void handleFocus() {
        removePreviewPanelFromOtherTabs();
        this.splitPane.getItems().clear();
        this.splitPane.getItems().add(previewPanel);
    }
}
