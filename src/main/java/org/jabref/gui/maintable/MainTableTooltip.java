package org.jabref.gui.maintable;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class MainTableTooltip extends Tooltip {

    private final PreviewViewer preview;
    private final PreferencesService preferences;
    private final VBox tooltipContent = new VBox();
    private final Label fieldValueLabel = new Label();

    public MainTableTooltip(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(databaseContext, dialogService, preferences, themeManager, taskExecutor);
        this.setShowDelay(Duration.seconds(1));
        this.tooltipContent.getChildren().addAll(fieldValueLabel, preview);
    }

    public Tooltip createTooltip(BibEntry entry, String fieldValue) {
        fieldValueLabel.setText(fieldValue + "\n");
        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
            preview.setEntry(entry);
            this.setGraphic(tooltipContent);
        } else {
            this.setGraphic(fieldValueLabel);
        }
        return this;
    }
}
