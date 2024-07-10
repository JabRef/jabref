package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.DialogService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gives you the ability to open a chat history file for a BIB database.
 * <p>
 * If the history was not opened, this class will create it for you. Otherwise, it will return already used object.
 */
public class AiChatHistoryManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatHistoryManager.class);

    private final DialogService dialogService;

    private final Map<Path, BibDatabaseChatHistory> bibDatabaseChatHistoryMap = new HashMap<>();

    public AiChatHistoryManager(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public BibDatabaseChatHistory getChatHistoryForBibDatabase(Path bibDatabasePath) {
        return bibDatabaseChatHistoryMap.computeIfAbsent(
                bibDatabasePath,
                path -> new BibDatabaseChatHistory(bibDatabasePath, dialogService)
        );
    }

    @Override
    public void close() {
        bibDatabaseChatHistoryMap.values().forEach(BibDatabaseChatHistory::close);
        bibDatabaseChatHistoryMap.clear();
        LOGGER.trace("All chat histories closed");
    }
}
