package org.jabref.gui.maintable;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class MainTableTooltip extends Tooltip {

    private final PreviewViewer preview;
    private final GuiPreferences preferences;
    private final VBox tooltipContent = new VBox();
    private final Label fieldValueLabel = new Label();

    public MainTableTooltip(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);

        // Ensure tooltip does not exceed a reasonable size
        tooltipContent.setMaxWidth(Double.MAX_VALUE);
        tooltipContent.setMaxHeight(Double.MAX_VALUE);
        tooltipContent.setPrefWidth(300);
        tooltipContent.setPrefHeight(200);

        // Enable text wrapping to prevent overflow
        fieldValueLabel.setWrapText(true);
        fieldValueLabel.setMaxWidth(300);

        this.getStyleClass().add("dynamic-tooltip");

        tooltipContent.getChildren().addAll(fieldValueLabel, preview);
    }

    public Tooltip createTooltip(BibDatabaseContext databaseContext, BibEntry entry, String fieldValue) {
        fieldValueLabel.setText(fieldValue + "\n");

        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
            preview.setDatabaseContext(databaseContext);
            preview.setEntry(entry);
            this.setGraphic(tooltipContent);
        } else {
            this.setGraphic(fieldValueLabel);
        }

        return this;
    }
}
