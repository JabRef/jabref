package org.jabref.gui.entryeditor;

import java.util.Objects;

import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

import org.jabref.model.entry.identifier.MathSciNetId;

public class MathSciNetPaneView {

    private MathSciNetId mathSciNetId;

    public MathSciNetPaneView(MathSciNetId mathSciNetId) {
        this.mathSciNetId = Objects.requireNonNull(mathSciNetId);
    }

    StackPane getPane() {
        StackPane root = new StackPane();
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(100, 100);
        WebView browser = new WebView();

        // Quick hack to disable navigating
        browser.addEventFilter(javafx.scene.input.MouseEvent.ANY, javafx.scene.input.MouseEvent::consume);
        browser.setContextMenuEnabled(false);

        root.getChildren().addAll(browser, progress);

        browser.getEngine().load(mathSciNetId.getItemUrl());

        // Hide progress indicator if finished (over 70% loaded)
        browser.getEngine().getLoadWorker().progressProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() >= 0.7) {
                progress.setVisible(false);
            }
        });
        return root;
    }
}
