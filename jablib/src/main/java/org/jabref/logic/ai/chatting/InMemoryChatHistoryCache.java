package org.jabref.logic.ai.chatting;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// An in-memory storage layer for chat history with [BibEntry]. This allows to have an AI chat even if the entry does not have a citation key or it is not unique.
///
/// Every change of a chat is written through to the on-disk storage ([ChatHistoryRepository]) as soon as the
/// chat has a valid [ChatIdentifier]; [#close()] flushes once more as a safety net.
public class InMemoryChatHistoryCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryChatHistoryCache.class);

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

    /// Where each chat history currently lives in the repository (loaded from there or written there).
    /// Absent: the chat was never persisted, so an empty history must not overwrite stored data.
    private final Map<ObservableList<ChatMessage>, ChatIdentifier> persistedAt = new IdentityHashMap<>();

    private final ChatHistoryRepository repository;

    private boolean closed;

    public InMemoryChatHistoryCache(ChatHistoryRepository repository) {
        this.repository = repository;
    }

    /// Returns the chat history for `entry`. If none exists in RAM, loads from repository
    /// and caches it. The returned [ObservableList] is the primary working storage - mutations
    /// are written through to the repository.
    ///
    /// @param databaseContext the database context for the entry (needed for persistence)
    /// @param entry           the entry to get chat history for
    /// @return the live, mutable chat history
    public synchronized ObservableList<ChatMessage> getForEntry(BibDatabaseContext databaseContext, BibEntry entry) {
        return entryChats.computeIfAbsent(entry, _ -> {
            ObservableList<ChatMessage> chatHistory = load(ChatIdentifier.from(databaseContext, entry), entry.getCitationKey().orElse("<no key>"));
            ListChangeListener<ChatMessage> writeThrough = _ -> flushEntryChat(entry, databaseContext, chatHistory);
            chatHistory.addListener(writeThrough);
            return new CachedEntryChat(databaseContext, chatHistory, writeThrough);
        }).chatHistory();
    }

    /// Returns the chat history for `group`. If none exists in RAM, loads from repository
    /// and caches it. The returned [ObservableList] is the primary working storage - mutations
    /// are written through to the repository.
    ///
    /// @param databaseContext the database context for the group (needed for persistence)
    /// @param group           the group to get chat history for
    /// @return the live, mutable chat history
    public synchronized ObservableList<ChatMessage> getForGroup(BibDatabaseContext databaseContext, GroupTreeNode group) {
        return groupChats.computeIfAbsent(group, _ -> {
            ObservableList<ChatMessage> chatHistory = load(ChatIdentifier.from(databaseContext, group), group.getName());
            ListChangeListener<ChatMessage> writeThrough = _ -> flushGroupChat(group, databaseContext, chatHistory);
            chatHistory.addListener(writeThrough);
            return new CachedGroupChat(databaseContext, group, chatHistory, writeThrough);
        }).chatHistory();
    }

    private ObservableList<ChatMessage> load(Optional<ChatIdentifier> identifier, String name) {
        if (identifier.isEmpty()) {
            LOGGER.debug("Created new in-memory chat history for {} (no valid identifier)", name);
            return FXCollections.observableArrayList();
        }
        ObservableList<ChatMessage> chatHistory = FXCollections.observableArrayList(repository.getAllMessages(identifier.get()));
        persistedAt.put(chatHistory, identifier.get());
        LOGGER.debug("Loaded chat history for {} from repository ({} messages)", name, chatHistory.size());
        return chatHistory;
    }

    /// Removes the cached chat history for an entry.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeEntry(BibEntry entry) {
        Optional.ofNullable(entryChats.remove(entry)).ifPresent(cached -> forget(cached.chatHistory(), cached.writeThrough()));
    }

    /// Removes the cached chat history for a group.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeGroup(GroupTreeNode group) {
        Optional.ofNullable(groupChats.remove(group)).ifPresent(cached -> forget(cached.chatHistory(), cached.writeThrough()));
    }

    private void forget(ObservableList<ChatMessage> chatHistory, ListChangeListener<ChatMessage> writeThrough) {
        chatHistory.removeListener(writeThrough);
        persistedAt.remove(chatHistory);
    }

    public synchronized void close() {
        LOGGER.debug("Flushing {} entry chats and {} group chats to repository",
                entryChats.size(), groupChats.size());

        entryChats.forEach((entry, cached) -> flushEntryChat(entry, cached.databaseContext(), cached.chatHistory()));
        groupChats.forEach((group, cached) -> flushGroupChat(group, cached.databaseContext(), cached.chatHistory()));

        entryChats.values().forEach(cached -> cached.chatHistory().removeListener(cached.writeThrough()));
        groupChats.values().forEach(cached -> cached.chatHistory().removeListener(cached.writeThrough()));
        closed = true;

        LOGGER.debug("Finished flushing chat histories to repository");
    }

    private synchronized void flushEntryChat(BibEntry entry, BibDatabaseContext databaseContext, ObservableList<ChatMessage> chatHistory) {
        ChatIdentifier.from(databaseContext, entry).ifPresent(identifier ->
                flushChat(databaseContext.getDatabase().getEntries().contains(entry), identifier, chatHistory, "entry"));
    }

    private synchronized void flushGroupChat(GroupTreeNode group, BibDatabaseContext databaseContext, ObservableList<ChatMessage> chatHistory) {
        ChatIdentifier.from(databaseContext, group).ifPresent(identifier ->
                flushChat(databaseContext.getMetaData().getGroups().map(g -> g.containsGroup(group.getGroup())).orElse(false), identifier, chatHistory, "group"));
    }

    /// Generic flush logic for both entry and group chats
    private void flushChat(
            boolean entityExists,
            ChatIdentifier currentIdentifier,
            ObservableList<ChatMessage> chatHistory,
            String entityType
    ) {
        // Algorithm:
        // 1. After close() or if the entity was deleted from the database, the chat history must not be saved.
        // 2. An empty chat that was never persisted (identifier invalid at load time) must not wipe stored history.
        // 3. If the chat lives under another identifier (name/key changed): clear that location first.
        // 4. Write to the current location and remember it.

        if (closed || !entityExists) {
            return;
        }

        Optional<ChatIdentifier> previousIdentifier = Optional.ofNullable(persistedAt.get(chatHistory));
        if (previousIdentifier.isEmpty() && chatHistory.isEmpty()) {
            return;
        }

        previousIdentifier.filter(previous -> !previous.equals(currentIdentifier)).ifPresent(previous -> {
            repository.clear(previous);
            LOGGER.debug("Cleared old chat history for {} {}", entityType, previous.chatName());
        });

        repository.clear(currentIdentifier);
        chatHistory.forEach(message -> repository.addMessage(currentIdentifier, message));
        repository.commit();
        persistedAt.put(chatHistory, currentIdentifier);

        LOGGER.debug("Flushed chat history for {} {} ({} messages)", entityType, currentIdentifier.chatName(), chatHistory.size());
    }
}
