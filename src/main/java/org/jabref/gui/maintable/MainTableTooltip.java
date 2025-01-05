package org.jabref.gui.maintable;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

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
    private final VBox tooltipContent = new VBox(); // A vertical box to hold the tooltip content
    private final Label fieldValueLabel = new Label(); // Label to show field value

    public MainTableTooltip(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);

        // Reduce delay before showing the tooltip
        this.setShowDelay(Duration.millis(100)); // 100ms delay
        this.setHideDelay(Duration.millis(500)); // Slight delay before hiding

        // Add the field value label and preview area to the tooltip content
        this.tooltipContent.getChildren().addAll(fieldValueLabel, preview);

        // Add some basic styling to the tooltip
        tooltipContent.setStyle("-fx-padding: 10; -fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 5;");
    }

    public Tooltip createTooltip(BibDatabaseContext databaseContext, BibEntry entry, String fieldValue) {
        // Set the text for the field value label
        fieldValueLabel.setText(fieldValue + "\n");

        // Make sure the text wraps if it's too long
        fieldValueLabel.setWrapText(true);
        fieldValueLabel.setMaxWidth(400); // Set a limit for how wide the text can be

        // Automatically size the tooltip content based on what's inside
        tooltipContent.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tooltipContent.setMaxWidth(500); // Prevent the tooltip from being too wide
        tooltipContent.setMaxHeight(400); // Prevent the tooltip from being too tall

        // If the user has enabled preview in the preferences
        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            // Set up the preview
            preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
            preview.setDatabaseContext(databaseContext);
            preview.setEntry(entry);

            // Show both the field value and the preview
            this.setGraphic(tooltipContent);
        } else {
            // If preview is disabled, only show the field value
            this.setGraphic(fieldValueLabel);
        }

        return this; // Return the tooltip object
    }
}

