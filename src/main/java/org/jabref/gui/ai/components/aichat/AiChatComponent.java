package org.jabref.gui.ai.components.aichat;

import java.util.stream.Stream;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.chathistory.ChatHistoryComponent;
import org.jabref.gui.ai.components.chatprompt.ChatPromptComponent;
import org.jabref.gui.ai.components.loadable.Loadable;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.ai.misc.ErrorMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
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

    @FXML private Loadable uiLoadableChatHistory;
    @FXML private ChatHistoryComponent uiChatHistory;
    @FXML private ChatPromptComponent chatPrompt;
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
        initializeChatHistory();
        initializeChatPrompt();
        initializeNotice();
    }

    private void initializeNotice() {
        String newNotice = noticeText
                .getText()
                .replaceAll("%0", aiPreferences.getAiProvider().getLabel() + " " + aiPreferences.getSelectedChatModel());

        noticeText.setText(newNotice);
    }

    private void initializeChatPrompt() {
        chatPrompt.setSendCallback(this::onSendMessage);

        chatPrompt.setCancelCallback(() -> chatPrompt.switchToNormalState());

        chatPrompt.setRetryCallback((userMessage) -> {
            deleteLastMessage();
            deleteLastMessage();
            chatPrompt.switchToNormalState();
            onSendMessage(userMessage);
        });

        chatPrompt.requestPromptFocus();
    }

    private void initializeChatHistory() {
        uiChatHistory.setDeleteMessageCallback(this::deleteMessage);

        aiChatLogic
                .getChatHistory()
                .getMessages()
                .forEach(uiChatHistory::addMessage);
    }

    private void deleteMessage(int index) {
        uiChatHistory.deleteMessage(index);
        aiChatLogic.getChatHistory().remove(index);
    }

    private void onSendMessage(String userPrompt) {
        UserMessage userMessage = new UserMessage(userPrompt);
        uiChatHistory.addMessage(userMessage);
        updatePromptHistory();
        setLoading(true);

        BackgroundTask<AiMessage> task =
                BackgroundTask
                        .wrap(() -> aiChatLogic.execute(userMessage))
                        .showToUser(true)
                        .onSuccess(aiMessage -> {
                            setLoading(false);
                            uiChatHistory.addMessage(aiMessage);
                            chatPrompt.requestPromptFocus();
                        })
                        .onFailure(e -> {
                            LOGGER.error("Got an error while sending a message to AI", e);
                            setLoading(false);

                            if ("401 - null".equals(e.getMessage()) || "404 - null".equals(e.getMessage())) {
                                addError(Localization.lang("API base URL setting appears to be incorrect. Please check it in AI expert settings."));
                            } else {
                                addError(e.getMessage());
                            }

                            chatPrompt.switchToErrorState(userPrompt);
                        });

        task.titleProperty().set(Localization.lang("Waiting for AI reply for %0...", citationKey));

        task.executeWith(taskExecutor);
    }

    private void addError(String error) {
        ErrorMessage chatMessage = new ErrorMessage(error);
        uiChatHistory.addMessage(chatMessage);
        aiChatLogic.getChatHistory().add(chatMessage);
    }

    private void updatePromptHistory() {
        chatPrompt.getHistory().clear();
        chatPrompt.getHistory().addAll(getReversedUserMessagesStream().map(UserMessage::singleText).toList());
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

    private void setLoading(boolean loading) {
        uiLoadableChatHistory.setLoading(loading);
        chatPrompt.setDisableToButtons(loading);
    }

    @FXML
    private void onClearChatHistory() {
        boolean agreed = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Clear chat history"),
                Localization.lang("Are you sure you want to clear the chat history of this entry?")
        );

        if (agreed) {
            uiChatHistory.clearAll();
            aiChatLogic.getChatHistory().clear();
        }
    }

    private void deleteLastMessage() {
        if (!aiChatLogic.getChatHistory().getMessages().isEmpty()) {
            int index = aiChatLogic.getChatHistory().getMessages().size() - 1;
            aiChatLogic.getChatHistory().remove(index);
            uiChatHistory.deleteMessage(index);
        }
    }
}
