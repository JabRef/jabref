package org.jabref.gui.ai.components.aichat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.chathistory.ChatHistoryComponent;
import org.jabref.gui.ai.components.aichat.chatprompt.ChatPromptComponent;
import org.jabref.gui.ai.components.util.Loadable;
import org.jabref.gui.ai.components.util.notifications.Notification;
import org.jabref.gui.ai.components.util.notifications.NotificationType;
import org.jabref.gui.ai.components.util.notifications.NotificationsComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.chatting.AiChatLogic;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.util.ErrorMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.ListUtil;

import com.airhacks.afterburner.views.ViewLoader;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.controlsfx.control.PopOver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatComponent.class);

    private final AiService aiService;
    private final ObservableList<BibEntry> entries;
    private final BibDatabaseContext bibDatabaseContext;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    private final AiChatLogic aiChatLogic;

    @FXML private Loadable uiLoadableChatHistory;
    @FXML private ChatHistoryComponent uiChatHistory;
    @FXML private Button notificationsButton;
    @FXML private ChatPromptComponent chatPrompt;
    @FXML private Label noticeText;

    public AiChatComponent(AiService aiService,
                           StringProperty name,
                           ObservableList<ChatMessage> chatHistory,
                           ObservableList<BibEntry> entries,
                           BibDatabaseContext bibDatabaseContext,
                           DialogService dialogService,
                           TaskExecutor taskExecutor
    ) {
        this.aiService = aiService;
        this.entries = entries;
        this.bibDatabaseContext = bibDatabaseContext;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.aiChatLogic = new AiChatLogic(aiService, name, chatHistory, entries);

        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @FXML
    public void initialize() {
        initializeChatHistory();
        initializeChatPrompt();
        initializeNotice();

        ListUtil.getLinkedFiles(entries).forEach(file ->
                aiService.getIngestionService().ingest(file, bibDatabaseContext).state().addListener((obs) -> updateNotifications()));

        updateNotifications();
    }

    private void initializeNotice() {
        String newNotice = noticeText
                .getText()
                .replaceAll("%0", aiService.getPreferences().getAiProvider().getLabel() + " " + aiService.getPreferences().getSelectedChatModel());

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
                .forEach(uiChatHistory::addMessage);
    }

    private void updateNotifications() {
        List<Notification> notifications = entries.stream().map(this::updateNotificationsForEntry).flatMap(List::stream).toList();

        if (notifications.isEmpty()) {
            notificationsButton.setManaged(false);
        } else {
            notificationsButton.setManaged(true);
            notificationsButton.setGraphic(NotificationsComponent.findSuitableIcon(notifications).getGraphicNode());
            notificationsButton.setOnAction(event ->
                new PopOver(new NotificationsComponent(notifications))
                        .show(notificationsButton)
            );
        }
    }

    private List<Notification> updateNotificationsForEntry(BibEntry entry) {
        List<Notification> notifications = new ArrayList<>();

        if (entry.getCitationKey().isEmpty()) {
            notifications.add(new Notification(NotificationType.ERROR, Localization.lang("No citation key"), Localization.lang("The chat history will not be stored in next sessions")));
        } else if (!aiService.getChatHistoryService().citationKeyIsValid(entry)) {
            notifications.add(new Notification(NotificationType.ERROR, Localization.lang("Invalid citation key"), Localization.lang("The chat history will not be stored in next sessions")));
        }

        if (entry.getFiles().isEmpty()) {
            notifications.add(new Notification(NotificationType.ERROR, Localization.lang("Unable to chat"), Localization.lang("No files attached")));
        }

        entry.getFiles().forEach(file -> {
            if (!FileUtil.isPDFFile(Path.of(file.getLink()))) {
                notifications.add(new Notification(
                        NotificationType.WARNING,
                        Localization.lang("File %0 is not a PDF file", file.getLink()),
                        Localization.lang("Only PDF files can be used for chatting")
                ));
            }
        });

        entry.getFiles().stream().map(file -> aiService.getIngestionService().ingest(file, bibDatabaseContext)).forEach(ingestionStatus -> {
            switch (ingestionStatus.state().get()) {
                case PROCESSING -> notifications.add(new Notification(
                    NotificationType.WARNING,
                    Localization.lang("File %0 is currently being processed", ingestionStatus.object().getLink()),
                    Localization.lang("After the file will be ingested, you will be able to chat with it")
                ));

                case ERROR -> notifications.add(new Notification(
                    NotificationType.ERROR,
                    Localization.lang("File %0 could not be ingested", ingestionStatus.object().getLink()),
                    ingestionStatus.exception().get().getLocalizedMessage()
                ));

                case SUCCESS -> { }
            }
        });

        return notifications;
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

        task.titleProperty().set(Localization.lang("Waiting for AI reply..."));

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
        if (!aiChatLogic.getChatHistory().isEmpty()) {
            int index = aiChatLogic.getChatHistory().size() - 1;
            aiChatLogic.getChatHistory().remove(index);
            uiChatHistory.deleteMessage(index);
        }
    }
}
