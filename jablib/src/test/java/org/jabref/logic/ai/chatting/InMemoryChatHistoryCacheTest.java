package org.jabref.logic.ai.chatting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.logic.ai.chatting.util.ChatHistoryUtils;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryChatHistoryCacheTest {

    private static final String LIBRARY_ID = "test-lib-id";

    private FakeChatHistoryRepository fakeRepository;
    private InMemoryChatHistoryCache cache;

    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        fakeRepository = new FakeChatHistoryRepository();
        cache = new InMemoryChatHistoryCache(fakeRepository);

        MetaData metaData = new MetaData();
        metaData.setAiLibraryId(LIBRARY_ID);
        BibDatabase database = new BibDatabase();
        databaseContext = new BibDatabaseContext(database, metaData);
    }

    @Test
    void getForEntryReturnsNonNullList() {
        BibEntry entry = new BibEntry().withCitationKey("Smith2024");
        databaseContext.getDatabase().insertEntry(entry);

        assertNotNull(cache.getForEntry(databaseContext, entry));
    }

    @Test
    void getForEntryReturnsSameInstanceOnSecondCall() {
        BibEntry entry = new BibEntry().withCitationKey("Smith2024");
        databaseContext.getDatabase().insertEntry(entry);

        var first = cache.getForEntry(databaseContext, entry);
        var second = cache.getForEntry(databaseContext, entry);

        assertSame(first, second);
    }

    @Test
    void getForEntryLoadsExistingMessagesFromRepository() {
        BibEntry entry = new BibEntry().withCitationKey("Jones2024");
        databaseContext.getDatabase().insertEntry(entry);

        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Jones2024");
        ChatMessage msg = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "pre-existing");
        fakeRepository.addMessage(id, msg);

        var history = cache.getForEntry(databaseContext, entry);

        assertEquals(1, history.size());
        assertEquals("pre-existing", history.getFirst().content());
    }

    @Test
    void closeFlushesEntryChatsToRepository() {
        BibEntry entry = new BibEntry().withCitationKey("Flush2024");
        databaseContext.getDatabase().insertEntry(entry);

        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("new message"));

        cache.close();

        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Flush2024");
        List<ChatMessage> persisted = fakeRepository.getAllMessages(id);
        assertEquals(1, persisted.size());
        assertEquals("new message", persisted.getFirst().content());
    }

    @Test
    void addedMessageIsPersistedBeforeClose() {
        BibEntry entry = new BibEntry().withCitationKey("Write2024");
        databaseContext.getDatabase().insertEntry(entry);

        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("written through"));

        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Write2024");
        assertEquals(List.of("written through"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
    }

    @Test
    void deleteAndRegenerateArePersisted() {
        BibEntry entry = new BibEntry().withCitationKey("Edit2024");
        databaseContext.getDatabase().insertEntry(entry);
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Edit2024");

        var history = cache.getForEntry(databaseContext, entry);
        ChatMessage first = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "first");
        ChatMessage second = ChatMessage.userMessage(Instant.ofEpochMilli(2000), "second");
        ChatMessage third = ChatMessage.userMessage(Instant.ofEpochMilli(3000), "third");
        history.addAll(first, second, third);

        ChatHistoryUtils.delete(history, third.id());
        assertEquals(List.of("first", "second"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());

        ChatHistoryUtils.regenerate(history, second.id());
        assertEquals(List.of("first"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
    }

    @Test
    void invalidIdentifierAtLoadDoesNotWipeStoredHistory() {
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Dup2024");
        fakeRepository.addMessage(id, ChatMessage.userMessage("stored"));

        BibEntry entry = new BibEntry().withCitationKey("Dup2024");
        BibEntry duplicate = new BibEntry().withCitationKey("Dup2024");
        databaseContext.getDatabase().insertEntries(entry, duplicate);

        var history = cache.getForEntry(databaseContext, entry);
        assertTrue(history.isEmpty());

        databaseContext.getDatabase().removeEntry(duplicate);
        cache.close();

        assertEquals(1, fakeRepository.getAllMessages(id).size());
        assertEquals("stored", fakeRepository.getAllMessages(id).getFirst().content());
    }

    @Test
    void messagesAfterEntryRemovalAreNotPersisted() {
        BibEntry entry = new BibEntry().withCitationKey("Deleted2024");
        databaseContext.getDatabase().insertEntry(entry);

        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("persisted"));

        databaseContext.getDatabase().removeEntry(entry);
        history.add(ChatMessage.userMessage("should not be persisted"));

        cache.close();

        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Deleted2024");
        assertEquals(List.of("persisted"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
    }

    @Test
    void getForGroupReturnsSameInstanceOnSecondCall() {
        GroupTreeNode group = GroupTreeNode.fromGroup(new AllEntriesGroup("All Entries"));
        databaseContext.getMetaData().setGroups(group);

        var first = cache.getForGroup(databaseContext, group);
        var second = cache.getForGroup(databaseContext, group);

        assertSame(first, second);
    }

    @Test
    void closeFlushesGroupChatsToRepository() {
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All Entries"));
        databaseContext.getMetaData().setGroups(root);

        var history = cache.getForGroup(databaseContext, root);
        history.add(ChatMessage.userMessage("group message"));

        cache.close();

        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_GROUP, "All Entries");
        List<ChatMessage> persisted = fakeRepository.getAllMessages(id);
        assertEquals(1, persisted.size());
        assertEquals("group message", persisted.getFirst().content());
    }

    /// Minimal in-memory implementation of [ChatHistoryRepository] used for testing.
    private static class FakeChatHistoryRepository implements ChatHistoryRepository {

        private final Map<String, List<ChatMessage>> store = new HashMap<>();

        private String key(ChatIdentifier id) {
            return id.libraryId() + "/" + id.chatType() + "/" + id.chatName();
        }

        @Override
        public void addMessage(ChatIdentifier chatIdentifier, ChatMessage chatMessage) {
            store.computeIfAbsent(key(chatIdentifier), _ -> new ArrayList<>()).add(chatMessage);
        }

        @Override
        public void deleteMessage(ChatIdentifier chatIdentifier, String id) {
            store.getOrDefault(key(chatIdentifier), List.of()).removeIf(m -> m.id().equals(id));
        }

        @Override
        public void clear(ChatIdentifier chatIdentifier) {
            store.put(key(chatIdentifier), new ArrayList<>());
        }

        @Override
        public List<ChatMessage> getAllMessages(ChatIdentifier chatIdentifier) {
            return store.getOrDefault(key(chatIdentifier), List.of());
        }

        @Override
        public boolean isEmpty(ChatIdentifier chatIdentifier) {
            return getAllMessages(chatIdentifier).isEmpty();
        }

        @Override
        public int size(ChatIdentifier chatIdentifier) {
            return getAllMessages(chatIdentifier).size();
        }
    }
}
