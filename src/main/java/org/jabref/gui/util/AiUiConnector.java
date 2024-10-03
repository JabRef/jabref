package org.jabref.gui.util;

import org.jabref.gui.ai.chatting.chathistory.ChatHistoryService;
import org.jabref.logic.ai.AiService;
import org.jabref.model.database.BibDatabaseContext;

public class AiUiConnector {
    public static void setupDatabase(AiService aiService, ChatHistoryService chatHistoryService, BibDatabaseContext context) {
        chatHistoryService.setupDatabase(context);
        aiService.getIngestionService().setupDatabase(context);
        aiService.getSummariesService().setupDatabase(context);
    }
}
