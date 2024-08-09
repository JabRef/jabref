package org.jabref.gui.ai.components.aichat;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.chatmessage.ChatMessageComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.ai.misc.ErrorMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.AiPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatComponent.class);

    private final AiPreferences aiPreferences;
    private final AiChatLogic aiChatLogic;
    private final String citationKey;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatVBox;
    @FXML private HBox promptHBox;
    @FXML private ExpandingTextArea userPromptTextArea;
    @FXML private Button submitButton;
    @FXML private StackPane stackPane;
    @FXML private Text noticeText;

    public AiChatComponent(AiPreferences aiPreferences, AiChatLogic aiChatLogic, String citationKey, DialogService dialogService, TaskExecutor taskExecutor) {
        this.aiPreferences = aiPreferences;
        this.aiChatLogic = aiChatLogic;
        this.citationKey = citationKey;
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

        chatVBox
                .getChildren()
                .addAll(aiChatLogic.getChatHistory()
                                   .getMessages()
                                   .stream()
                                   .map(ChatMessageComponent::new)
                                   .toList()
                );

        String newNotice = noticeText
                .getText()
                .replaceAll("%0", aiPreferences.getAiProvider().getLabel() + " " + aiPreferences.getSelectedChatModel());

        noticeText.setText(newNotice);

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

            BackgroundTask<AiMessage> task =
                    BackgroundTask
                            .wrap(() -> aiChatLogic.execute(userMessage))
                            .showToUser(true)
                            .onSuccess(aiMessage -> {
                                setLoading(false);
                                addMessage(aiMessage);
                                requestUserPromptTextFieldFocus();
                            })
                            .onFailure(e -> {
                                LOGGER.error("Got an error while sending a message to AI", e);
                                setLoading(false);

                                if (e.getMessage().equals("401 - null") || e.getMessage().equals("404 - null")) {
                                    addError(Localization.lang("API base URL setting appears to be incorrect. Please check it in AI expert settings."));
                                } else {
                                    addError(e.getMessage());
                                }

                                switchToErrorState(userPrompt);
                            });

            task.titleProperty().set(Localization.lang("Waiting for AI reply for %0...", citationKey));

            task.executeWith(taskExecutor);
        }
    }

    private void switchToErrorState(String userMessage) {
        promptHBox.getChildren().clear();

        Button retryButton = new Button(Localization.lang("Retry"));

        retryButton.setOnAction(event -> {
            userPromptTextArea.setText(userMessage);

            chatVBox.getChildren().removeLast();
            chatVBox.getChildren().removeLast();

            switchToNormalState();

            onSendMessage();
        });

        Button cancelButton = new Button(Localization.lang("Cancel"));

        cancelButton.setOnAction(event -> {
            chatVBox.getChildren().removeLast();
            chatVBox.getChildren().removeLast();

            switchToNormalState();
        });

        promptHBox.getChildren().add(retryButton);
        promptHBox.getChildren().add(cancelButton);
    }

    private void switchToNormalState() {
        promptHBox.getChildren().clear();
        promptHBox.getChildren().add(userPromptTextArea);
        promptHBox.getChildren().add(submitButton);
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
        aiChatLogic.getChatHistory().add(new ErrorMessage(message));
        chatVBox.getChildren().add(new ChatMessageComponent(new ErrorMessage(message)));
    }

    private void requestUserPromptTextFieldFocus() {
        userPromptTextArea.requestFocus();
    }

    @FXML
    private void onClearChatHistory() {
        boolean agreed = dialogService.showConfirmationDialogAndWait(Localization.lang("Clear chat history"), Localization.lang("Are you sure you want to clear the chat history of this entry?"));

        if (agreed) {
            chatVBox.getChildren().clear();
            aiChatLogic.getChatHistory().clear();
        }
    }
}
