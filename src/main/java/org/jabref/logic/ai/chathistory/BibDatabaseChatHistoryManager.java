package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jabref.gui.DialogService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class gives you the ability to open a chat history file for a BIB database.
 * It manages the lifetime of chat history objects.
 * <p>
 * If the history was not opened, this class will create it for you. Otherwise, it will return already used object.
 */
public class BibDatabaseChatHistoryManager implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabaseChatHistoryManager.class);

    private final DialogService dialogService;

    private final Map<Path, BibDatabaseChatHistoryFile> bibDatabaseChatHistoryMap = new HashMap<>();

    public BibDatabaseChatHistoryManager(DialogService dialogService) {
        this.dialogService = dialogService;
    }

    public BibDatabaseChatHistoryFile getChatHistoryForBibDatabase(Path bibDatabasePath) {
        return bibDatabaseChatHistoryMap.computeIfAbsent(
                bibDatabasePath,
                path -> new BibDatabaseChatHistoryFile(bibDatabasePath, dialogService)
        );
    }

    @Override
    public void close() {
        bibDatabaseChatHistoryMap.values().forEach(BibDatabaseChatHistoryFile::close);
        bibDatabaseChatHistoryMap.clear();
        LOGGER.trace("All chat histories closed");
    }
}
