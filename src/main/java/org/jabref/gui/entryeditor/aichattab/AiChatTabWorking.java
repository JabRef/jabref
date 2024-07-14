package org.jabref.gui.entryeditor.aichattab;

import java.nio.file.Path;
import java.util.Optional;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.ai.components.aichat.AiChatComponent;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.ai.AiChat;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.chathistory.AiChatHistory;
import org.jabref.logic.ai.chathistory.BibDatabaseChatHistoryFile;
import org.jabref.logic.ai.chathistory.InMemoryAiChatHistory;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiChatTabWorking {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiChatTabWorking.class);

    private final AiChatComponent aiChatComponent;

    public AiChatTabWorking(AiService aiService,
                            BibEntry entry,
                            BibDatabaseContext bibDatabaseContext,
                            TaskExecutor taskExecutor,
                            DialogService dialogService) {
        AiChatHistory aiChatHistory = getAiChatHistory(aiService, entry, bibDatabaseContext);
        AiChat aiChat = AiChat.forBibEntry(aiService, aiChatHistory, entry);

        aiChatComponent = new AiChatComponent(aiChat, dialogService, taskExecutor);
    }

    private static AiChatHistory getAiChatHistory(AiService aiService, BibEntry entry, BibDatabaseContext bibDatabaseContext) {
        AiChatHistory aiChatHistory;
        Optional<Path> databasePath = bibDatabaseContext.getDatabasePath();
        if (databasePath.isEmpty() || entry.getCitationKey().isEmpty()) {
            LOGGER.warn("AiChatTabWorking is constructed, but the database path is empty. Cannot store chat history");
            aiChatHistory = new InMemoryAiChatHistory();
        } else if (entry.getCitationKey().isEmpty()) {
            LOGGER.warn("AiChatTabWorking is constructed, but the entry citation key is empty. Cannot store chat history");
            aiChatHistory = new InMemoryAiChatHistory();
        } else {
            BibDatabaseChatHistoryFile bibDatabaseChatHistoryFile = aiService.getChatHistoryManager().getChatHistoryForBibDatabase(databasePath.get());
            aiChatHistory = bibDatabaseChatHistoryFile.getChatHistoryForEntry(entry.getCitationKey().get());
        }
        return aiChatHistory;
    }

    public Node getNode() {
        return aiChatComponent;
    }
}
