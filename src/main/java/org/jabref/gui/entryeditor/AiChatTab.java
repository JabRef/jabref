package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.control.*;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatComponent;
import org.jabref.gui.ai.components.errorstate.ErrorStateComponent;
import org.jabref.gui.ai.components.privacynotice.PrivacyNoticeComponent;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.chat.AiChatLogic;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.logic.ai.chathistory.ChatMessage;
import org.jabref.logic.ai.embeddings.EmbeddingsGenerationTaskManager;
import org.jabref.logic.ai.embeddings.events.FileIngestedEvent;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.google.common.eventbus.Subscribe;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class.getName());

    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final EmbeddingsGenerationTaskManager embeddingsGenerationTaskManager;
    private final TaskExecutor taskExecutor;

    private AiChatComponent aiChatComponent = null;

    private final AiService aiService;

    private AiChatLogic aiChatLogic = null;

    private BibEntry currentBibEntry = null;

    private final @Nullable BibDatabaseChatHistory bibDatabaseChatHistory;

    public AiChatTab(DialogService dialogService, PreferencesService preferencesService, AiService aiService,
                     BibDatabaseContext bibDatabaseContext, EmbeddingsGenerationTaskManager embeddingsGenerationTaskManager, TaskExecutor taskExecutor) {
        this.dialogService = dialogService;
        this.filePreferences = preferencesService.getFilePreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();
        this.citationKeyPatternPreferences = preferencesService.getCitationKeyPatternPreferences();

        this.aiService = aiService;

        this.bibDatabaseContext = bibDatabaseContext;

        if (bibDatabaseContext.getDatabasePath().isPresent()) {
            this.bibDatabaseChatHistory = aiService.getChatHistoryManager().getChatHistoryForBibDatabase(bibDatabaseContext.getDatabasePath().get());
        } else {
            this.bibDatabaseChatHistory = null;
        }

        this.embeddingsGenerationTaskManager = embeddingsGenerationTaskManager;

        this.taskExecutor = taskExecutor;

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));

        aiService.getEmbeddingsManager().getIngestedFilesTracker().registerListener(new FileIngestedListener());
    }

    private class FileIngestedListener {
        @Subscribe
        public void listen(FileIngestedEvent event) {
            if (currentBibEntry != null) {
                if (aiService.getEmbeddingsManager().getIngestedFilesTracker().haveIngestedLinkedFiles(currentBibEntry.getFiles())) {
                    DefaultTaskExecutor.runInJavaFXThread(() -> bindToEntry(currentBibEntry));
                }
            }
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        currentBibEntry = entry;

        if (!aiService.getPreferences().getEnableChatWithFiles()) {
            showPrivacyNotice(entry);
        } else if (!checkIfCitationKeyIsAppropriate(bibDatabaseContext, entry)) {
            tryToGenerateCitationKey(entry);
        } else if (entry.getFiles().isEmpty()) {
            showErrorNoFiles();
        } else if (!entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).allMatch(FileUtil::isPDFFile)) {
            showErrorNotPdfs();
        } else if (!aiService.getEmbeddingsManager().getIngestedFilesTracker().haveIngestedLinkedFiles(currentBibEntry.getFiles())) {
            showErrorNotIngested();
        } else {
            bindToCorrectEntry(entry);
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

    private void tryToGenerateCitationKey(BibEntry entry) {
        CitationKeyGenerator citationKeyGenerator = new CitationKeyGenerator(bibDatabaseContext, citationKeyPatternPreferences);
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

    private static boolean checkIfCitationKeyIsAppropriate(BibDatabaseContext bibDatabaseContext, BibEntry bibEntry) {
        //noinspection OptionalGetWithoutIsPresent
        return !checkIfCitationKeyIsEmpty(bibEntry) && checkIfCitationKeyIsUnique(bibDatabaseContext, bibEntry.getCitationKey().get());
    }

    private static boolean checkIfCitationKeyIsEmpty(BibEntry bibEntry) {
        return bibEntry.getCitationKey().isEmpty() || bibEntry.getCitationKey().get().isEmpty();
    }

    private static boolean checkIfCitationKeyIsUnique(BibDatabaseContext bibDatabaseContext, String citationKey) {
        return bibDatabaseContext.getDatabase().getEntries().stream()
              .map(BibEntry::getCitationKey)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter(key -> key.equals(citationKey))
              .count() == 1;
    }

    private void bindToCorrectEntry(BibEntry entry) {
        embeddingsGenerationTaskManager.moveToFront(entry.getFiles());
        createAiChat();

        if (bibDatabaseChatHistory != null) {
            assert entry.getCitationKey().isPresent();
            aiChatLogic.restoreMessages(bibDatabaseChatHistory.getAllMessagesForEntry(entry.getCitationKey().get()));
        }

        buildChatUI(entry);
    }

    private void createAiChat() {
        aiChatLogic = new AiChatLogic(aiService, MetadataFilterBuilder.metadataKey("linkedFile").isIn(currentBibEntry.getFiles().stream().map(LinkedFile::getLink).toList()));
    }

    private void buildChatUI(BibEntry entry) {
        aiChatComponent = new AiChatComponent((userPrompt) -> {
            ChatMessage userMessage = ChatMessage.user(userPrompt);
            aiChatComponent.addMessage(userMessage);

            if (bibDatabaseChatHistory != null) {
                assert entry.getCitationKey().isPresent();
                bibDatabaseChatHistory.addMessage(entry.getCitationKey().get(), userMessage);
            }

            aiChatComponent.setLoading(true);

            BackgroundTask.wrap(() -> aiChatLogic.execute(userPrompt))
                    .onSuccess(aiMessageText -> {
                        aiChatComponent.setLoading(false);

                        ChatMessage aiMessage = ChatMessage.assistant(aiMessageText);
                        aiChatComponent.addMessage(aiMessage);

                        if (bibDatabaseChatHistory != null) {
                            bibDatabaseChatHistory.addMessage(entry.getCitationKey().get(), aiMessage);
                        }

                        aiChatComponent.requestUserPromptTextFieldFocus();
                    })
                    .onFailure(e -> {
                        // TODO: User-friendly error message.
                        LOGGER.error("Got an error while sending a message to AI", e);
                        aiChatComponent.setLoading(false);
                        aiChatComponent.addError(e.getMessage());
                    })
                    .executeWith(taskExecutor);
        });

        if (bibDatabaseChatHistory != null) {
            bibDatabaseChatHistory.getAllMessagesForEntry(entry.getCitationKey().get()).forEach(aiChatComponent::addMessage);
        }

        setContent(aiChatComponent);
    }
}
