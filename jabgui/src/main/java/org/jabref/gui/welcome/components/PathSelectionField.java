package org.jabref.gui.welcome.components;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.icon.IconTheme;

public class PathSelectionField extends HBox {
    private final TextField pathField;
    private final Button browseButton;

    public PathSelectionField(String promptText) {
        pathField = new TextField();
        pathField.setPromptText(promptText);
        HBox.setHgrow(pathField, Priority.ALWAYS);

        browseButton = new Button();
        browseButton.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        browseButton.getStyleClass().addAll("icon-button");

        setSpacing(4);
        getChildren().addAll(pathField, browseButton);
    }

    public void setOnBrowseAction(Runnable action) {
        browseButton.setOnAction(_ -> action.run());
    }

    public String getText() {
        return pathField.getText();
    }

    public void setText(String text) {
        pathField.setText(text);
    }

    public TextField getTextField() {
        return pathField;
    }
}
