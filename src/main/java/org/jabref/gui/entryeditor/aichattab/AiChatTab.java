package org.jabref.gui.entryeditor.aichattab;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import org.checkerframework.checker.units.qual.C;
import org.jabref.gui.DialogService;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChat;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.AiIngestor;
import org.jabref.logic.ai.ChatMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.preferences.AiPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.slf4j.LoggerFactory;
import org.tinylog.Logger;

public class AiChatTab extends EntryEditorTab {
    public static final String NAME = "AI chat";

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AiChatTab.class.getName());

    private static final String QA_SYSTEM_MESSAGE = """
            You are an AI research assistant. You read and analyze scientific articles.
            The user will send you a question regarding a paper. You will be supplied also with the relevant information found in the article.
            Answer the question only by using the relevant information. Don't make up the answer.
            If you can't answer the user question using the provided information, then reply that you couldn't do it.""";

    private final FilePreferences filePreferences;
    private final AiPreferences aiPreferences;
    private final EntryEditorPreferences entryEditorPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;

    private AiChatComponent aiChatComponent = null;

    private AiService aiService = null;
    private AiChat aiChat = null;

    private BibEntry currentBibEntry = null;

    // TODO: Proper embeddings.
    private EmbeddingStore<TextSegment> embeddingStore = null;

    public AiChatTab(PreferencesService preferencesService,
                     BibDatabaseContext bibDatabaseContext, TaskExecutor taskExecutor) {
        this.filePreferences = preferencesService.getFilePreferences();
        this.aiPreferences = preferencesService.getAiPreferences();
        this.entryEditorPreferences = preferencesService.getEntryEditorPreferences();

        this.bibDatabaseContext = bibDatabaseContext;

        this.taskExecutor = taskExecutor;

        setText(Localization.lang(NAME));
        setTooltip(new Tooltip(Localization.lang("AI chat with full-text article")));

        setUpAiConnection();
    }

    private void setUpAiConnection() {
        if (aiPreferences.getEnableChatWithFiles()) {
            aiService = new AiService(aiPreferences.getOpenAiToken());
        }

        EasyBind.listen(aiPreferences.enableChatWithFilesProperty(), (obs, oldValue, newValue) -> {
            if (newValue && !aiPreferences.getOpenAiToken().isEmpty()) {
                aiService = new AiService(aiPreferences.getOpenAiToken());
                rebuildAiChat();
            } else {
                aiService = null;
                aiChat = null;
            }
        });

        EasyBind.listen(aiPreferences.openAiTokenProperty(), (obs, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                aiService = new AiService(newValue);
                rebuildAiChat();
            }
        });
    }

    private void rebuildAiChat() {
        if (aiChat != null) {
            createAiChat();
            if (currentBibEntry != null) {
                aiChat.restoreMessages(currentBibEntry.getAiChatMessages());
            }
        }
    }

    @Override
    public boolean shouldShow(BibEntry entry) {
        return entryEditorPreferences.shouldShowAiChatTab();
    }

    @Override
    protected void bindToEntry(BibEntry entry) {
        if (!aiPreferences.getEnableChatWithFiles()) {
            setContent(new Label(Localization.lang("JabRef uses OpenAI to enable \"chatting\" with PDF files. OpenAI is an external service. To enable JabRef chatgting with PDF files, the content of the PDF files need to be shared with OpenAI. As soon as you ask a question, the text content of all PDFs attached to the entry are send to OpenAI. The privacy policy of OpenAI applies. You find it at <https://openai.com/policies/privacy-policy/>.")));
        } else if (entry.getCitationKey().isEmpty()) {
            setContent(new Label(Localization.lang("Please provide a citation key for the entry in order to enable chatting with PDF files.")));
        } else if (!checkIfCitationKeyIsUnique(bibDatabaseContext, entry.getCitationKey().get())) {
            setContent(new Label(Localization.lang("Please provide a unique citation key for the entry in order to enable chatting with PDF files.")));
        } else if (entry.getFiles().isEmpty()) {
            setContent(new Label(Localization.lang("No files attached")));
        } else if (!entry.getFiles().stream().map(LinkedFile::getLink).map(Path::of).allMatch(FileUtil::isPDFFile)) {
            setContent(new Label(Localization.lang("Only PDF files are supported")));
        } else {
            bindToCorrectEntry(entry);
        }
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
        currentBibEntry = entry;

        createAiChat();
        aiChat.restoreMessages(entry.getAiChatMessages());
        ingestFiles(entry);
        buildChatUI(entry);
    }

    private void createAiChat() {
        embeddingStore = new InMemoryEmbeddingStore<>();
        aiChat = new AiChat(aiService, embeddingStore);
        aiChat.setSystemMessage(QA_SYSTEM_MESSAGE);
    }

    private void ingestFiles(BibEntry entry) {
        AiIngestor aiIngestor = new AiIngestor(embeddingStore, aiService.getEmbeddingModel());
        entry.getFiles().forEach(file -> {
            aiIngestor.ingestLinkedFile(file, bibDatabaseContext, filePreferences);
        });
    }

    private void buildChatUI(BibEntry entry) {
        aiChatComponent = new AiChatComponent((userPrompt) -> {
            ChatMessage userMessage = ChatMessage.user(userPrompt);
            aiChatComponent.addMessage(new ChatMessageComponent(userMessage));
            entry.getAiChatMessages().add(userMessage);

            ChatMessageComponent aiChatMessageComponent = new ChatMessageComponent();
            aiChatComponent.addMessage(aiChatMessageComponent);

            BackgroundTask.wrap(() -> aiChat.execute(userPrompt))
                    .onSuccess(aiMessageText -> {
                        ChatMessage aiMessage = ChatMessage.assistant(aiMessageText);
                        aiChatMessageComponent.setMessage(aiMessage);
                        entry.getAiChatMessages().add(aiMessage);
                    })
                    .onFailure(e -> {
                        // TODO: User-friendly error message.
                        LOGGER.error("Got an error while sending a message to AI", e);
                        aiChatMessageComponent.setError(e.getMessage());
                    })
                    .executeWith(taskExecutor);
        });

        aiChatComponent.restoreMessages(entry.getAiChatMessages());

        setContent(aiChatComponent.getNode());
    }
}
