package org.jabref.gui.ai.components.errormessage;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;

public class ErrorMessageComponent extends HBox {
    private final String message;

    @FXML private ExpandingTextArea contentTextArea;

    public ErrorMessageComponent(String message) {
        this.message = message;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        contentTextArea.setText(message);
    }
}
