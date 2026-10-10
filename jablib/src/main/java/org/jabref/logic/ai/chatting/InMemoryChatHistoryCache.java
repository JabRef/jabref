package org.jabref.logic.ai.chatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// An in-memory storage layer for chat history with [BibEntry]. This allows to have an AI chat even if the entry does not have a citation key or it is not unique.
///
/// Changed chats are saved to [ChatHistoryRepository] after a short delay.
/// [#close()] flushes any outstanding changes.
@NullMarked
public class InMemoryChatHistoryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryChatHistoryCache.class);
    private static final int SAVE_DELAY_MILLIS = 1_000;

    private record ChatSnapshot(ChatIdentifier identifier, List<ChatMessage> messages, String entityType, BooleanSupplier canPersist) {
    }

    private record PendingChat(ChatIdentifier identifier, String entityType, BooleanSupplier canPersist) {
    }

    private record CachedEntryChat(
            BibDatabaseContext databaseContext,
            ObservableList<ChatMessage> chatHistory,
            ListChangeListener<ChatMessage> writeThrough
    ) {
    }

    private record CachedGroupChat(
            BibDatabaseContext databaseContext,
            GroupTreeNode group,
            ObservableList<ChatMessage> chatHistory,
            ListChangeListener<ChatMessage> writeThrough
    ) {
    }

    // IdentityHashMap: compares keys by reference (==), NOT by equals()/hashCode().
    private final Map<BibEntry, CachedEntryChat> entryChats = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<GroupTreeNode, CachedGroupChat> groupChats = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Set<BibEntry> openEntryChats = Collections.newSetFromMap(new IdentityHashMap<>());

    /// Where each chat history currently lives in the repository (loaded from there or written there).
    /// Absent: the chat was never persisted, so an empty history must not overwrite stored data.
    private final Map<ObservableList<ChatMessage>, ChatIdentifier> persistedAt = new IdentityHashMap<>();
    private final Map<ObservableList<ChatMessage>, List<ChatMessage>> writerHistories = new IdentityHashMap<>();
    private final Map<ObservableList<ChatMessage>, PendingChat> pending = new IdentityHashMap<>();

    private final ChatHistoryRepository repository;
    private final ScheduledThreadPoolExecutor writer = new ScheduledThreadPoolExecutor(1);
    private final int saveDelayMillis;
    private @Nullable ScheduledFuture<?> scheduledSave;

    private boolean closed;

    public InMemoryChatHistoryCache(ChatHistoryRepository repository) {
        this(repository, SAVE_DELAY_MILLIS);
    }

    InMemoryChatHistoryCache(ChatHistoryRepository repository, int saveDelayMillis) {
        this.repository = repository;
        this.saveDelayMillis = saveDelayMillis;
        writer.setRemoveOnCancelPolicy(true);
    }

    /// Returns the chat history for `entry`. If none exists in RAM, loads from repository
    /// and caches it. The returned [ObservableList] is the primary working storage - mutations
    /// are saved to the repository after a short delay.
    ///
    /// @param databaseContext the database context for the entry (needed for persistence)
    /// @param entry           the entry to get chat history for
    /// @return the live, mutable chat history
    public synchronized ObservableList<ChatMessage> getForEntry(BibDatabaseContext databaseContext, BibEntry entry) {
        openEntryChats.add(entry);
        return entryChats.computeIfAbsent(entry, _ -> {
            ObservableList<ChatMessage> chatHistory = load(ChatIdentifier.from(databaseContext, entry), entry.getCitationKey().orElse("<no key>"));
            ListChangeListener<ChatMessage> writeThrough = change -> queueEntryChat(entry, databaseContext, chatHistory, change);
            chatHistory.addListener(writeThrough);
            return new CachedEntryChat(databaseContext, chatHistory, writeThrough);
        }).chatHistory();
    }

    /// Returns the chat history for `group`. If none exists in RAM, loads from repository
    /// and caches it. The returned [ObservableList] is the primary working storage - mutations
    /// are saved to the repository after a short delay.
    ///
    /// @param databaseContext the database context for the group (needed for persistence)
    /// @param group           the group to get chat history for
    /// @return the live, mutable chat history
    public synchronized ObservableList<ChatMessage> getForGroup(BibDatabaseContext databaseContext, GroupTreeNode group) {
        return groupChats.computeIfAbsent(group, _ -> {
            ObservableList<ChatMessage> chatHistory = load(ChatIdentifier.from(databaseContext, group), group.getName());
            ListChangeListener<ChatMessage> writeThrough = change -> queueGroupChat(group, databaseContext, chatHistory, change);
            chatHistory.addListener(writeThrough);
            return new CachedGroupChat(databaseContext, group, chatHistory, writeThrough);
        }).chatHistory();
    }

    private ObservableList<ChatMessage> load(Optional<ChatIdentifier> identifier, String name) {
        if (identifier.isEmpty()) {
            LOGGER.debug("Created new in-memory chat history for {} (no valid identifier)", name);
            ObservableList<ChatMessage> chatHistory = FXCollections.observableArrayList();
            writerHistories.put(chatHistory, new ArrayList<>());
            return chatHistory;
        }
        List<ChatMessage> messages;
        synchronized (repository) {
            messages = repository.getAllMessages(identifier.get());
        }
        ObservableList<ChatMessage> chatHistory = FXCollections.observableArrayList(messages);
        writerHistories.put(chatHistory, new ArrayList<>(messages));
        persistedAt.put(chatHistory, identifier.get());
        LOGGER.debug("Loaded chat history for {} from repository ({} messages)", name, chatHistory.size());
        return chatHistory;
    }

    /// Removes the cached chat history for an entry.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeEntry(BibEntry entry) {
        openEntryChats.remove(entry);
        Optional.ofNullable(entryChats.remove(entry)).ifPresent(cached -> forget(cached.chatHistory(), cached.writeThrough()));
    }

    /// Marks an entry chat as closed while retaining its cached history for a later reload.
    public synchronized void closeEntryChat(BibEntry entry) {
        openEntryChats.remove(entry);
        Optional.ofNullable(entryChats.get(entry)).ifPresent(cached -> pending.remove(cached.chatHistory()));
    }

    /// Removes the cached chat history for a group.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeGroup(GroupTreeNode group) {
        Optional.ofNullable(groupChats.remove(group)).ifPresent(cached -> forget(cached.chatHistory(), cached.writeThrough()));
    }

    private void forget(ObservableList<ChatMessage> chatHistory, ListChangeListener<ChatMessage> writeThrough) {
        chatHistory.removeListener(writeThrough);
        pending.remove(chatHistory);
        writerHistories.remove(chatHistory);
        persistedAt.remove(chatHistory);
    }

    public void close() {
        Map<ObservableList<ChatMessage>, PendingChat> finalPending;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            if (scheduledSave != null) {
                scheduledSave.cancel(false);
            }
            entryChats.forEach((entry, cached) -> captureEntryChat(entry, cached.databaseContext(), cached.chatHistory()));
            groupChats.forEach((group, cached) -> captureGroupChat(group, cached.databaseContext(), cached.chatHistory()));
            entryChats.values().forEach(cached -> cached.chatHistory().removeListener(cached.writeThrough()));
            groupChats.values().forEach(cached -> cached.chatHistory().removeListener(cached.writeThrough()));
            finalPending = new IdentityHashMap<>(pending);
            pending.clear();
        }
        LOGGER.debug("Flushing {} entry chats and {} group chats to repository",
                entryChats.size(), groupChats.size());
        var finalSave = writer.submit(() -> saveSnapshots(snapshotPending(finalPending)));
        boolean interrupted = false;
        try {
            while (true) {
                try {
                    finalSave.get();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                    LOGGER.debug("Waiting for chat history flush after interruption", e);
                }
            }
        } catch (ExecutionException e) {
            LOGGER.error("Unable to flush chat histories", e);
        } finally {
            writer.shutdown();
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.debug("Finished flushing chat histories to repository");
    }

    private synchronized void queueEntryChat(
            BibEntry entry,
            BibDatabaseContext databaseContext,
            ObservableList<ChatMessage> chatHistory,
            Change<? extends ChatMessage> change
    ) {
        applyChange(chatHistory, change);
        captureEntryChat(entry, databaseContext, chatHistory);
        scheduleSave();
    }

    private void captureEntryChat(BibEntry entry, BibDatabaseContext databaseContext, ObservableList<ChatMessage> chatHistory) {
        if (!openEntryChats.contains(entry)) {
            return;
        }
        ChatIdentifier.from(databaseContext, entry).ifPresent(identifier ->
                // Identity, not equals: two equal duplicates must not keep each other's chat alive
                captureChat(
                        () -> isEntryChatPersistable(entry, databaseContext),
                        identifier,
                        chatHistory,
                        "entry"));
    }

    private synchronized boolean isEntryChatPersistable(BibEntry entry, BibDatabaseContext databaseContext) {
        return openEntryChats.contains(entry)
                && databaseContext.getDatabase().getEntryById(entry.getId()).filter(inDatabase -> inDatabase == entry).isPresent();
    }

    private synchronized void queueGroupChat(
            GroupTreeNode group,
            BibDatabaseContext databaseContext,
            ObservableList<ChatMessage> chatHistory,
            Change<? extends ChatMessage> change
    ) {
        applyChange(chatHistory, change);
        captureGroupChat(group, databaseContext, chatHistory);
        scheduleSave();
    }

    private void captureGroupChat(GroupTreeNode group, BibDatabaseContext databaseContext, ObservableList<ChatMessage> chatHistory) {
        ChatIdentifier.from(databaseContext, group).ifPresent(identifier ->
                captureChat(
                        () -> databaseContext.getMetaData().getGroups().map(g -> g.containsGroup(group.getGroup())).orElse(false),
                        identifier,
                        chatHistory,
                        "group"));
    }

    private void captureChat(BooleanSupplier canPersist, ChatIdentifier identifier, ObservableList<ChatMessage> chatHistory, String entityType) {
        if (canPersist.getAsBoolean()) {
            pending.put(chatHistory, new PendingChat(identifier, entityType, canPersist));
        } else {
            pending.remove(chatHistory);
        }
    }

    private void scheduleSave() {
        if (closed || pending.isEmpty() || scheduledSave != null) {
            return;
        }
        scheduledSave = writer.schedule(this::savePending, saveDelayMillis, TimeUnit.MILLISECONDS);
    }

    private void savePending() {
        Map<ObservableList<ChatMessage>, ChatSnapshot> snapshots;
        synchronized (this) {
            scheduledSave = null;
            Map<ObservableList<ChatMessage>, PendingChat> pendingToSave = new IdentityHashMap<>(pending);
            pending.clear();
            snapshots = snapshotPending(pendingToSave);
        }
        saveSnapshots(snapshots);
    }

    private synchronized Map<ObservableList<ChatMessage>, ChatSnapshot> snapshotPending(
            Map<ObservableList<ChatMessage>, PendingChat> pendingChats
    ) {
        Map<ObservableList<ChatMessage>, ChatSnapshot> snapshots = new IdentityHashMap<>();
        pendingChats.forEach((chatHistory, pendingChat) ->
                snapshots.put(chatHistory, new ChatSnapshot(
                        pendingChat.identifier(),
                        List.copyOf(writerHistories.getOrDefault(chatHistory, List.of())),
                        pendingChat.entityType(),
                        pendingChat.canPersist())));
        return snapshots;
    }

    private void applyChange(ObservableList<ChatMessage> chatHistory, Change<? extends ChatMessage> change) {
        List<ChatMessage> writerHistory = writerHistories.get(chatHistory);
        while (change.next()) {
            int from = change.getFrom();
            int to = change.getTo();
            if (change.wasPermutated()) {
                List<ChatMessage> original = new ArrayList<>(writerHistory.subList(from, to));
                List<ChatMessage> reordered = new ArrayList<>(original);
                for (int index = from; index < to; index++) {
                    reordered.set(change.getPermutation(index) - from, original.get(index - from));
                }
                writerHistory.subList(from, to).clear();
                writerHistory.addAll(from, reordered);
            } else {
                if (change.wasRemoved()) {
                    writerHistory.subList(from, from + change.getRemovedSize()).clear();
                }
                if (change.wasAdded()) {
                    writerHistory.addAll(from, change.getAddedSubList());
                }
            }
        }
    }

    private void saveSnapshots(Map<ObservableList<ChatMessage>, ChatSnapshot> snapshots) {
        snapshots.forEach(this::saveChat);
    }

    private void saveChat(ObservableList<ChatMessage> chatHistory, ChatSnapshot snapshot) {
        if (!snapshot.canPersist().getAsBoolean()) {
            return;
        }
        Optional<ChatIdentifier> previousIdentifier;
        synchronized (this) {
            previousIdentifier = Optional.ofNullable(persistedAt.get(chatHistory));
        }
        if (previousIdentifier.isEmpty() && snapshot.messages().isEmpty()) {
            return;
        }

        synchronized (repository) {
            try {
                previousIdentifier.filter(previous -> !previous.equals(snapshot.identifier())).ifPresent(repository::clear);
                repository.clear(snapshot.identifier());
                snapshot.messages().forEach(message -> repository.addMessage(snapshot.identifier(), message));
                repository.commit();
            } catch (RuntimeException e) {
                LOGGER.error("Unable to store chat history for {} {}", snapshot.entityType(), snapshot.identifier().chatName(), e);
                repository.rollback();
                return;
            }
        }
        synchronized (this) {
            if (entryChats.values().stream().anyMatch(cached -> cached.chatHistory() == chatHistory) ||
                    groupChats.values().stream().anyMatch(cached -> cached.chatHistory() == chatHistory)) {
                persistedAt.put(chatHistory, snapshot.identifier());
            }
        }
        LOGGER.debug("Flushed chat history for {} {} ({} messages)", snapshot.entityType(), snapshot.identifier().chatName(), snapshot.messages().size());
    }
}
