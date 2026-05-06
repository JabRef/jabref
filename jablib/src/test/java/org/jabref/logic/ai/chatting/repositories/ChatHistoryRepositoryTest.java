package org.jabref.logic.ai.chatting.repositories;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->req~ai.chat.entries.history-storage~1]
// [utest->req~ai.chat.groups.history-storage~1]
class ChatHistoryRepositoryTest {

    private static final ChatIdentifier IDENTIFIER = new ChatIdentifier("lib-1", ChatType.WITH_ENTRY, "Smith2024");

    @TempDir
    private static Path tempDir;

    static List<ChatHistoryRepository> repositories() {
        return List.of(
                new MVStoreChatHistoryRepository(tempDir.resolve("chat-test.mv"), notification -> {
                })
        );
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void addAndRetrieveMessage(ChatHistoryRepository repo) {
        repo.clear(IDENTIFIER);

        ChatMessage msg = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "hello world");
        repo.addMessage(IDENTIFIER, msg);

        List<ChatMessage> all = repo.getAllMessages(IDENTIFIER);
        assertEquals(1, all.size());
        assertEquals("hello world", all.getFirst().content());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void messagesAreReturnedSortedByTimestamp(ChatHistoryRepository repo) {
        repo.clear(IDENTIFIER);

        ChatMessage first = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "first");
        ChatMessage second = ChatMessage.aiMessage("second", List.of());

        // Add in reverse order to verify sorting
        repo.addMessage(IDENTIFIER, new ChatMessage(second.id(), Instant.ofEpochMilli(2000), second.role(), second.content(), second.relevantInformation()));
        repo.addMessage(IDENTIFIER, new ChatMessage(first.id(), Instant.ofEpochMilli(1000), first.role(), first.content(), first.relevantInformation()));

        List<ChatMessage> all = repo.getAllMessages(IDENTIFIER);
        assertEquals("first", all.getFirst().content());
        assertEquals("second", all.get(1).content());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void deleteMessageRemovesCorrectEntry(ChatHistoryRepository repo) {
        repo.clear(IDENTIFIER);

        ChatMessage keep = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "keep me");
        ChatMessage remove = ChatMessage.userMessage(Instant.ofEpochMilli(2000), "remove me");
        repo.addMessage(IDENTIFIER, keep);
        repo.addMessage(IDENTIFIER, remove);

        repo.deleteMessage(IDENTIFIER, remove.id());

        List<ChatMessage> all = repo.getAllMessages(IDENTIFIER);
        assertEquals(1, all.size());
        assertEquals("keep me", all.getFirst().content());
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void clearRemovesAllMessages(ChatHistoryRepository repo) {
        repo.clear(IDENTIFIER);

        repo.addMessage(IDENTIFIER, ChatMessage.userMessage("msg1"));
        repo.addMessage(IDENTIFIER, ChatMessage.userMessage("msg2"));
        repo.clear(IDENTIFIER);

        assertTrue(repo.isEmpty(IDENTIFIER));
        assertEquals(0, repo.size(IDENTIFIER));
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void isEmptyReturnsTrueForNewIdentifier(ChatHistoryRepository repo) {
        ChatIdentifier fresh = new ChatIdentifier("lib-fresh", ChatType.WITH_ENTRY, "FreshKey");
        repo.clear(fresh);

        assertTrue(repo.isEmpty(fresh));
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void isEmptyReturnsFalseAfterAddingMessage(ChatHistoryRepository repo) {
        repo.clear(IDENTIFIER);

        repo.addMessage(IDENTIFIER, ChatMessage.userMessage("hi"));

        assertFalse(repo.isEmpty(IDENTIFIER));
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void sizeReflectsNumberOfMessages(ChatHistoryRepository repo) {
        repo.clear(IDENTIFIER);

        repo.addMessage(IDENTIFIER, ChatMessage.userMessage("a"));
        repo.addMessage(IDENTIFIER, ChatMessage.userMessage("b"));
        repo.addMessage(IDENTIFIER, ChatMessage.aiMessage("c", List.of()));

        assertEquals(3, repo.size(IDENTIFIER));
    }

    @ParameterizedTest
    @MethodSource("repositories")
    void differentIdentifiersAreSeparated(ChatHistoryRepository repo) {
        ChatIdentifier id1 = new ChatIdentifier("lib-1", ChatType.WITH_ENTRY, "Alpha");
        ChatIdentifier id2 = new ChatIdentifier("lib-1", ChatType.WITH_ENTRY, "Beta");
        repo.clear(id1);
        repo.clear(id2);

        repo.addMessage(id1, ChatMessage.userMessage("alpha message"));
        repo.addMessage(id2, ChatMessage.userMessage("beta message"));

        assertEquals(1, repo.size(id1));
        assertEquals(1, repo.size(id2));
        assertEquals("alpha message", repo.getAllMessages(id1).getFirst().content());
        assertEquals("beta message", repo.getAllMessages(id2).getFirst().content());
    }
}
