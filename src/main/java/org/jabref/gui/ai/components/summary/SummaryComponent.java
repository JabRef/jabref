package org.jabref.gui.ai.components.summary;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

import com.airhacks.afterburner.views.ViewLoader;

public class SummaryComponent extends VBox {
    @FXML private TextArea summaryTextArea;
    @FXML private Button regenerateButton;

    private final String content;
    private final Runnable regenerateCallback;

    public SummaryComponent(String content, Runnable regenerateCallback) {
        this.content = content;
        this.regenerateCallback = regenerateCallback;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        summaryTextArea.setText(content);
    }

    @FXML
    private void onRegenerateButtonClick() {
        regenerateCallback.run();
    }
}
