package org.jabref.gui.entryeditor.aichattab;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.V;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import oracle.jdbc.driver.Const;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.ai.AiChat;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class AiChatComponent {
    private final Consumer<String> sendMessageCallback;

    private final VBox aiChatBox = new VBox(10);
    private final VBox chatVBox = new VBox(10);
    private final TextField userPromptTextField = new TextField();

    public AiChatComponent(Consumer<String> sendMessageCallback) {
        this.sendMessageCallback = sendMessageCallback;

        buildUI();
    }

    private void buildUI() {
        aiChatBox.setPadding(new Insets(10));

        aiChatBox.getChildren().add(constructChatScrollPane());
        aiChatBox.getChildren().add(constructUserPromptBox());
    }

    private Node constructChatScrollPane() {
        ScrollPane chatScrollPane = new ScrollPane();
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-border-color: black;");
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        chatVBox.setPadding(new Insets(10));

        chatScrollPane.setContent(chatVBox);

        chatScrollPane.vvalueProperty().bind(chatVBox.heightProperty());

        return chatScrollPane;
    }

    private Node constructUserPromptBox() {
        HBox userPromptHBox = new HBox(10);
        userPromptHBox.setAlignment(Pos.CENTER);

        HBox.setHgrow(userPromptTextField, Priority.ALWAYS);
        userPromptTextField.setOnAction(e -> internalSendMessageEvent());

        userPromptHBox.getChildren().add(userPromptTextField);

        Button userPromptSubmitButton = new Button(Localization.lang("Submit"));
        userPromptSubmitButton.setOnAction(e -> internalSendMessageEvent());

        userPromptHBox.getChildren().add(userPromptSubmitButton);

        return userPromptHBox;
    }

    public void addMessage(ChatMessageComponent chatMessageComponent) {
        chatVBox.getChildren().add(chatMessageComponent.getNode());
    }

    private void internalSendMessageEvent() {
        String userPrompt = userPromptTextField.getText();
        userPromptTextField.clear();
        sendMessageCallback.accept(userPrompt);
    }

    public void restoreMessages(List<ChatMessage> messages) {
        messages.forEach(message -> addMessage(new ChatMessageComponent(message)));
    }

    public Node getNode() {
        return aiChatBox;
    }
}
