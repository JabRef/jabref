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

        fieldValueLabel.setWrapText(true);
        
        fieldValueLabel.setMaxWidth(Double.MAX_VALUE);

        tooltipContent.getChildren().addAll(fieldValueLabel, preview);
        tooltipContent.setSpacing(5);

        this.setMaxWidth(500);
        this.setWrapText(true);

        final double previewWidthPadding = 16.0;

        preview.contentHeightProperty().addListener((obs, old, val) -> {
            double contentH = val == null ? 0 : val.doubleValue();
            if (contentH <= 0) {
                return;
            }
            
            preview.setPrefHeight(contentH + 8);
        });

        preview.contentWidthProperty().addListener((obs, old, val) -> {
            double contentW = val == null ? 0 : val.doubleValue();
            if (contentW <= 0) {
                return;
            }
            
            double desired = Math.max(contentW + previewWidthPadding, 200.0);
            
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
