package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.search.indexing.IndexingTaskManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class PreviewTab extends EntryEditorTab {
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final PreferencesService preferences;
    private final StateManager stateManager;
    private final ThemeManager themeManager;
    private final IndexingTaskManager indexingTaskManager;
    private PreviewPanel previewPanel;

    public PreviewTab(BibDatabaseContext databaseContext,
                      DialogService dialogService,
                      PreferencesService preferences,
                      StateManager stateManager,
                      ThemeManager themeManager,
                      IndexingTaskManager indexingTaskManager) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;
        this.themeManager = themeManager;
        this.indexingTaskManager = indexingTaskManager;

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
        return preferences.getPreviewPreferences().shouldShowPreviewAsExtraTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (previewPanel == null) {
            previewPanel = new PreviewPanel(databaseContext, dialogService, preferences.getKeyBindingRepository(), preferences, stateManager, themeManager, indexingTaskManager);
            setContent(previewPanel);
        }

        previewPanel.setEntry(entry);
    }
}
