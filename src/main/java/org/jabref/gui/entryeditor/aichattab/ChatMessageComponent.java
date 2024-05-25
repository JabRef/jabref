package org.jabref.gui.entryeditor.aichattab;

import dev.langchain4j.data.message.*;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.jabref.logic.l10n.Localization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMessageComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageComponent.class);

    private final Pane pane;
    private final VBox paneVBox;
    private final Label authorLabel;
    private final Pane contentPane;

    public ChatMessageComponent() {
        pane = new Pane();

        paneVBox = new VBox(10);
        paneVBox.setMaxWidth(500);

        paneVBox.setPadding(new Insets(10));
        paneVBox.setStyle("-fx-background-color: -jr-ai-message-ai");

        authorLabel = new Label(Localization.lang("AI"));
        authorLabel.setStyle("-fx-font-weight: bold");
        paneVBox.getChildren().add(authorLabel);

        contentPane = new Pane();
        contentPane.getChildren().add(new ProgressIndicator());
        paneVBox.getChildren().add(contentPane);

        pane.getChildren().add(paneVBox);
    }

    public ChatMessageComponent(ChatMessage chatMessage) {
        this();
        setMessage(chatMessage);
    }

    public void setMessage(ChatMessage chatMessage) {
        if (!(chatMessage instanceof UserMessage) && !(chatMessage instanceof AiMessage)) {
            return;
        }

        boolean isUser = chatMessage instanceof UserMessage;

        if (isUser) {
            pane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        }

        paneVBox.setStyle("-fx-background-color: " + (isUser ? "-jr-ai-message-user" : "-jr-ai-message-ai") + ";");

        authorLabel.setText(Localization.lang(isUser ? "User" : "AI"));

        contentPane.getChildren().clear();
        contentPane.setStyle("");

        contentPane.getChildren().add(makeMessageTextArea(getChatMessageText(chatMessage)));
    }

    public void setError(String message) {
        contentPane.getChildren().clear();
        contentPane.setStyle("");
        contentPane.setStyle("-fx-background-color: -jr-red");

        VBox paneVBox = new VBox(10);
        paneVBox.setMaxWidth(500);
        paneVBox.setPadding(new Insets(10));

        Label errorLabel = new Label(Localization.lang("Error"));
        errorLabel.setStyle("-fx-font-weight: bold");
        paneVBox.getChildren().add(errorLabel);

        TextArea messageTextArea = makeMessageTextArea(message);
        paneVBox.getChildren().add(messageTextArea);

        contentPane.getChildren().add(paneVBox);
    }

    private static TextArea makeMessageTextArea(String content) {
        TextArea message = new TextArea(content);
        message.setWrapText(true);
        message.setEditable(false);
        return message;
    }

    // Safely gets text from a chat message.
    private static String getChatMessageText(ChatMessage chatMessage) {
        // This mangling is needed because ChatMessage.text() is deprecated.

        switch (chatMessage) {
            case AiMessage aiChatMessage -> {
                return aiChatMessage.text();
            }

            case UserMessage userChatMessage -> {
                if (userChatMessage.hasSingleText()) {
                    return userChatMessage.singleText();
                } else {
                    return "";
                }
            }

            case SystemMessage systemChatMessage -> {
                return systemChatMessage.text();
            }

            case ToolExecutionResultMessage toolChatMessage -> {
                return toolChatMessage.text();
            }

            case null, default -> {
                LOGGER.error("Unimplemented getChatMessageText for a new kind of ChatMessage");
                return "";
            }
        }
    }

    public Node getNode() {
        return pane;
    }
}
