package org.jabref.gui.ai.components.aichat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.chatmessage.ChatMessageComponent;
import org.jabref.gui.ai.components.errormessage.ErrorMessageComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatComponent.class);

    private final AiChatLogic aiChatLogic;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatVBox;
    @FXML private ExpandingTextArea userPromptTextArea;
    @FXML private Button submitButton;
    @FXML private StackPane stackPane;

    public AiChatComponent(AiChatLogic aiChatLogic, DialogService dialogService, TaskExecutor taskExecutor) {
        this.aiChatLogic = aiChatLogic;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @FXML
    public void initialize() {
        userPromptTextArea.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ENTER) {
                if (keyEvent.isControlDown()) {
                    userPromptTextArea.appendText("\n");
                } else {
                    onSendMessage();
                }
            }
        });

        scrollPane.needsLayoutProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                scrollPane.setVvalue(1.0);
            }
        });

        chatVBox.getChildren().addAll(aiChatLogic.getChatHistory().getMessages().stream().map(ChatMessageComponent::new).toList());

        Platform.runLater(() -> userPromptTextArea.requestFocus());
    }

    @FXML
    private void onSendMessage() {
        String userPrompt = userPromptTextArea.getText();

        if (!userPrompt.isEmpty()) {
            userPromptTextArea.clear();

            UserMessage userMessage = new UserMessage(userPrompt);
            addMessage(userMessage);
            setLoading(true);

            BackgroundTask.wrap(() -> aiChatLogic.execute(userMessage))
                          .onSuccess(aiMessage -> {
                              setLoading(false);
                              addMessage(aiMessage);
                              requestUserPromptTextFieldFocus();
                          })
                          .onFailure(e -> {
                              LOGGER.error("Got an error while sending a message to AI", e);
                              setLoading(false);
                              addError(e.getMessage());
                          })
                          .executeWith(taskExecutor);
        }
    }

    private void setLoading(boolean loading) {
        userPromptTextArea.setDisable(loading);
        submitButton.setDisable(loading);

        if (loading) {
            stackPane.getChildren().add(new BorderPane(new ProgressIndicator()));
        } else {
            stackPane.getChildren().clear();
            stackPane.getChildren().add(scrollPane);
        }
    }

    private void addMessage(ChatMessage chatMessage) {
        ChatMessageComponent component = new ChatMessageComponent(chatMessage);
        chatVBox.getChildren().add(component);
    }

    private void addError(String message) {
        ErrorMessageComponent component = new ErrorMessageComponent(message);
        chatVBox.getChildren().add(component);
    }

    private void requestUserPromptTextFieldFocus() {
        userPromptTextArea.requestFocus();
    }

    @FXML
    private void onClearChatHistory() {
        boolean agreed = dialogService.showConfirmationDialogAndWait(Localization.lang("Clear chat history"), Localization.lang("Are you sure you want to clear the chat history with this entry?"));

        if (agreed) {
            chatVBox.getChildren().clear();
            aiChatLogic.getChatHistory().clear();
        }
    }
}
