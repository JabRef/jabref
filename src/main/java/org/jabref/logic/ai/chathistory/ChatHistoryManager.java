package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ChatHistoryManager {
    private final Map<Path, BibDatabaseChatHistory> bibDatabaseChatHistoryMap = new HashMap<>();

    public BibDatabaseChatHistory getChatHistoryForBibDatabase(Path bibDatabasePath) {
        BibDatabaseChatHistory found = bibDatabaseChatHistoryMap.get(bibDatabasePath);
        if (found != null) {
            return found;
        }

        // TODO: Error handling
        BibDatabaseChatHistory bibDatabaseChatHistory = new BibDatabaseChatHistory(bibDatabasePath);

        bibDatabaseChatHistoryMap.put(bibDatabasePath, bibDatabaseChatHistory);

        return bibDatabaseChatHistory;
    }

    public void close() {
        bibDatabaseChatHistoryMap.values().forEach(BibDatabaseChatHistory::close);
        bibDatabaseChatHistoryMap.clear();
    }
}
