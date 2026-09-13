package org.jabref.logic.ai.chatting;

import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.l10n.Localization;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatCompletion;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JvmOpenAiChatLanguageModel implements ChatModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(JvmOpenAiChatLanguageModel.class);

    private final String modelName;
    private final double temperature;

    private final String baseUrl;
    private final ChatClient chatClient;

    public JvmOpenAiChatLanguageModel(String apiKey, String modelName, double temperature, String baseUrl, HttpClient httpClient) {
        this.modelName = modelName;
        this.temperature = temperature;
        this.baseUrl = baseUrl;

        OpenAI openAI = OpenAI
                .newBuilder(apiKey)
                .httpClient(httpClient)
                .baseUrl(baseUrl)
                .build();

        this.chatClient = openAI.chatClient();
    }

    @Override
    public ChatResponse chat(List<ChatMessage> list) {
        LOGGER.debug("Generating response from jvm-openai chat model with {} messages: {}", list.size(), list);

        List<io.github.stefanbratanov.jvm.openai.ChatMessage> messages =
                list.stream().map(chatMessage -> (io.github.stefanbratanov.jvm.openai.ChatMessage) switch (chatMessage) {
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
                }).toList();

        CreateChatCompletionRequest request = CreateChatCompletionRequest
                .newBuilder()
                .model(modelName)
                .temperature(temperature)
                .n(1)
                .messages(messages)
                .build();

        ChatCompletion chatCompletion;
        try {
            chatCompletion = chatClient.createChatCompletion(request);
        } catch (UncheckedIOException e) {
            // jvm-openai wraps connection failures without any message (e.g., "java.net.ConnectException"), so name the URL and the root cause
            Throwable rootCause = e;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }
            String reason = rootCause.getMessage() == null ? rootCause.getClass().getSimpleName() : rootCause.getMessage();
            String redactedUrl = FetcherException.getRedactedUrl(baseUrl);
            LOGGER.debug("Could not connect to {}", redactedUrl, e);
            throw new UncheckedIOException(Localization.lang("Could not connect to %0.\n\n%1", redactedUrl, reason), e.getCause());
        }
        Usage usage = chatCompletion.usage();
        List<ChatCompletion.Choice> choices = chatCompletion.choices();

        if (choices.isEmpty()) {
            // The rationale for RuntimeExceptions in this method:
            // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
            //    everywhere. Because this method implements a langchain4j interface,
            //    we follow the same "practice".
            // 2. There is no way to encode error information from type system: nor
            //    in the result type, nor "throws" in method signature. Actually,
            //    it's possible, but langchain4j doesn't do it.
            throw new RuntimeException("OpenAI returned no chat completion");
        }

        ChatCompletion.Choice choice = choices.getFirst();

        return new ChatResponse.Builder().aiMessage(new AiMessage(choice.message().content()))
                                         .tokenUsage(new TokenUsage(usage.promptTokens(), usage.completionTokens()))
                                         .finishReason(FinishReason.OTHER).build();
    }
}
