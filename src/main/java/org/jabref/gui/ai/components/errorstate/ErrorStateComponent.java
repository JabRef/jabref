package org.jabref.gui.ai.components.errorstate;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import com.airhacks.afterburner.views.ViewLoader;

public class ErrorStateComponent extends BorderPane {
    @FXML private Text titleText;
    @FXML private Text contentText;

    public ErrorStateComponent(String title, String content) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        setTitle(title);
        setContent(content);
    }

    public static ErrorStateComponent withSpinner(String title, String content) {
        ErrorStateComponent errorStateComponent = new ErrorStateComponent(title, content);

        ((VBox)errorStateComponent.getCenter()).getChildren().add(new ProgressIndicator());

        return errorStateComponent;
    }

    public String getTitle() {
        return titleText.getText();
    }

    public void setTitle(String title) {
        titleText.setText(title);
    }

    public String getContent() {
        return contentText.getText();
    }

    public void setContent(String content) {
        contentText.setText(content);
    }
}
