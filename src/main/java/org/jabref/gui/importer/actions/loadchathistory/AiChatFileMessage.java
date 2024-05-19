package org.jabref.gui.importer.actions.loadchathistory;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatFileMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatFileMessage.class);

    public static final String USER_MESSAGE_TYPE = "user";
    public static final String AI_MESSAGE_TYPE = "ai";
    public static final String SYSTEM_MESSAGE_TYPE = "system";

    private final String type;
    private final String content;

    @JsonCreator
    public AiChatFileMessage(@JsonProperty("type") String type, @JsonProperty("content") String content) {
        this.type = type;
        this.content = content;
    }

    public static AiChatFileMessage fromLangchain(ChatMessage chatMessage) {
        switch (chatMessage) {
            case UserMessage userMessage -> {
                return new AiChatFileMessage(USER_MESSAGE_TYPE, userMessage.singleText());
            }
            case AiMessage aiMessage -> {
                return new AiChatFileMessage(AI_MESSAGE_TYPE, aiMessage.text());
            }
            case SystemMessage systemMessage -> {
                return new AiChatFileMessage(SYSTEM_MESSAGE_TYPE, systemMessage.text());
            }
            case ToolExecutionResultMessage toolExecutionResultMessage -> {
                return new AiChatFileMessage(SYSTEM_MESSAGE_TYPE, toolExecutionResultMessage.text());
            }
            default -> {
                LOGGER.error("Found an unknown message type while parsing AI chat history: {}", chatMessage.getClass());
                return null;
            }
        }
    }

    public Optional<ChatMessage> toLangchainMessage() {
        switch (type) {
            case USER_MESSAGE_TYPE -> {
                return Optional.of(new UserMessage(content));
            }
            case AI_MESSAGE_TYPE -> {
                return Optional.of(new AiMessage(content));
            }
            case SYSTEM_MESSAGE_TYPE -> {
                return Optional.of(new SystemMessage(content));
            }
            default -> {
                LOGGER.error("Found an unknown message type while parsing AI chat history: {}", type);
                return Optional.empty();
            }
        }
    }

    public String getType() {
        return type;
    }

    public String getContent() {
        return content;
    }
}
