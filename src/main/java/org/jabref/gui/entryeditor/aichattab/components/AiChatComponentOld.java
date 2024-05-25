package org.jabref.gui.entryeditor.aichattab.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.jabref.gui.entryeditor.aichattab.components.chatmessage.ChatMessageComponent;
import org.jabref.logic.ai.ChatMessage;
import org.jabref.logic.l10n.Localization;

import java.util.List;
import java.util.function.Consumer;

public class AiChatComponentOld {
    private final Consumer<String> sendMessageCallback;

    private final VBox aiChatBox = new VBox(10);
    private final VBox chatVBox = new VBox(10);
    private final TextField userPromptTextField = new TextField();

    public AiChatComponentOld(Consumer<String> sendMessageCallback) {
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

    public void addMessage(ChatMessage chatMessage) {
        chatVBox.getChildren().add(new ChatMessageComponent().withChatMessage(chatMessage));
    }

    private void internalSendMessageEvent() {
        String userPrompt = userPromptTextField.getText();
        userPromptTextField.clear();
        sendMessageCallback.accept(userPrompt);
    }

    public void restoreMessages(List<ChatMessage> messages) {
        messages.forEach(this::addMessage);
    }

    public Node getNode() {
        return aiChatBox;
    }
}
