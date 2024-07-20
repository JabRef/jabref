package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

public class BibDatabaseChatHistory {
    private final Path path;
    private final BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager;

    public BibDatabaseChatHistory(Path path, BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager) {
        this.path = path;
        this.bibDatabaseChatHistoryManager = bibDatabaseChatHistoryManager;
    }

    public BibEntryChatHistory getChatHistoryForEntry(String citationKey) {
        return new BibEntryChatHistory(this, citationKey);
    }

    public List<ChatMessage> getMessagesForEntry(String citationKey) {
        return bibDatabaseChatHistoryManager.getMessagesForEntry(path, citationKey);
    }

    public synchronized void addMessage(String citationKey, ChatMessage message) {
        bibDatabaseChatHistoryManager.addMessage(path, citationKey, message);
    }

    public void clearMessagesForEntry(String citationKey) {
        bibDatabaseChatHistoryManager.clearMessagesForEntry(path, citationKey);
    }
}
