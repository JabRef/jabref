package org.jabref.gui.entryeditor;

import javafx.scene.control.SplitPane;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class PreviewTab extends EntryEditorTab {
    public static final String NAME = "Preview";
    private final BibDatabaseContext databaseContext;
    private final GuiPreferences preferences;
    private final PreviewPanel previewPanel;

    public PreviewTab(BibDatabaseContext databaseContext,
                      GuiPreferences preferences,
                      PreviewPanel previewPanel) {
        this.databaseContext = databaseContext;
        this.preferences = preferences;
        this.previewPanel = previewPanel;

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(Localization.lang("Preview"));
        setContent(new SplitPane(previewPanel));
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.getPreviewPreferences().shouldShowPreviewAsExtraTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        previewPanel.setDatabase(databaseContext);
        previewPanel.setEntry(entry);
    }
}
