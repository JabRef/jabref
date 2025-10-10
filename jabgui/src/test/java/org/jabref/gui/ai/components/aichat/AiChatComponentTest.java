package org.jabref.gui.ai.components.aichat;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.AiChatLogic;
import org.jabref.logic.ai.chatting.AiChatService;
import org.jabref.logic.ai.ingestion.IngestionService;
import org.jabref.logic.ai.templates.AiTemplate;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.AiProvider;
import org.jabref.model.ai.EmbeddingModel;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiChatComponentTest {

    private AiPreferences prefs;
    private AiService aiService;
    private BibDatabaseContext bibDatabaseContext;
    private DialogService dialogService;
    private TaskExecutor taskExecutor;

    @BeforeAll
    static void initFxAndLocalization() throws Exception {
        Localization.setLanguage(Language.ENGLISH);
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        latch.await(5, TimeUnit.SECONDS);
    }

    @BeforeEach
    void setUp() {
        prefs = new AiPreferences(
                /* enableAi */ true,
                /* autoGenerateEmbeddings */ false,
                /* autoGenerateSummaries */ false,
                /* aiProvider */ AiProvider.OPEN_AI,
                /* openAiChatModel */ "gpt-4o",
                /* mistralAiChatModel */ "mistral-large",
                /* geminiChatModel */ "gemini-1.5-pro",
                /* huggingFaceChatModel */ "hf-model",
                /* gpt4AllModel */ "gpt4all",
                /* customizeExpertSettings */ true,
                /* openAiApiBaseUrl */ "https://api.openai.com/v1",
                /* mistralAiApiBaseUrl */ "https://api.mistral.ai",
                /* geminiApiBaseUrl */ "https://generativelanguage.googleapis.com",
                /* huggingFaceApiBaseUrl */ "https://api-inference.huggingface.co",
                /* gpt4AllApiBaseUrl */ "http://localhost:4891",
                /* embeddingModel */ EmbeddingModel.values()[0],
                /* temperature */ 0.7,
                /* contextWindowSize */ 8192,
                /* documentSplitterChunkSize */ 1000,
                /* documentSplitterOverlapSize */ 200,
                /* ragMaxResultsCount */ 5,
                /* ragMinScore */ 0.5,
                Map.of(
                        AiTemplate.CHATTING_SYSTEM_MESSAGE, "",
                        AiTemplate.CHATTING_USER_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_CHUNK_SYSTEM_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_CHUNK_USER_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_COMBINE_SYSTEM_MESSAGE, "",
                        AiTemplate.SUMMARIZATION_COMBINE_USER_MESSAGE, "",
                        AiTemplate.CITATION_PARSING_SYSTEM_MESSAGE, "",
                        AiTemplate.CITATION_PARSING_USER_MESSAGE, ""
                )
        );

        aiService = mock(AiService.class, Mockito.RETURNS_DEEP_STUBS);
        AiChatService chatService = mock(AiChatService.class);
        AiChatLogic chatLogic = mock(AiChatLogic.class, Mockito.RETURNS_DEEP_STUBS);
        when(chatLogic.getChatHistory()).thenReturn(FXCollections.observableArrayList());
        when(chatService.makeChat(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(chatLogic);
        when(aiService.getAiChatService()).thenReturn(chatService);

        IngestionService ingestionService = mock(IngestionService.class, Mockito.RETURNS_DEEP_STUBS);
        when(aiService.getIngestionService()).thenReturn(ingestionService);

        bibDatabaseContext = mock(BibDatabaseContext.class);
        dialogService = mock(DialogService.class);
        taskExecutor = mock(TaskExecutor.class);
    }

    private AiChatComponent createComponent(ObservableList<ChatMessage> chatHistory, ObservableList<BibEntry> entries) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final AiChatComponent[] holder = new AiChatComponent[1];
        Platform.runLater(() -> {
            holder[0] = new AiChatComponent(
                    aiService,
                    new SimpleStringProperty("entry"),
                    chatHistory,
                    entries,
                    bibDatabaseContext,
                    prefs,
                    dialogService,
                    taskExecutor
            );
            latch.countDown();
        });
        latch.await(5, TimeUnit.SECONDS);
        return holder[0];
    }

    @Test
    void noticeTextUpdatesWhenProviderChanges() throws Exception {
        AiChatComponent component = createComponent(FXCollections.observableArrayList(), FXCollections.observableArrayList());

        String expectedOpenAI = "Current AI model: " + AiProvider.OPEN_AI.getLabel() + " " + prefs.getSelectedChatModel()
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        assertEquals(expectedOpenAI, component.noticeTextProperty().get());

        // Change provider to Mistral; notice should update to use mistral model string
        prefs.aiProviderProperty().set(AiProvider.MISTRAL_AI);
        String expectedMistral = "Current AI model: " + AiProvider.MISTRAL_AI.getLabel() + " " + prefs.getSelectedChatModel()
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";

        // Wait briefly for FX binding to react
        Thread.sleep(50);
        assertEquals(expectedMistral, component.noticeTextProperty().get());
    }

    @Test
    void noticeTextUpdatesWhenCurrentModelChangesForSelectedProvider() throws Exception {
        AiChatComponent component = createComponent(FXCollections.observableArrayList(), FXCollections.observableArrayList());

        // Change the OpenAI chat model while the provider is OpenAI
        prefs.openAiChatModelProperty().set("gpt-4o-mini");
        String expected = "Current AI model: " + AiProvider.OPEN_AI.getLabel() + " gpt-4o-mini"
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        Thread.sleep(50);
        assertEquals(expected, component.noticeTextProperty().get());

        // Switch provider to Gemini and change a Gemini model
        prefs.aiProviderProperty().set(AiProvider.GEMINI);
        prefs.geminiChatModelProperty().set("gemini-1.5-flash");
        String expectedGemini = "Current AI model: " + AiProvider.GEMINI.getLabel() + " gemini-1.5-flash"
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        Thread.sleep(50);
        assertEquals(expectedGemini, component.noticeTextProperty().get());
    }
}
