package org.jabref.gui.ai.components.util.errorstate;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import com.airhacks.afterburner.views.ViewLoader;

public class ErrorStateComponent extends BorderPane {
    @FXML private Label titleText;
    @FXML private Label contentText;
    @FXML private VBox contentsVBox;

    public ErrorStateComponent(String title, String content) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        setTitle(title);
        setContent(content);
    }

    public static ErrorStateComponent withSpinner(String title, String content) {
        ErrorStateComponent errorStateComponent = new ErrorStateComponent(title, content);

        errorStateComponent.contentsVBox.getChildren().add(new ProgressIndicator());

        return errorStateComponent;
    }

    public static ErrorStateComponent withTextArea(String title, String content, String textAreaContent) {
        ErrorStateComponent errorStateComponent = new ErrorStateComponent(title, content);

        TextArea textArea = new TextArea(textAreaContent);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        errorStateComponent.contentsVBox.getChildren().add(textArea);

        return errorStateComponent;
    }

    public static ErrorStateComponent withTextAreaAndButton(String title, String content, String textAreaContent, String buttonText, Runnable onClick) {
        ErrorStateComponent errorStateComponent = ErrorStateComponent.withTextArea(title, content, textAreaContent);

        Button button = new Button(buttonText);
        button.setOnAction(e -> onClick.run());

        errorStateComponent.contentsVBox.getChildren().add(button);

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
