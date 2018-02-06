package org.jabref.logic.shared;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.shared.event.ConnectionLostEvent;
import org.jabref.logic.shared.event.SharedEntryNotPresentEvent;
import org.jabref.logic.shared.event.UpdateRefusedEvent;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.database.event.EntryRemovedEvent;
import org.jabref.model.database.shared.DatabaseConnection;
import org.jabref.model.database.shared.DatabaseConnectionProperties;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.model.database.shared.DatabaseSynchronizer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.entry.event.EntryEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangedEvent;
import org.jabref.model.util.FileUpdateMonitor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizes the shared or local databases with their opposite side.
 * Local changes are pushed by {@link EntryEvent} using Google's Guava EventBus.
 */
public class DBMSSynchronizer implements DatabaseSynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DBMSSynchronizer.class);

    private DBMSProcessor dbmsProcessor;
    private String dbName;
    private final BibDatabaseContext bibDatabaseContext;
    private MetaData metaData;
    private final BibDatabase bibDatabase;
    private final EventBus eventBus;
    private Connection currentConnection;
    private final Character keywordSeparator;
    private final GlobalBibtexKeyPattern globalCiteKeyPattern;
    private FileUpdateMonitor fileMonitor;

    public DBMSSynchronizer(BibDatabaseContext bibDatabaseContext, Character keywordSeparator,
                            GlobalBibtexKeyPattern globalCiteKeyPattern, FileUpdateMonitor fileMonitor) {
        this.bibDatabaseContext = Objects.requireNonNull(bibDatabaseContext);
        this.bibDatabase = bibDatabaseContext.getDatabase();
        this.metaData = bibDatabaseContext.getMetaData();
        this.fileMonitor = fileMonitor;
        this.eventBus = new EventBus();
        this.keywordSeparator = keywordSeparator;
        this.globalCiteKeyPattern = Objects.requireNonNull(globalCiteKeyPattern);
    }

    /**
     * Listening method. Inserts a new {@link BibEntry} into shared database.
     *
     * @param event {@link EntryAddedEvent} object
     */
    @Subscribe
    public void listen(EntryAddedEvent event) {
        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntryEvents may be posted.
        // In this case DBSynchronizer should not try to insert the bibEntry entry again (but it would not harm).
        if (isEventSourceAccepted(event) && checkCurrentConnection()) {
            synchronizeLocalMetaData();
            synchronizeLocalDatabase(); // Pull changes for the case that there were some
            dbmsProcessor.insertEntry(event.getBibEntry());
        }
    }

    /**
     * Listening method. Updates an existing shared {@link BibEntry}.
     *
     * @param event {@link FieldChangedEvent} object
     */
    @Subscribe
    public void listen(FieldChangedEvent event) {
        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntryEvents may be posted.
        // In this case DBSynchronizer should not try to update the bibEntry entry again (but it would not harm).
        if (isPresentLocalBibEntry(event.getBibEntry()) && isEventSourceAccepted(event) && checkCurrentConnection()) {
            synchronizeLocalMetaData();
            BibEntry bibEntry = event.getBibEntry();
            synchronizeSharedEntry(bibEntry);
            synchronizeLocalDatabase(); // Pull changes for the case that there were some
        }
    }

    /**
     * Listening method. Deletes the given {@link BibEntry} from shared database.
     *
     * @param event {@link EntryRemovedEvent} object
     */
    @Subscribe
    public void listen(EntryRemovedEvent event) {
        // While synchronizing the local database (see synchronizeLocalDatabase() below), some EntryEvents may be posted.
        // In this case DBSynchronizer should not try to delete the bibEntry entry again (but it would not harm).
        if (isEventSourceAccepted(event) && checkCurrentConnection()) {
            dbmsProcessor.removeEntry(event.getBibEntry());
            synchronizeLocalMetaData();
            synchronizeLocalDatabase(); // Pull changes for the case that there where some
        }
    }

    /**
     * Listening method. Synchronizes the shared {@link MetaData} and applies them locally.
     *
     * @param event
     */
    @Subscribe
    public void listen(MetaDataChangedEvent event) {
        if (checkCurrentConnection()) {
            synchronizeSharedMetaData(event.getMetaData(), globalCiteKeyPattern);
            synchronizeLocalDatabase();
            applyMetaData();
            dbmsProcessor.notifyClients();
        }
    }

    @Subscribe
    public void listen(EntryEvent event) {
        if (isEventSourceAccepted(event)) {
            dbmsProcessor.notifyClients();
        }
    }

    /**
     * Sets the table structure of shared database if needed and pulls all shared entries
     * to the new local database.
     *
     * @throws DatabaseNotSupportedException if the version of shared database does not match
     *          the version of current shared database support ({@link DBMSProcessor}).
     */
    public void initializeDatabases() throws DatabaseNotSupportedException {
        try {
            if (!dbmsProcessor.checkBaseIntegrity()) {
                LOGGER.info("Integrity check failed. Fixing...");
                dbmsProcessor.setupSharedDatabase();

                // This check should only be performed once on initial database setup.
                // Calling dbmsProcessor.setupSharedDatabase() lets dbmsProcessor.checkBaseIntegrity() be true.
                if (dbmsProcessor.checkForPre3Dot6Intergrity()) {
                    throw new DatabaseNotSupportedException();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }

        dbmsProcessor.startNotificationListener(this);
        synchronizeLocalMetaData();
        synchronizeLocalDatabase();
    }

    /**
     * Synchronizes the local database with shared one.
     * Possible update types are removal, update or insert of a {@link BibEntry}.
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

        // compare versions and update local entry if needed
        for (Map.Entry<Integer, Integer> idVersionEntry : idVersionMap.entrySet()) {
            boolean match = false;
            for (BibEntry localEntry : localEntries) {
                if (idVersionEntry.getKey() == localEntry.getSharedBibEntryData().getSharedID()) {
                    match = true;
                    if (idVersionEntry.getValue() > localEntry.getSharedBibEntryData().getVersion()) {
                        Optional<BibEntry> sharedEntry = dbmsProcessor.getSharedEntry(idVersionEntry.getKey());
                        if (sharedEntry.isPresent()) {
                            // update fields
                            localEntry.setType(sharedEntry.get().getType(), EntryEventSource.SHARED);
                            localEntry.getSharedBibEntryData()
                                    .setVersion(sharedEntry.get().getSharedBibEntryData().getVersion());
                            for (String field : sharedEntry.get().getFieldNames()) {
                                localEntry.setField(field, sharedEntry.get().getField(field), EntryEventSource.SHARED);
                            }

                            Set<String> redundantLocalEntryFields = localEntry.getFieldNames();
                            redundantLocalEntryFields.removeAll(sharedEntry.get().getFieldNames());

                            // remove not existing fields
                            for (String redundantField : redundantLocalEntryFields) {
                                localEntry.clearField(redundantField, EntryEventSource.SHARED);
                            }
                        }
                    }
                }
            }
            if (!match) {
                Optional<BibEntry> bibEntry = dbmsProcessor.getSharedEntry(idVersionEntry.getKey());
                if (bibEntry.isPresent()) {
                    bibDatabase.insertEntry(bibEntry.get(), EntryEventSource.SHARED);
                }
            }
        }
    }

    /**
     * Removes all local entries which are not present on shared database.
     *
     * @param localEntries List of {@link BibEntry} the entries should be removed from
     * @param sharedIDs Set of all IDs which are present on shared database
     */
    private void removeNotSharedEntries(List<BibEntry> localEntries, Set<Integer> sharedIDs) {
        for (int i = 0; i < localEntries.size(); i++) {
            BibEntry localEntry = localEntries.get(i);
            boolean match = false;
            for (int sharedID : sharedIDs) {
                if (localEntry.getSharedBibEntryData().getSharedID() == sharedID) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                eventBus.post(new SharedEntryNotPresentEvent(localEntry));
                bibDatabase.removeEntry(localEntry, EntryEventSource.SHARED); // Should not reach the listeners above.
                i--; // due to index shift on localEntries
            }
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
            BibDatabaseWriter.applySaveActions(bibEntry, metaData); // perform possibly existing save actions
            dbmsProcessor.updateEntry(bibEntry);
        } catch (OfflineLockException exception) {
            eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
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
    private void synchronizeSharedMetaData(MetaData data, GlobalBibtexKeyPattern globalCiteKeyPattern) {
        if (!checkCurrentConnection()) {
            return;
        }
        try {
            dbmsProcessor.setSharedMetaData(MetaDataSerializer.getSerializedStringMap(data, globalCiteKeyPattern));
        } catch (SQLException e) {
            LOGGER.error("SQL Error: ", e);
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
            // synchronize only if changes were present
            if (!BibDatabaseWriter.applySaveActions(bibEntry, metaData).isEmpty()) {
                try {
                    dbmsProcessor.updateEntry(bibEntry);
                } catch (OfflineLockException exception) {
                    eventBus.post(new UpdateRefusedEvent(bibDatabaseContext, exception.getLocalBibEntry(), exception.getSharedBibEntry()));
                } catch (SQLException e) {
                    LOGGER.error("SQL Error: ", e);
                }
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

        synchronizeLocalDatabase();
        synchronizeLocalMetaData();
    }

    /**
     *  Checks whether the current SQL connection is valid.
     *  In case that the connection is not valid a new {@link ConnectionLostEvent} is going to be sent.
     *
     *  @return <code>true</code> if the connection is valid, else <code>false</code>.
     */
    public boolean checkCurrentConnection() {
        try {
            boolean isValid = currentConnection.isValid(0);
            if (!isValid) {
                eventBus.post(new ConnectionLostEvent(bibDatabaseContext));
            }
            return isValid;

        } catch (SQLException e) {
            LOGGER.error("SQL Error:", e);
            return false;
        }
    }

    /**
     * Checks whether the {@link EntryEventSource} of an {@link EntryEvent} is crucial for this class.
     *
     * @param event An {@link EntryEvent}
     * @return <code>true</code> if the event is able to trigger operations in {@link DBMSSynchronizer}, else <code>false</code>
     */
    public boolean isEventSourceAccepted(EntryEvent event) {
        EntryEventSource eventSource = event.getEntryEventSource();
        return ((eventSource == EntryEventSource.LOCAL) || (eventSource == EntryEventSource.UNDO));
    }

    @Override
    public void openSharedDatabase(DatabaseConnection connection) throws DatabaseNotSupportedException {
        this.dbName = connection.getProperties().getDatabase();
        this.currentConnection = connection.getConnection();
        this.dbmsProcessor = DBMSProcessor.getProcessorInstance(connection);
        initializeDatabases();
    }

    @Override
    public void closeSharedDatabase() {
        try {
            dbmsProcessor.stopNotificationListener();
            currentConnection.close();
        } catch (SQLException e) {
            LOGGER.error("SQL Error:", e);
        }
    }

    private boolean isPresentLocalBibEntry(BibEntry bibEntry) {
        return bibDatabase.getEntries().contains(bibEntry);
    }

    @Override
    public String getDBName() {
        return dbName;
    }

    public DBMSProcessor getDBProcessor() {
        return dbmsProcessor;
    }

    @Override
    public DatabaseConnectionProperties getConnectionProperties() {
        return dbmsProcessor.getDBMSConnectionProperties();
    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }
}
