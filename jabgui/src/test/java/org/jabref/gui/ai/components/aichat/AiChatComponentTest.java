package org.jabref.gui.ai.components.aichat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chatting.AiChatLogic;
import org.jabref.logic.ai.chatting.AiChatService;
import org.jabref.logic.ai.ingestion.IngestionService;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.AiProvider;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class AiChatComponentTest {
    private AiPreferences prefs;
    private AiService aiService;
    private BibDatabaseContext bibDatabaseContext;
    private DialogService dialogService;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        Localization.setLanguage(Language.ENGLISH);

        prefs = mock(AiPreferences.class, Mockito.RETURNS_DEEP_STUBS);
        var providerProp = new SimpleObjectProperty<>(AiProvider.OPEN_AI);
        var openAiModelProp = new SimpleStringProperty("gpt-4o");
        var mistralModelProp = new SimpleStringProperty("mistral-large");
        var geminiModelProp = new SimpleStringProperty("gemini-1.5-pro");
        var hfModelProp = new SimpleStringProperty("hf-model");
        var gpt4AllModelProp = new SimpleStringProperty("gpt4all");

        when(prefs.aiProviderProperty()).thenReturn(providerProp);
        when(prefs.openAiChatModelProperty()).thenReturn(openAiModelProp);
        when(prefs.mistralAiChatModelProperty()).thenReturn(mistralModelProp);
        when(prefs.geminiChatModelProperty()).thenReturn(geminiModelProp);
        when(prefs.huggingFaceChatModelProperty()).thenReturn(hfModelProp);
        when(prefs.gpt4AllChatModelProperty()).thenReturn(gpt4AllModelProp);

        when(prefs.getAiProvider()).thenAnswer(inv -> providerProp.get());
        when(prefs.getSelectedChatModel()).thenAnswer(inv -> {
            return switch (providerProp.get()) {
                case OPEN_AI ->
                        openAiModelProp.get();
                case MISTRAL_AI ->
                        mistralModelProp.get();
                case GEMINI ->
                        geminiModelProp.get();
                case HUGGING_FACE ->
                        hfModelProp.get();
                case GPT4ALL ->
                        gpt4AllModelProp.get();
            };
        });

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

    @ParameterizedTest
    @EnumSource(AiProvider.class)
    void noticeTextUpdatesWhenProviderChanges(AiProvider provider) throws Exception {
        AiChatComponent component = createComponent(FXCollections.observableArrayList(), FXCollections.observableArrayList());

        // Change provider
        prefs.aiProviderProperty().set(provider);

        Thread.sleep(50);

        String expected = "Current AI model: " + provider.getLabel() + " " + prefs.getSelectedChatModel()
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        assertEquals(expected, component.computeNoticeText());
    }

    @ParameterizedTest
    @MethodSource("providerAndModel")
    void noticeTextUpdatesWhenCurrentModelChangesForSelectedProvider(AiProvider provider, String newModel) throws Exception {
        AiChatComponent component = createComponent(FXCollections.observableArrayList(), FXCollections.observableArrayList());

        // Set the provider and the corresponding model
        prefs.aiProviderProperty().set(provider);
        switch (provider) {
            case OPEN_AI ->
                    prefs.openAiChatModelProperty().set(newModel);
            case MISTRAL_AI ->
                    prefs.mistralAiChatModelProperty().set(newModel);
            case GEMINI ->
                    prefs.geminiChatModelProperty().set(newModel);
            case HUGGING_FACE ->
                    prefs.huggingFaceChatModelProperty().set(newModel);
            case GPT4ALL ->
                    prefs.gpt4AllChatModelProperty().set(newModel);
        }

        Thread.sleep(50);
        String expected = "Current AI model: " + provider.getLabel() + " " + newModel
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        assertEquals(expected, component.computeNoticeText());
    }

    private static Stream<Arguments> providerAndModel() {
        return Stream.of(
                Arguments.of(AiProvider.OPEN_AI, "gpt-4o-mini"),
                Arguments.of(AiProvider.MISTRAL_AI, "mistral-medium"),
                Arguments.of(AiProvider.GEMINI, "gemini-1.5-flash"),
                Arguments.of(AiProvider.HUGGING_FACE, "hf-new-model"),
                Arguments.of(AiProvider.GPT4ALL, "gpt4all-new")
        );
    }
}
