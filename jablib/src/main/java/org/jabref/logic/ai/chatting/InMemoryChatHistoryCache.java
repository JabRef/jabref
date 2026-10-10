package org.jabref.logic.ai.chatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

import org.jabref.logic.ai.chatting.repositories.ChatHistoryRepository;
import org.jabref.model.ai.chatting.ChatIdentifier;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.chatting.ChatType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.event.GroupUpdatedEvent;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.eventbus.Subscribe;
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
    private static final int MAX_RETAINED_MESSAGES = 10_000;
    private static final int SAVE_DELAY_MILLIS = 1_000;

    @NullMarked
    private record PendingChat(ChatIdentifier identifier, long revision) {
    }

    @NullMarked
    private record ChatChange(int from, int removedSize, List<ChatMessage> added, int[] permutation) {
    }

    @NullMarked
    private class CachedChat {
        private final BibDatabaseContext context;
        private final ChatType type;
        private final Supplier<Optional<ChatIdentifier>> identifier;
        private final BooleanSupplier exists;
        private final ObservableList<ChatMessage> history = FXCollections.observableArrayList();
        private final CompletableFuture<ObservableList<ChatMessage>> loaded = new CompletableFuture<>();
        private final ListChangeListener<ChatMessage> writeThrough = change -> queueChat(this, change);
        private final AtomicBoolean persistable = new AtomicBoolean();
        // The serial storage worker owns the mirror and its durable identifier.
        private final List<ChatMessage> writerHistory = new ArrayList<>();
        private @Nullable ChatIdentifier persistedAt;
        private Optional<ChatIdentifier> currentIdentifier = Optional.empty();
        private Optional<String> currentLibraryId = Optional.empty();
        private long identifierRevision = -1;
        private long revision;
        private volatile int weight = 1;
        private boolean removed;

        private CachedChat(BibDatabaseContext context, ChatType type, Supplier<Optional<ChatIdentifier>> identifier, BooleanSupplier exists) {
            this.context = context;
            this.type = type;
            this.identifier = identifier;
            this.exists = exists;
        }
    }

    @NullMarked
    private class LibraryListener {
        private final BibDatabaseContext context;
        private long revision;

        private LibraryListener(BibDatabaseContext context) {
            this.context = context;
            context.getDatabase().registerListener(this);
            context.getMetaData().registerListener(this);
        }

        @Subscribe
        public void entriesAdded(EntriesAddedEvent event) {
            synchronized (InMemoryChatHistoryCache.this) {
                revision++;
            }
        }

        @Subscribe
        public void citationKeyChanged(FieldChangedEvent event) {
            if (event.getField().equals(InternalField.KEY_FIELD)) {
                synchronized (InMemoryChatHistoryCache.this) {
                    revision++;
                }
            }
        }

        @Subscribe
        public void entriesRemoved(EntriesRemovedEvent event) {
            synchronized (InMemoryChatHistoryCache.this) {
                revision++;
                event.getBibEntries().forEach(entry -> Optional.ofNullable(entryChats.getIfPresent(entry))
                                                               .filter(cached -> cached.context == context)
                                                               .ifPresent(_ -> removeEntry(entry)));
            }
        }

        @Subscribe
        public void groupsUpdated(GroupUpdatedEvent event) {
            synchronized (InMemoryChatHistoryCache.this) {
                groupChats.asMap().forEach((group, cached) -> {
                    if (cached.context == context) {
                        if (cached.exists.getAsBoolean()) {
                            captureChat(cached);
                        } else {
                            removeGroup(group);
                        }
                    }
                });
                scheduleSave();
            }
        }

        private void close() {
            context.getDatabase().unregisterListener(this);
            context.getMetaData().unregisterListener(this);
        }
    }

    // Weak keys use identity; weak values keep histories held by a view or AI task discoverable after eviction.
    private final Cache<BibEntry, CachedChat> entryChats = Caffeine.newBuilder().weakKeys().weakValues().build();
    private final Cache<GroupTreeNode, CachedChat> groupChats = Caffeine.newBuilder().weakKeys().weakValues().build();
    private final Cache<CachedChat, CachedChat> retainedChats;
    private final Set<CachedChat> unsavedChats = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<BibDatabaseContext, LibraryListener> libraryListeners = new IdentityHashMap<>();
    private Map<CachedChat, PendingChat> pending = new IdentityHashMap<>();
    private List<Runnable> queuedChanges = new ArrayList<>();

    private final ChatHistoryRepository repository;
    private final ScheduledThreadPoolExecutor writer = new ScheduledThreadPoolExecutor(1);
    private final int saveDelayMillis;
    private @Nullable ScheduledFuture<?> scheduledSave;

    private boolean closed;

    public InMemoryChatHistoryCache(ChatHistoryRepository repository) {
        this(repository, SAVE_DELAY_MILLIS);
    }

    InMemoryChatHistoryCache(ChatHistoryRepository repository, int saveDelayMillis) {
        this(repository, saveDelayMillis, MAX_RETAINED_MESSAGES);
    }

    InMemoryChatHistoryCache(ChatHistoryRepository repository, int saveDelayMillis, int maxRetainedMessages) {
        this.repository = repository;
        this.saveDelayMillis = saveDelayMillis;
        retainedChats = Caffeine.newBuilder()
                                .maximumWeight(maxRetainedMessages)
                                .weigher((CachedChat chat, CachedChat _) -> chat.weight)
                                .build();
        writer.setRemoveOnCancelPolicy(true);
    }

    /// Returns the chat history for `entry`. If none exists in RAM, loads from repository
    /// and caches it. The returned [ObservableList] is the primary working storage - mutations
    /// are saved to the repository after a short delay.
    ///
    /// This accessor waits for storage. UI callers should use [#getForEntryAsync(BibDatabaseContext, BibEntry)].
    ///
    /// @param databaseContext the database context for the entry (needed for persistence)
    /// @param entry           the entry to get chat history for
    /// @return the live, mutable chat history
    public ObservableList<ChatMessage> getForEntry(BibDatabaseContext databaseContext, BibEntry entry) {
        return getForEntryAsync(databaseContext, entry).join();
    }

    /// Loads an entry history on the serial storage worker. Call on the model's owning thread;
    /// apply the result on the JavaFX thread before binding it to a view.
    public synchronized CompletableFuture<ObservableList<ChatMessage>> getForEntryAsync(BibDatabaseContext databaseContext, BibEntry entry) {
        if (closed) {
            return CompletableFuture.completedFuture(FXCollections.observableArrayList());
        }
        libraryListeners.computeIfAbsent(databaseContext, LibraryListener::new);
        CachedChat cached = entryChats.asMap().compute(entry, (_, existing) -> Optional.ofNullable(existing)
                                                                                       .filter(chat -> !chat.removed)
                                                                                       .orElseGet(() -> createChat(databaseContext, ChatType.WITH_ENTRY,
                                                                                               () -> ChatIdentifier.from(databaseContext, entry),
                                                                                               () -> databaseContext.getDatabase().getEntryById(entry.getId()).filter(inDatabase -> inDatabase == entry).isPresent())));
        retainChat(cached);
        return cached.loaded.copy();
    }

    /// Returns the chat history for `group`. If none exists in RAM, loads from repository
    /// and caches it. The returned [ObservableList] is the primary working storage - mutations
    /// are saved to the repository after a short delay.
    ///
    /// This accessor waits for storage. UI callers should use [#getForGroupAsync(BibDatabaseContext, GroupTreeNode)].
    ///
    /// @param databaseContext the database context for the group (needed for persistence)
    /// @param group           the group to get chat history for
    /// @return the live, mutable chat history
    public ObservableList<ChatMessage> getForGroup(BibDatabaseContext databaseContext, GroupTreeNode group) {
        return getForGroupAsync(databaseContext, group).join();
    }

    /// Loads a group history on the serial storage worker. Call on the model's owning thread;
    /// apply the result on the JavaFX thread before binding it to a view.
    public synchronized CompletableFuture<ObservableList<ChatMessage>> getForGroupAsync(BibDatabaseContext databaseContext, GroupTreeNode group) {
        if (closed) {
            return CompletableFuture.completedFuture(FXCollections.observableArrayList());
        }
        libraryListeners.computeIfAbsent(databaseContext, LibraryListener::new);
        CachedChat cached = groupChats.asMap().compute(group, (_, existing) -> Optional.ofNullable(existing)
                                                                                       .filter(chat -> !chat.removed)
                                                                                       .orElseGet(() -> createChat(databaseContext, ChatType.WITH_GROUP,
                                                                                               () -> ChatIdentifier.from(databaseContext, group),
                                                                                               () -> databaseContext.getMetaData().getGroups().map(root -> root.containsGroup(group.getGroup())).orElse(false))));
        retainChat(cached);
        return cached.loaded.copy();
    }

    private CachedChat createChat(BibDatabaseContext context, ChatType type, Supplier<Optional<ChatIdentifier>> identifier, BooleanSupplier exists) {
        CachedChat cached = new CachedChat(context, type, identifier, exists);
        Optional<ChatIdentifier> initialIdentifier = resolveIdentifier(cached);
        writer.execute(() -> load(cached, initialIdentifier));
        return cached;
    }

    private void load(CachedChat cached, Optional<ChatIdentifier> identifier) {
        List<ChatMessage> messages;
        try {
            synchronized (repository) {
                messages = identifier.map(repository::getAllMessages).orElseGet(List::of);
                cached.writerHistory.addAll(messages);
            }
        } catch (RuntimeException e) {
            LOGGER.error("Unable to load chat history", e);
            synchronized (this) {
                forget(cached);
            }
            cached.loaded.completeExceptionally(e);
            return;
        }
        cached.history.setAll(messages);
        cached.persistedAt = identifier.orElse(null);
        synchronized (this) {
            if (!closed && !cached.removed) {
                cached.history.addListener(cached.writeThrough);
                cached.weight = Math.max(1, messages.size());
                retainChat(cached);
            }
        }
        cached.loaded.complete(cached.history);
    }

    long retainedChatCount() {
        retainedChats.cleanUp();
        return retainedChats.estimatedSize();
    }

    /// Releases library listeners and cached retention without discarding pending edits or late responses.
    /// Call on the model's owning thread when the library closes.
    public synchronized void releaseLibrary(BibDatabaseContext context) {
        Optional.ofNullable(libraryListeners.remove(context)).ifPresent(LibraryListener::close);
        entryChats.asMap().values().stream().filter(cached -> cached.context == context).forEach(this::releaseChat);
        groupChats.asMap().values().stream().filter(cached -> cached.context == context).forEach(this::releaseChat);
        scheduleSave();
    }

    private void releaseChat(CachedChat cached) {
        captureChat(cached);
        retainedChats.invalidate(cached);
    }

    private void retainChat(CachedChat cached) {
        if (libraryListeners.containsKey(cached.context)) {
            retainedChats.put(cached, cached);
        }
    }

    /// Removes the cached chat history for an entry.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeEntry(BibEntry entry) {
        Optional.ofNullable(entryChats.asMap().remove(entry)).ifPresent(this::forget);
    }

    /// Removes the cached chat history for a group.
    /// The chat history is NOT persisted before removal - it's simply discarded from RAM.
    public synchronized void removeGroup(GroupTreeNode group) {
        Optional.ofNullable(groupChats.asMap().remove(group)).ifPresent(this::forget);
    }

    private void forget(CachedChat cached) {
        cached.removed = true;
        cached.persistable.set(false);
        cached.history.removeListener(cached.writeThrough);
        pending.remove(cached);
        unsavedChats.remove(cached);
        retainedChats.invalidate(cached);
    }

    public void close() {
        Map<CachedChat, PendingChat> finalPending;
        List<Runnable> finalChanges;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            if (scheduledSave != null) {
                scheduledSave.cancel(false);
            }
            entryChats.asMap().values().forEach(this::captureChat);
            groupChats.asMap().values().forEach(this::captureChat);
            entryChats.asMap().values().forEach(cached -> cached.history.removeListener(cached.writeThrough));
            groupChats.asMap().values().forEach(cached -> cached.history.removeListener(cached.writeThrough));
            libraryListeners.values().forEach(LibraryListener::close);
            libraryListeners.clear();
            finalPending = pending;
            pending = new IdentityHashMap<>();
            finalChanges = queuedChanges;
            queuedChanges = new ArrayList<>();
        }
        var finalSave = writer.submit(() -> {
            finalChanges.forEach(Runnable::run);
            finalPending.forEach(this::saveChat);
        });
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
            synchronized (this) {
                entryChats.invalidateAll();
                groupChats.invalidateAll();
                retainedChats.invalidateAll();
                unsavedChats.clear();
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        LOGGER.debug("Finished flushing chat histories to repository");
    }

    private synchronized void queueChat(CachedChat cached, Change<? extends ChatMessage> change) {
        queueChanges(cached, change);
        cached.weight = Math.max(1, cached.history.size());
        unsavedChats.add(cached);
        retainChat(cached);
        captureChat(cached);
        scheduleSave();
    }

    private void captureChat(CachedChat cached) {
        cached.revision++;
        Optional<ChatIdentifier> identifier = resolveIdentifier(cached);
        cached.persistable.set(!cached.removed && identifier.isPresent() && cached.exists.getAsBoolean());
        if (cached.persistable.get()) {
            identifier.ifPresent(id -> pending.put(cached, new PendingChat(id, cached.revision)));
        } else {
            pending.remove(cached);
        }
    }

    private Optional<ChatIdentifier> resolveIdentifier(CachedChat cached) {
        if (cached.type == ChatType.WITH_GROUP) {
            return cached.identifier.get();
        }
        Optional<LibraryListener> library = Optional.ofNullable(libraryListeners.get(cached.context));
        Optional<String> libraryId = cached.context.getMetaData().getAiLibraryId();
        return library.filter(listener -> listener.revision == cached.identifierRevision && libraryId.equals(cached.currentLibraryId))
                      .map(_ -> cached.currentIdentifier)
                      .orElseGet(() -> {
                          cached.identifierRevision = library.map(listener -> listener.revision).orElse(-1L);
                          cached.currentLibraryId = libraryId;
                          cached.currentIdentifier = cached.identifier.get();
                          return cached.currentIdentifier;
                      });
    }

    private void scheduleSave() {
        if (closed || (pending.isEmpty() && queuedChanges.isEmpty()) || scheduledSave != null) {
            return;
        }
        scheduledSave = writer.schedule(this::savePending, saveDelayMillis, TimeUnit.MILLISECONDS);
    }

    private void savePending() {
        Map<CachedChat, PendingChat> pendingToSave;
        List<Runnable> changesToApply;
        synchronized (this) {
            scheduledSave = null;
            pendingToSave = pending;
            pending = new IdentityHashMap<>();
            changesToApply = queuedChanges;
            queuedChanges = new ArrayList<>();
        }
        changesToApply.forEach(Runnable::run);
        pendingToSave.forEach(this::saveChat);
    }

    private void queueChanges(CachedChat cached, Change<? extends ChatMessage> change) {
        List<ChatChange> changes = new ArrayList<>();
        while (change.next()) {
            int from = change.getFrom();
            int[] permutation = new int[0];
            if (change.wasPermutated()) {
                permutation = new int[change.getTo() - from];
                for (int index = from; index < change.getTo(); index++) {
                    permutation[index - from] = change.getPermutation(index) - from;
                }
            }
            List<ChatMessage> added = change.wasUpdated()
                                      ? List.copyOf(change.getList().subList(from, change.getTo()))
                                      : change.wasAdded() ? List.copyOf(change.getAddedSubList()) : List.of();
            int removedSize = change.wasUpdated() ? change.getTo() - from : change.getRemovedSize();
            changes.add(new ChatChange(from, removedSize, added, permutation));
        }
        queuedChanges.add(() -> applyChanges(cached.writerHistory, changes));
    }

    private void applyChanges(List<ChatMessage> writerHistory, List<ChatChange> changes) {
        for (ChatChange change : changes) {
            int from = change.from();
            if (change.permutation().length > 0) {
                List<ChatMessage> original = new ArrayList<>(writerHistory.subList(from, from + change.permutation().length));
                for (int index = 0; index < change.permutation().length; index++) {
                    writerHistory.set(from + change.permutation()[index], original.get(index));
                }
            } else {
                writerHistory.subList(from, from + change.removedSize()).clear();
                writerHistory.addAll(from, change.added());
            }
        }
    }

    private void saveChat(CachedChat cached, PendingChat snapshot) {
        if (cached.persistedAt == null && cached.writerHistory.isEmpty()) {
            return;
        }

        synchronized (repository) {
            if (!cached.persistable.get()) {
                return;
            }
            try {
                if (cached.persistedAt != null && !cached.persistedAt.equals(snapshot.identifier())) {
                    repository.clear(cached.persistedAt);
                }
                repository.clear(snapshot.identifier());
                cached.writerHistory.forEach(message -> repository.addMessage(snapshot.identifier(), message));
                repository.commit();
            } catch (RuntimeException e) {
                LOGGER.error("Unable to store chat history for {}", snapshot.identifier(), e);
                repository.rollback();
                synchronized (this) {
                    if (!cached.removed) {
                        unsavedChats.add(cached);
                    }
                }
                return;
            }
        }
        cached.persistedAt = snapshot.identifier();
        synchronized (this) {
            if (cached.revision == snapshot.revision()) {
                unsavedChats.remove(cached);
            }
        }
        LOGGER.debug("Flushed chat history for {} ({} messages)", snapshot.identifier(), cached.writerHistory.size());
    }
}
