package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.SharedEntriesNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.logic.shared.notifications.Notifier;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEvent;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.postgresql.PGConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes the shared or local databases with their opposite side. Local changes are pushed by {@link EntriesEvent}
 * using Google's Guava EventBus.
 */
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

    @SuppressWarnings("unused")
    // will be required later for save actions
    private final FieldPreferences fieldPreferences;

    private final FileUpdateMonitor fileMonitor;

    private Optional<BibEntry> lastEntryChanged_REMOVEME;

    public DBMSSynchronizer(BibDatabaseContext bibDatabaseContext, Character keywordSeparator,
                            FieldPreferences fieldPreferences,
                            GlobalCitationKeyPatterns globalCiteKeyPattern, FileUpdateMonitor fileMonitor) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.bibDatabase = bibDatabaseContext.getDatabase();
        this.metaData = bibDatabaseContext.getMetaData();
        this.fieldPreferences = fieldPreferences;
        this.fileMonitor = fileMonitor;
        this.eventBus = new EventBus();
        this.keywordSeparator = keywordSeparator;
        this.globalCiteKeyPattern = Objects.requireNonNull(globalCiteKeyPattern);
        this.lastEntryChanged_REMOVEME = Optional.empty();
    }

    /**
     * Listening method. Inserts a new {@link BibEntry} into shared database.
     */
    @Subscribe
    public void listen(EntriesAddedEvent event) {
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
            lastEntryChanged_REMOVEME = Optional.empty();
        }
    }

    /**
     * Listening method. Updates an existing shared {@link BibEntry}.
     *
     * In JabRef (UI), the field is modified
     */
    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (event.isFiltered() || !isEventSourceAccepted(event)) {
            return;
        }

        notifier.notifyAboutChangedField(event);
    }

    /**
     * Listening method. Deletes the given list of {@link BibEntry} from shared database.
     */
    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntriesEvents may be posted.
        // In this case DBSynchronizer should not try to delete the bibEntry entry again (but it would not harm).
        if (isEventSourceAccepted(event) && checkCurrentConnection()) {
            synchronizeLocalMetaData();
            pullWithLastEntry();
            dbmsProcessor.removeEntries(event.getBibEntries());
            synchronizeLocalDatabase();
        }
    }

    /**
     * Listening method. Synchronizes the shared {@link MetaData} and applies them locally.
     */
    @Subscribe
    public void listen(MetaDataChangedEvent event) {
        if (checkCurrentConnection()) {
            synchronizeSharedMetaData(event.getMetaData(), globalCiteKeyPattern);
            // TODO: applyMetaData();
        }
    }

    /**
     * Sets the table structure of shared database if needed and pulls all shared entries to the new local database.
     *
     * @throws DatabaseNotSupportedException if the version of shared database does not match the version of current
     *                                       shared database support ({@link DBMSProcessor}).
     */
    public void initializeDatabases() throws DatabaseNotSupportedException {
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

    /**
     * Synchronizes the local database with shared one. Possible update types are: removal, update, or insert of a
     * {@link BibEntry}.
     */
    @Override
    public void synchronizeLocalDatabase() {
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

    /**
     * Removes all local entries which are not present on shared database.
     *
     * @param localEntries List of {@link BibEntry} the entries should be removed from
     * @param sharedIDs    Set of all IDs which are present on shared database
     */
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

    /**
     * Synchronizes the shared {@link BibEntry} with the local one.
     */
    @Override
    public void synchronizeSharedEntry(BibEntry bibEntry) {
        if (!checkCurrentConnection()) {
            return;
        }
        try {
            // TODO: Either reenable (compare with fix https://github.com/JabRef/jabref/pull/11282) or write user documentation that "cleanup actions" should be used or introduce SQL databse cleanup (stored procedure, ...)
            // BibDatabaseWriter.applySaveActions(bibEntry, metaData, fieldPreferences); // perform possibly existing save actions

            dbmsProcessor.updateEntry(bibEntry);
        } catch (OfflineLockException exception) {
            eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
        } catch (SQLException e) {
            LOGGER.error("SQL Error", e);
        }
    }

    /**
     * Synchronizes all meta data locally.
     */
    public void synchronizeLocalMetaData() {
        if (!checkCurrentConnection()) {
            return;
        }

        try {
            metaData.setEventPropagation(false);
            MetaDataParser parser = new MetaDataParser(fileMonitor);
            parser.parse(metaData, dbmsProcessor.getSharedMetaData(), keywordSeparator);
            metaData.setEventPropagation(true);
        } catch (ParseException e) {
            LOGGER.error("Parse error", e);
        }
    }

    /**
     * Synchronizes all shared meta data.
     */
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

    /**
     * Applies the {@link MetaData} on all local and shared BibEntries.
     */
    public void applyMetaData() {
        if (!checkCurrentConnection()) {
            return;
        }
        for (BibEntry bibEntry : bibDatabase.getEntries()) {
            try {
                // synchronize only if changes were present
                // TODO: apply save actions
                // if (!BibDatabaseWriter.applySaveActions(bibEntry, metaData, fieldPreferences).isEmpty()) {
                if (false) {
                    dbmsProcessor.updateEntry(bibEntry);
                }
            } catch (OfflineLockException exception) {
                eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
            } catch (SQLException e) {
                LOGGER.error("SQL Error", e);
            }
        }
    }

    /**
     * Synchronizes the local BibEntries and applies the fetched MetaData on them.
     */
    @Override
    public void pullChanges() {
        if (!checkCurrentConnection()) {
            return;
        }
        // First synchronize entry, then synchronize database
        pullWithLastEntry();
        synchronizeLocalDatabase();
        synchronizeLocalMetaData();
    }

    /**
     * Synchronizes local BibEntries only if last entry changes still remain
     */
    public void pullLastEntryChanges() {
        if (lastEntryChanged_REMOVEME.isPresent()) {
            if (!checkCurrentConnection()) {
                return;
            }
            synchronizeLocalMetaData();
            pullWithLastEntry();
            // Pull changes for the case that there were some
            synchronizeLocalDatabase();
        }
    }

    /**
     * Synchronizes local BibEntries and pulls remaining last entry changes
     */
    private void pullWithLastEntry() {
        if (lastEntryChanged_REMOVEME.isPresent() && isPresentLocalBibEntry(lastEntryChanged_REMOVEME.get())) {
            synchronizeSharedEntry(lastEntryChanged_REMOVEME.get());
        }
        lastEntryChanged_REMOVEME = Optional.empty();
    }

    /**
     * Checks whether the current SQL connection is valid. In case that the connection is not valid a new {@link
     * ConnectionLostEvent} is going to be sent.
     *
     * @return <code>true</code> if the connection is valid, else <code>false</code>.
     */
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

    /**
     * Checks whether the {@link EntriesEventSource} of an {@link EntriesEvent} is crucial for this class.
     *
     * @param event An {@link EntriesEvent}
     * @return <code>true</code> if the event is able to trigger operations in {@link DBMSSynchronizer}, else
     * <code>false</code>
     */
    public boolean isEventSourceAccepted(EntriesEvent event) {
        EntriesEventSource eventSource = event.getEntriesEventSource();
        return (eventSource == EntriesEventSource.LOCAL) || (eventSource == EntriesEventSource.UNDO);
    }

    @Override
    public void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException {
        this.dbName = connection.getProperties().getDatabase();
        this.currentConnection = connection.getConnection();
        try {
            this.notifier = new Notifier(currentConnection.unwrap(PGConnection.class));
        } catch (SQLException e) {
            LOGGER.error("Could not get Postgres driver", e);
            throw new DatabaseNotSupportedException();
        }
        this.dbmsProcessor = new DBMSProcessor(connection);
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
