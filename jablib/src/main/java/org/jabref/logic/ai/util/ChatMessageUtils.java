package org.jabref.logic.ai.util;

import java.util.Optional;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;

public class ChatMessageUtils {

    private ChatMessageUtils() {
    }

    public static Optional<String> getContent(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return Optional.empty();
        }

        String content = switch (chatMessage) {
            case UserMessage user ->
                    user.singleText();
            case AiMessage ai ->
                    ai.text();
            case ErrorMessage err ->
                    err.getText();
            default ->
                    null;
        };

        return Optional.ofNullable(content);
    }
}
