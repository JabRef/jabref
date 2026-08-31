package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.OfflineLockException;
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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Synchronizes the shared or local databases with their opposite side. Local changes are pushed by [EntriesEvent]
/// using Google's Guava EventBus.
// Synchronization methods are `synchronized`: they are invoked both from EventBus dispatch
// threads and from the notification listener thread, and concurrent pulls would insert
// entries twice. ponytail: coarse instance lock; finer locking only if profiling demands it.
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

    private Optional<BibEntry> entryWithPendingChanges;
    private final String userAndHost;

    public DBMSSynchronizer(@NonNull BibDatabaseContext bibDatabaseContext,
                            Character keywordSeparator,
                            FieldPreferences fieldPreferences,
                            @NonNull GlobalCitationKeyPatterns globalCiteKeyPattern,
                            FileUpdateMonitor fileMonitor,
                            String userAndHost) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.bibDatabase = bibDatabaseContext.getDatabase();
        this.metaData = bibDatabaseContext.getMetaData();
        this.fieldPreferences = fieldPreferences;
        this.fileMonitor = fileMonitor;
        this.eventBus = new EventBus();
        this.keywordSeparator = keywordSeparator;
        this.globalCiteKeyPattern = globalCiteKeyPattern;
        this.entryWithPendingChanges = Optional.empty();
        this.userAndHost = userAndHost;
    }

    /// Listening method. Inserts a new [BibEntry] into shared database.
    @Subscribe
    public synchronized void listen(EntriesAddedEvent event) {
        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntriesEvents may be posted.
        // In this case DBSynchronizer should not try to insert the bibEntry entry again (but it would not harm).
        if (isEventSourceAccepted(event) && checkCurrentConnection()) {
            // TODO: Make use of org.jabref.model.metadata.event.MetaDataChangedEvent (and also do Added/Removed there)
            synchronizeLocalMetaData();

            pullWithLastEntry();
            synchronizeLocalDatabase();
            // TODO: Rewrite
            dbmsProcessor.insertEntries(event.getBibEntries());
            // Reset last changed entry because it just has already been synchronized -> Why necessary?
            entryWithPendingChanges = Optional.empty();
            // Insertions are not described by a single field change, so other clients have to pull
            notifier.notifyClientsToPull();
        }
    }

    /// Listening method. Updates an existing shared [BibEntry].
    ///
    /// In JabRef (UI), the field is modified
    @Subscribe
    public synchronized void listen(FieldChangedEvent event) {
        if (!isEventSourceAccepted(event)) {
            return;
        }
        BibEntry bibEntry = event.getBibEntry();
        if (event.isFiltered() || !isPresentLocalBibEntry(bibEntry) || !checkCurrentConnection()) {
            // Filtered micro-edits are accumulated here and written on the next major change or on close
            entryWithPendingChanges = Optional.of(bibEntry);
            return;
        }
        synchronizeLocalMetaData();
        pullWithLastEntry();
        synchronizeSharedEntry(bibEntry);
        // Pull changes to detect concurrent modifications - e.g. the entry meanwhile being deleted remotely
        synchronizeLocalDatabase();
        notifier.notifyAboutChangedField(event);
    }

    /// Listening method. Deletes the given list of [BibEntry] from shared database.
    @Subscribe
    public synchronized void listen(EntriesRemovedEvent event) {
        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntriesEvents may be posted.
        // In this case DBSynchronizer should not try to delete the bibEntry entry again (but it would not harm).
        if (isEventSourceAccepted(event) && checkCurrentConnection()) {
            synchronizeLocalMetaData();
            pullWithLastEntry();
            dbmsProcessor.removeEntries(event.getBibEntries());
            synchronizeLocalDatabase();
            // Removals are not described by a single field change, so other clients have to pull
            notifier.notifyClientsToPull();
        }
    }

    /// Listening method. Synchronizes the shared [MetaData] and applies them locally.
    @Subscribe
    public synchronized void listen(MetaDataChangedEvent event) {
        if (checkCurrentConnection()) {
            synchronizeSharedMetaData(event.getMetaData(), globalCiteKeyPattern);
            // Other clients are notified through the upsert_metadata function (see DBMSProcessor.setUp)
            applyMetaData();
        }
    }

    /// Sets the table structure of shared database if needed and pulls all shared entries to the new local database.
    ///
    /// @throws DatabaseNotSupportedException if the version of shared database does not match the version of current shared database support ([DBMSProcessor]).
    public synchronized void initializeDatabases() throws DatabaseNotSupportedException {
        try {
            if (!dbmsProcessor.checkBaseIntegrity()) {
                LOGGER.info("Integrity check failed. Fixing...");

                // This check should only be performed once on initial database setup.
                if (dbmsProcessor.databaseIsAtMostJabRef35()) {
                    throw new DatabaseNotSupportedException();
                }

                // Calling dbmsProcessor.setupSharedDatabase() lets dbmsProcessor.checkBaseIntegrity() be true.
                dbmsProcessor.setupSharedDatabase();
            }
        } catch (SQLException e) {
            LOGGER.error("Could not check integrity", e);
            throw new IllegalStateException(e);
        }

        dbmsProcessor.startNotificationListener(this);
        synchronizeLocalMetaData();
        synchronizeLocalDatabase();
    }

    /// Synchronizes the local database with shared one. Possible update types are: removal, update, or insert of a
    /// [BibEntry].
    @Override
    public synchronized void synchronizeLocalDatabase() {
        if (!checkCurrentConnection()) {
            return;
        }

        List<BibEntry> localEntries = bibDatabase.getEntries();
        Map<Integer, Integer> idVersionMap = dbmsProcessor.getSharedIDVersionMapping();

        // remove old entries locally
        removeNotSharedEntries(localEntries, idVersionMap.keySet());
        List<Integer> entriesToInsertIntoLocalDatabase = new ArrayList<>();
        // compare versions and update local entry if needed
        for (Map.Entry<Integer, Integer> idVersionEntry : idVersionMap.entrySet()) {
            boolean remoteEntryMatchingOneLocalEntryFound = false;
            for (BibEntry localEntry : localEntries) {
                if (idVersionEntry.getKey().equals(localEntry.getSharedBibEntryData().getSharedIdAsInt())) {
                    remoteEntryMatchingOneLocalEntryFound = true;
                    if (idVersionEntry.getValue() > localEntry.getSharedBibEntryData().getVersion()) {
                        Optional<BibEntry> sharedEntry = dbmsProcessor.getSharedEntry(idVersionEntry.getKey());
                        if (sharedEntry.isPresent()) {
                            // update fields
                            localEntry.setType(sharedEntry.get().getType(), EntriesEventSource.SHARED);
                            localEntry.getSharedBibEntryData()
                                      .setVersion(sharedEntry.get().getSharedBibEntryData().getVersion());
                            sharedEntry.get().getFieldMap().forEach(
                                    // copy remote values to local entry
                                    (field, value) -> localEntry.setField(field, value, EntriesEventSource.SHARED)
                            );

                            // locally remove not existing fields
                            localEntry.getFields().stream()
                                      .filter(field -> !sharedEntry.get().hasField(field))
                                      .forEach(
                                              field -> localEntry.clearField(field, EntriesEventSource.SHARED)
                                      );
                        }
                    }
                }
            }
            if (!remoteEntryMatchingOneLocalEntryFound) {
                entriesToInsertIntoLocalDatabase.add(idVersionEntry.getKey());
            }
        }

        if (!entriesToInsertIntoLocalDatabase.isEmpty()) {
            // in case entries should be added into the local database, insert them
            bibDatabase.insertEntries(dbmsProcessor.partitionAndGetSharedEntries(entriesToInsertIntoLocalDatabase), EntriesEventSource.SHARED);
        }
    }

    /// Removes all local entries which are not present on shared database.
    ///
    /// @param localEntries List of [BibEntry] the entries should be removed from
    /// @param sharedIDs    Set of all IDs which are present on shared database
    private void removeNotSharedEntries(List<BibEntry> localEntries, Set<Integer> sharedIDs) {
        List<BibEntry> entriesToRemove =
                localEntries.stream()
                            .filter(localEntry -> !sharedIDs.contains(localEntry.getSharedBibEntryData().getSharedIdAsInt()))
                            .collect(Collectors.toList());
        if (!entriesToRemove.isEmpty()) {
            eventBus.post(new SharedEntriesNotPresentEvent(entriesToRemove));
            // remove all non-shared entries without triggering listeners
            bibDatabase.removeEntries(entriesToRemove, EntriesEventSource.SHARED);
        }
    }

    /// Synchronizes the shared [BibEntry] with the local one.
    @Override
    public synchronized void synchronizeSharedEntry(BibEntry bibEntry) {
        if (!checkCurrentConnection()) {
            return;
        }
        try {
            // Save actions here used to cause cursor jumping while typing (issue #5904, fixed in
            // PR #11282 by taking whitespace normalization out of the write path). A save action
            // that actually changes the value still rewrites the edited field - as in JabRef 5.x.
            // Micro-edit buffering keeps this sync path out of continuous typing anyway.
            BibDatabaseWriter.applySaveActions(bibEntry, metaData, fieldPreferences);
            dbmsProcessor.updateEntry(bibEntry);
        } catch (OfflineLockException exception) {
            eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
    }

    /// Synchronizes all meta data locally.
    public synchronized void synchronizeLocalMetaData() {
        if (!checkCurrentConnection()) {
            return;
        }

        try {
            metaData.setEventPropagation(false);
            MetaDataParser parser = new MetaDataParser(fileMonitor);
            parser.parse(metaData, dbmsProcessor.getSharedMetaData(), keywordSeparator, userAndHost);
            metaData.setEventPropagation(true);
        } catch (ParseException e) {
            LOGGER.error("Parse error", e);
        }
    }

    /// Synchronizes all shared meta data.
    private void synchronizeSharedMetaData(MetaData data, GlobalCitationKeyPatterns globalCiteKeyPattern) {
        if (!checkCurrentConnection()) {
            return;
        }
        try {
            dbmsProcessor.setSharedMetaData(MetaDataSerializer.getSerializedStringMap(data, globalCiteKeyPattern));
            // TODO: synchronize with server - currently, only data is written to the server
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
    }

    /// Applies the [MetaData] on all local and shared BibEntries.
    public synchronized void applyMetaData() {
        if (!checkCurrentConnection()) {
            return;
        }
        for (BibEntry bibEntry : bibDatabase.getEntries()) {
            try {
                // synchronize only if changes were present
                if (!BibDatabaseWriter.applySaveActions(bibEntry, metaData, fieldPreferences).isEmpty()) {
                    dbmsProcessor.updateEntry(bibEntry);
                }
            } catch (OfflineLockException exception) {
                eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
            } catch (SQLException e) {
                LOGGER.error("SQL Error", e);
            }
        }
    }

    /// Synchronizes the local BibEntries and applies the fetched MetaData on them.
    @Override
    public synchronized void pullChanges() {
        if (!checkCurrentConnection()) {
            return;
        }
        // First synchronize entry, then synchronize database
        pullWithLastEntry();
        synchronizeLocalDatabase();
        synchronizeLocalMetaData();
    }

    /// Applies a field change received from another client. Any state that does not exactly
    /// match the received change (content-less payload, unknown entry, diverged field value)
    /// falls back to pulling everything from the database.
    // [impl->req~shared-database.change-content-in-notification~1]
    public synchronized void applyRemoteFieldChange(FieldChange fieldChange) {
        if (fieldChange.field() == null) {
            // The sender could not include the change content
            pullChanges();
            return;
        }
        Optional<BibEntry> localEntry = bibDatabase.getEntries().stream()
                                                   .filter(entry -> fieldChange.bibEntryId().equals(entry.getSharedBibEntryData().getSharedIdAsString()))
                                                   .findFirst();
        if (localEntry.isEmpty()) {
            // Entry unknown locally - e.g. inserted remotely after our last pull
            pullChanges();
            return;
        }
        BibEntry bibEntry = localEntry.get();
        Field field = FieldFactory.parseField(fieldChange.field());
        if (!bibEntry.getField(field).equals(Optional.ofNullable(fieldChange.oldValue()))) {
            // Local state diverged from the sender's sanity-check value
            pullChanges();
            return;
        }
        if (fieldChange.newValue() == null) {
            bibEntry.clearField(field, EntriesEventSource.SHARED);
        } else {
            bibEntry.setField(field, fieldChange.newValue(), EntriesEventSource.SHARED);
        }
        bibEntry.getSharedBibEntryData().setVersion(fieldChange.version());
    }

    /// Synchronizes local BibEntries only if last entry changes still remain
    public synchronized void pullLastEntryChanges() {
        if (entryWithPendingChanges.isEmpty() || !checkCurrentConnection()) {
            return;
        }
        synchronizeLocalMetaData();
        pullWithLastEntry();
        // Pull changes for the case that there were some
        synchronizeLocalDatabase();
    }

    /// Synchronizes local BibEntries and pulls remaining last entry changes
    // [impl->req~shared-database.micro-edit-batching~1]
    private void pullWithLastEntry() {
        entryWithPendingChanges.filter(this::isPresentLocalBibEntry).ifPresent(entry -> {
            synchronizeSharedEntry(entry);
            // The flush writes the whole entry without a describing field-change event,
            // so other clients have to pull
            notifier.notifyClientsToPull();
        });
        entryWithPendingChanges = Optional.empty();
    }

    /// Checks whether the current SQL connection is valid. In case that the connection is not valid a new [ConnectionLostEvent] is going to be sent.
    ///
    /// @return `true` if the connection is valid, else `false`.
    public boolean checkCurrentConnection() {
        try {
            boolean isValid = currentConnection.isValid(0);
            if (!isValid) {
                LOGGER.warn("Lost SQL connection.");
                eventBus.post(new ConnectionLostEvent(bibDatabaseContext));
            }
            return isValid;
        } catch (SQLException e) {
            LOGGER.error("SQL Error during connection check", e);
            return false;
        }
    }

    /// Checks whether the [EntriesEventSource] of an [EntriesEvent] is crucial for this class.
    ///
    /// @param event An [EntriesEvent]
    /// @return `true` if the event is able to trigger operations in [DBMSSynchronizer], else
    /// `false`
    public boolean isEventSourceAccepted(EntriesEvent event) {
        EntriesEventSource eventSource = event.getEntriesEventSource();
        return (eventSource == EntriesEventSource.LOCAL) || (eventSource == EntriesEventSource.UNDO);
    }

    @Override
    public void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException {
        this.dbName = connection.getProperties().getDatabase();
        this.currentConnection = connection.getConnection();
        this.dbmsProcessor = new DBMSProcessor(connection);
        this.notifier = new Notifier(currentConnection, dbmsProcessor.getProcessorId());
        initializeDatabases();
    }

    @Override
    public void closeSharedDatabase() {
        // Submit remaining entry changes
        pullLastEntryChanges();
        try {
            dbmsProcessor.stopNotificationListener();
            currentConnection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
    }

    private boolean isPresentLocalBibEntry(BibEntry bibEntry) {
        return bibDatabase.getEntries().contains(bibEntry);
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
