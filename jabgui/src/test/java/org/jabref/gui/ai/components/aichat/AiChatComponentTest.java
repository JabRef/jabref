package org.jabref.gui.ai.components.aichat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
        SimpleObjectProperty<AiProvider> providerProp = new SimpleObjectProperty<>(AiProvider.OPEN_AI);
        SimpleStringProperty openAiModelProp = new SimpleStringProperty("gpt-4o");
        SimpleStringProperty mistralModelProp = new SimpleStringProperty("mistral-large");
        SimpleStringProperty geminiModelProp = new SimpleStringProperty("gemini-1.5-pro");
        SimpleStringProperty hfModelProp = new SimpleStringProperty("hf-model");
        SimpleStringProperty gpt4AllModelProp = new SimpleStringProperty("gpt4all");

        when(prefs.aiProviderProperty()).thenReturn(providerProp);
        when(prefs.openAiChatModelProperty()).thenReturn(openAiModelProp);
        when(prefs.mistralAiChatModelProperty()).thenReturn(mistralModelProp);
        when(prefs.geminiChatModelProperty()).thenReturn(geminiModelProp);
        when(prefs.huggingFaceChatModelProperty()).thenReturn(hfModelProp);
        when(prefs.gpt4AllChatModelProperty()).thenReturn(gpt4AllModelProp);

        when(prefs.getAiProvider()).thenAnswer(_ -> providerProp.get());
        when(prefs.getSelectedChatModel()).thenAnswer(_ -> {
            return switch (prefs.getAiProvider()) {
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

    private AiChatComponent createComponent() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final AiChatComponent[] holder = new AiChatComponent[1];
        Platform.runLater(() -> {
            holder[0] = new AiChatComponent(
                    aiService,
                    new SimpleStringProperty("entry"),
                    FXCollections.observableArrayList(),
                    FXCollections.observableArrayList(),
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
        AiChatComponent component = createComponent();

        // Change provider
        prefs.aiProviderProperty().set(provider);

        Thread.sleep(50);

        String expected = "Current AI model: " + provider.getLabel() + " " + prefs.getSelectedChatModel()
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        assertEquals(expected, component.computeNoticeText());
    }

    @ParameterizedTest
    @EnumSource(AiProvider.class)
    void noticeTextUpdatesWhenCurrentModelChangesForSelectedProvider(AiProvider provider) throws Exception {
        AiChatComponent component = createComponent();

        // Select provider
        prefs.aiProviderProperty().set(provider);

        // Change current model for all providers; only the selected provider should influence the notice text.
        String newModel = "new-model";
        prefs.openAiChatModelProperty().set(newModel);
        prefs.mistralAiChatModelProperty().set(newModel);
        prefs.geminiChatModelProperty().set(newModel);
        prefs.huggingFaceChatModelProperty().set(newModel);
        prefs.gpt4AllChatModelProperty().set(newModel);

        Thread.sleep(50);
        String expected = "Current AI model: " + provider.getLabel() + " " + newModel
                + ". The AI may generate inaccurate or inappropriate responses. Please verify any information provided.";
        assertEquals(expected, component.computeNoticeText());
    }
}
