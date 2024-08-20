package org.jabref.gui.ai.components.aichat;

import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.chatmessage.ChatMessageComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.ai.misc.ErrorMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.ExpandingTextArea;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatComponent.class);

    // If current message that user is typing in prompt is non-existent, new, or empty, then we use
    // this value in currentUserMessageScroll.
    private static final int NEW_NON_EXISTENT_MESSAGE = -1;

    private final AiPreferences aiPreferences;
    private final AiChatLogic aiChatLogic;
    private final String citationKey;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final IntegerProperty blockScroll = new SimpleIntegerProperty(0);

    // This property stores index of a user history message.
    // When user scrolls history in the prompt, this value is updated.
    // Whenever user edits the prompt, this value is reset to NEW_NON_EXISTENT_MESSAGE.
    private final IntegerProperty currentUserMessageScroll = new SimpleIntegerProperty(NEW_NON_EXISTENT_MESSAGE);

    // If the current content of the prompt is a history message, then this property is true.
    // If user begins to edit or type a new text, then this property is false.
    private final BooleanProperty showingHistoryMessage = new SimpleBooleanProperty(false);

    @FXML private ScrollPane scrollPane;
    @FXML private VBox chatVBox;
    @FXML private HBox promptHBox;
    @FXML private ExpandingTextArea userPromptTextArea;
    @FXML private Button submitButton;
    @FXML private StackPane stackPane;
    @FXML private Label noticeText;

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
            if (keyEvent.getCode() == KeyCode.DOWN) {
                // Do not go down in the history.
                if (currentUserMessageScroll.get() != NEW_NON_EXISTENT_MESSAGE) {
                    showingHistoryMessage.set(true);
                    currentUserMessageScroll.set(currentUserMessageScroll.get() - 1);

                    // There could be two effects after setting the properties:
                    // 1) User scrolls to a recent message, then we should properly update the prompt text.
                    // 2) Scroll is set to -1 (which is NEW_NON_EXISTENT_MESSAGE) and we should clear the prompt text.
                    // On the second event currentUserMessageScroll will be set to -1 and showingHistoryMessage
                    // will be true (this is important).
                }
            } else if (keyEvent.getCode() == KeyCode.UP) {
                if ((currentUserMessageScroll.get() < getReversedUserMessagesStream().count() - 1) && (userPromptTextArea.getText().isEmpty() || showingHistoryMessage.get())) {
                    // 1. We should not go up the maximum number of user messages.
                    // 2. We can scroll history only on two conditions:
                    //    1) The prompt is empty.
                    //    2) User has already been scrolling the history.
                    showingHistoryMessage.set(true);
                    currentUserMessageScroll.set(currentUserMessageScroll.get() + 1);
                }
            } else {
                // Cursor left/right should not stop history scrolling
                if (keyEvent.getCode() != KeyCode.RIGHT && keyEvent.getCode() != KeyCode.LEFT) {
                    // It is okay to go back and forth in the prompt while showing a history message.
                    // But if user begins doing something else, we should not track the history and reset
                    // all the properties.
                    showingHistoryMessage.set(false);
                    currentUserMessageScroll.set(NEW_NON_EXISTENT_MESSAGE);
                }

                if (keyEvent.getCode() == KeyCode.ENTER) {
                    if (keyEvent.isControlDown()) {
                        userPromptTextArea.appendText("\n");
                    } else {
                        onSendMessage();
                    }
                }
            }
        });

        currentUserMessageScroll.addListener((observable, oldValue, newValue) -> {
            // When currentUserMessageScroll is reset, then its value is
            // 1) either to NEW_NON_EXISTENT_MESSAGE,
            // 2) or to a new history entry.
            if (newValue.intValue() != NEW_NON_EXISTENT_MESSAGE && showingHistoryMessage.get()) {
                if (userPromptTextArea.getCaretPosition() == 0 || !userPromptTextArea.getText().contains("\n")) {
                    // If there are new lines in the prompt, then it is ambiguous whether the user tries to scroll up or down in history or editing lines in the current prompt.
                    // The easy way to get rid of this ambiguity is to disallow scrolling when there are new lines in the prompt.
                    // But the exception to this situation is when the caret position is at the beginning of the prompt.
                    getReversedUserMessagesStream()
                            .skip(newValue.intValue())
                            .findFirst()
                            .ifPresent(userMessage ->
                                    userPromptTextArea.setText(userMessage.singleText()));
                }
            } else {
                // When currentUserMessageScroll is set to NEW_NON_EXISTENT_MESSAGE, then we should:
                // 1) either clear the prompt, if user scrolls down the most recent history entry.
                // 2) do nothing, if user starts to edit the history entry.
                // We distinguish these two cases by checking showingHistoryMessage, which is true for -1 message, and false for others.
                if (showingHistoryMessage.get()) {
                    userPromptTextArea.setText("");
                }
            }
        });

        scrollPane.needsLayoutProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (blockScroll.get() == 0) {
                    scrollPane.setVvalue(1.0);
                } else {
                    blockScroll.set(blockScroll.get() - 1);
                }
            }
        });

        chatVBox
                .getChildren()
                .addAll(aiChatLogic.getChatHistory()
                                   .getMessages()
                                   .stream()
                                   .map(message -> new ChatMessageComponent(message, this::deleteMessage))
                                   .toList()
                );

        String newNotice = noticeText
                .getText()
                .replaceAll("%0", aiPreferences.getAiProvider().getLabel() + " " + aiPreferences.getSelectedChatModel());

        noticeText.setText(newNotice);

        Platform.runLater(() -> userPromptTextArea.requestFocus());
    }

    private Stream<UserMessage> getReversedUserMessagesStream() {
        return aiChatLogic
                .getChatHistory()
                .getMessages()
                .reversed()
                .stream()
                .filter(message -> message instanceof UserMessage)
                .map(UserMessage.class::cast);
    }

    @FXML
    private void onSendMessage() {
        String userPrompt = userPromptTextArea.getText().trim();

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

                                if ("401 - null".equals(e.getMessage()) || "404 - null".equals(e.getMessage())) {
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

            removeLastMessage();
            removeLastMessage();

            switchToNormalState();

            onSendMessage();
        });

        Button cancelButton = new Button(Localization.lang("Cancel"));

        cancelButton.setOnAction(event -> {
            switchToNormalState();
        });

        promptHBox.getChildren().add(retryButton);
        promptHBox.getChildren().add(cancelButton);
    }

    private void removeLastMessage() {
        if (!chatVBox.getChildren().isEmpty()) {
            ChatMessageComponent chatMessageComponent = (ChatMessageComponent) chatVBox.getChildren().getLast();
            deleteMessage(chatMessageComponent);
        }
    }

    private void switchToNormalState() {
        promptHBox.getChildren().clear();
        promptHBox.getChildren().add(userPromptTextArea);
        promptHBox.getChildren().add(submitButton);

        Platform.runLater(() -> userPromptTextArea.requestFocus());
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
        ChatMessageComponent component = new ChatMessageComponent(chatMessage, this::deleteMessage);
        // chatMessage will be added to chat history in {@link AiChatLogic}.
        chatVBox.getChildren().add(component);
    }

    private void addError(String message) {
        ErrorMessage errorMessage = new ErrorMessage(message);

        addMessage(errorMessage);
        aiChatLogic.getChatHistory().add(errorMessage);
    }

    private void deleteMessage(ChatMessageComponent chatMessageComponent) {
        blockScroll.set(2);

        int index = chatVBox.getChildren().indexOf(chatMessageComponent);
        chatVBox.getChildren().remove(chatMessageComponent);

        aiChatLogic.getChatHistory().remove(index);
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
