package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.List;

import dev.langchain4j.data.message.ChatMessage;

public interface ChatHistoryImplementation {
    List<ChatMessage> loadMessagesForEntry(Path bibDatabasePath, String citationKey);
    void storeMessagesForEntry(Path bibDatabasePath, String citationKey, List<ChatMessage> messages);
}
