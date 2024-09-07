package org.jabref.logic.ai;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.ai.components.aichat.AiChatWindow;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.chatting.AiChatService;
import org.jabref.logic.ai.chatting.chathistory.ChatHistoryService;
import org.jabref.logic.ai.chatting.chathistory.storages.MVStoreChatHistoryStorage;
import org.jabref.logic.ai.chatting.model.JabRefChatLanguageModel;
import org.jabref.logic.ai.ingestion.IngestionService;
import org.jabref.logic.ai.ingestion.MVStoreEmbeddingStore;
import org.jabref.logic.ai.ingestion.model.JabRefEmbeddingModel;
import org.jabref.logic.ai.ingestion.storages.MVStoreFullyIngestedDocumentsTracker;
import org.jabref.logic.ai.summarization.SummariesService;
import org.jabref.logic.ai.summarization.storages.MVStoreSummariesStorage;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.ai.AiApiKeyProvider;
import org.jabref.preferences.ai.AiPreferences;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.message.ChatMessage;

/**
 *  The main class for the AI functionality.
 *  <p>
 *  Holds all the AI components: LLM and embedding model, chat history and embeddings cache.
 */
public class AiService implements AutoCloseable {
    public static final String VERSION = "1";

    private static final String CHAT_HISTORY_FILE_NAME = "chat-histories.mv";
    private static final String EMBEDDINGS_FILE_NAME = "embeddings.mv";
    private static final String FULLY_INGESTED_FILE_NAME = "fully-ingested.mv";
    private static final String SUMMARIES_FILE_NAME = "summaries.mv";

    private final StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    private final AiPreferences aiPreferences;
    private final FilePreferences filePreferences;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;

    // This field is used to shut down AI-related background tasks.
    // If a background task processes a big document and has a loop, then the task should check the status
    // of this property for being true. If it's true, then it should abort the cycle.
    private final BooleanProperty shutdownSignal = new SimpleBooleanProperty(false);

    private final ExecutorService cachedThreadPool = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("ai-retrieval-pool-%d").build()
    );

    private final MVStoreEmbeddingStore mvStoreEmbeddingStore;
    private final MVStoreFullyIngestedDocumentsTracker mvStoreFullyIngestedDocumentsTracker;
    private final MVStoreChatHistoryStorage mvStoreChatHistoryStorage;
    private final MVStoreSummariesStorage mvStoreSummariesStorage;

    private final JabRefChatLanguageModel jabRefChatLanguageModel;
    private final ChatHistoryService chatHistoryService;
    private final JabRefEmbeddingModel jabRefEmbeddingModel;
    private final AiChatService aiChatService;
    private final IngestionService ingestionService;
    private final SummariesService summariesService;

    public AiService(AiPreferences aiPreferences,
                     FilePreferences filePreferences,
                     CitationKeyPatternPreferences citationKeyPatternPreferences,
                     AiApiKeyProvider aiApiKeyProvider,
                     DialogService dialogService,
                     TaskExecutor taskExecutor
    ) {
        this.aiPreferences = aiPreferences;
        this.filePreferences = filePreferences;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;

        this.jabRefChatLanguageModel = new JabRefChatLanguageModel(aiPreferences, aiApiKeyProvider);

        this.mvStoreEmbeddingStore = new MVStoreEmbeddingStore(JabRefDesktop.getAiFilesDirectory().resolve(EMBEDDINGS_FILE_NAME), dialogService);
        this.mvStoreFullyIngestedDocumentsTracker = new MVStoreFullyIngestedDocumentsTracker(JabRefDesktop.getAiFilesDirectory().resolve(FULLY_INGESTED_FILE_NAME), dialogService);
        this.mvStoreSummariesStorage = new MVStoreSummariesStorage(JabRefDesktop.getAiFilesDirectory().resolve(SUMMARIES_FILE_NAME), dialogService);
        this.mvStoreChatHistoryStorage = new MVStoreChatHistoryStorage(JabRefDesktop.getAiFilesDirectory().resolve(CHAT_HISTORY_FILE_NAME), dialogService);

        this.chatHistoryService = new ChatHistoryService(citationKeyPatternPreferences, mvStoreChatHistoryStorage);
        this.jabRefEmbeddingModel = new JabRefEmbeddingModel(aiPreferences, dialogService, taskExecutor);
        this.aiChatService = new AiChatService(aiPreferences, jabRefChatLanguageModel, jabRefEmbeddingModel, mvStoreEmbeddingStore, cachedThreadPool);
        this.ingestionService = new IngestionService(
                aiPreferences,
                shutdownSignal,
                jabRefEmbeddingModel,
                mvStoreEmbeddingStore,
                mvStoreFullyIngestedDocumentsTracker,
                filePreferences,
                taskExecutor
        );
        this.summariesService = new SummariesService(aiPreferences, mvStoreSummariesStorage, jabRefChatLanguageModel, shutdownSignal, filePreferences, taskExecutor);
    }

    public JabRefChatLanguageModel getChatLanguageModel() {
        return jabRefChatLanguageModel;
    }

    public JabRefEmbeddingModel getEmbeddingModel() {
        return jabRefEmbeddingModel;
    }

    public AiChatService getAiChatService() {
        return aiChatService;
    }

    public ChatHistoryService getChatHistoryService() {
        return chatHistoryService;
    }

    public IngestionService getIngestionService() {
        return ingestionService;
    }

    public SummariesService getSummariesService() {
        return summariesService;
    }

    public void openAiChat(StringProperty name, ObservableList<ChatMessage> chatHistory, BibDatabaseContext bibDatabaseContext, ObservableList<BibEntry> entries) {
        Optional<AiChatWindow> existingWindow = stateManager.getAiChatWindows().stream().filter(window -> window.getChatName().equals(name.get())).findFirst();

        if (existingWindow.isPresent()) {
            existingWindow.get().requestFocus();
        } else {
            AiChatWindow aiChatWindow = new AiChatWindow(
                    this,
                    dialogService,
                    aiPreferences,
                    filePreferences,
                    taskExecutor
            );

            aiChatWindow.setOnCloseRequest(event ->
                stateManager.getAiChatWindows().remove(aiChatWindow)
            );

            stateManager.getAiChatWindows().add(aiChatWindow);
            dialogService.showCustomWindow(aiChatWindow);
            aiChatWindow.setChat(name, chatHistory, bibDatabaseContext, entries);
            aiChatWindow.requestFocus();
        }
    }

    @Override
    public void close() {
        shutdownSignal.set(true);

        cachedThreadPool.shutdownNow();
        jabRefChatLanguageModel.close();
        jabRefEmbeddingModel.close();
        chatHistoryService.close();

        mvStoreFullyIngestedDocumentsTracker.close();
        mvStoreEmbeddingStore.close();
        mvStoreChatHistoryStorage.close();
        mvStoreSummariesStorage.close();
    }
}
