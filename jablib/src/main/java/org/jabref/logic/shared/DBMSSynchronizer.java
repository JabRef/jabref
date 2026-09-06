package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.SharedWriteFailedEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.logic.shared.exception.SharedEntryNotPresentException;
import org.jabref.logic.shared.notifications.FieldChange;
import org.jabref.logic.shared.notifications.Notifier;
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
public class DBMSSynchronizer implements DatabaseSynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBMSSynchronizer.class);

    private DBMSProcessor dbmsProcessor;
    private Connection currentConnection;
    private Notifier notifier;
    private String dbName;

    private MetaData metaData;
    private final BibDatabaseContext bibDatabaseContext;
    private final BibDatabase bibDatabase;
    private final EventBus eventBus;
    private final Character keywordSeparator;
    private final GlobalCitationKeyPatterns globalCiteKeyPattern;

    private final FieldPreferences fieldPreferences;

    private final FileUpdateMonitor fileMonitor;

    // Buffered micro-edits; set from EventBus dispatch threads, taken by the database worker
    private final AtomicReference<BibEntry> entryWithPendingChanges = new AtomicReference<>();
    private final ReentrantLock pullLock = new ReentrantLock();
    // Every queued operation fails once the connection is gone - the user is asked only once
    private final AtomicBoolean connectionLostReported = new AtomicBoolean();
    // Entries whose local changes were refused (see UpdateRefusedEvent): they keep their local
    // state until the user merges - a pull must not overwrite them meanwhile
    private final Set<Integer> sharedIdsInConflict = ConcurrentHashMap.newKeySet();
    private final String userAndHost;
    private final Executor remoteUpdateExecutor;
    private final Executor syncExecutor;
    private final @Nullable ExecutorService ownedSyncExecutor;

    /// What the shared database has that the local library has not. Determined on the database
    /// worker against the local ids as of the fetch, applied on the model thread.
    private record RemoteChanges(Set<Integer> removedIds, List<BibEntry> changedEntries) {
    }

    public DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                            Character keywordSeparator,
                            FieldPreferences fieldPreferences,
                            @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                            FileUpdateMonitor fileMonitor,
                            String userAndHost) {
        // Direct executors keep everything synchronous - for tests and headless use
        this(bibDatabaseContext, keywordSeparator, fieldPreferences, globalCiteKeyPattern, fileMonitor, userAndHost, Runnable::run, Runnable::run);
    }

    public DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                            Character keywordSeparator,
                            FieldPreferences fieldPreferences,
                            @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                            FileUpdateMonitor fileMonitor,
                            String userAndHost,
                            Executor remoteUpdateExecutor) {
        // One background worker so that typing never waits for the database (which may be remote)
        this(bibDatabaseContext, keywordSeparator, fieldPreferences, globalCiteKeyPattern, fileMonitor, userAndHost, remoteUpdateExecutor,
                Executors.newSingleThreadExecutor(runnable -> Thread.ofVirtual().name("JabRef - shared database writer").unstarted(runnable)));
    }

    @VisibleForTesting
    DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                     Character keywordSeparator,
                     FieldPreferences fieldPreferences,
                     @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                     FileUpdateMonitor fileMonitor,
                     String userAndHost,
                     Executor remoteUpdateExecutor,
                     Executor syncExecutor) {
        this.syncExecutor = syncExecutor;
        this.ownedSyncExecutor = (syncExecutor instanceof ExecutorService executorService) ? executorService : null;
        this.bibDatabaseContext = bibDatabaseContext;
        this.bibDatabase = bibDatabaseContext.getDatabase();
        this.metaData = bibDatabaseContext.getMetaData();
        this.fieldPreferences = fieldPreferences;
        this.fileMonitor = fileMonitor;
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
            try {
                dbmsProcessor.insertEntries(event.getBibEntries());
                // Insertions are not described by a single field change, so other clients have to pull
                notifier.notifyClientsToPull();
            } catch (SQLException e) {
                LOGGER.error("Could not insert entries into the shared database", e);
                reportWriteFailure();
            }
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
            try {
                dbmsProcessor.removeEntries(event.getBibEntries());
                // Removals are not described by a single field change, so other clients have to pull
                notifier.notifyClientsToPull();
            } catch (SQLException e) {
                LOGGER.error("Could not remove entries from the shared database", e);
                reportWriteFailure();
            }
        });
    }

    /// Listening method. Synchronizes the shared [MetaData] and applies them locally.
    @Subscribe
    public void listen(MetaDataChangedEvent event) {
        syncExecutor.execute(() -> synchronizeSharedMetaData(event.getMetaData(), globalCiteKeyPattern));
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
        int sharedId = bibEntry.getSharedBibEntryData().getSharedIdAsInt();
        try {
            dbmsProcessor.updateEntry(bibEntry);
            sharedIdsInConflict.remove(sharedId);
            return true;
        } catch (OfflineLockException exception) {
            sharedIdsInConflict.add(sharedId);
            eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
        } catch (SharedEntryNotPresentException exception) {
            // Deleted on the shared side: the pull removes it locally and tells the user
            pullEntries();
        } catch (SQLException e) {
            LOGGER.error("Could not write entry to the shared database", e);
            reportWriteFailure();
        }
        return false;
    }

    /// Tells the user that local changes did not reach the shared database. If the connection is
    /// down, checking it posts a [ConnectionLostEvent] (reconnect dialog); otherwise the failure
    /// is transient and reported as a [SharedWriteFailedEvent].
    private void reportWriteFailure() {
        if (checkCurrentConnection()) {
            eventBus.post(new SharedWriteFailedEvent(bibDatabaseContext));
        }
    }

    /// Model thread
    private void applyRemoteMetaData(Map<String, String> sharedMetaData) {
        try {
            metaData.setEventPropagation(false);
            new MetaDataParser(fileMonitor).parse(metaData, sharedMetaData, keywordSeparator, userAndHost);
        } catch (ParseException e) {
            LOGGER.error("Parse error", e);
        } finally {
            metaData.setEventPropagation(true);
        }
    }

    /// Database worker
    private void synchronizeSharedMetaData(MetaData data, GlobalCitationKeyPatterns globalCiteKeyPattern) {
        try {
            dbmsProcessor.setSharedMetaData(MetaDataSerializer.getSerializedStringMap(data, globalCiteKeyPattern));
        } catch (SQLException e) {
            LOGGER.error("Could not write metadata to the shared database", e);
            reportWriteFailure();
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

    /// Checks whether the current SQL connection is valid. In case that the connection is not valid a new [ConnectionLostEvent] is going to be sent.
    ///
    /// @return `true` if the connection is valid, else `false`.
    private boolean checkCurrentConnection() {
        try {
            boolean isValid = currentConnection.isValid(0);
            if (!isValid && connectionLostReported.compareAndSet(false, true)) {
                LOGGER.warn("Lost SQL connection.");
                eventBus.post(new ConnectionLostEvent(bibDatabaseContext));
            }
            return isValid;
        } catch (SQLException e) {
            LOGGER.error("SQL Error during connection check", e);
            return false;
        }
    }

    /// Only local changes (and their undo) are written; changes applied from the shared side are not echoed back.
    private static boolean isEventSourceAccepted(EntriesEvent event) {
        EntriesEventSource eventSource = event.getEntriesEventSource();
        return (eventSource == EntriesEventSource.LOCAL) || (eventSource == EntriesEventSource.UNDO);
    }

    @Override
    public void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException, SQLException {
        this.dbName = connection.getProperties().getDatabase();
        this.currentConnection = connection.getConnection();
        this.dbmsProcessor = new DBMSProcessor(connection);
        this.notifier = new Notifier(currentConnection, dbmsProcessor.getProcessorId());
        initializeDatabases();
    }

    @Override
    public void closeSharedDatabase() {
        applySaveActionsToBufferedEntry();
        // Flush the buffered micro-edits as the last queued write, then let the queue drain.
        // Strictly bounded: a dead remote connection must not block application shutdown -
        // the writer is a daemon thread and the connection is closed underneath it below.
        if (ownedSyncExecutor != null) {
            if (!connectionLostReported.get()) {
                ownedSyncExecutor.execute(this::writeBufferedEntry);
            }
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
        try {
            dbmsProcessor.stopNotificationListener();
            currentConnection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
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
