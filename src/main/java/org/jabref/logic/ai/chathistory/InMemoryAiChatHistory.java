package org.jabref.logic.ai.chathistory;

import java.util.ArrayList;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

public class InMemoryAiChatHistory implements AiChatHistory {
    private final List<ChatMessage> chatMessages = new ArrayList<>();

    @Override
    public List<ChatMessage> getMessages() {
        return chatMessages;
    }

    @Override
    public void add(ChatMessage chatMessage) {
        chatMessages.add(chatMessage);
    }

    @Override
    public void remove(ChatMessage chatMessage) {
        chatMessages.remove(chatMessage);
    }

    @Override
    public void clear() {
        chatMessages.clear();
    }
}
