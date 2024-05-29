package org.jabref.gui.entryeditor.aichattab.components.aichat;

import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.entryeditor.aichattab.components.chatmessage.ChatMessageComponent;
import org.jabref.logic.ai.ChatMessage;

import com.airhacks.afterburner.views.ViewLoader;

public class AiChatComponent extends VBox {
    private final Consumer<String> sendMessageCallback;

    @FXML private VBox chatVBox;
    @FXML private TextField userPromptTextField;
    @FXML private Button submitButton;

    public AiChatComponent(Consumer<String> sendMessageCallback) {
        this.sendMessageCallback = sendMessageCallback;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> userPromptTextField.requestFocus());
    }

    @FXML
    private void internalSendMessageEvent() {
        String userPrompt = userPromptTextField.getText();
        userPromptTextField.clear();
        sendMessageCallback.accept(userPrompt);
    }

    /**
     * This method pushes a progress indicator into the chat box on true, and pops the last element in chat box on false.
     * Be careful while using this method. If you called setLoading(true), then don't add any chat messages before you call setLoading(false).
     */
    public void setLoading(boolean loading) {
        userPromptTextField.setDisable(loading);
        submitButton.setDisable(loading);

        if (loading) {
            chatVBox.getChildren().add(makeProgressComponent());
        } else {
            if (!chatVBox.getChildren().isEmpty() && !(chatVBox.getChildren().getLast() instanceof ChatMessageComponent)) {
                chatVBox.getChildren().removeLast();
            }
        }
    }

    private Node makeProgressComponent() {
        return new BorderPane(new ProgressIndicator());
    }

    public void addMessage(ChatMessage chatMessage) {
        chatVBox.getChildren().add(new ChatMessageComponent().withChatMessage(chatMessage));
    }

    public void addError(String message) {
        chatVBox.getChildren().add(new ChatMessageComponent().withError(message));
    }
}
