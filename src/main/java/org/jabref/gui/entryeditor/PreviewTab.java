package org.jabref.gui.entryeditor;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

public class PreviewTab extends EntryEditorTab implements OffersPreview {
    public static final String NAME = "Preview";
    private final DialogService dialogService;
    private final BibDatabaseContext databaseContext;
    private final GuiPreferences preferences;
    private final ThemeManager themeManager;
    private final TaskExecutor taskExecutor;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    private PreviewPanel previewPanel;

    public PreviewTab(BibDatabaseContext databaseContext,
                      DialogService dialogService,
                      GuiPreferences preferences,
                      ThemeManager themeManager,
                      TaskExecutor taskExecutor,
                      OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.themeManager = themeManager;
        this.taskExecutor = taskExecutor;
        this.searchQueryProperty = searchQueryProperty;

        setGraphic(IconTheme.JabRefIcons.TOGGLE_ENTRY_PREVIEW.getGraphicNode());
        setText(Localization.lang("Preview"));
    }

    @Override
    public void nextPreviewStyle() {
        if (previewPanel != null) {
            previewPanel.nextPreviewStyle();
        }
    }

    @Override
    public void previousPreviewStyle() {
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
            previewPanel = new PreviewPanel(databaseContext, dialogService, preferences.getKeyBindingRepository(), preferences, themeManager, taskExecutor, searchQueryProperty);
            setContent(previewPanel);
        }

        previewPanel.setEntry(entry);
    }
}
