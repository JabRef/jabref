package org.jabref.logic.ai.chathistory;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

public interface AiChatHistory {
    List<ChatMessage> getMessages();

    void add(ChatMessage chatMessage);

    void remove(int index);

    void clear();
}
