package org.jabref.logic.ai.chathistory;

import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

/**
 * This class helps in storing chat messages for a specific entry in a BIB database.
 * <p>
 * It basically wraps the {@link BibDatabaseChatHistoryFile} with the entry's citation key.
 */
public class BibEntryChatHistory implements AiChatHistory {
    private final BibDatabaseChatHistoryFile bibDatabaseChatHistoryFile;
    private final String citationKey;

    public BibEntryChatHistory(BibDatabaseChatHistoryFile bibDatabaseChatHistoryFile, String citationKey) {
        this.bibDatabaseChatHistoryFile = bibDatabaseChatHistoryFile;
        this.citationKey = citationKey;
    }

    public List<ChatMessage> getMessages() {
        return bibDatabaseChatHistoryFile.getMessagesForEntry(citationKey);
    }

    public void add(ChatMessage message) {
        bibDatabaseChatHistoryFile.addMessage(citationKey, message);
    }

    public void clear() {
        bibDatabaseChatHistoryFile.clearMessagesForEntry(citationKey);
    }
}
