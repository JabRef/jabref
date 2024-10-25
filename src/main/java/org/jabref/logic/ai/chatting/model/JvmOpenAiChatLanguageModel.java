package org.jabref.logic.ai.chatting.model;

import java.net.http.HttpClient;
import java.util.List;

import org.jabref.logic.ai.AiPreferences;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import io.github.stefanbratanov.jvm.openai.ChatClient;
import io.github.stefanbratanov.jvm.openai.ChatCompletion;
import io.github.stefanbratanov.jvm.openai.CreateChatCompletionRequest;
import io.github.stefanbratanov.jvm.openai.OpenAI;
import io.github.stefanbratanov.jvm.openai.Usage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JvmOpenAiChatLanguageModel implements ChatLanguageModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(JvmOpenAiChatLanguageModel.class);

    private final AiPreferences aiPreferences;

    private final ChatClient chatClient;

    public JvmOpenAiChatLanguageModel(AiPreferences aiPreferences, HttpClient httpClient) {
        this.aiPreferences = aiPreferences;

        OpenAI openAI = OpenAI
                .newBuilder(aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider()))
                .httpClient(httpClient)
                .baseUrl(aiPreferences.getSelectedApiBaseUrl())
                .build();

        this.chatClient = openAI.chatClient();
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> list) {
        LOGGER.debug("Generating response from jvm-openai chat model with {} messages: {}", list.size(), list);

        List<io.github.stefanbratanov.jvm.openai.ChatMessage> messages =
                list.stream().map(chatMessage -> (io.github.stefanbratanov.jvm.openai.ChatMessage) switch (chatMessage) {
                    case AiMessage aiMessage -> io.github.stefanbratanov.jvm.openai.ChatMessage.assistantMessage(aiMessage.text());
                    case SystemMessage systemMessage -> io.github.stefanbratanov.jvm.openai.ChatMessage.systemMessage(systemMessage.text());
                    case ToolExecutionResultMessage toolExecutionResultMessage -> io.github.stefanbratanov.jvm.openai.ChatMessage.toolMessage(toolExecutionResultMessage.text(), toolExecutionResultMessage.id());
                    case UserMessage userMessage -> io.github.stefanbratanov.jvm.openai.ChatMessage.userMessage(userMessage.singleText());
                    default -> throw new IllegalStateException("unknown conversion of chat message from langchain4j to jvm-openai");
                }).toList();

        CreateChatCompletionRequest request = CreateChatCompletionRequest
                .newBuilder()
                .model(aiPreferences.getSelectedChatModel())
                .temperature(aiPreferences.getTemperature())
                .n(1)
                .messages(messages)
                .build();

        ChatCompletion chatCompletion = chatClient.createChatCompletion(request);
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

        return new Response<>(new AiMessage(choice.message().content()), new TokenUsage(usage.promptTokens(), usage.completionTokens()), FinishReason.OTHER);
    }
}
