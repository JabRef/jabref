package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.Tooltip;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatComponent;
import org.jabref.gui.ai.components.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chat.AiChatLogic;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.logic.ai.chathistory.BibEntryChatHistory;
import org.jabref.logic.ai.chathistory.ChatMessage;
import org.jabref.logic.ai.embeddings.EmbeddingsGenerationTaskManager;
import org.jabref.logic.ai.embeddings.events.FileIngestedEvent;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.WorkspacePreferences;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class AiChatTab extends EntryEditorTab {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class.getName());

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final WorkspacePreferences workspacePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final EmbeddingsGenerationTaskManager embeddingsGenerationTaskManager;
    private final TaskExecutor taskExecutor;
    private final CitationKeyGenerator citationKeyGenerator;

    private AiChatComponent aiChatComponent = null;

    private final AiService aiService;

    private AiChatLogic aiChatLogic = null;

    private Optional<BibEntry> currentBibEntry = Optional.empty();

    private @Nullable BibEntryChatHistory bibEntryChatHistory;

    public AiChatTab(DialogService dialogService, PreferencesService preferencesService, AiService aiService,
                     BibDatabaseContext bibDatabaseContext, EmbeddingsGenerationTaskManager embeddingsGenerationTaskManager, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.workspacePreferences = preferencesService.getWorkspacePreferences();
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.aiService = aiService;
        this.bibDatabaseContext = bibDatabaseContext;
        this.embeddingsGenerationTaskManager = embeddingsGenerationTaskManager;
        this.taskExecutor = taskExecutor;
        this.citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, preferencesService.getCitationKeyPatternPreferences());
        setText(Localization.lang("AI chat"));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));
        aiService.getEmbeddingsManager().getIngestedFilesTracker().registerListener(new FileIngestedListener());
    }

    private class FileIngestedListener {
        @Subscribe
        public void listen(FileIngestedEvent event) {
            currentBibEntry.ifPresent(entry -> {
                if (aiService.getEmbeddingsManager().getIngestedFilesTracker().haveIngestedLinkedFiles(entry.getFiles())) {
                    UiTaskExecutor.runInJavaFXThread(() -> bindToEntry(entry));
                }
            });
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        currentBibEntry = Optional.of(entry);

        if (!aiService.getPreferences().getEnableChatWithFiles()) {
            showPrivacyNotice(entry);
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (!entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).anyMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (!citationKeyIsValid(bibDatabaseContext, entry)) {
            tryToGenerateCitationKeyThenBind(entry);
        } else if (!aiService.getEmbeddingsManager().getIngestedFilesTracker().haveIngestedLinkedFiles(entry.getFiles())) {
            showErrorNotIngested();
        } else {
            // All preconditions met
            embeddingsGenerationTaskManager.moveToFront(entry.getFiles());
            createAiChat();
            setupChatHistory();
            restoreLogicalChatHistory();
            buildChatUI();
        }
    }

    private void showErrorNotIngested() {
        setContent(ErrorStateComponent.withSpinner(Localization.lang("Please wait"), Localization.lang("The embeddings of the file are currently being generated. Please wait, and at the end you will be able to chat.")));
    }

    private void showErrorNotPdfs() {
        setContent(new ErrorStateComponent(Localization.lang("Unable to chat"), Localization.lang("Only PDF files are supported.")));
    }

    private void showErrorNoFiles() {
        setContent(new ErrorStateComponent(Localization.lang("Unable to chat"), Localization.lang("Please attach at least one PDF file to enable chatting with PDF files.")));
    }

    private void tryToGenerateCitationKeyThenBind(BibEntry entry) {
        if (citationKeyGenerator.generateAndSetKey(entry).isEmpty()) {
            setContent(new ErrorStateComponent(Localization.lang("Unable to chat"), Localization.lang("Please provide a non-empty and unique citation key for this entry.")));
        } else {
            bindToEntry(entry);
        }
    }

    private void showPrivacyNotice(BibEntry entry) {
        setContent(new PrivacyNoticeComponent(dialogService, aiService.getPreferences(), filePreferences, () -> {
            bindToEntry(entry);
        }));
    }

    private static boolean citationKeyIsValid(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        return !hasEmptyCitationKey(bibEntry) && bibEntry.getCitationKey().map(key -> citationKeyIsUnique(bibDatabaseContext, key)).orElse(false);
    }

    private static boolean hasEmptyCitationKey(BibEntry bibEntry) {
        return bibEntry.getCitationKey().map(key -> key.isEmpty()).orElse(true);
    }

    private static boolean citationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) == 1;
    }

    private void createAiChat() {
        aiChatLogic = new AiChatLogic(aiService, MetadataFilterBuilder.metadataKey("linkedFile").isIn(currentBibEntry.get().getFiles().stream().map(LinkedFile::getLink).toList()));
    }

    private void setupChatHistory() {
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();
        if (!databasePath.isPresent()) {
            bibEntryChatHistory = null;
        }
        currentBibEntry.flatMap(entry -> entry.getCitationKey())
                       .ifPresent(citationKey -> {
                           BibDatabaseChatHistory bibDatabaseChatHistory = aiService.getChatHistoryManager().getChatHistoryForBibDatabase(databasePath.get());
                           bibEntryChatHistory = bibDatabaseChatHistory.getChatHistoryForEntry(citationKey);
                       });
    }

    private void restoreLogicalChatHistory() {
        if (bibEntryChatHistory != null) {
            aiChatLogic.restoreMessages(bibEntryChatHistory.getAllMessages());
        }
    }

    private void buildChatUI() {
        aiChatComponent = new AiChatComponent(userPrompt -> {
            ChatMessage userMessage = ChatMessage.user(userPrompt);
            aiChatComponent.addMessage(userMessage);

            addMessageToChatHistory(userMessage);

            aiChatComponent.setLoading(true);

            BackgroundTask.wrap(() -> aiChatLogic.execute(userPrompt))
                    .onSuccess(aiMessageText -> {
                        aiChatComponent.setLoading(false);

                        ChatMessage aiMessage = ChatMessage.assistant(aiMessageText);
                        aiChatComponent.addMessage(aiMessage);

                        addMessageToChatHistory(aiMessage);

                        aiChatComponent.requestUserPromptTextFieldFocus();
                    })
                    .onFailure(e -> {
                        // TODO: User-friendly error message.
                        LOGGER.error("Got an error while sending a message to AI", e);
                        aiChatComponent.setLoading(false);
                        aiChatComponent.addError(e.getMessage());
                    })
                    .executeWith(taskExecutor);
        }, this::clearMessagesFromChatHistory, workspacePreferences, dialogService);

        restoreUIChatHistory();

        setContent(aiChatComponent);
    }

    private void clearMessagesFromChatHistory() {
        if (bibEntryChatHistory != null) {
            bibEntryChatHistory.clearMessages();
        }
    }

    private void restoreUIChatHistory() {
        if (bibEntryChatHistory != null) {
            bibEntryChatHistory.getAllMessages().forEach(aiChatComponent::addMessage);
        }
    }

    private void addMessageToChatHistory(ChatMessage userMessage) {
        if (bibEntryChatHistory != null) {
            bibEntryChatHistory.addMessage(userMessage);
        }
    }
}
