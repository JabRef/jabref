package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.gui.DialogService;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
