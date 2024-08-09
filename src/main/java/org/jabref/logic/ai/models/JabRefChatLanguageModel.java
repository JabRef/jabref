package org.jabref.logic.ai.models;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.logic.ai.AiChatLogic;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.AiPreferences;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.output.Response;
import org.h2.mvstore.MVStore;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * This class listens to preferences changes.
 */
public class JabRefChatLanguageModel implements ChatLanguageModel, AutoCloseable {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private final AiPreferences aiPreferences;

    private final HttpClient httpClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("ai-api-connection-pool-%d").build()
    );

    private Optional<ChatLanguageModel> langchainChatModel = Optional.empty();

    public JabRefChatLanguageModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).executor(executorService).build();

        if (aiPreferences.getEnableAi()) {
            rebuild();
        }

        setupListeningToPreferencesChanges();
    }

    /**
     * Update the underlying {@link dev.langchain4j.model.chat.ChatLanguageModel} by current {@link AiPreferences} parameters.
     * When the model is updated, the chat messages are not lost.
     * See {@link AiChatLogic}, where messages are stored in {@link ChatMemory},
     * and using {@link org.jabref.logic.ai.chathistory.BibDatabaseChatHistoryManager}, where messages are stored in {@link MVStore}.
     */
    private void rebuild() {
        if (!aiPreferences.getEnableAi() || aiPreferences.getSelectedApiKey().isEmpty()) {
            langchainChatModel = Optional.empty();
            return;
        }

        switch (aiPreferences.getAiProvider()) {
            case OPEN_AI -> {
                langchainChatModel = Optional.of(new JvmOpenAiChatLanguageModel(aiPreferences, httpClient));
            }

            case MISTRAL_AI -> {
                langchainChatModel = Optional.of(MistralAiChatModel
                        .builder()
                        .apiKey(aiPreferences.getSelectedApiKey())
                        .modelName(aiPreferences.getSelectedChatModel())
                        .temperature(aiPreferences.getTemperature())
                        .baseUrl(aiPreferences.getSelectedApiBaseUrl())
                        .logRequests(true)
                        .logResponses(true)
                        .build()
                );
            }

            case HUGGING_FACE -> {
                // NOTE: {@link HuggingFaceChatModel} doesn't support API base url :(
                langchainChatModel = Optional.of(HuggingFaceChatModel
                        .builder()
                        .accessToken(aiPreferences.getSelectedApiKey())
                        .modelId(aiPreferences.getSelectedChatModel())
                        .temperature(aiPreferences.getTemperature())
                        .timeout(Duration.ofMinutes(2))
                        .build()
                );
            }
        }
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.enableAiProperty().addListener(obs -> rebuild());
        aiPreferences.aiProviderProperty().addListener(obs -> rebuild());
        aiPreferences.customizeExpertSettingsProperty().addListener(obs -> rebuild());
        aiPreferences.listenToChatModels(this::rebuild);
        aiPreferences.listenToApiTokens(this::rebuild);
        aiPreferences.listenToApiBaseUrls(this::rebuild);
        aiPreferences.temperatureProperty().addListener(obs -> rebuild());
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> list) {
        // The rationale for RuntimeExceptions in this method:
        // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
        //    everywhere. Because this method implements a langchain4j interface,
        //    we follow the same "practice".
        // 2. There is no way to encode error information from type system: nor
        //    in the result type, nor "throws" in method signature. Actually,
        //    it's possible, but langchain4j doesn't do it.

        if (langchainChatModel.isEmpty()) {
            if (!aiPreferences.getEnableAi()) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, you need to enable chatting with attached PDF files in JabRef preferences (AI tab)."));
            } else if (aiPreferences.getSelectedApiKey().isEmpty()) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, set OpenAI API key inside JabRef preferences (AI tab)."));
            } else {
                throw new RuntimeException(Localization.lang("Unable to chat with AI."));
            }
        }

        return langchainChatModel.get().generate(list);
    }

    @Override
    public void close() {
        httpClient.shutdownNow();
        executorService.shutdownNow();
    }
}
