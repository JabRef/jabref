package org.jabref.logic.shared;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.logic.shared.exception.SharedEntryNotPresentException;
import org.jabref.logic.shared.notifications.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// More tests are located at [org.jabref.logic.shared.SynchronizationSimulatorTest] and [org.jabref.logic.shared.DBMSProcessorTest].
@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
class DBMSSynchronizerTest {

    @TempDir
    Path offlineChangesDirectory;

    private DBMSSynchronizer dbmsSynchronizer;
    private BibDatabase bibDatabase;
    private final GlobalCitationKeyPatterns pattern = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
    private DBMSConnection dbmsConnection;
    private DBMSProcessor dbmsProcessor;
    private ConnectorTest connectorTest;

    private BibEntry createExampleBibEntry(int index) {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Wirthlin, Michael J" + index)
                .withField(StandardField.TITLE, "The nano processor" + index);
        bibEntry.getSharedBibEntryData().setSharedId(index);
        return bibEntry;
    }

    @BeforeEach
    void setup() throws Exception {
        this.connectorTest = new ConnectorTest();
        this.dbmsConnection = connectorTest.getTestDBMSConnection();
        this.dbmsProcessor = new DBMSProcessor(this.dbmsConnection);
        TestManager.clearTables(this.dbmsConnection);
        this.dbmsProcessor.setupSharedDatabase();

        bibDatabase = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());

        dbmsSynchronizer = new DBMSSynchronizer(context, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost");
        bibDatabase.registerListener(dbmsSynchronizer);

        dbmsSynchronizer.openSharedDatabase(dbmsConnection);
    }

    @AfterEach
    void closeDbmsConnection() throws Exception {
        connectorTest.close();
    }

    @Test
    void entryAddedEventListener() throws SQLException {
        BibEntry expectedEntry = createExampleBibEntry(1);
        BibEntry furtherEntry = createExampleBibEntry(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add into shared database.
        bibDatabase.insertEntry(furtherEntry, EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(List.of(expectedEntry), actualEntries);
    }

    @Test
    void twoLocalFieldChangesAreSynchronizedCorrectly() throws SQLException {
        BibEntry expectedEntry = createExampleBibEntry(1);
        expectedEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(expectedEntry);

        expectedEntry.setField(StandardField.AUTHOR, "Brad L and Gilson");
        expectedEntry.setField(StandardField.TITLE, "The micro multiplexer");

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(List.of(expectedEntry), actualEntries);
    }

    @Test
    void oneLocalAndOneSharedFieldChangeIsSynchronizedCorrectly() throws SQLException {
        BibEntry exampleBibEntry = createExampleBibEntry(1);
        exampleBibEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(exampleBibEntry);
        exampleBibEntry.setField(StandardField.AUTHOR, "Brad L and Gilson");
        // shared updates are not synchronized back to the remote database
        exampleBibEntry.setField(StandardField.TITLE, "The micro multiplexer", EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        BibEntry expectedBibEntry = createExampleBibEntry(1)
                .withField(StandardField.AUTHOR, "Brad L and Gilson");

        assertEquals(List.of(expectedBibEntry), actualEntries);
    }

    @Test
    void remoteFieldChangeIsAppliedOnConfiguredExecutor() throws Exception {
        List<Runnable> remoteUpdates = new ArrayList<>();
        BibDatabase remoteDatabase = new BibDatabase();
        BibDatabaseContext remoteContext = new BibDatabaseContext(remoteDatabase);
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        DBMSSynchronizer remoteSynchronizer = new DBMSSynchronizer(
                remoteContext,
                ',',
                fieldPreferences,
                pattern,
                new DummyFileUpdateMonitor(),
                "UserAndHost",
                remoteUpdates::add);
        remoteDatabase.registerListener(remoteSynchronizer);
        remoteSynchronizer.openSharedDatabase(connectorTest.getTestDBMSConnection());

        try {
            BibEntry localEntry = createExampleBibEntry(1);
            remoteDatabase.insertEntry(localEntry);

            String oldTitle = localEntry.getField(StandardField.TITLE).orElseThrow();
            remoteSynchronizer.handleRemoteFieldChange(new FieldChange(
                    "remote",
                    localEntry.getSharedBibEntryData().getSharedIdAsString(),
                    StandardField.TITLE.getName(),
                    oldTitle,
                    "Updated title",
                    localEntry.getSharedBibEntryData().getVersion() + 1));

            assertEquals(1, remoteUpdates.size());
            assertEquals(oldTitle, localEntry.getField(StandardField.TITLE).orElseThrow());

            remoteUpdates.getFirst().run();

            assertEquals("Updated title", localEntry.getField(StandardField.TITLE).orElseThrow());
        } finally {
            remoteSynchronizer.closeSharedDatabase();
        }
    }

    @Test
    void entriesRemovedEventListener() throws SQLException {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(bibEntry, actualEntries.getFirst());

        bibDatabase.removeEntry(bibEntry);
        actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(0, actualEntries.size());

        bibDatabase.insertEntry(bibEntry);
        bibDatabase.removeEntry(bibEntry, EntriesEventSource.SHARED);

        actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(bibEntry, actualEntries.getFirst());
    }

    @Test
    void metaDataChangedEventListener() throws SQLException {
        MetaData testMetaData = new MetaData();
        testMetaData.registerListener(dbmsSynchronizer);
        dbmsSynchronizer.setMetaData(testMetaData);
        testMetaData.setMode(BibDatabaseMode.BIBTEX);

        Map<String, String> expectedMap = MetaDataSerializer.getSerializedStringMap(testMetaData, pattern);
        Map<String, String> actualMap = dbmsProcessor.getSharedMetaData();
        actualMap.remove("VersionDBStructure");

        assertEquals(expectedMap, actualMap);
    }

    @Test
    void initializeDatabases() throws DatabaseNotSupportedException, SQLException {
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    void synchronizeLocalDatabaseWithEntryRemoval() throws SQLException {
        List<BibEntry> expectedBibEntries = Arrays.asList(createExampleBibEntry(1), createExampleBibEntry(2));

        dbmsProcessor.insertEntry(expectedBibEntries.getFirst());
        dbmsProcessor.insertEntry(expectedBibEntries.get(1));

        assertTrue(bibDatabase.getEntries().isEmpty());

        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(expectedBibEntries, bibDatabase.getEntries());

        dbmsProcessor.removeEntries(List.of(expectedBibEntries.getFirst()));

        expectedBibEntries = List.of(expectedBibEntries.get(1));

        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(expectedBibEntries, bibDatabase.getEntries());
    }

    @Test
    void synchronizeLocalDatabaseWithEntryUpdate() throws SQLException, OfflineLockException, SharedEntryNotPresentException {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);
        assertEquals(List.of(bibEntry), bibDatabase.getEntries());

        BibEntry modifiedBibEntry = createExampleBibEntry(1)
                .withField(new UnknownField("custom"), "custom value");
        modifiedBibEntry.clearField(StandardField.TITLE);
        modifiedBibEntry.setType(StandardEntryType.Article);

        dbmsProcessor.updateEntry(modifiedBibEntry);
        assertEquals(1, modifiedBibEntry.getSharedBibEntryData().getSharedIdAsInt());
        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(List.of(modifiedBibEntry), bibDatabase.getEntries());
        assertEquals(List.of(modifiedBibEntry), dbmsProcessor.getSharedEntries());
    }

    @Test
    void updateEntryDoesNotModifyLocalDatabase() throws SQLException, OfflineLockException, SharedEntryNotPresentException {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);
        assertEquals(List.of(bibEntry), bibDatabase.getEntries());

        BibEntry modifiedBibEntry = createExampleBibEntry(1)
                .withField(new UnknownField("custom"), "custom value");
        modifiedBibEntry.clearField(StandardField.TITLE);
        modifiedBibEntry.setType(StandardEntryType.Article);

        dbmsProcessor.updateEntry(modifiedBibEntry);

        assertEquals(List.of(bibEntry), bibDatabase.getEntries());
        assertEquals(List.of(modifiedBibEntry), dbmsProcessor.getSharedEntries());
    }

    @Test
    void applyMetaData() {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);

        MetaData testMetaData = new MetaData();
        testMetaData.setSaveActions(new FieldFormatterCleanupActions(true, List.of(new FieldFormatterCleanup(StandardField.AUTHOR, new LowerCaseFormatter()))));
        dbmsSynchronizer.setMetaData(testMetaData);

        dbmsSynchronizer.applyMetaData();

        assertEquals("wirthlin, michael j1", bibEntry.getField(StandardField.AUTHOR).get());
    }

    @Test
    void failedPullKeepsLocalEntries() throws SQLException {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);

        // Make the pull's query fail although the connection itself is fine
        dbmsConnection.getConnection().createStatement().executeUpdate("ALTER TABLE jabref.entry RENAME TO entry_unavailable");
        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(List.of(bibEntry), bibDatabase.getEntries());
    }

    @Test
    void pullDoesNotRemoveEntryInsertedWhileApplying() throws Exception {
        List<Runnable> remoteUpdates = new ArrayList<>();
        BibDatabase database = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(database);
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        // Database work synchronous, model updates captured
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(context, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost", remoteUpdates::add, Runnable::run, offlineChangesDirectory);
        database.registerListener(synchronizer);
        synchronizer.openSharedDatabase(connectorTest.getTestDBMSConnection());

        try {
            // The fetch happens now, the apply is captured
            synchronizer.synchronizeLocalDatabase();
            assertEquals(1, remoteUpdates.size());

            // Meanwhile the user adds an entry, which reaches the database before the apply runs
            BibEntry newEntry = createExampleBibEntry(1);
            database.insertEntry(newEntry);
            assertEquals(1, dbmsProcessor.getSharedEntries().size());

            remoteUpdates.getFirst().run();

            assertEquals(List.of(newEntry), database.getEntries());
        } finally {
            synchronizer.closeSharedDatabase();
        }
    }

    /// Awaits an asynchronously arriving state change (reconnect-thread work) with a bounded deadline.
    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
        while (!condition.getAsBoolean() && (System.currentTimeMillis() < deadline)) {
            Thread.sleep(50);
        }
    }

    private DBMSSynchronizer newSynchronousSynchronizer(BibDatabaseContext context) {
        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        DBMSSynchronizer synchronizer = new DBMSSynchronizer(context, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost", Runnable::run, Runnable::run, offlineChangesDirectory);
        context.getDatabase().registerListener(synchronizer);
        return synchronizer;
    }

    @Test
    void changesMadeWhileDisconnectedAreSynchronizedAfterReconnect() throws Exception {
        BibDatabase database = new BibDatabase();
        DBMSSynchronizer synchronizer = newSynchronousSynchronizer(new BibDatabaseContext(database));
        DBMSConnection connection = connectorTest.getTestDBMSConnection();
        synchronizer.openSharedDatabase(connection);
        try {
            BibEntry entry = createExampleBibEntry(1);
            database.insertEntry(entry);
            int sharedId = entry.getSharedBibEntryData().getSharedIdAsInt();

            // The connection dies underneath the synchronizer
            connection.getConnection().close();
            entry.setField(StandardField.TITLE, "Edited while disconnected");
            BibEntry added = createExampleBibEntry(2);
            database.insertEntry(added);

            // Both changes are recorded on disk, not written
            assertTrue(Files.exists(offlineChangesDirectory.resolve(OfflineChanges.fileName(connection.getProperties()))));
            assertEquals(Optional.of("The nano processor1"), dbmsProcessor.getSharedEntry(sharedId).map(shared -> shared.getField(StandardField.TITLE).orElseThrow()));

            // The reconnect loop finds the database again and writes them
            waitUntil(() -> {
                try {
                    return dbmsProcessor.getSharedEntries().size() == 2;
                } catch (SQLException e) {
                    return false;
                }
            });
            assertEquals(Optional.of("Edited while disconnected"), dbmsProcessor.getSharedEntry(sharedId).map(shared -> shared.getField(StandardField.TITLE).orElseThrow()));
            assertEquals(2, dbmsProcessor.getSharedEntries().size());
            assertFalse(Files.exists(offlineChangesDirectory.resolve(OfflineChanges.fileName(connection.getProperties()))));
        } finally {
            synchronizer.closeSharedDatabase();
        }
    }

    @Test
    void recordedChangesAreSynchronizedOnNextConnect() throws Exception {
        BibEntry sharedEntry = createExampleBibEntry(1);
        dbmsProcessor.insertEntry(sharedEntry);
        int sharedId = sharedEntry.getSharedBibEntryData().getSharedIdAsInt();

        // What an earlier session left behind: an edit against version 1 and a new entry
        DBMSConnection connection = connectorTest.getTestDBMSConnection();
        OfflineChanges recorded = OfflineChanges.load(offlineChangesDirectory, connection.getProperties());
        BibEntry editedOffline = createExampleBibEntry(1).withField(StandardField.TITLE, "Edited in an earlier session");
        recorded.recordChange(editedOffline);
        recorded.recordInsert(List.of(createExampleBibEntry(2)));

        BibDatabase database = new BibDatabase();
        DBMSSynchronizer synchronizer = newSynchronousSynchronizer(new BibDatabaseContext(database));
        synchronizer.openSharedDatabase(connection);
        try {
            assertEquals(2, database.getEntryCount());
            assertEquals(Optional.of("Edited in an earlier session"), dbmsProcessor.getSharedEntry(sharedId).map(shared -> shared.getField(StandardField.TITLE).orElseThrow()));
            assertEquals(2, dbmsProcessor.getSharedEntries().size());
            assertTrue(OfflineChanges.load(offlineChangesDirectory, connection.getProperties()).isEmpty());
        } finally {
            synchronizer.closeSharedDatabase();
        }
    }

    @Test
    void recordedChangeAgainstOutdatedVersionIsReportedAsConflict() throws Exception {
        BibEntry sharedEntry = createExampleBibEntry(1);
        dbmsProcessor.insertEntry(sharedEntry);
        // Recorded against version 1 ...
        DBMSConnection connection = connectorTest.getTestDBMSConnection();
        OfflineChanges.load(offlineChangesDirectory, connection.getProperties())
                      .recordChange(createExampleBibEntry(1).withField(StandardField.TITLE, "Edited offline"));
        // ... but another client changed the entry meanwhile
        sharedEntry.setField(StandardField.TITLE, "Changed by another client");
        dbmsProcessor.updateEntry(sharedEntry);

        BibDatabase database = new BibDatabase();
        DBMSSynchronizer synchronizer = newSynchronousSynchronizer(new BibDatabaseContext(database));
        SynchronizationEventListenerTest events = new SynchronizationEventListenerTest();
        synchronizer.registerListener(events);
        synchronizer.openSharedDatabase(connection);
        try {
            assertNotNull(events.getUpdateRefusedEvent());
            assertEquals(Optional.of("Edited offline"), events.getUpdateRefusedEvent().localBibEntry().getField(StandardField.TITLE));
            assertEquals(Optional.of("Changed by another client"), events.getUpdateRefusedEvent().sharedBibEntry().getField(StandardField.TITLE));
            // The local entry keeps the offline edit for the merge
            assertEquals(Optional.of("Edited offline"), database.getEntries().getFirst().getField(StandardField.TITLE));
        } finally {
            synchronizer.closeSharedDatabase();
        }
    }
}
