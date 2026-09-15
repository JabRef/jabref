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
            Optional<String> originalCitationKey,
            boolean loadedFromRepository,
            ObservableList<ChatMessage> chatHistory
    ) {
    }

    private record CachedGroupChat(
            BibDatabaseContext databaseContext,
            String originalGroupName,
            GroupTreeNode group,
            boolean loadedFromRepository,
            ObservableList<ChatMessage> chatHistory
    ) {
    }

    // IdentityHashMap: compares keys by reference (==), NOT by equals()/hashCode().
    private final Map<BibEntry, CachedEntryChat> entryChats = Collections.synchronizedMap(new IdentityHashMap<>());
    private final Map<GroupTreeNode, CachedGroupChat> groupChats = Collections.synchronizedMap(new IdentityHashMap<>());

    private final ChatHistoryRepository repository;

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
            ObservableList<ChatMessage> chatHistory;
            Optional<String> originalCitationKey = Optional.empty();

            Optional<ChatIdentifier> identifierOpt = ChatIdentifier.from(databaseContext, entry);
            if (identifierOpt.isPresent()) {
                chatHistory = FXCollections.observableArrayList(
                        repository.getAllMessages(identifierOpt.get())
                );

                originalCitationKey = entry.getCitationKey();
                LOGGER.debug("Loaded chat history for entry {} from repository ({} messages)",
                        originalCitationKey.orElse("<no key>"), chatHistory.size());
            } else {
                chatHistory = FXCollections.observableArrayList();
                LOGGER.debug("Created new in-memory chat history for entry {} (no valid identifier)",
                        entry.getCitationKey().orElse("<no key>"));
            }

            CachedEntryChat cached = new CachedEntryChat(databaseContext, originalCitationKey, identifierOpt.isPresent(), chatHistory);
            chatHistory.addListener((ListChangeListener<ChatMessage>) _ -> flushEntryChat(entry, cached));
            return cached;
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
            ObservableList<ChatMessage> chatHistory;
            String originalGroupName = group.getName();

            Optional<ChatIdentifier> identifierOpt = ChatIdentifier.from(databaseContext, group);
            if (identifierOpt.isPresent()) {
                chatHistory = FXCollections.observableArrayList(
                        repository.getAllMessages(identifierOpt.get())
                );

                LOGGER.debug("Loaded chat history for group {} from repository ({} messages)",
                        originalGroupName, chatHistory.size());
            } else {
                chatHistory = FXCollections.observableArrayList();

                LOGGER.debug("Created new in-memory chat history for group {} (no valid identifier)",
                        originalGroupName);
            }

            CachedGroupChat cached = new CachedGroupChat(databaseContext, originalGroupName, group, identifierOpt.isPresent(), chatHistory);
            chatHistory.addListener((ListChangeListener<ChatMessage>) _ -> flushGroupChat(group, cached));
            return cached;
        }).chatHistory();
    }

    /// Removes the cached chat history for an entry.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeEntry(BibEntry entry) {
        entryChats.remove(entry);
    }

    /// Removes the cached chat history for a group.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeGroup(GroupTreeNode group) {
        groupChats.remove(group);
    }

    public synchronized void close() {
        LOGGER.debug("Flushing {} entry chats and {} group chats to repository",
                entryChats.size(), groupChats.size());

        entryChats.forEach(this::flushEntryChat);
        groupChats.forEach(this::flushGroupChat);

        LOGGER.debug("Finished flushing chat histories to repository");
    }

    private synchronized void flushEntryChat(BibEntry entry, CachedEntryChat cached) {
        Optional<ChatIdentifier> currentIdentifierOpt = ChatIdentifier.from(cached.databaseContext(), entry);
        if (currentIdentifierOpt.isEmpty()) {
            return;
        }

        flushChat(
                cached.databaseContext.getDatabase().getEntries().contains(entry),
                currentIdentifierOpt.get(),
                cached.originalCitationKey().orElse("<empty>"),
                entry.getCitationKey().orElse("<no key>"),
                cached.loadedFromRepository(),
                cached.chatHistory(),
                "entry"
        );
    }

    private synchronized void flushGroupChat(GroupTreeNode group, CachedGroupChat cached) {
        Optional<ChatIdentifier> currentIdentifierOpt = ChatIdentifier.from(cached.databaseContext(), cached.group());
        if (currentIdentifierOpt.isEmpty()) {
            return;
        }

        flushChat(
                cached.databaseContext.getMetaData().getGroups().map(g -> g.containsGroup(group.getGroup())).orElse(false),
                currentIdentifierOpt.get(),
                cached.originalGroupName(),
                group.getName(),
                cached.loadedFromRepository(),
                cached.chatHistory(),
                "group"
        );
    }

    /// Generic flush logic for both entry and group chats
    private void flushChat(
            boolean entityExists,
            ChatIdentifier currentIdentifier,
            String originalName,
            String currentName,
            boolean loadedFromRepository,
            ObservableList<ChatMessage> chatHistory,
            String entityType
    ) {
        // Algorithm:
        // 1. If the entity was deleted from the database, the chat history must not be saved.
        // 2. An empty chat that was never loaded (identifier invalid at load time) must not wipe stored history.
        // 3. If name/key changed: clear old location first (only if old location was valid, not a placeholder)
        // 4. Write to current location (whether name/key changed or not)

        if (!entityExists) {
            return;
        }

        if (!loadedFromRepository && chatHistory.isEmpty()) {
            return;
        }

        boolean nameChanged = !originalName.equals(currentName);

        if (nameChanged && !"<empty>".equals(originalName)) {
            ChatIdentifier oldIdentifier = new ChatIdentifier(
                    currentIdentifier.libraryId(),
                    currentIdentifier.chatType(),
                    originalName
            );

            repository.clear(oldIdentifier);

            LOGGER.debug("Cleared old chat history for {} with old {}: {}",
                    entityType,
                    "entry".equals(entityType) ? "key" : "name",
                    originalName);
        }

        repository.clear(currentIdentifier);
        chatHistory.forEach(message -> repository.addMessage(currentIdentifier, message));

        if (nameChanged) {
            if ("entry".equals(entityType)) {
                LOGGER.debug("Transferred chat history from {} to {} ({} messages)",
                        originalName, currentName, chatHistory.size());
            } else {
                LOGGER.debug("Transferred chat history from {} '{}' to '{}' ({} messages)",
                        entityType, originalName, currentName, chatHistory.size());
            }
        } else {
            LOGGER.debug("Flushed chat history for {} {} ({} messages)",
                    entityType, currentName, chatHistory.size());
        }
    }
}
