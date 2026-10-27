package org.jabref.logic.ai.chatting;

import java.util.List;
import java.util.stream.Collectors;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A dummy AI chat model used for testing and debugging AI features without external API calls.
@NullMarked
public class DummyChatLanguageModel implements ChatModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyChatLanguageModel.class);

    @Override
    public ChatResponse chat(List<ChatMessage> messages) {
        LOGGER.debug("Generating response from dummy chat model with {} messages: {}", messages.size(), messages);

        String systemMessage = messages.stream()
                .filter(SystemMessage.class::isInstance)
                .map(SystemMessage.class::cast)
                .map(SystemMessage::text)
                .collect(Collectors.joining("\n"));

        String userMessage = messages.stream()
                .filter(UserMessage.class::isInstance)
                .map(UserMessage.class::cast)
                .map(UserMessage::singleText)
                .reduce((_, second) -> second)
                .orElse("");

        String response = """
                Hi! I am a dummy AI provider, I am made to test and debug the AI features.

                I have this system message:
                %s

                And I received this user message:
                %s""".formatted(systemMessage, userMessage);

        return new ChatResponse.Builder()
                .aiMessage(new AiMessage(response))
                .tokenUsage(new TokenUsage(0, 0))
                .finishReason(FinishReason.STOP)
                .build();
    }
}
