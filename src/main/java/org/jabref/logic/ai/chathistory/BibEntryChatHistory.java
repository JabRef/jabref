package org.jabref.logic.ai.chathistory;

import java.util.stream.Stream;

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
}
