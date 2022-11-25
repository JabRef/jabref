package org.jabref.logic.shared;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
public class DBMSSynchronizerTest {

    private DBMSSynchronizer dbmsSynchronizer;
    private BibDatabase bibDatabase;
    private final GlobalCitationKeyPattern pattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");
    private DBMSConnection dbmsConnection;
    private DBMSProcessor dbmsProcessor;
    private DBMSType dbmsType;

    private BibEntry createExampleBibEntry(int index) {
        BibEntry bibEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Wirthlin, Michael J" + index)
                .withField(StandardField.TITLE, "The nano processor" + index);
        bibEntry.getSharedBibEntryData().setSharedID(index);
        return bibEntry;
    }

    @BeforeEach
    public void setup() throws Exception {
        this.dbmsType = TestManager.getDBMSTypeTestParameter();
        this.dbmsConnection = TestConnector.getTestDBMSConnection(dbmsType);
        this.dbmsProcessor = DBMSProcessor.getProcessorInstance(this.dbmsConnection);
        TestManager.clearTables(this.dbmsConnection);
        this.dbmsProcessor.setupSharedDatabase();

        bibDatabase = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);

        dbmsSynchronizer = new DBMSSynchronizer(context, ',', pattern, new DummyFileUpdateMonitor());
        bibDatabase.registerListener(dbmsSynchronizer);

        dbmsSynchronizer.openSharedDatabase(dbmsConnection);
    }

    @AfterEach
    public void clear() {
        dbmsSynchronizer.closeSharedDatabase();
    }

    @Test
    public void testEntryAddedEventListener() throws Exception {
        BibEntry expectedEntry = createExampleBibEntry(1);
        BibEntry furtherEntry = createExampleBibEntry(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add into shared database.
        bibDatabase.insertEntry(furtherEntry, EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(List.of(expectedEntry), actualEntries);
    }

    @Test
    public void twoLocalFieldChangesAreSynchronizedCorrectly() throws Exception {
        BibEntry expectedEntry = createExampleBibEntry(1);
        expectedEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(expectedEntry);
        expectedEntry.setField(StandardField.AUTHOR, "Brad L and Gilson");
        expectedEntry.setField(StandardField.TITLE, "The micro multiplexer");

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(Collections.singletonList(expectedEntry), actualEntries);
    }

    @Test
    public void oneLocalAndOneSharedFieldChangeIsSynchronizedCorrectly() throws Exception {
        BibEntry exampleBibEntry = createExampleBibEntry(1);
        exampleBibEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(exampleBibEntry);
        exampleBibEntry.setField(StandardField.AUTHOR, "Brad L and Gilson");
        // shared updates are not synchronized back to the remote database
        exampleBibEntry.setField(StandardField.TITLE, "The micro multiplexer", EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        BibEntry expectedBibEntry = createExampleBibEntry(1)
                .withField(StandardField.AUTHOR, "Brad L and Gilson");

        assertEquals(Collections.singletonList(expectedBibEntry), actualEntries);
    }

    @Test
    public void testEntriesRemovedEventListener() throws Exception {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(bibEntry, actualEntries.get(0));

        bibDatabase.removeEntry(bibEntry);
        actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(0, actualEntries.size());

        bibDatabase.insertEntry(bibEntry);
        bibDatabase.removeEntry(bibEntry, EntriesEventSource.SHARED);

        actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(bibEntry, actualEntries.get(0));
    }

    @Test
    public void testMetaDataChangedEventListener() throws Exception {
        MetaData testMetaData = new MetaData();
        testMetaData.registerListener(dbmsSynchronizer);
        dbmsSynchronizer.setMetaData(testMetaData);
        testMetaData.setMode(BibDatabaseMode.BIBTEX);

        Map<String, String> expectedMap = MetaDataSerializer.getSerializedStringMap(testMetaData, pattern);
        Map<String, String> actualMap = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void testInitializeDatabases() throws Exception {
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    public void testSynchronizeLocalDatabaseWithEntryRemoval() throws Exception {
        List<BibEntry> expectedBibEntries = Arrays.asList(createExampleBibEntry(1), createExampleBibEntry(2));

        dbmsProcessor.insertEntry(expectedBibEntries.get(0));
        dbmsProcessor.insertEntry(expectedBibEntries.get(1));

        assertTrue(bibDatabase.getEntries().isEmpty());

        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(expectedBibEntries, bibDatabase.getEntries());

        dbmsProcessor.removeEntries(Collections.singletonList(expectedBibEntries.get(0)));

        expectedBibEntries = Collections.singletonList(expectedBibEntries.get(1));

        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(expectedBibEntries, bibDatabase.getEntries());
    }

    @Test
    public void testSynchronizeLocalDatabaseWithEntryUpdate() throws Exception {
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
    public void updateEntryDoesNotModifyLocalDatabase() throws Exception {
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
    public void testApplyMetaData() throws Exception {
        BibEntry bibEntry = createExampleBibEntry(1);
        bibDatabase.insertEntry(bibEntry);

        MetaData testMetaData = new MetaData();
        testMetaData.setSaveActions(new FieldFormatterCleanups(true, Collections.singletonList(new FieldFormatterCleanup(StandardField.AUTHOR, new LowerCaseFormatter()))));
        dbmsSynchronizer.setMetaData(testMetaData);

        dbmsSynchronizer.applyMetaData();

        assertEquals("wirthlin, michael j1", bibEntry.getField(StandardField.AUTHOR).get());
    }
}
