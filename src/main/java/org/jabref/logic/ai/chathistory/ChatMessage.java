package org.jabref.logic.ai.chathistory;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatMessage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessage.class);

    public enum Type {
        USER,
        ASSISTANT;
    }

    private final Type type;
    private final String content;

    public ChatMessage(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    public static ChatMessage user(String content) {
        return new ChatMessage(Type.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Type.ASSISTANT, content);
    }

    public Type getType() {
        return type;
    }

    public String getTypeLabel() {
        return switch (type) {
            case USER ->
                    Localization.lang("User");
            case ASSISTANT ->
                    Localization.lang("AI");
        };
    }

    public String getContent() {
        return content;
    }

    public static Optional<ChatMessage> fromLangchain(dev.langchain4j.data.message.ChatMessage chatMessage) {
        switch (chatMessage) {
            case UserMessage userMessage -> {
                return Optional.of(ChatMessage.user(userMessage.singleText()));
            }
            case AiMessage aiMessage -> {
                return Optional.of(ChatMessage.assistant(aiMessage.text()));
            }
            default -> {
                LOGGER.error("Unable to convert langchain4j chat message to JabRef chat message, the type is {}", chatMessage.getClass());
                return Optional.empty();
            }
        }
    }

    public Optional<dev.langchain4j.data.message.ChatMessage> toLangchainMessage() {
        switch (type) {
            case Type.USER -> {
                return Optional.of(new UserMessage(content));
            }
            case Type.ASSISTANT -> {
                return Optional.of(new AiMessage(content));
            }
            default -> {
                LOGGER.error("Unable to convert JabRef chat message to langchain4j chat message, the type is {}", type);
                return Optional.empty();
            }
        }
    }
}
