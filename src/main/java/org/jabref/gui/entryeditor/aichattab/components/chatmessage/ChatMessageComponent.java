package org.jabref.gui.entryeditor.aichattab.components.chatmessage;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.logic.ai.ChatMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ChatMessageComponent extends HBox {
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private TextArea contentTextArea;

    public ChatMessageComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        /*
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "ChatMessageComponent.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);

        }
         */
    }

    public ChatMessageComponent withChatMessage(ChatMessage chatMessage) {
        sourceLabel.setText(chatMessage.getTypeLabel());
        contentTextArea.setText(chatMessage.getContent());

        switch (chatMessage.getType()) {
            case USER:
                vBox.setStyle("-fx-background-color: -jr-ai-message-user;");
                setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                break;
            case ASSISTANT:
                vBox.setStyle("-fx-background-color: -jr-ai-message-ai;");
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                break;
        }

        return this;
    }

    public ChatMessageComponent withError(String message) {
        sourceLabel.setText(Localization.lang("Error"));
        contentTextArea.setText(message);
        vBox.setStyle("-fx-background-color: -jr-red;");
        return this;
    }

    public String getSourceLabel() {
        return sourceLabel.getText();
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel.setText(sourceLabel);
    }

    public String getContentTextArea() {
        return contentTextArea.getText();
    }

    public void setContentTextArea(String contentTextArea) {
        this.contentTextArea.setText(contentTextArea);
    }

    public void setColor(String color) {
        vBox.setStyle("-fx-background-color: " + color + ";");
    }
}
