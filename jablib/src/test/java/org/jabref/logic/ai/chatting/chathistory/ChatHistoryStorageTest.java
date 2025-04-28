package org.jabref.logic.ai.chatting.chathistory;

import java.nio.file.Path;
import java.util.List;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class ChatHistoryStorageTest {
    @TempDir Path tempDir;

    private ChatHistoryStorage storage;

    abstract ChatHistoryStorage makeStorage(Path path);

    abstract void close(ChatHistoryStorage storage);

    @BeforeEach
    void setUp() {
        storage = makeStorage(tempDir.resolve("test.bib"));
    }

    private void reopen() {
        close(storage);
        setUp();
    }

    @AfterEach
    void tearDown() {
        close(storage);
    }

    @Test
    void entryChatHistory() {
        List<ChatMessage> messages = List.of(
                new UserMessage("hi!"),
                new AiMessage("hello!")
        );

        storage.storeMessagesForEntry(tempDir.resolve("test.bib"), "citationKey", messages);
        reopen();
        assertEquals(messages, storage.loadMessagesForEntry(tempDir.resolve("test.bib"), "citationKey"));
    }

    @Test
    void groupChatHistory() {
        List<ChatMessage> messages = List.of(
                new UserMessage("hi!"),
                new AiMessage("hello!")
        );

        storage.storeMessagesForGroup(tempDir.resolve("test.bib"), "group", messages);
        reopen();
        assertEquals(messages, storage.loadMessagesForGroup(tempDir.resolve("test.bib"), "group"));
    }
}
