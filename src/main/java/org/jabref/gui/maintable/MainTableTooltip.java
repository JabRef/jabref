package org.jabref.gui.maintable;

import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class MainTableTooltip extends Tooltip {

    private final PreviewViewer preview;
    private final PreferencesService preferences;

    public MainTableTooltip(BibDatabaseContext databaseContext, DialogService dialogService, PreferencesService preferences,
                            StateManager stateManager, ThemeManager themeManager, TaskExecutor taskExecutor) {

        this.preferences = preferences;
        this.preview = new PreviewViewer(databaseContext, dialogService, preferences, stateManager, themeManager, taskExecutor);
    }

    public Tooltip createTooltip(BibEntry entry, String fieldValue) {
        preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
        preview.setEntry(entry);
        this.setShowDelay(Duration.seconds(1));
        Label label = new Label(fieldValue);

        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            VBox vBox = new VBox();
            label.setText(label.getText() + "\n ");
            vBox.getChildren().add(label);
            vBox.getChildren().add(preview);
           this.setGraphic(vBox);
        } else {
            this.setGraphic(label);
        }
        return this;
    }
}
