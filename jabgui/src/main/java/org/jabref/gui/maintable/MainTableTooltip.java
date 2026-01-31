package org.jabref.gui.maintable;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.web.WebView;

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
    private final Label fieldValueLabel = new Label();
    private final WebView webView;

    public MainTableTooltip(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this.preferences = preferences;
        this.preview = new PreviewViewer(dialogService, preferences, themeManager, taskExecutor);

        this.webView = (WebView) preview.getContent();

        webView.setPrefWidth(600);
        webView.setMaxWidth(600);
        webView.setPrefHeight(10);
        webView.setMinHeight(10);

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(() -> {
                    Object result = webView.getEngine().executeScript(
                            "var content = document.getElementById('content');" +
                                    "content ? content.getBoundingClientRect().height : document.body.scrollHeight;"
                    );

                    if (result instanceof Number height) {
                        double actualH = height.doubleValue() + 15;

                        webView.setPrefHeight(actualH);
                        webView.setMaxHeight(actualH);
                        webView.setMinHeight(actualH);

                        sizeToScene();
                    }
                });
            }
        });
    }

    public Tooltip createTooltip(BibDatabaseContext databaseContext, BibEntry entry, String fieldValue) {
        if (preferences.getPreviewPreferences().shouldShowPreviewEntryTableTooltip()) {
            preview.setLayout(preferences.getPreviewPreferences().getSelectedPreviewLayout());
            preview.setDatabaseContext(databaseContext);
            preview.setEntry(entry);
            setGraphic(webView);
        } else {
            fieldValueLabel.setText(fieldValue);
            setGraphic(fieldValueLabel);
        }
        return this;
    }
}
