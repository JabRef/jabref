package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyDirectoryUpdateMonitor;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
class DBMSSynchronizerTest {

    private DBMSSynchronizer dbmsSynchronizer;
    private BibDatabase bibDatabase;
    private final GlobalCitationKeyPatterns pattern = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
    private DBMSProcessor dbmsProcessor;

    private BibEntry createExampleBibEntry(int index) {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Wirthlin, Michael J" + index)
                .withField(StandardField.TITLE, "The nano processor" + index);
        bibEntry.getSharedBibEntryData().setSharedID(index);
        return bibEntry;
    }

    @BeforeEach
    void setup() throws SQLException, InvalidDBMSConnectionPropertiesException, DatabaseNotSupportedException {
        DBMSType dbmsType = TestManager.getDBMSTypeTestParameter();
        DBMSConnection dbmsConnection = ConnectorTest.getTestDBMSConnection(dbmsType);
        this.dbmsProcessor = DBMSProcessor.getProcessorInstance(dbmsConnection);
        TestManager.clearTables(dbmsConnection);
        this.dbmsProcessor.setupSharedDatabase();

        bibDatabase = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());

        dbmsSynchronizer = new DBMSSynchronizer(context, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), new DummyDirectoryUpdateMonitor(), "UserAndHost");
        bibDatabase.registerListener(dbmsSynchronizer);

        dbmsSynchronizer.openSharedDatabase(dbmsConnection);
    }

    @AfterEach
    void clear() {
        dbmsSynchronizer.closeSharedDatabase();
    }

    @Test
    void entryAddedEventListener() {
        BibEntry expectedEntry = createExampleBibEntry(1);
        BibEntry furtherEntry = createExampleBibEntry(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add into shared database.
        bibDatabase.insertEntry(furtherEntry, EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(List.of(expectedEntry), actualEntries);
    }

    @Test
    void twoLocalFieldChangesAreSynchronizedCorrectly() {
        BibEntry expectedEntry = createExampleBibEntry(1);
        expectedEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(expectedEntry);
        expectedEntry.setField(StandardField.AUTHOR, "Brad L and Gilson");
        expectedEntry.setField(StandardField.TITLE, "The micro multiplexer");

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(List.of(expectedEntry), actualEntries);
    }

    @Test
    void oneLocalAndOneSharedFieldChangeIsSynchronizedCorrectly() {
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
    void entriesRemovedEventListener() {
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
    void metaDataChangedEventListener() {
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
    void synchronizeLocalDatabaseWithEntryRemoval() {
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
    void synchronizeLocalDatabaseWithEntryUpdate() throws SQLException, OfflineLockException {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);
        assertEquals(List.of(bibEntry), bibDatabase.getEntries());

        BibEntry modifiedBibEntry = createExampleBibEntry(1)
                .withField(new UnknownField("custom"), "custom value");
        modifiedBibEntry.clearField(StandardField.TITLE);
        modifiedBibEntry.setType(StandardEntryType.Article);

        dbmsProcessor.updateEntry(modifiedBibEntry);
        assertEquals(1, modifiedBibEntry.getSharedBibEntryData().getSharedID());
        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(List.of(modifiedBibEntry), bibDatabase.getEntries());
        assertEquals(List.of(modifiedBibEntry), dbmsProcessor.getSharedEntries());
    }

    @Test
    void updateEntryDoesNotModifyLocalDatabase() throws SQLException, OfflineLockException {
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
        testMetaData.setSaveActions(new FieldFormatterCleanups(true, List.of(new FieldFormatterCleanup(StandardField.AUTHOR, new LowerCaseFormatter()))));
        dbmsSynchronizer.setMetaData(testMetaData);

        dbmsSynchronizer.applyMetaData();

        assertEquals("wirthlin, michael j1", bibEntry.getField(StandardField.AUTHOR).get());
    }
}
