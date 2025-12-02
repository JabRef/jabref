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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTableTooltip extends Tooltip {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableTooltip.class);
    private final PreviewViewer preview;
    private final GuiPreferences preferences;
    private final VBox tooltipContent = new VBox();
    private final Label fieldValueLabel = new Label();

    public MainTableTooltip(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);

        fieldValueLabel.setWrapText(true);
        fieldValueLabel.setMaxWidth(Double.MAX_VALUE);

        tooltipContent.getChildren().addAll(fieldValueLabel, preview);
        tooltipContent.setSpacing(5);

        this.setMaxWidth(500);
        this.setWrapText(true);

        final double previewWidthPadding = 16.0;
        final double PREVIEW_HEIGHT_PADDING = 8.0;  // Padding to avoid bottom clipping of the preview
        final double MIN_TOOLTIP_WIDTH = 200.0; // Minimum width of the tooltip to keep layout stable even with small content

        preview.contentHeightProperty().addListener((_, _, val) -> {
            double contentH = val == null ? 0 : val.doubleValue();
            if (contentH <= 0) {
                LOGGER.debug("contentHeightProperty emitted non-positive value: {}", contentH);
                return;
            }

            preview.setPrefHeight(contentH + PREVIEW_HEIGHT_PADDING);
        });

        preview.contentWidthProperty().addListener((_, _, val) -> {
            double contentW = val == null ? 0 : val.doubleValue();
            if (contentW <= 0) {
                LOGGER.debug("contentWidthProperty emitted non-positive value: {}", contentW);
                return;
            }

            double desired = Math.max(contentW + previewWidthPadding, MIN_TOOLTIP_WIDTH);

            // We set a very large max width so that JavaFX does not artificially clamp the tooltip.
            // The effective width is still limited by the window and screen bounds.
            this.setMaxWidth(Double.MAX_VALUE);
            this.setPrefWidth(desired);
        });
    }

    public Tooltip createTooltip(BibDatabaseContext databaseContext, BibEntry entry, String fieldValue) {
        fieldValueLabel.setText(fieldValue);

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
