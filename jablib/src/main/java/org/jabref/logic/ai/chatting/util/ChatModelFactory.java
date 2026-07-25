package org.jabref.logic.ai.chatting.util;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.chatting.JvmOpenAiChatLanguageModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.tokenization.logic.TokenEstimator;
import org.jabref.logic.ai.tokenization.util.TokenEstimatorFactory;
import org.jabref.model.ai.llm.AiProvider;
import org.jabref.model.ai.tokenization.TokenEstimatorKind;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

/// Static factory for creating {@link ChatModel} instances.
///
/// Each call creates its own {@link HttpClient} and {@link ExecutorService}.
/// The returned model implements {@link AutoCloseable}; callers should close it
/// when it is no longer needed so the underlying resources are released.
///
/// Returns {@code null} when AI is disabled or the API key is empty.
public final class ChatModelFactory {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private ChatModelFactory() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }

    /// Creates a {@link ChatModel} from explicit parameters.
    /// All decisions about provider, model name, API key, etc. must be made by the caller.
    public static ChatModel create(
            AiProvider provider,
            String modelName,
            String apiKey,
            double temperature,
            // [impl->req~ai.llms.custom.base-url~1]
            // [impl->feat~ai.llms.custom~1]
            String baseUrl,
            int contextWindowSize,
            TokenEstimatorKind tokenEstimatorKind
    ) {
        TokenEstimator tokenEstimator = TokenEstimatorFactory.create(tokenEstimatorKind);

        ExecutorService executorService = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat("ai-api-connection-pool-%d").build()
        );
        HttpClient httpClient = HttpClient.newBuilder()
                                          .connectTimeout(CONNECTION_TIMEOUT)
                                          .executor(executorService)
                                          .build();

        dev.langchain4j.model.chat.ChatModel langchainModel = switch (provider) {
            case OPEN_AI ->
                    new JvmOpenAiChatLanguageModel(apiKey, modelName, temperature, baseUrl, httpClient);
            case HUGGING_FACE -> // NOTE: Hugging Face is implemented via OpenAI API.
                    new JvmOpenAiChatLanguageModel(apiKey, modelName, temperature, baseUrl, httpClient);
            case MISTRAL_AI ->
                    MistralAiChatModel.builder()
                                      .apiKey(apiKey)
                                      .modelName(modelName)
                                      .temperature(temperature)
                                      .baseUrl(baseUrl)
                                      .logRequests(true)
                                      .logResponses(true)
                                      .build();
            case GEMINI -> // NOTE: GoogleAiGeminiChatModel doesn't support API base URL.
                    GoogleAiGeminiChatModel.builder()
                                           .apiKey(apiKey)
                                           .modelName(modelName)
                                           .temperature(temperature)
                                           .logRequestsAndResponses(true)
                                           .build();
        };

        return new ChatModelImpl(langchainModel, tokenEstimator, provider, modelName, contextWindowSize, httpClient, executorService);
    }

    public static ChatModel create(AiPreferences aiPreferences) {
        return create(
                aiPreferences.getAiProvider(),
                aiPreferences.getSelectedChatModel(),
                aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider()),
                aiPreferences.getTemperature(),
                aiPreferences.getSelectedApiBaseUrl(),
                aiPreferences.getContextWindowSize(),
                aiPreferences.getTokenEstimatorKind()
        );
    }

    private static class ChatModelImpl implements ChatModel {
        private final dev.langchain4j.model.chat.ChatModel delegate;
        private final TokenEstimator tokenEstimator;
        private final AiProvider provider;
        private final String name;
        private final int contextWindowSize;
        private final HttpClient httpClient;
        private final ExecutorService executorService;

        ChatModelImpl(
                dev.langchain4j.model.chat.ChatModel delegate,
                TokenEstimator tokenEstimator,
                AiProvider provider,
                String name,
                int contextWindowSize,
                HttpClient httpClient,
                ExecutorService executorService
        ) {
            this.delegate = delegate;
            this.tokenEstimator = tokenEstimator;
            this.provider = provider;
            this.name = name;
            this.contextWindowSize = contextWindowSize;
            this.httpClient = httpClient;
            this.executorService = executorService;
        }

        @Override
        public ChatResponse chat(List<dev.langchain4j.data.message.ChatMessage> messages) {
            return delegate.chat(messages);
        }

        @Override
        public TokenEstimator getTokenizer() {
            return tokenEstimator;
        }

        @Override
        public AiProvider getAiProvider() {
            return provider;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getContextWindowSize() {
            return contextWindowSize;
        }

        @Override
        public void close() {
            httpClient.shutdownNow();
            executorService.shutdownNow();
        }
    }
}
