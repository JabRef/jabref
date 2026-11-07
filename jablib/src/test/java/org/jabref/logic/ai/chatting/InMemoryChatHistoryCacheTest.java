package org.jabref.logic.ai.chatting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InMemoryChatHistoryCacheTest {

    private static final String LIBRARY_ID = "test-lib-id";

    private FakeChatHistoryRepository fakeRepository;
    private InMemoryChatHistoryCache cache;

    private BibDatabaseContext databaseContext;

    @BeforeEach
    void setUp() {
        fakeRepository = new FakeChatHistoryRepository();
        cache = new InMemoryChatHistoryCache(fakeRepository, 30);

        MetaData metaData = new MetaData();
        metaData.setAiLibraryId(LIBRARY_ID);
        BibDatabase database = new BibDatabase();
        databaseContext = new BibDatabaseContext(database, metaData);
    }

    @AfterEach
    void tearDown() {
        cache.close();
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
    void addedMessageIsPersistedBeforeClose() throws InterruptedException {
        BibEntry entry = new BibEntry().withCitationKey("Write2024");
        databaseContext.getDatabase().insertEntry(entry);

        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("written through"));

        assertTrue(fakeRepository.awaitCommit());

        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Write2024");
        assertEquals(List.of("written through"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
    }

    @Test
    void deleteAndRegenerateArePersisted() throws InterruptedException {
        BibEntry entry = new BibEntry().withCitationKey("Edit2024");
        databaseContext.getDatabase().insertEntry(entry);
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Edit2024");

        var history = cache.getForEntry(databaseContext, entry);
        ChatMessage first = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "first");
        ChatMessage second = ChatMessage.userMessage(Instant.ofEpochMilli(2000), "second");
        ChatMessage third = ChatMessage.userMessage(Instant.ofEpochMilli(3000), "third");
        history.addAll(first, second, third);

        ChatHistoryUtils.delete(history, third.id());
        assertTrue(fakeRepository.awaitCommit());
        assertEquals(List.of("first", "second"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());

        ChatHistoryUtils.regenerate(history, second.id());
        assertTrue(fakeRepository.awaitCommit());
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
    void addedMessageIsCommitted() throws InterruptedException {
        BibEntry entry = new BibEntry().withCitationKey("Commit2024");
        databaseContext.getDatabase().insertEntry(entry);

        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("durable"));

        assertTrue(fakeRepository.awaitCommit());
        assertEquals(1, fakeRepository.commits);
    }

    @Test
    void rapidChangesAreCommittedTogether() throws InterruptedException {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 250);

        BibEntry entry = new BibEntry().withCitationKey("Coalesced2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);

        history.add(ChatMessage.userMessage("first"));
        history.add(ChatMessage.userMessage("second"));

        assertEquals(0, fakeRepository.commits);
        assertTrue(fakeRepository.awaitCommit());
        assertEquals(1, fakeRepository.commits);
        assertEquals(List.of("first", "second"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Coalesced2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void batchedReplacementAndPermutationArePersisted() {
        BibEntry entry = new BibEntry().withCitationKey("Batched2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);
        ChatMessage first = ChatMessage.userMessage(Instant.ofEpochMilli(1000), "first");
        ChatMessage second = ChatMessage.userMessage(Instant.ofEpochMilli(2000), "second");

        history.setAll(first, second);
        history.sort(Comparator.comparing(ChatMessage::content).reversed());
        cache.close();

        assertEquals(List.of("second", "first"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Batched2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void changingChatDoesNotWaitForStorage() throws InterruptedException, ExecutionException, TimeoutException {
        BibEntry entry = new BibEntry().withCitationKey("Background2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);
        fakeRepository.blockCommits = true;

        try {
            CompletableFuture.runAsync(() -> history.add(ChatMessage.userMessage("background")))
                             .get(3, TimeUnit.SECONDS);
            assertTrue(fakeRepository.commitStarted.await(3, TimeUnit.SECONDS));
            assertEquals(0, fakeRepository.commits);
        } finally {
            fakeRepository.blockCommits = false;
            fakeRepository.commitPermit.release();
        }

        assertTrue(fakeRepository.awaitCommit());
    }

    @Test
    void persistedHistoryIsLoadedAgainAfterRemovalFromCache() throws InterruptedException {
        BibEntry entry = new BibEntry().withCitationKey("Reload2024");
        databaseContext.getDatabase().insertEntry(entry);

        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("stored"));
        assertTrue(fakeRepository.awaitCommit());

        cache.removeEntry(entry);

        assertEquals(List.of("stored"), cache.getForEntry(databaseContext, entry).stream().map(ChatMessage::content).toList());
    }

    @Test
    void lateResponseToPreviouslyLoadedEntryChatIsPersisted() {
        BibEntry entry = new BibEntry().withCitationKey("Closed2024");
        databaseContext.getDatabase().insertEntry(entry);

        var history = cache.getForEntry(databaseContext, entry);
        BibEntry nextEntry = new BibEntry().withCitationKey("Next2024");
        databaseContext.getDatabase().insertEntry(nextEntry);
        cache.getForEntry(databaseContext, nextEntry);
        history.add(ChatMessage.userMessage("late response"));

        cache.close();

        assertEquals(List.of("late response"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Closed2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void pendingChangesArePreservedWhenAnotherEntryChatLoads() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000);
        BibEntry entry = new BibEntry().withCitationKey("PendingClosed2024");
        databaseContext.getDatabase().insertEntry(entry);

        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("accepted before switching"));
        BibEntry nextEntry = new BibEntry().withCitationKey("Next2024");
        databaseContext.getDatabase().insertEntry(nextEntry);
        cache.getForEntry(databaseContext, nextEntry);
        cache.close();

        assertEquals(List.of("accepted before switching"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "PendingClosed2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void editsDuringCommitAreQueuedWithoutChangingTheRunningWrite() throws InterruptedException, ExecutionException, TimeoutException {
        BibEntry entry = new BibEntry().withCitationKey("Concurrent2024");
        databaseContext.getDatabase().insertEntry(entry);
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Concurrent2024");
        var history = cache.getForEntry(databaseContext, entry);
        fakeRepository.blockCommits = true;

        try {
            history.add(ChatMessage.userMessage("first"));
            assertTrue(fakeRepository.commitStarted.await(3, TimeUnit.SECONDS));

            CompletableFuture.runAsync(() -> {
                history.setAll(ChatMessage.userMessage("second"), ChatMessage.userMessage("third"));
                history.sort(Comparator.comparing(ChatMessage::content).reversed());
                history.removeLast();
            }).get(3, TimeUnit.SECONDS);

            assertEquals(List.of("first"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
        } finally {
            fakeRepository.blockCommits = false;
            fakeRepository.commitPermit.release();
        }

        cache.close();
        assertEquals(List.of("third"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
    }

    @Test
    void deletedEntryIsNotPersistedByDelayedSave() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000);
        BibEntry entry = new BibEntry().withCitationKey("Deleted2024");
        databaseContext.getDatabase().insertEntry(entry);

        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("not persisted"));
        databaseContext.getDatabase().removeEntry(entry);
        cache.close();

        assertEquals(List.of(), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Deleted2024")));
    }

    @Test
    void clearingChatThatBecameValidAfterLoadIsPersisted() throws InterruptedException {
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Late2024");
        BibEntry entry = new BibEntry().withCitationKey("Late2024");
        BibEntry duplicate = new BibEntry().withCitationKey("Late2024");
        databaseContext.getDatabase().insertEntries(entry, duplicate);

        var history = cache.getForEntry(databaseContext, entry);
        databaseContext.getDatabase().removeEntry(duplicate);

        history.add(ChatMessage.userMessage("late"));
        assertTrue(fakeRepository.awaitCommit());
        assertEquals(1, fakeRepository.getAllMessages(id).size());

        history.clear();
        assertTrue(fakeRepository.awaitCommit());
        assertTrue(fakeRepository.getAllMessages(id).isEmpty());
    }

    @Test
    void renamingTwiceLeavesNoStaleChat() {
        BibEntry entry = new BibEntry().withCitationKey("A");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("first"));

        entry.setCitationKey("B");
        history.add(ChatMessage.userMessage("second"));
        entry.setCitationKey("C");
        history.add(ChatMessage.userMessage("third"));

        cache.close();

        assertEquals(List.of(), fakeRepository.getAllMessages(new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "A")));
        assertEquals(List.of(), fakeRepository.getAllMessages(new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "B")));
        assertEquals(List.of("first", "second", "third"), fakeRepository.getAllMessages(new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "C")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void retainedListDoesNotWriteAfterRemovalOrClose() {
        BibEntry entry = new BibEntry().withCitationKey("Gone2024");
        databaseContext.getDatabase().insertEntry(entry);
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Gone2024");

        var removed = cache.getForEntry(databaseContext, entry);
        cache.removeEntry(entry);
        removed.add(ChatMessage.userMessage("after removal"));
        assertTrue(fakeRepository.getAllMessages(id).isEmpty());

        var retained = cache.getForEntry(databaseContext, entry);
        cache.close();
        retained.add(ChatMessage.userMessage("after close"));
        assertTrue(fakeRepository.getAllMessages(id).isEmpty());
    }

    @Test
    void storageFailureKeepsHistoryAndRetriesOnNextChange() throws InterruptedException {
        BibEntry entry = new BibEntry().withCitationKey("Failing2024");
        databaseContext.getDatabase().insertEntry(entry);
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Failing2024");

        var history = cache.getForEntry(databaseContext, entry);
        fakeRepository.failing = true;
        history.add(ChatMessage.userMessage("first"));

        assertTrue(fakeRepository.awaitFailedAttempt());

        assertEquals(List.of("first"), history.stream().map(ChatMessage::content).toList());
        assertEquals(List.of(), fakeRepository.getAllMessages(id));

        fakeRepository.failing = false;
        history.add(ChatMessage.userMessage("second"));

        assertTrue(fakeRepository.awaitCommit());

        assertEquals(List.of("first", "second"), fakeRepository.getAllMessages(id).stream().map(ChatMessage::content).toList());
    }

    @Test
    void failedWriteIsRolledBackSoAnotherChatCannotCommitIt() throws InterruptedException {
        BibEntry first = new BibEntry().withCitationKey("First2024");
        BibEntry second = new BibEntry().withCitationKey("Second2024");
        databaseContext.getDatabase().insertEntries(first, second);
        ChatIdentifier firstId = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "First2024");

        var firstHistory = cache.getForEntry(databaseContext, first);
        firstHistory.add(ChatMessage.userMessage("kept"));

        assertTrue(fakeRepository.awaitCommit());

        fakeRepository.failing = true;
        firstHistory.add(ChatMessage.userMessage("lost in storage"));
        assertTrue(fakeRepository.awaitFailedAttempt());
        fakeRepository.failing = false;

        // A different chat commits the shared store; the half-written first chat must not ride along
        cache.getForEntry(databaseContext, second).add(ChatMessage.userMessage("unrelated"));

        assertTrue(fakeRepository.awaitCommit());

        assertEquals(List.of("kept"), fakeRepository.getAllMessages(firstId).stream().map(ChatMessage::content).toList());
    }

    @Test
    void deletedDuplicateDoesNotOverwriteTheSurvivingChat() {
        ChatIdentifier id = new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Twin2024");
        BibEntry survivor = new BibEntry().withCitationKey("Twin2024");
        BibEntry duplicate = new BibEntry().withCitationKey("Twin2024");
        databaseContext.getDatabase().insertEntries(survivor, duplicate);

        var duplicateHistory = cache.getForEntry(databaseContext, duplicate);
        databaseContext.getDatabase().removeEntry(duplicate);

        // A late AI response still appends to the deleted duplicate's retained history
        duplicateHistory.add(ChatMessage.userMessage("belongs to the deleted duplicate"));

        assertEquals(List.of(), fakeRepository.getAllMessages(id));
    }

    @Test
    void messagesAfterEntryRemovalAreNotPersisted() throws InterruptedException {
        BibEntry entry = new BibEntry().withCitationKey("Deleted2024");
        databaseContext.getDatabase().insertEntry(entry);

        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("persisted"));

        assertTrue(fakeRepository.awaitCommit());

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

    @Test
    void deletedGroupIsNotPersistedByDelayedSave() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000);
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("Deleted group"));
        databaseContext.getMetaData().setGroups(root);
        var history = cache.getForGroup(databaseContext, root);

        history.add(ChatMessage.userMessage("not persisted"));
        databaseContext.getMetaData().clearGroups();
        cache.close();

        assertEquals(List.of(), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_GROUP, "Deleted group")));
    }

    @Test
    void entryDeletedWhileAnotherChatCommitsIsNotWritten() throws InterruptedException {
        BibEntry first = new BibEntry().withCitationKey("Blocking2024");
        BibEntry deleted = new BibEntry().withCitationKey("Queued2024");
        databaseContext.getDatabase().insertEntries(first, deleted);
        var firstHistory = cache.getForEntry(databaseContext, first);
        var deletedHistory = cache.getForEntry(databaseContext, deleted);
        fakeRepository.blockCommits = true;

        try {
            firstHistory.add(ChatMessage.userMessage("kept"));
            assertTrue(fakeRepository.commitStarted.await(3, TimeUnit.SECONDS));
            deletedHistory.add(ChatMessage.userMessage("not persisted"));
            databaseContext.getDatabase().removeEntry(deleted);
            deletedHistory.add(ChatMessage.userMessage("late response"));
        } finally {
            fakeRepository.blockCommits = false;
            fakeRepository.commitPermit.release();
        }

        cache.close();
        assertEquals(List.of(), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Queued2024")));
    }

    @Test
    void loadingAnotherChatDoesNotWaitForStorageOrBlockEdits() throws InterruptedException, ExecutionException, TimeoutException {
        BibEntry first = new BibEntry().withCitationKey("First2024");
        BibEntry second = new BibEntry().withCitationKey("Second2024");
        databaseContext.getDatabase().insertEntries(first, second);
        var firstHistory = cache.getForEntry(databaseContext, first);
        fakeRepository.blockReads = true;

        try {
            var loading = cache.getForEntryAsync(databaseContext, second);
            assertTrue(fakeRepository.readStarted.await(3, TimeUnit.SECONDS));
            CompletableFuture.runAsync(() -> firstHistory.add(ChatMessage.userMessage("while loading")))
                             .get(3, TimeUnit.SECONDS);
            assertFalse(loading.isDone());
        } finally {
            fakeRepository.blockReads = false;
            fakeRepository.readPermit.release();
        }

        cache.close();
        assertEquals(List.of("while loading"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "First2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void cancellingOneLoadDoesNotCancelAnotherConsumer() throws InterruptedException, ExecutionException, TimeoutException {
        BibEntry entry = new BibEntry().withCitationKey("Shared2024");
        databaseContext.getDatabase().insertEntry(entry);
        fakeRepository.blockReads = true;
        CompletableFuture<?> cancelled = cache.getForEntryAsync(databaseContext, entry);
        var retained = cache.getForEntryAsync(databaseContext, entry);

        try {
            assertTrue(fakeRepository.readStarted.await(3, TimeUnit.SECONDS));
            cancelled.cancel(false);
        } finally {
            fakeRepository.blockReads = false;
            fakeRepository.readPermit.release();
        }

        assertSame(retained.get(3, TimeUnit.SECONDS), cache.getForEntry(databaseContext, entry));
    }

    @Test
    void cacheRetentionIsBoundedWhileLiveHistoriesRemainShared() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000, 1);
        BibEntry first = new BibEntry().withCitationKey("First2024");
        BibEntry second = new BibEntry().withCitationKey("Second2024");
        databaseContext.getDatabase().insertEntries(first, second);
        var firstHistory = cache.getForEntry(databaseContext, first);
        var secondHistory = cache.getForEntry(databaseContext, second);

        assertEquals(1, cache.retainedChatCount());
        assertSame(firstHistory, cache.getForEntry(databaseContext, first));
        assertSame(secondHistory, cache.getForEntry(databaseContext, second));
    }

    @Test
    void growingHistoryUpdatesItsCacheWeightWithoutLosingPendingChanges() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000, 1);
        BibEntry entry = new BibEntry().withCitationKey("Growing2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);

        history.addAll(ChatMessage.userMessage("first"), ChatMessage.userMessage("second"));

        assertEquals(0, cache.retainedChatCount());
        assertSame(history, cache.getForEntry(databaseContext, entry));
        cache.close();
        assertEquals(List.of("first", "second"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Growing2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void historyWithoutAnIdentifierSurvivesCachePressureUntilShutdown() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000, 0);
        BibEntry entry = new BibEntry();
        databaseContext.getDatabase().insertEntry(entry);
        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("unsaved"));
        assertEquals(0, cache.retainedChatCount());

        entry.setCitationKey("Valid2024");
        cache.close();

        assertEquals(List.of("unsaved"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Valid2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void failedWriteSurvivesCachePressureAndRetriesAtShutdown() throws InterruptedException {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 30, 0);
        BibEntry entry = new BibEntry().withCitationKey("Retry2024");
        databaseContext.getDatabase().insertEntry(entry);
        fakeRepository.failing = true;
        cache.getForEntry(databaseContext, entry).add(ChatMessage.userMessage("retry"));
        assertTrue(fakeRepository.awaitFailedAttempt());
        assertEquals(0, cache.retainedChatCount());

        fakeRepository.failing = false;
        cache.close();

        assertEquals(List.of("retry"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Retry2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void releasingLibraryDropsRetentionAndPreservesPendingEditsAndLateResponses() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000);
        BibEntry entry = new BibEntry().withCitationKey("Released2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("pending"));

        cache.releaseLibrary(databaseContext);
        assertEquals(0, cache.retainedChatCount());
        history.add(ChatMessage.userMessage("late response"));
        assertEquals(0, cache.retainedChatCount());
        cache.close();

        assertEquals(List.of("pending", "late response"), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Released2024")).stream().map(ChatMessage::content).toList());
    }

    @Test
    void releasingLibraryKeepsAnotherLibraryCached() {
        BibDatabaseContext otherContext = new BibDatabaseContext();
        otherContext.getMetaData().setAiLibraryId("other-library");
        BibEntry entry = new BibEntry().withCitationKey("Released2024");
        BibEntry otherEntry = new BibEntry().withCitationKey("Retained2024");
        databaseContext.getDatabase().insertEntry(entry);
        otherContext.getDatabase().insertEntry(otherEntry);
        cache.getForEntry(databaseContext, entry);
        var otherHistory = cache.getForEntry(otherContext, otherEntry);

        cache.releaseLibrary(databaseContext);

        assertEquals(1, cache.retainedChatCount());
        assertSame(otherHistory, cache.getForEntry(otherContext, otherEntry));
    }

    @Test
    void unchangedLibraryDoesNotRepeatIdentifierValidationOnChatEdits() {
        BibDatabase database = spy(databaseContext.getDatabase());
        databaseContext = new BibDatabaseContext(database, databaseContext.getMetaData());
        BibEntry entry = new BibEntry().withCitationKey("Indexed2024");
        database.insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);

        history.add(ChatMessage.userMessage("first"));
        history.add(ChatMessage.userMessage("second"));
        history.removeLast();

        verify(database, times(1)).getNumberOfCitationKeyOccurrences("Indexed2024");
    }

    @Test
    void addingDuplicateInvalidatesIdentifierValidation() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000);
        BibEntry entry = new BibEntry().withCitationKey("Duplicate2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);

        databaseContext.getDatabase().insertEntry(new BibEntry().withCitationKey("Duplicate2024"));
        history.add(ChatMessage.userMessage("not persisted"));
        cache.close();

        assertEquals(List.of(), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Duplicate2024")));
    }

    @Test
    void changingLibraryIdInvalidatesIdentifierValidation() {
        cache.close();
        cache = new InMemoryChatHistoryCache(fakeRepository, 60_000);
        BibEntry entry = new BibEntry().withCitationKey("Moved2024");
        databaseContext.getDatabase().insertEntry(entry);
        var history = cache.getForEntry(databaseContext, entry);
        history.add(ChatMessage.userMessage("first"));

        databaseContext.getMetaData().setAiLibraryId("new-library");
        history.add(ChatMessage.userMessage("second"));
        cache.close();

        assertEquals(List.of(), fakeRepository.getAllMessages(
                new ChatIdentifier(LIBRARY_ID, ChatType.WITH_ENTRY, "Moved2024")));
        assertEquals(List.of("first", "second"), fakeRepository.getAllMessages(
                new ChatIdentifier("new-library", ChatType.WITH_ENTRY, "Moved2024")).stream().map(ChatMessage::content).toList());
    }

    /// Minimal in-memory implementation of [ChatHistoryRepository] used for testing.
    private static class FakeChatHistoryRepository implements ChatHistoryRepository {

        private final Map<String, List<ChatMessage>> store = new HashMap<>();
        private Map<String, List<ChatMessage>> committed = new HashMap<>();
        private int commits;
        private volatile boolean failing;
        private volatile boolean blockCommits;
        private volatile boolean blockReads;
        private final CountDownLatch readStarted = new CountDownLatch(1);
        private final Semaphore readPermit = new Semaphore(0);
        private CountDownLatch commitLatch = new CountDownLatch(1);
        private final CountDownLatch commitStarted = new CountDownLatch(1);
        private CountDownLatch failedAttemptLatch = new CountDownLatch(1);
        private final Semaphore commitPermit = new Semaphore(0);

        private boolean awaitCommit() throws InterruptedException {
            boolean committed = commitLatch.await(3, TimeUnit.SECONDS);
            commitLatch = new CountDownLatch(1);
            return committed;
        }

        private boolean awaitFailedAttempt() throws InterruptedException {
            boolean attempted = failedAttemptLatch.await(3, TimeUnit.SECONDS);
            failedAttemptLatch = new CountDownLatch(1);
            return attempted;
        }

        private String key(ChatIdentifier id) {
            return id.libraryId() + "/" + id.chatType() + "/" + id.chatName();
        }

        @Override
        public void addMessage(ChatIdentifier chatIdentifier, ChatMessage chatMessage) {
            if (failing) {
                failedAttemptLatch.countDown();
                throw new IllegalStateException("storage is unavailable");
            }
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
            if (blockReads) {
                readStarted.countDown();
                readPermit.acquireUninterruptibly();
            }
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

        @Override
        public void commit() {
            if (blockCommits) {
                commitStarted.countDown();
                commitPermit.acquireUninterruptibly();
            }
            commits++;
            committed = deepCopy(store);
            commitLatch.countDown();
        }

        @Override
        public void rollback() {
            store.clear();
            store.putAll(deepCopy(committed));
        }

        private static Map<String, List<ChatMessage>> deepCopy(Map<String, List<ChatMessage>> source) {
            Map<String, List<ChatMessage>> copy = new HashMap<>();
            source.forEach((key, messages) -> copy.put(key, new ArrayList<>(messages)));
            return copy;
        }
    }
}
