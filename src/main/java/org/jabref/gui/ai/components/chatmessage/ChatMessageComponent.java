package org.jabref.gui.ai.components.chatmessage;

import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.logic.ai.misc.ErrorMessage;
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
    private final Consumer<ChatMessageComponent> onDeleteCallback;

    @FXML private VBox vBox;
    @FXML private Label sourceLabel;
    @FXML private ExpandingTextArea contentTextArea;
    @FXML private HBox buttonsHBox;

    public ChatMessageComponent(ChatMessage chatMessage, Consumer<ChatMessageComponent> onDeleteCallback) {
        this.chatMessage = chatMessage;
        this.onDeleteCallback = onDeleteCallback;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public ChatMessage getChatMessage() {
        return chatMessage;
    }

    @FXML
    private void initialize() {
        switch (chatMessage) {
            case UserMessage userMessage -> {
                setColor("-jr-ai-message-user", "-jr-ai-message-user-border");
                setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
                sourceLabel.setText(Localization.lang("User"));
                contentTextArea.setText(userMessage.singleText());
            }

            case AiMessage aiMessage -> {
                setColor("-jr-ai-message-ai", "-jr-ai-message-ai-border");
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                sourceLabel.setText(Localization.lang("AI"));
                contentTextArea.setText(aiMessage.text());
            }

            case ErrorMessage errorMessage -> {
                setColor("-jr-ai-message-error", "-jr-ai-message-error-border");
                setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
                sourceLabel.setText(Localization.lang("Error"));
                contentTextArea.setText(errorMessage.getText());
            }

            default ->
                LOGGER.warn("ChatMessageComponent supports only user, AI, or error messages, but other type was passed: {}", chatMessage.type().name());
        }
    }

    @FXML
    private void onDeleteClick() {
        onDeleteCallback.accept(this);
    }

    private void setColor(String fillColor, String borderColor) {
        vBox.setStyle("-fx-background-color: " + fillColor + "; -fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: " + borderColor + "; -fx-border-width: 3;");
    }
}
