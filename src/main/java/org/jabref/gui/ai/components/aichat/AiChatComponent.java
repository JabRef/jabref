package org.jabref.gui.ai.components.aichat;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.chatmessage.ChatMessageComponent;
import org.jabref.logic.ai.chathistory.ChatMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.WorkspacePreferences;

import java.util.function.Consumer;

public class AiChatComponent extends VBox {
    private final Consumer<String> sendMessageCallback;
    private final Runnable clearChatHistoryCallback;

    private final WorkspacePreferences workspacePreferences;
    private final DialogService dialogService;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatVBox;
    @FXML private TextField userPromptTextField;
    @FXML private Button submitButton;
    @FXML private StackPane stackPane;

    public AiChatComponent(Consumer<String> sendMessageCallback, Runnable clearChatHistoryCallback, WorkspacePreferences workspacePreferences, DialogService dialogService) {
        this.sendMessageCallback = sendMessageCallback;
        this.clearChatHistoryCallback = clearChatHistoryCallback;

        this.workspacePreferences = workspacePreferences;
        this.dialogService = dialogService;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @FXML
    public void initialize() {
        scrollPane.needsLayoutProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                scrollPane.setVvalue(1.0);
            }
        });

        Platform.runLater(() -> userPromptTextField.requestFocus());
    }

    @FXML
    private void internalSendMessageEvent() {
        String userPrompt = userPromptTextField.getText();

        if (!userPrompt.isEmpty()) {
            userPromptTextField.clear();
            sendMessageCallback.accept(userPrompt);
        }
    }

    public void setLoading(boolean loading) {
        userPromptTextField.setDisable(loading);
        submitButton.setDisable(loading);

        if (loading) {
            stackPane.getChildren().add(new BorderPane(new ProgressIndicator()));
        } else {
            stackPane.getChildren().clear();
            stackPane.getChildren().add(scrollPane);
        }
    }

    public void addMessage(ChatMessage chatMessage) {
        ChatMessageComponent component = new ChatMessageComponent().withChatMessage(chatMessage);
        chatVBox.getChildren().add(component);
    }

    public void addError(String message) {
        ChatMessageComponent component = new ChatMessageComponent().withError(message);
        chatVBox.getChildren().add(component);
    }

    public void requestUserPromptTextFieldFocus() {
        userPromptTextField.requestFocus();
    }

    @FXML
    private void onClearChatHistoryClick() {
        boolean result = dialogService.showConfirmationDialogAndWait(Localization.lang("Clear chat history"), Localization.lang("Are you sure you want to clear the chat history with this library entry?"));

        if (result) {
            chatVBox.getChildren().clear();
            clearChatHistoryCallback.run();
        }
    }
}
