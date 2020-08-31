package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.JabRefPreferences;

public class PreviewTab extends EntryEditorTab {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final JabRefPreferences preferences;
    private final ExternalFileTypes externalFileTypes;
    private PreviewPanel previewPanel;

    public PreviewTab(BibDatabaseContext databaseContext, DialogService dialogService, JabRefPreferences preferences, ExternalFileTypes externalFileTypes) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.externalFileTypes = externalFileTypes;

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(Localization.lang("Preview"));
    }

    @Override
    protected void nextPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.nextPreviewStyle();
        }
    }

    @Override
    protected void previousPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.previousPreviewStyle();
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return preferences.getPreviewPreferences().showPreviewAsExtraTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (previewPanel == null) {
            previewPanel = new PreviewPanel(databaseContext, dialogService, externalFileTypes, preferences.getKeyBindingRepository(), preferences);
            setContent(previewPanel);
        }

        previewPanel.setEntry(entry);
    }
}
