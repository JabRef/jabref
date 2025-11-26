package org.jabref.gui.maintable;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
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
    private final VBox tooltipContent = new VBox();
    private final Label fieldValueLabel = new Label();

    public MainTableTooltip(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);

        // Configuration simple
        fieldValueLabel.setWrapText(true);
        fieldValueLabel.setMaxWidth(400);

        tooltipContent.getChildren().addAll(fieldValueLabel, preview);
        tooltipContent.setSpacing(5);
        tooltipContent.setStyle("-fx-padding: 8;");

        // Tooltip configuré pour le scroll
        this.setMaxWidth(400);
        this.setWrapText(true);
        this.setShowDelay(Duration.millis(100));
        this.setHideDelay(Duration.seconds(10));
        //this.setShowDuration(Duration.seconds(30));
        //this.setAutoHide(false);
        //this.setHideOnEscape(true);
        //this.setConsumeAutoHidingEvents(false);
    }

    public Tooltip createTooltip(BibDatabaseContext databaseContext, BibEntry entry, String fieldValue) {
        fieldValueLabel.setText(fieldValue);

        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
            preview.setDatabaseContext(databaseContext);
            preview.setEntry(entry);

            // Taille fixe simple et efficace
            preview.setMaxHeight(200); // Hauteur constante
            this.setGraphic(tooltipContent);
        } else {
            this.setGraphic(fieldValueLabel);
        }
        return this;
    }
}
