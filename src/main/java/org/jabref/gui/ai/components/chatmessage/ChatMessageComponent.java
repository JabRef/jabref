package org.jabref.gui.ai.components.chatmessage;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.ai.chathistory.ChatMessage;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;

public class ChatMessageComponent extends HBox {
    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private ExpandingTextArea contentTextArea;

    public ChatMessageComponent() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public ChatMessageComponent withChatMessage(ChatMessage chatMessage) {
        sourceLabel.setText(chatMessage.getTypeLabel());
        contentTextArea.setText(chatMessage.content());

        switch (chatMessage.type()) {
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
        contentTextArea.setText(message);
        vBox.setStyle("-fx-background-color: -jr-red;");
        return this;
    }

    public void setColor(String color) {
        vBox.setStyle("-fx-background-color: " + color + ";");
    }
}
