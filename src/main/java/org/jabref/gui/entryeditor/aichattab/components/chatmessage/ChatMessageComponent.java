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

import org.jabref.gui.entryeditor.aichattab.components.JabRefMarkdownView;
import org.jabref.logic.ai.ChatMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import one.jpro.platform.mdfx.MarkdownView;

public class ChatMessageComponent extends HBox {
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private JabRefMarkdownView contentMarkdownView;

    public ChatMessageComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public ChatMessageComponent withChatMessage(ChatMessage chatMessage) {
        sourceLabel.setText(chatMessage.getTypeLabel());
        contentMarkdownView.setMdString(chatMessage.getContent());

        switch (chatMessage.getType()) {
            case USER:
                setColor("-jr-ai-message-user");
                setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                break;
            case ASSISTANT:
                setColor("-jr-ai-message-ai");
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                break;
        }

        return this;
    }

    public ChatMessageComponent withError(String message) {
        sourceLabel.setText(Localization.lang("Error"));
        contentMarkdownView.setMdString(message);
        vBox.setStyle("-fx-background-color: -jr-red;");
        return this;
    }

    public String getSourceLabel() {
        return sourceLabel.getText();
    }

    public void setSourceLabel(String sourceLabel) {
        this.sourceLabel.setText(sourceLabel);
    }

    public String getContent() {
        return contentMarkdownView.getMdString();
    }

    public void setContent(String contentTextArea) {
        contentMarkdownView.setMdString(contentTextArea);
    }

    public void setColor(String color) {
        vBox.setStyle("-fx-background-color: " + color + ";");
    }
}
