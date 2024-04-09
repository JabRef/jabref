package org.jabref.gui.maintable;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class MainTableTooltip {

    private final PreviewViewer preview;
    private final PreferencesService preferences;

    public MainTableTooltip(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferences,
                            StateManager stateManager, ThemeManager themeManager, TaskExecutor taskExecutor) {

        this.preferences = preferences;
        this.preview = new PreviewViewer(databaseContext, dialogService, preferences, stateManager, themeManager, taskExecutor);
    }

    public String createTooltip(BibEntry entry, String fieldValue) {
        preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
        preview.setEntry(entry);

        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            return new StringBuilder()
                    .append(fieldValue)
                    .append("\n\n")
                    .append(preview.getContent().getAccessibleText()).toString();
        } else {
            return fieldValue;
        }
    }
}
