package org.jabref.gui.maintable;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class MainTableTooltip extends Tooltip {

    private final PreviewViewer preview;
    private final GuiPreferences preferences;
    private final Label fieldValueLabel = new Label();

    private boolean resizeScheduled;

    public MainTableTooltip(DialogService dialogService, GuiPreferences preferences, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(dialogService, preferences, taskExecutor);

        // Keep the tooltip open while the cursor stays on the cell, so that long abstracts can be read
        setShowDuration(Duration.INDEFINITE);

        preview.resizeForTooltipContent();

        // Re-fit the tooltip once the rendered preview has its final size. Rendering touches the
        // bounds several times, so re-fits are coalesced: at most one sizeToScene per pulse.
        preview.getContent().layoutBoundsProperty().addListener((_, oldBounds, newBounds) -> {
            if (Math.abs(newBounds.getHeight() - oldBounds.getHeight()) < 1
                    && Math.abs(newBounds.getWidth() - oldBounds.getWidth()) < 1) {
                return;
            }
            if (resizeScheduled) {
                return;
            }
            resizeScheduled = true;
            Platform.runLater(() -> {
                resizeScheduled = false;
                sizeToScene();
            });
        });
    }

    public Tooltip createTooltip(BibDatabaseContext databaseContext, BibEntry entry, String fieldValue) {
        updateShowDuration();
        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
            preview.setDatabaseContext(databaseContext);
            preview.setEntry(entry);
            setGraphic(preview);
        } else {
            fieldValueLabel.setText(fieldValue);
            setGraphic(fieldValueLabel);
        }
        return this;
    }

    private void updateShowDuration() {
        int seconds = preferences.getPreviewPreferences().getTooltipDuration();
        setShowDuration(seconds == 0 ? Duration.INDEFINITE : Duration.seconds(seconds));
    }

    public boolean shouldAlwaysShowTooltip() {
        return preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip();
    }

    public Tooltip createPreviewTooltip(BibDatabaseContext databaseContext, BibEntry entry) {
        updateShowDuration();
        preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
        preview.setDatabaseContext(databaseContext);
        preview.setEntry(entry);
        setGraphic(preview);
        return this;
    }
}
