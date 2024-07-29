package org.jabref.logic.ai.chathistory;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

/**
 * This class helps in storing chat messages for a specific entry in a BIB database.
 * <p>
 * It basically wraps the {@link BibDatabaseChatHistory} with the entry's citation key.
 */
public class BibEntryChatHistory implements AiChatHistory {
    private final BibDatabaseChatHistory bibDatabaseChatHistory;
    private final String citationKey;

    public BibEntryChatHistory(BibDatabaseChatHistory bibDatabaseChatHistory, String citationKey) {
        this.bibDatabaseChatHistory = bibDatabaseChatHistory;
        this.citationKey = citationKey;
    }

    public List<ChatMessage> getMessages() {
        return bibDatabaseChatHistory.getMessagesForEntry(citationKey);
    }

    public void add(ChatMessage message) {
        bibDatabaseChatHistory.addMessage(citationKey, message);
    }

    public void clear() {
        bibDatabaseChatHistory.clearMessagesForEntry(citationKey);
    }
}
