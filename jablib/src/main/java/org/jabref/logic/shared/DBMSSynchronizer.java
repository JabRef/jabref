package org.jabref.logic.shared;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.ConnectionRestoredEvent;
import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.SharedWriteFailedEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.logic.shared.exception.SharedEntryNotPresentException;
import org.jabref.logic.shared.notifications.FieldChange;
import org.jabref.logic.shared.notifications.Notifier;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Synchronizes the shared or local databases with their opposite side. Local changes are pushed by [EntriesEvent]
/// using Google's Guava EventBus.
///
/// Threading model:
///
/// * The `listen` methods run on the thread posting the event - the UI thread while typing - and
///   therefore never touch the database themselves. Every database access runs on `syncExecutor`,
///   a single worker, so that writes and pulls happen in the order they were queued.
/// * The local model is only mutated on the model thread (`remoteUpdateExecutor`, the UI thread in
///   the GUI): a pull fetches on the database worker and applies the result on the model thread.
/// * Multi-step apply sequences are serialized with `pullLock`, because in headless use (tests)
///   the notification listener thread applies concurrently with the caller. EventBus-dispatched
///   `listen` methods only `tryLock` and skip the work when the lock is contended - they may hold
///   EventBus monitors that the lock holder needs for posting its own events.
///
/// Connection loss: once a write finds the connection dead, the synchronizer goes offline - every
/// change is recorded in [OfflineChanges] (in memory and on disk) instead of being written, pulls
/// are skipped, and a background loop reconnects with backoff. Back online, the recorded changes
/// are written through the same optimistic lock as any other change, then everything is pulled.
public class DBMSSynchronizer implements DatabaseSynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBMSSynchronizer.class);

    private static final long INITIAL_RECONNECT_DELAY_MILLIS = 1_000L;
    private static final long MAX_RECONNECT_DELAY_MILLIS = 30_000L;

    private DatabaseConnection dbmsConnection;
    private DBMSProcessor dbmsProcessor;
    private Connection currentConnection;
    private Notifier notifier;
    private String dbName;
    private OfflineChanges offlineChanges;

    private MetaData metaData;
    private final BibDatabaseContext bibDatabaseContext;
    private final BibDatabase bibDatabase;
    private final EventBus eventBus;
    private final Character keywordSeparator;
    private final GlobalCitationKeyPatterns globalCiteKeyPattern;

    private final FieldPreferences fieldPreferences;

    private final FileUpdateMonitor fileMonitor;
    private final Path offlineChangesDirectory;

    // Buffered micro-edits; set from EventBus dispatch threads, taken by the database worker
    private final AtomicReference<BibEntry> entryWithPendingChanges = new AtomicReference<>();
    private final ReentrantLock pullLock = new ReentrantLock();
    // Cleared when the connection is found dead; set again by the reconnect loop
    private final AtomicBoolean connected = new AtomicBoolean(true);
    private volatile boolean closed;
    // Entries whose local changes were refused (see UpdateRefusedEvent) or are about to be
    // written after a reconnect: they keep their local state until written or merged - a pull
    // must not overwrite them meanwhile
    private final Set<Integer> sharedIdsInConflict = ConcurrentHashMap.newKeySet();
    // The shared metadata as last pulled or written: the merge base for metadata recorded offline
    private volatile Map<String, String> lastSharedMetaData = Map.of();
    private final String userAndHost;
    private final Executor remoteUpdateExecutor;
    private final Executor syncExecutor;
    private final TaskExecutor taskExecutor;
    private final @Nullable ExecutorService ownedSyncExecutor;

    /// What the shared database has that the local library has not. Determined on the database
    /// worker against the local ids as of the fetch, applied on the model thread.
    private record RemoteChanges(Set<Integer> removedIds, List<BibEntry> changedEntries) {
    }

    @FunctionalInterface
    private interface DatabaseWrite {
        void run() throws SQLException;
    }

    public DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                            Character keywordSeparator,
                            FieldPreferences fieldPreferences,
                            @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                            FileUpdateMonitor fileMonitor,
                            String userAndHost,
                            TaskExecutor taskExecutor) {
        // Direct executors keep everything synchronous - for tests and headless use
        this(bibDatabaseContext, keywordSeparator, fieldPreferences, globalCiteKeyPattern, fileMonitor, userAndHost, taskExecutor,
                Runnable::run, Runnable::run, Directories.getSharedDatabaseDirectory());
    }

    public DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                            Character keywordSeparator,
                            FieldPreferences fieldPreferences,
                            @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                            FileUpdateMonitor fileMonitor,
                            String userAndHost,
                            TaskExecutor taskExecutor,
                            Executor remoteUpdateExecutor) {
        // One background worker so that typing never waits for the database (which may be remote)
        this(bibDatabaseContext, keywordSeparator, fieldPreferences, globalCiteKeyPattern, fileMonitor, userAndHost, taskExecutor, remoteUpdateExecutor,
                Executors.newSingleThreadExecutor(runnable -> Thread.ofVirtual().name("JabRef - shared database writer").unstarted(runnable)),
                Directories.getSharedDatabaseDirectory());
    }

    @VisibleForTesting
    DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                     Character keywordSeparator,
                     FieldPreferences fieldPreferences,
                     @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                     FileUpdateMonitor fileMonitor,
                     String userAndHost,
                     TaskExecutor taskExecutor,
                     Executor remoteUpdateExecutor,
                     Executor syncExecutor,
                     Path offlineChangesDirectory) {
        this.taskExecutor = taskExecutor;
        this.syncExecutor = syncExecutor;
        this.ownedSyncExecutor = (syncExecutor instanceof ExecutorService executorService) ? executorService : null;
        this.bibDatabaseContext = bibDatabaseContext;
        this.bibDatabase = bibDatabaseContext.getDatabase();
        this.metaData = bibDatabaseContext.getMetaData();
        this.fieldPreferences = fieldPreferences;
        this.fileMonitor = fileMonitor;
        this.offlineChangesDirectory = offlineChangesDirectory;
        this.eventBus = new EventBus();
        this.keywordSeparator = keywordSeparator;
        this.globalCiteKeyPattern = globalCiteKeyPattern;
        this.userAndHost = userAndHost;
        this.remoteUpdateExecutor = remoteUpdateExecutor;
    }

    /// Listening method. Inserts a new [BibEntry] into shared database.
    @Subscribe
    public void listen(EntriesAddedEvent event) {
        if (!isEventSourceAccepted(event)) {
            return;
        }
        applySaveActionsToBufferedEntry();
        syncExecutor.execute(() -> {
            writeBufferedEntry();
            insertSharedEntries(event.getBibEntries());
        });
    }

    /// Listening method. Updates an existing shared [BibEntry].
    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (!isEventSourceAccepted(event)) {
            return;
        }
        BibEntry bibEntry = event.getBibEntry();
        if (event.isFiltered() || !isPresentLocalBibEntry(bibEntry)) {
            // Filtered micro-edits are accumulated here and written on the next major change or on close
            entryWithPendingChanges.set(bibEntry);
            return;
        }
        // Save actions here used to cause cursor jumping while typing (issue #5904, fixed in
        // PR #11282 by taking whitespace normalization out of the write path). A save action
        // that actually changes the value still rewrites the edited field - as in JabRef 5.x.
        // Micro-edit buffering keeps this path out of continuous typing anyway.
        applySaveActionsToBufferedEntry();
        applySaveActions(bibEntry);
        syncExecutor.execute(() -> {
            if (entryWithPendingChanges.compareAndSet(bibEntry, null)) {
                // The buffered micro-edits are part of this write, which the event does not
                // describe - other clients have to pull
                if (writeSharedEntry(bibEntry)) {
                    notifier.notifyClientsToPull();
                }
                return;
            }
            writeBufferedEntry();
            if (writeSharedEntry(bibEntry)) {
                // updateEntry refreshed the entry's version, which travels in the notification -
                // no pull is needed for receivers to stay consistent
                notifier.notifyAboutChangedField(event);
            }
        });
    }

    /// Listening method. Deletes the given list of [BibEntry] from shared database.
    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        if (!isEventSourceAccepted(event)) {
            return;
        }
        applySaveActionsToBufferedEntry();
        syncExecutor.execute(() -> {
            writeBufferedEntry();
            removeSharedEntries(event.getBibEntries());
        });
    }

    /// Listening method. Synchronizes the shared [MetaData] and applies them locally.
    @Subscribe
    public void listen(MetaDataChangedEvent event) {
        Map<String, String> serializedMetaData = MetaDataSerializer.getSerializedStringMap(event.getMetaData(), globalCiteKeyPattern);
        syncExecutor.execute(() -> writeSharedMetaData(serializedMetaData));
        // Other clients are notified through the upsert_metadata function (see DBMSProcessor.setUp)
        ifNotPullingAlready(this::doApplyMetaData);
    }

    /// Sets the table structure of shared database if needed and pulls all shared entries to the new local database.
    ///
    /// @throws DatabaseNotSupportedException if the version of shared database does not match the version of current shared database support ([DBMSProcessor]).
    public void initializeDatabases() throws DatabaseNotSupportedException, SQLException {
        if (!dbmsProcessor.checkBaseIntegrity()) {
            LOGGER.info("Integrity check failed. Fixing...");

            // This check should only be performed once on initial database setup.
            if (dbmsProcessor.databaseIsAtMostJabRef35()) {
                throw new DatabaseNotSupportedException();
            }

            // Calling dbmsProcessor.setupSharedDatabase() lets dbmsProcessor.checkBaseIntegrity() be true.
            dbmsProcessor.setupSharedDatabase();
        }

        dbmsProcessor.startNotificationListener(this);
        // Synchronously on the caller's thread: the library is not shown before this returns
        Map<String, String> sharedMetaData = dbmsProcessor.getSharedMetaData();
        RemoteChanges remoteChanges = fetchRemoteChanges();
        withPullLock(() -> {
            applyRemoteMetaData(sharedMetaData);
            applyRemoteChanges(remoteChanges);
        });
        // Changes recorded by an earlier session that lost its connection
        replayOfflineChanges(true);
    }

    /// Synchronizes the local database with shared one. Possible update types are: removal, update, or insert of a
    /// [BibEntry].
    @Override
    public void synchronizeLocalDatabase() {
        pullEntries();
    }

    /// Synchronizes the local BibEntries and applies the fetched MetaData on them.
    @Override
    public void pullChanges() {
        pullEntries();
        pullMetaData();
    }

    /// Schedules a full synchronization requested by another shared-database client.
    public void handleRemoteDatabaseChange() {
        pullEntries();
    }

    /// Schedules a metadata update received from another shared-database client.
    public void handleRemoteMetaDataChange() {
        pullMetaData();
    }

    /// Brings the local entries up to date with the shared database: fetches on the database
    /// worker, applies on the model thread. A failed fetch leaves the local library as it is.
    private void pullEntries() {
        syncExecutor.execute(() -> {
            if (!connected.get()) {
                // Everything is pulled once the connection is back
                return;
            }
            // A buffered edit is written first: if it conflicts with what is pulled, the write
            // is refused and the user merges - instead of the pull silently overwriting the edit
            writeBufferedEntry();
            RemoteChanges remoteChanges;
            try {
                remoteChanges = fetchRemoteChanges();
            } catch (SQLException e) {
                LOGGER.error("Could not fetch changes from the shared database", e);
                checkCurrentConnection();
                return;
            }
            remoteUpdateExecutor.execute(() -> withPullLock(() -> applyRemoteChanges(remoteChanges)));
        });
    }

    private void pullMetaData() {
        syncExecutor.execute(() -> {
            if (!connected.get()) {
                return;
            }
            Map<String, String> sharedMetaData;
            try {
                sharedMetaData = dbmsProcessor.getSharedMetaData();
            } catch (SQLException e) {
                LOGGER.error("Could not fetch metadata from the shared database", e);
                checkCurrentConnection();
                return;
            }
            remoteUpdateExecutor.execute(() -> withPullLock(() -> applyRemoteMetaData(sharedMetaData)));
        });
    }

    /// Database worker. Transfers only what differs: the id/version mapping plus the entries
    /// that are new or newer on the shared side.
    private RemoteChanges fetchRemoteChanges() throws SQLException {
        Map<Integer, Integer> localVersions = new HashMap<>();
        for (BibEntry entry : bibDatabase.getEntriesSnapshot()) {
            localVersions.put(entry.getSharedBibEntryData().getSharedIdAsInt(), entry.getSharedBibEntryData().getVersion());
        }
        Map<Integer, Integer> remoteVersions = dbmsProcessor.getSharedIDVersionMapping();

        List<Integer> changedIds = remoteVersions.entrySet().stream()
                                                 .filter(remote -> {
                                                     Integer localVersion = localVersions.get(remote.getKey());
                                                     return (localVersion == null) || (remote.getValue() > localVersion);
                                                 })
                                                 .map(Map.Entry::getKey)
                                                 .toList();

        // Removal candidates are taken from the same snapshot as the remote state: an entry
        // inserted locally afterwards (its insert is queued behind this fetch on the same
        // worker) is not in the snapshot and thus never mistaken for a remotely deleted one.
        // Entries without a shared id (-1) have not reached the database yet either.
        Set<Integer> removedIds = new HashSet<>(localVersions.keySet());
        removedIds.removeAll(remoteVersions.keySet());
        removedIds.remove(-1);

        return new RemoteChanges(removedIds, dbmsProcessor.partitionAndGetSharedEntries(changedIds));
    }

    /// Model thread
    private void applyRemoteChanges(RemoteChanges remoteChanges) {
        Map<Integer, BibEntry> localEntriesById = new HashMap<>();
        List<BibEntry> removedEntries = new ArrayList<>();
        for (BibEntry localEntry : bibDatabase.getEntriesSnapshot()) {
            int sharedId = localEntry.getSharedBibEntryData().getSharedIdAsInt();
            if (remoteChanges.removedIds().contains(sharedId)) {
                removedEntries.add(localEntry);
            } else {
                localEntriesById.put(sharedId, localEntry);
            }
        }
        if (!removedEntries.isEmpty()) {
            eventBus.post(new SharedEntriesNotPresentEvent(bibDatabaseContext, removedEntries));
            // remove all non-shared entries without triggering listeners
            bibDatabase.removeEntries(removedEntries, EntriesEventSource.SHARED);
        }

        List<BibEntry> entriesToInsert = new ArrayList<>();
        for (BibEntry sharedEntry : remoteChanges.changedEntries()) {
            BibEntry localEntry = localEntriesById.get(sharedEntry.getSharedBibEntryData().getSharedIdAsInt());
            if (localEntry == null) {
                entriesToInsert.add(sharedEntry);
            } else if ((sharedEntry.getSharedBibEntryData().getVersion() > localEntry.getSharedBibEntryData().getVersion())
                    && !sharedIdsInConflict.contains(sharedEntry.getSharedBibEntryData().getSharedIdAsInt())) {
                // The local entry may have moved on since the fetch (e.g. by an own write) - re-checked here
                overwriteLocalEntry(localEntry, sharedEntry);
            }
        }
        if (!entriesToInsert.isEmpty()) {
            bibDatabase.insertEntries(entriesToInsert, EntriesEventSource.SHARED);
        }
    }

    private static void overwriteLocalEntry(BibEntry localEntry, BibEntry sharedEntry) {
        localEntry.setType(sharedEntry.getType(), EntriesEventSource.SHARED);
        localEntry.getSharedBibEntryData().setVersion(sharedEntry.getSharedBibEntryData().getVersion());
        sharedEntry.getFieldMap().forEach((field, value) -> localEntry.setField(field, value, EntriesEventSource.SHARED));
        localEntry.getFields().stream()
                  .filter(field -> !sharedEntry.hasField(field))
                  .forEach(field -> localEntry.clearField(field, EntriesEventSource.SHARED));
    }

    /// Writes the merged entry of a resolved conflict. The entry is a detached copy, so the
    /// save actions may run on the caller's thread.
    @Override
    public void synchronizeSharedEntry(BibEntry bibEntry) {
        applySaveActions(bibEntry);
        syncExecutor.execute(() -> {
            if (writeSharedEntry(bibEntry)) {
                notifier.notifyClientsToPull();
            }
        });
    }

    /// Database worker.
    ///
    /// @return whether the entry reached the shared database - a refused or failed (and thus
    ///         rolled back) update must not be announced to other clients
    private boolean writeSharedEntry(BibEntry bibEntry) {
        if (!connected.get()) {
            offlineChanges.recordChange(bibEntry);
            return false;
        }
        int sharedId = bibEntry.getSharedBibEntryData().getSharedIdAsInt();
        try {
            dbmsProcessor.updateEntry(bibEntry);
            sharedIdsInConflict.remove(sharedId);
            offlineChanges.forget(List.of(bibEntry));
            return true;
        } catch (OfflineLockException exception) {
            sharedIdsInConflict.add(sharedId);
            eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
        } catch (SharedEntryNotPresentException exception) {
            // Deleted on the shared side: the pull removes it locally and tells the user
            pullEntries();
        } catch (SQLException e) {
            handleWriteFailure("Could not write entry to the shared database", e, () -> offlineChanges.recordChange(bibEntry));
        }
        return false;
    }

    /// Database worker
    ///
    /// @return whether the entries reached the shared database
    private boolean insertSharedEntries(List<BibEntry> bibEntries) {
        // A replayed entry carries the version of the shared row it was recorded against; the row
        // it gets here is a new one, which the database starts at version 1
        bibEntries.stream()
                  .filter(bibEntry -> bibEntry.getSharedBibEntryData().getSharedIdAsInt() == -1)
                  .forEach(bibEntry -> bibEntry.getSharedBibEntryData().setVersion(1));
        return writeOrRecord("Could not insert entries into the shared database",
                () -> dbmsProcessor.insertEntries(bibEntries),
                () -> offlineChanges.recordInsert(bibEntries));
    }

    /// Database worker
    ///
    /// @return whether the removal reached the shared database
    private boolean removeSharedEntries(List<BibEntry> bibEntries) {
        return writeOrRecord("Could not remove entries from the shared database",
                () -> dbmsProcessor.removeEntries(bibEntries),
                () -> offlineChanges.recordRemoval(bibEntries));
    }

    /// Database worker. Other clients are notified by the database function (see DBMSProcessor.setUp).
    private boolean writeSharedMetaData(Map<String, String> serializedMetaData) {
        return writeOrRecord("Could not write metadata to the shared database",
                () -> {
                    dbmsProcessor.setSharedMetaData(serializedMetaData);
                    lastSharedMetaData = serializedMetaData;
                },
                () -> offlineChanges.recordMetaData(serializedMetaData, lastSharedMetaData));
    }

    /// Database worker: recorded metadata is merged into what the shared side has now, so that
    /// metadata other clients changed during the outage (a group they added, say) survives
    private boolean writeRecordedMetaData(Map<String, String> recordedMetaData, OfflineChanges.Recorded recorded) {
        return writeOrRecord("Could not write metadata to the shared database",
                () -> {
                    Map<String, String> merged = recorded.mergeMetaDataInto(dbmsProcessor.getSharedMetaData());
                    dbmsProcessor.setSharedMetaData(merged);
                    lastSharedMetaData = merged;
                },
                () -> offlineChanges.recordMetaData(recordedMetaData, recorded.metaDataBase()));
    }

    /// Database worker: runs the write and asks other clients to pull, or records the change
    /// when the connection is gone
    ///
    /// @return whether the write reached the shared database
    private boolean writeOrRecord(String failureMessage, DatabaseWrite write, Runnable record) {
        if (!connected.get()) {
            record.run();
            return false;
        }
        try {
            write.run();
            // Insertions and removals are not described by a single field change, so other
            // clients have to pull; for metadata, the notification is a harmless duplicate
            notifier.notifyClientsToPull();
            return true;
        } catch (SQLException e) {
            handleWriteFailure(failureMessage, e, record);
            return false;
        }
    }

    /// Database worker. A dead connection takes the synchronizer offline and the change is
    /// recorded for later; a failure on a live connection is transient and only reported
    /// (repeating that write would fail the same way).
    private void handleWriteFailure(String message, SQLException exception, Runnable record) {
        LOGGER.error(message, exception);
        if (checkCurrentConnection()) {
            eventBus.post(new SharedWriteFailedEvent(bibDatabaseContext));
        } else {
            record.run();
        }
    }

    /// Model thread
    private void applyRemoteMetaData(Map<String, String> sharedMetaData) {
        lastSharedMetaData = sharedMetaData;
        try {
            metaData.setEventPropagation(false);
            new MetaDataParser(fileMonitor).parse(metaData, sharedMetaData, keywordSeparator, userAndHost);
        } catch (ParseException e) {
            LOGGER.error("Parse error", e);
        } finally {
            metaData.setEventPropagation(true);
        }
    }

    /// Applies the [MetaData] on all local and shared BibEntries.
    public void applyMetaData() {
        withPullLock(this::doApplyMetaData);
    }

    /// Model thread: applies the save actions locally; the entries they changed are written afterwards.
    private void doApplyMetaData() {
        List<BibEntry> changedEntries = bibDatabase.getEntriesSnapshot().stream()
                                                   .filter(this::applySaveActions)
                                                   .toList();
        if (changedEntries.isEmpty()) {
            return;
        }
        syncExecutor.execute(() -> {
            boolean written = false;
            for (BibEntry bibEntry : changedEntries) {
                written |= writeSharedEntry(bibEntry);
            }
            if (written) {
                notifier.notifyClientsToPull();
            }
        });
    }

    /// Applies a field change received from another client. Any state that does not exactly
    /// match the received change (content-less payload, unknown entry, diverged field value)
    /// falls back to pulling everything from the database.
    // [impl->req~shared-database.change-content-in-notification~1]
    public void applyRemoteFieldChange(FieldChange fieldChange) {
        withPullLock(() -> doApplyRemoteFieldChange(fieldChange));
    }

    /// Schedules a field update received from another shared-database client.
    public void handleRemoteFieldChange(FieldChange fieldChange) {
        remoteUpdateExecutor.execute(() -> applyRemoteFieldChange(fieldChange));
    }

    private void doApplyRemoteFieldChange(FieldChange fieldChange) {
        if (fieldChange.field() == null) {
            // The sender could not include the change content
            pullEntries();
            return;
        }
        Optional<BibEntry> localEntry = bibDatabase.getEntriesSnapshot().stream()
                                                   .filter(entry -> fieldChange.bibEntryId().equals(entry.getSharedBibEntryData().getSharedIdAsString()))
                                                   .findFirst();
        if (localEntry.isEmpty()) {
            // Entry unknown locally - e.g. inserted remotely after our last pull
            pullEntries();
            return;
        }
        BibEntry bibEntry = localEntry.get();
        Field field = FieldFactory.parseField(fieldChange.field());
        if (!bibEntry.getField(field).equals(Optional.ofNullable(fieldChange.oldValue()))
                || sharedIdsInConflict.contains(bibEntry.getSharedBibEntryData().getSharedIdAsInt())) {
            // Local state diverged from the sender's sanity-check value - or is a refused local
            // change, which must not adopt the remote version and thereby bypass the merge
            pullEntries();
            return;
        }
        if (fieldChange.newValue() == null) {
            bibEntry.clearField(field, EntriesEventSource.SHARED);
        } else {
            bibEntry.setField(field, fieldChange.newValue(), EntriesEventSource.SHARED);
        }
        bibEntry.getSharedBibEntryData().setVersion(fieldChange.version());
    }

    /// @return whether a save action changed the entry
    private boolean applySaveActions(BibEntry bibEntry) {
        return !BibDatabaseWriter.applySaveActions(bibEntry, metaData, fieldPreferences).isEmpty();
    }

    /// Model thread: the save actions mutate the entry, which the database worker must not do.
    /// Only the flush queued from here includes them; a flush before a remote-triggered pull
    /// writes the entry as it is.
    private void applySaveActionsToBufferedEntry() {
        BibEntry bufferedEntry = entryWithPendingChanges.get();
        if ((bufferedEntry != null) && isPresentLocalBibEntry(bufferedEntry)) {
            applySaveActions(bufferedEntry);
        }
    }

    /// Database worker: writes the buffered micro-edited entry (if any) as a whole
    // [impl->req~shared-database.micro-edit-batching~1]
    private void writeBufferedEntry() {
        BibEntry bufferedEntry = entryWithPendingChanges.getAndSet(null);
        if ((bufferedEntry != null) && isPresentLocalBibEntry(bufferedEntry) && writeSharedEntry(bufferedEntry)) {
            // No field-change event describes the flushed edits, so other clients have to pull
            notifier.notifyClientsToPull();
        }
    }

    /// Takes the synchronizer offline: from now on changes are recorded instead of written, and
    /// a scheduled task tries to get a new connection.
    private void goOffline() {
        // A flush failing during close is not worth a notification or a reconnect attempt
        if (closed || !connected.compareAndSet(true, false)) {
            return;
        }
        LOGGER.warn("Lost the connection to the shared database - keeping changes locally until it is back");
        eventBus.post(new ConnectionLostEvent(bibDatabaseContext));
        scheduleReconnect(INITIAL_RECONNECT_DELAY_MILLIS);
    }

    /// One attempt per scheduled task, so that waiting for the next one occupies no thread
    // [impl->req~shared-database.automatic-reconnect~1]
    private void scheduleReconnect(long delayMillis) {
        taskExecutor.schedule(BackgroundTask.wrap(() -> reconnect(delayMillis)), delayMillis, TimeUnit.MILLISECONDS);
    }

    private void reconnect(long delayMillis) {
        if (closed) {
            return;
        }
        DatabaseConnection newConnection;
        try {
            newConnection = dbmsConnection.openNewConnection();
        } catch (SQLException e) {
            long nextDelayMillis = Math.min(delayMillis * 2, MAX_RECONNECT_DELAY_MILLIS);
            LOGGER.debug("Reconnecting to the shared database failed - next attempt in {} ms", nextDelayMillis, e);
            scheduleReconnect(nextDelayMillis);
            return;
        }
        // On the database worker: queued writes recorded themselves offline, the swap must
        // not interleave with them
        try {
            syncExecutor.execute(() -> useConnection(newConnection));
        } catch (RejectedExecutionException e) {
            // Closed while connecting
            closeQuietly(newConnection.getConnection());
        }
    }

    /// Database worker: replaces the dead connection, then synchronizes what happened meanwhile
    private void useConnection(DatabaseConnection newConnection) {
        if (closed) {
            closeQuietly(newConnection.getConnection());
            return;
        }
        dbmsProcessor.stopNotificationListener();
        closeQuietly(currentConnection);
        dbmsConnection = newConnection;
        currentConnection = newConnection.getConnection();
        dbmsProcessor = new DBMSProcessor(newConnection);
        notifier = new Notifier(currentConnection, dbmsProcessor.getProcessorId());
        dbmsProcessor.startNotificationListener(this);
        connected.set(true);
        LOGGER.info("Reconnected to the shared database");
        eventBus.post(new ConnectionRestoredEvent(bibDatabaseContext));
        if (!replayOfflineChanges(false)) {
            // Nothing to write, but the shared side may have moved on during the outage
            pullChanges();
        }
    }

    /// Writes the changes recorded while offline through the same paths as live changes:
    /// applied to the local library (model thread) where a restart lost them, then written
    /// (database worker) - a shared entry that moved on meanwhile refuses the write and the
    /// user merges, like any other conflict. Ends with a pull.
    ///
    /// Every record is dropped only once it reached the shared database, so that closing, a crash
    /// or a refused write in between leaves it recorded for the next attempt.
    ///
    /// @param afterRestart whether the local library was just loaded from the shared database and
    ///                     needs the recorded state restored - after a reconnect it holds the
    ///                     changes already, plus micro-edits made since the record
    /// @return whether there was anything to replay
    private boolean replayOfflineChanges(boolean afterRestart) {
        OfflineChanges.Recorded recorded = offlineChanges.peek();
        if (recorded.isEmpty()) {
            return false;
        }
        LOGGER.info("Synchronizing the changes made while the shared database was unavailable");
        remoteUpdateExecutor.execute(() -> withPullLock(() -> {
            Map<Integer, BibEntry> localEntriesById = new HashMap<>();
            for (BibEntry localEntry : bibDatabase.getEntriesSnapshot()) {
                localEntriesById.put(localEntry.getSharedBibEntryData().getSharedIdAsInt(), localEntry);
            }

            List<BibEntry> entriesToRemove = new ArrayList<>();
            List<BibEntry> locallyPresentRemovedEntries = new ArrayList<>();
            recorded.removedEntries().forEach((sharedId, version) -> {
                BibEntry localEntry = localEntriesById.get(sharedId);
                if (localEntry != null) {
                    // Pulled back in by a fresh session
                    locallyPresentRemovedEntries.add(localEntry);
                }
                BibEntry removedEntry = new BibEntry();
                removedEntry.getSharedBibEntryData().setSharedId(sharedId);
                removedEntry.getSharedBibEntryData().setVersion(version);
                entriesToRemove.add(removedEntry);
            });
            if (!locallyPresentRemovedEntries.isEmpty()) {
                bibDatabase.removeEntries(locallyPresentRemovedEntries, EntriesEventSource.SHARED);
            }

            List<BibEntry> entriesToWrite = new ArrayList<>();
            List<BibEntry> entriesToInsert = new ArrayList<>();
            recorded.changedEntries().forEach((sharedId, state) -> {
                BibEntry localEntry = localEntriesById.get(sharedId);
                if (localEntry == null) {
                    // Deleted on the shared side meanwhile - the user's version is kept as a new entry
                    BibEntry restoredEntry = state.toBibEntry();
                    bibDatabase.insertEntries(List.of(restoredEntry), EntriesEventSource.SHARED);
                    entriesToInsert.add(restoredEntry);
                    offlineChanges.recordAsNew(sharedId, restoredEntry);
                    return;
                }
                // After a reconnect the local entry is the truth - unless a remote state reached it
                // meanwhile (a pull or a field-change notification moves its version); then the
                // recorded state is restored so that the write is refused and the user merges,
                // instead of the pulled state silently replacing the offline changes
                if (afterRestart || (localEntry.getSharedBibEntryData().getVersion() != state.baseVersion())) {
                    state.applyTo(localEntry);
                }
                // Protected from the pull that follows until written (or refused and merged)
                sharedIdsInConflict.add(sharedId);
                entriesToWrite.add(localEntry);
            });
            Map<String, BibEntry> restoredNewEntries = new LinkedHashMap<>();
            recorded.newEntries().forEach((localId, state) -> {
                BibEntry newEntry = bibDatabase.getEntryById(localId).orElseGet(() -> {
                    BibEntry restoredEntry = state.toBibEntry();
                    bibDatabase.insertEntries(List.of(restoredEntry), EntriesEventSource.SHARED);
                    restoredNewEntries.put(localId, restoredEntry);
                    return restoredEntry;
                });
                entriesToInsert.add(newEntry);
            });
            if (!restoredNewEntries.isEmpty()) {
                offlineChanges.rekeyInserts(restoredNewEntries);
            }

            syncExecutor.execute(() -> {
                if (!entriesToRemove.isEmpty()) {
                    try {
                        List<BibEntry> stillUnchangedEntries = onlyUnchangedSharedEntries(entriesToRemove);
                        if (stillUnchangedEntries.isEmpty() || removeSharedEntries(stillUnchangedEntries)) {
                            offlineChanges.forgetRemovals(recorded.removedEntries().keySet());
                        }
                    } catch (SQLException e) {
                        LOGGER.error("Could not check which recorded removals are still up to date", e);
                    }
                }
                reviveEntriesDeletedMeanwhile(entriesToWrite, entriesToInsert);
                if (!entriesToInsert.isEmpty() && insertSharedEntries(entriesToInsert)) {
                    offlineChanges.forget(entriesToInsert);
                }
                boolean written = false;
                for (BibEntry bibEntry : entriesToWrite) {
                    written |= writeSharedEntry(bibEntry);
                }
                if (written) {
                    notifier.notifyClientsToPull();
                }
                Map<String, String> recordedMetaData = recorded.metaData();
                if ((recordedMetaData != null) && writeRecordedMetaData(recordedMetaData, recorded)) {
                    offlineChanges.forgetMetaData();
                }
                // Also applies the merged metadata locally
                pullChanges();
            });
        }));
        return true;
    }

    /// Database worker: the optimistic lock for a recorded removal, which [DBMSProcessor#removeEntries]
    /// cannot express. An entry another client changed during the outage is kept - the pull that
    /// ends the replay brings it back locally, rather than the collaborator losing their work.
    ///
    /// @return the entries whose shared version still is the one the removal was recorded against
    private List<BibEntry> onlyUnchangedSharedEntries(List<BibEntry> entriesToRemove) throws SQLException {
        if (!connected.get()) {
            return entriesToRemove;
        }
        Map<Integer, Integer> sharedVersions = dbmsProcessor.getSharedIDVersionMapping();
        List<BibEntry> unchangedEntries = new ArrayList<>();
        for (BibEntry bibEntry : entriesToRemove) {
            Integer sharedVersion = sharedVersions.get(bibEntry.getSharedBibEntryData().getSharedIdAsInt());
            if (sharedVersion == null) {
                // Already gone from the shared database
                continue;
            }
            if (sharedVersion == bibEntry.getSharedBibEntryData().getVersion()) {
                unchangedEntries.add(bibEntry);
            } else {
                LOGGER.info("Keeping shared entry {}, which was changed while it was removed locally",
                        bibEntry.getSharedBibEntryData().getSharedIdAsInt());
            }
        }
        return unchangedEntries;
    }

    /// Database worker: a shared row deleted during the outage must not take the changes recorded
    /// against it with it. Such an entry is re-inserted as a new shared entry instead of being
    /// updated - an update would fail with [SharedEntryNotPresentException] and pull the deletion,
    /// which removes the user's edited entry.
    private void reviveEntriesDeletedMeanwhile(List<BibEntry> entriesToWrite, List<BibEntry> entriesToInsert) {
        if (!connected.get() || entriesToWrite.isEmpty()) {
            return;
        }
        Set<Integer> stillPresentIds;
        try {
            stillPresentIds = dbmsProcessor.getSharedIDVersionMapping().keySet();
        } catch (SQLException e) {
            LOGGER.error("Could not check which recorded entries are still present in the shared database", e);
            return;
        }
        Iterator<BibEntry> iterator = entriesToWrite.iterator();
        while (iterator.hasNext()) {
            BibEntry bibEntry = iterator.next();
            int sharedId = bibEntry.getSharedBibEntryData().getSharedIdAsInt();
            if (stillPresentIds.contains(sharedId)) {
                continue;
            }
            // Inserted below, which assigns a fresh shared id and version
            bibEntry.getSharedBibEntryData().setSharedId(-1);
            sharedIdsInConflict.remove(sharedId);
            offlineChanges.recordAsNew(sharedId, bibEntry);
            entriesToInsert.add(bibEntry);
            iterator.remove();
        }
    }

    private void withPullLock(Runnable work) {
        pullLock.lock();
        try {
            work.run();
        } finally {
            pullLock.unlock();
        }
    }

    /// Runs the given work only when no other thread is applying a pull right now.
    ///
    /// @return whether the work ran
    private boolean ifNotPullingAlready(Runnable work) {
        if (!pullLock.tryLock()) {
            return false;
        }
        try {
            work.run();
        } finally {
            pullLock.unlock();
        }
        return true;
    }

    /// Checks whether the current SQL connection is valid; a dead one takes the synchronizer offline.
    ///
    /// @return `true` if the connection is valid, else `false`.
    private boolean checkCurrentConnection() {
        try {
            if (currentConnection.isValid(0)) {
                return true;
            }
        } catch (SQLException e) {
            LOGGER.debug("SQL Error during connection check", e);
        }
        goOffline();
        return false;
    }

    /// Only local changes (and their undo) are written; changes applied from the shared side are not echoed back.
    private static boolean isEventSourceAccepted(EntriesEvent event) {
        EntriesEventSource eventSource = event.getEntriesEventSource();
        return (eventSource == EntriesEventSource.LOCAL) || (eventSource == EntriesEventSource.UNDO);
    }

    @Override
    public void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException, SQLException {
        this.dbmsConnection = connection;
        this.dbName = connection.getProperties().getDatabase();
        this.currentConnection = connection.getConnection();
        this.dbmsProcessor = new DBMSProcessor(connection);
        this.notifier = new Notifier(currentConnection, dbmsProcessor.getProcessorId());
        this.offlineChanges = OfflineChanges.load(offlineChangesDirectory, connection.getProperties());
        initializeDatabases();
    }

    @Override
    public void closeSharedDatabase() {
        closed = true;
        applySaveActionsToBufferedEntry();
        BibEntry bufferedEntry = entryWithPendingChanges.get();
        if ((bufferedEntry != null) && isPresentLocalBibEntry(bufferedEntry)) {
            // Recorded before the flush is even attempted: a dead or slow connection must not
            // cost the edit - a successful flush forgets it again
            offlineChanges.recordChange(bufferedEntry);
        }
        // Flush the buffered micro-edits as the last queued write, then let the queue drain.
        // Strictly bounded: a dead remote connection must not block application shutdown -
        // the writer is a daemon thread and the connection is closed underneath it below.
        if (ownedSyncExecutor != null) {
            ownedSyncExecutor.execute(this::writeBufferedEntry);
            ownedSyncExecutor.shutdown();
            try {
                if (!ownedSyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    LOGGER.warn("Queued shared database writes did not finish in time - closing anyway");
                    ownedSyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            writeBufferedEntry();
        }
        dbmsProcessor.stopNotificationListener();
        closeQuietly(currentConnection);
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.debug("Could not close the shared database connection", e);
        }
    }

    private boolean isPresentLocalBibEntry(BibEntry bibEntry) {
        return bibDatabase.getEntryById(bibEntry.getId()).isPresent();
    }

    @Override
    public String getDBName() {
        return dbName;
    }

    @Override
    public DatabaseConnectionProperties getConnectionProperties() {
        return dbmsProcessor.getDBMSConnectionProperties();
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public void registerListener(Object listener) {
        eventBus.register(listener);
    }
}
