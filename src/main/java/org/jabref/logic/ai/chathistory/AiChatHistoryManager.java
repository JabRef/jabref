package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.DialogService;

/**
 * This class gives you the ability to open a chat history file for a BIB database.
 * <p>
 * If the history was not opened, this class will create it for you. Otherwise, it will return already used object.
 */
public class AiChatHistoryManager {
    private final DialogService dialogService;

    private final Map<Path, BibDatabaseChatHistory> bibDatabaseChatHistoryMap = new HashMap<>();

    public AiChatHistoryManager(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public BibDatabaseChatHistory getChatHistoryForBibDatabase(Path bibDatabasePath) {
        BibDatabaseChatHistory found = bibDatabaseChatHistoryMap.get(bibDatabasePath);
        if (found != null) {
            return found;
        }

        BibDatabaseChatHistory bibDatabaseChatHistory = new BibDatabaseChatHistory(bibDatabasePath, dialogService);

        bibDatabaseChatHistoryMap.put(bibDatabasePath, bibDatabaseChatHistory);

        return bibDatabaseChatHistory;
    }

    public void close() {
        bibDatabaseChatHistoryMap.values().forEach(BibDatabaseChatHistory::close);
        bibDatabaseChatHistoryMap.clear();
    }
}
