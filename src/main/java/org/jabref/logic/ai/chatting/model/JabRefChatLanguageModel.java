package org.jabref.logic.ai.chatting.model;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.logic.ai.chatting.AiChatLogic;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.ai.AiApiKeyProvider;
import org.jabref.preferences.ai.AiPreferences;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;
import dev.langchain4j.model.output.Response;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * This class listens to preferences changes.
 */
public class JabRefChatLanguageModel implements ChatLanguageModel, AutoCloseable {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private final AiPreferences aiPreferences;
    private final AiApiKeyProvider apiKeyProvider;

    private final HttpClient httpClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("ai-api-connection-pool-%d").build()
    );

    private Optional<ChatLanguageModel> langchainChatModel = Optional.empty();

    public JabRefChatLanguageModel(AiPreferences aiPreferences, AiApiKeyProvider apiKeyProvider) {
        this.aiPreferences = aiPreferences;
        this.apiKeyProvider = apiKeyProvider;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).executor(executorService).build();

        setupListeningToPreferencesChanges();
    }

    /**
     * Update the underlying {@link dev.langchain4j.model.chat.ChatLanguageModel} by current {@link AiPreferences} parameters.
     * When the model is updated, the chat messages are not lost.
     * See {@link AiChatLogic}, where messages are stored in {@link ChatMemory},
     * and see {@link org.jabref.logic.ai.chatting.chathistory.ChatHistoryStorage}.
     */
    private void rebuild() {
        String apiKey = apiKeyProvider.getApiKeyForAiProvider(aiPreferences.getAiProvider());
        if (!aiPreferences.getEnableAi() || apiKey.isEmpty()) {
            langchainChatModel = Optional.empty();
            return;
        }

        switch (aiPreferences.getAiProvider()) {
            case OPEN_AI -> {
                langchainChatModel = Optional.of(new JvmOpenAiChatLanguageModel(aiPreferences, apiKeyProvider, httpClient));
            }

            case MISTRAL_AI -> {
                langchainChatModel = Optional.of(MistralAiChatModel
                        .builder()
                        .apiKey(apiKey)
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
                        .accessToken(apiKey)
                        .modelId(aiPreferences.getSelectedChatModel())
                        .temperature(aiPreferences.getTemperature())
                        .timeout(Duration.ofMinutes(2))
                        .build()
                );
            }
        }
    }

    private void setupListeningToPreferencesChanges() {
        // Setting "langchainChatModel" to "Optional.empty()" will trigger a rebuild on the next usage

        aiPreferences.enableAiProperty().addListener(obs -> langchainChatModel = Optional.empty());
        aiPreferences.aiProviderProperty().addListener(obs -> langchainChatModel = Optional.empty());
        aiPreferences.customizeExpertSettingsProperty().addListener(obs -> langchainChatModel = Optional.empty());
        aiPreferences.temperatureProperty().addListener(obs -> langchainChatModel = Optional.empty());

        aiPreferences.addListenerToChatModels(() -> langchainChatModel = Optional.empty());
        aiPreferences.addListenerToApiBaseUrls(() -> langchainChatModel = Optional.empty());
        aiPreferences.setApiKeyChangeListener(() -> langchainChatModel = Optional.empty());
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
            } else if (apiKeyProvider.getApiKeyForAiProvider(aiPreferences.getAiProvider()).isEmpty()) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, set an API key inside JabRef preferences (AI tab)."));
            } else {
                rebuild();
                if (langchainChatModel.isEmpty()) {
                    throw new RuntimeException(Localization.lang("Unable to chat with AI."));
                }
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
