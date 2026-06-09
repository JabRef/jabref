package org.jabref.logic.ai.chatting.repositories;

import java.util.List;

import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;

// [impl->req~ai.chat.entries.history-storage~1]
// [impl->req~ai.chat.groups.history-storage~1]
public interface ChatHistoryRepository {
    void addMessage(ChatIdentifier chatIdentifier, ChatMessage chatMessage);

    void deleteMessage(ChatIdentifier chatIdentifier, String id);

    void clear(ChatIdentifier chatIdentifier);

    List<ChatMessage> getAllMessages(ChatIdentifier chatIdentifier);

    boolean isEmpty(ChatIdentifier chatIdentifier);

    int size(ChatIdentifier chatIdentifier);
}
