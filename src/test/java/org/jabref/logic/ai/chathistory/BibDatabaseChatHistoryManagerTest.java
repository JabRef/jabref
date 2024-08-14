package org.jabref.logic.ai.chathistory;

import java.nio.file.Path;
import java.util.List;

import dev.langchain4j.data.message.AiMessage;
import org.h2.mvstore.MVStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibDatabaseChatHistoryManagerTest {
    @TempDir Path tempDir;

    private MVStore mvStore;
    private BibDatabaseChatHistoryManager bibDatabaseChatHistoryManager;
    private Path bibPath;

    @BeforeEach
    void setUp() {
        mvStore = MVStore.open(tempDir.resolve("test.mv").toString());
        bibDatabaseChatHistoryManager = new BibDatabaseChatHistoryManager(mvStore);
        bibPath = tempDir.resolve("test.bib");
    }

    private void reopen() {
        mvStore.close();
        setUp();
    }

    @AfterEach
    void tearDown() {
        mvStore.close();
    }

    @Test
    void getMessagesForEntry() {
        AiChatHistory aiChatHistory = bibDatabaseChatHistoryManager.getChatHistory(bibPath, "citationKey");
        AiMessage aiMessage = new AiMessage("contents");
        aiChatHistory.add(aiMessage);
        assertEquals(List.of(aiMessage), aiChatHistory.getMessages());

        reopen();
        aiChatHistory = bibDatabaseChatHistoryManager.getChatHistory(bibPath, "citationKey");
        assertEquals(List.of(aiMessage), aiChatHistory.getMessages());
    }
}
