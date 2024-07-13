package org.jabref.gui.ai.components.chatmessage;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMessageComponent extends HBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageComponent.class);

    private final ChatMessage chatMessage;

    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private ExpandingTextArea contentTextArea;

    public ChatMessageComponent(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        if (chatMessage instanceof UserMessage userMessage) {
            setColor("-jr-ai-message-user");
            setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
            sourceLabel.setText(Localization.lang("User"));
            contentTextArea.setText(userMessage.singleText());
        } else if (chatMessage instanceof AiMessage aiMessage) {
            setColor("-jr-ai-message-ai");
            setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
            sourceLabel.setText(Localization.lang("AI"));
            contentTextArea.setText(aiMessage.text());
        } else {
            LOGGER.warn("ChatMessageComponent supports only user or AI messages, but other type was passed: " + chatMessage.type().name());
        }
    }

    public ChatMessageComponent withError(String message) {
        sourceLabel.setText(Localization.lang("Error"));
        contentTextArea.setText(message);
        setColor("-jr-red");
        return this;
    }

    public void setColor(String color) {
        vBox.setStyle("-fx-background-color: " + color + ";");
    }
}
