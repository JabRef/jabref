package org.jabref.logic.ai.chat;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.jabref.logic.ai.chathistory.BibDatabaseChatHistory;
import org.jabref.preferences.AiPreferences;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatCompletion;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.Usage;
import org.h2.mvstore.MVStore;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * This class listens to preferences changes.
 */
public class AiChatLanguageModel implements ChatLanguageModel, AutoCloseable {
    private final AiPreferences aiPreferences;

    private final HttpClient httpClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Optional<ChatClient> chatClient = Optional.empty();

    public AiChatLanguageModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;
        this.httpClient = HttpClient.newBuilder().executor(executorService).build();

        if (aiPreferences.getEnableChatWithFiles()) {
            rebuild();
        }

        setupListeningToPreferencesChanges();
    }

    /**
     * Update the underlying {@link ChatLanguageModel} by current {@link AiPreferences} parameters.
     * When the model is updated, the chat messages are not lost.
     * See {@link AiChatLogic}, where messages are stored in {@link ChatMemory},
     * and {@link BibDatabaseChatHistory}, where messages are stored in {@link MVStore}.
     */
    private void rebuild() {
        if (!aiPreferences.getEnableChatWithFiles() || aiPreferences.getOpenAiToken().isEmpty()) {
            chatClient = Optional.empty();
            return;
        }

        OpenAI openAI = OpenAI.newBuilder(aiPreferences.getOpenAiToken()).httpClient(httpClient).build();
        chatClient = Optional.of(openAI.chatClient());
    }

    private void setupListeningToPreferencesChanges() {
        aiPreferences.enableChatWithFilesProperty().addListener(obs -> rebuild());
        aiPreferences.openAiTokenProperty().addListener(obs -> rebuild());
        aiPreferences.chatModelProperty().addListener(obs -> rebuild());
        aiPreferences.temperatureProperty().addListener(obs -> rebuild());
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> list) {
        if (chatClient.isEmpty()) {
            throw new RuntimeException("Chat is not enabled");
        }

        List<io.github.stefanbratanov.jvm.openai.ChatMessage> messages =
                list.stream().map(chatMessage -> {
                    // Do not inline this variable. Java compiler will argue that we return Record & ChatMessage.
                    //noinspection UnnecessaryLocalVariable
                    io.github.stefanbratanov.jvm.openai.ChatMessage result = switch (chatMessage) {
                        case AiMessage aiMessage ->
                                io.github.stefanbratanov.jvm.openai.ChatMessage.assistantMessage(aiMessage.text());
                        case SystemMessage systemMessage ->
                                io.github.stefanbratanov.jvm.openai.ChatMessage.systemMessage(systemMessage.text());
                        case ToolExecutionResultMessage toolExecutionResultMessage ->
                                io.github.stefanbratanov.jvm.openai.ChatMessage.toolMessage(toolExecutionResultMessage.text(), toolExecutionResultMessage.id());
                        case UserMessage userMessage ->
                                io.github.stefanbratanov.jvm.openai.ChatMessage.userMessage(userMessage.singleText());
                        default ->
                                throw new IllegalStateException("unknown conversion of chat message from langchain4j to jvm-openai");
                    };
                    return result;
                }).toList();

        CreateChatCompletionRequest request = CreateChatCompletionRequest
                .newBuilder()
                .model(aiPreferences.getChatModel().getLabel())
                .temperature(aiPreferences.getTemperature())
                .n(1)
                .messages(messages)
                .build();

        ChatCompletion chatCompletion = chatClient.get().createChatCompletion(request);
        Usage usage = chatCompletion.usage();
        List<ChatCompletion.Choice> choices = chatCompletion.choices();

        if (choices.isEmpty()) {
            throw new RuntimeException("OpenAI returned no chat completion");
        }

        ChatCompletion.Choice choice = choices.getFirst();

        return new Response<>(new AiMessage(choice.message().content()), new TokenUsage(usage.promptTokens(), usage.completionTokens()), FinishReason.OTHER);
    }

    @Override
    public void close() throws Exception {
        httpClient.shutdownNow();
        executorService.shutdownNow();
    }
}
