package org.jabref.logic.ai.util;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;

/**
 * Class representing an error from AI side.
 * This is a dummy class, that extends from langchain4j's {@link ChatMessage}, but it should not be used in any
 * of langchain4j's classes or algorithms as langchain4j does not support adding new message types.
 * The primary use of this class is to be stored in a chat history and displayed in the UI.
 */
public class ErrorMessage implements ChatMessage {
    String text;

    public ErrorMessage(String text) {
        this.text = text;
    }

    public ChatMessageType type() {
        // In order to make new chat message type you need to:
        // 1. Make a class that implements {@link ChatMessage}.
        // 2. Add it to {@link ChatMessageType}.
        // Only the first point is possible to do in the external code.

        return ChatMessageType.AI;
    }

    @Override
    @Deprecated
    public String text() {
        return text;
    }

    public String getText() {
        return text;
    }
}
