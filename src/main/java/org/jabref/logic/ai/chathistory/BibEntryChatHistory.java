package org.jabref.logic.ai.chathistory;

import java.util.stream.Stream;

/**
 * This class helps in storing chat messages for a specific entry in a BIB database.
 * <p>
 * It basically wraps the {@link BibDatabaseChatHistory} with the entry's citation key.
 */
public class BibEntryChatHistory {
    private final BibDatabaseChatHistory bibDatabaseChatHistory;
    private final String citationKey;

    public BibEntryChatHistory(BibDatabaseChatHistory bibDatabaseChatHistory, String citationKey) {
        this.bibDatabaseChatHistory = bibDatabaseChatHistory;
        this.citationKey = citationKey;
    }

    public Stream<ChatMessage> getAllMessages() {
        return bibDatabaseChatHistory.getAllMessagesForEntry(citationKey);
    }

    public void addMessage(ChatMessage message) {
        bibDatabaseChatHistory.addMessage(citationKey, message);
    }

    public void clearMessages() {
        bibDatabaseChatHistory.clearMessagesForEntry(citationKey);
    }
}
