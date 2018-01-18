package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.exception.OfflineLockException;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEventSource;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseTest
public class DBMSSynchronizerTest {

    private DBMSSynchronizer dbmsSynchronizer;
    private DBMSConnection dbmsConnection;
    private DBMSProcessor dbmsProcessor;
    private BibDatabase bibDatabase;
    private GlobalBibtexKeyPattern pattern;

    @Parameter
    public DBMSType dbmsType;

    @BeforeEach
    public void setUp() throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException {

        dbmsConnection = TestConnector.getTestDBMSConnection(dbmsType);

        bibDatabase = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);

        pattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");

        dbmsSynchronizer = new DBMSSynchronizer(context, ',', pattern, new DummyFileUpdateMonitor());
        dbmsProcessor = DBMSProcessor.getProcessorInstance(dbmsConnection);

        bibDatabase.registerListener(dbmsSynchronizer);

        dbmsSynchronizer.openSharedDatabase(dbmsConnection);

    }

    @Parameters(name = "Test with {0} database system")
    public static Collection<DBMSType> getTestingDatabaseSystems() {
        return TestManager.getDBMSTypeTestParameter();
    }

    @Test
    public void testEntryAddedEventListener() {
        BibEntry expectedEntry = getBibEntryExample(1);
        BibEntry furtherEntry = getBibEntryExample(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add into shared database.
        bibDatabase.insertEntry(furtherEntry, EntryEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(1, actualEntries.size());
        assertEquals(expectedEntry, actualEntries.get(0));
    }

    @Test
    public void testFieldChangedEventListener() {
        BibEntry expectedEntry = getBibEntryExample(1);
        expectedEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(expectedEntry);
        expectedEntry.setField("author", "Brad L and Gilson");
        expectedEntry.setField("title", "The micro multiplexer", EntryEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(expectedEntry.getField("author"), actualEntries.get(0).getField("author"));
        assertEquals("The nano processor1", actualEntries.get(0).getField("title").get());

    }

    @Test
    public void testEntryRemovedEventListener() {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(bibEntry, actualEntries.get(0));

        bibDatabase.removeEntry(bibEntry);
        actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(0, actualEntries.size());

        bibDatabase.insertEntry(bibEntry);
        bibDatabase.removeEntry(bibEntry, EntryEventSource.SHARED);

        actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(1, actualEntries.size());
        assertEquals(bibEntry, actualEntries.get(0));
    }

    @Test
    public void testMetaDataChangedEventListener() {
        MetaData testMetaData = new MetaData();
        testMetaData.registerListener(dbmsSynchronizer);
        dbmsSynchronizer.setMetaData(testMetaData);
        testMetaData.setMode(BibDatabaseMode.BIBTEX);

        Map<String, String> expectedMap = MetaDataSerializer.getSerializedStringMap(testMetaData, pattern);
        Map<String, String> actualMap = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMap, actualMap);
    }

    @Test
    public void testInitializeDatabases() throws SQLException, DatabaseNotSupportedException {
        clear();
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    public void testSynchronizeLocalDatabaseWithEntryRemoval() {
        List<BibEntry> expectedBibEntries = Arrays.asList(getBibEntryExample(1), getBibEntryExample(2));

        dbmsProcessor.insertEntry(expectedBibEntries.get(0));
        dbmsProcessor.insertEntry(expectedBibEntries.get(1));

        assertTrue(bibDatabase.getEntries().isEmpty());

        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(expectedBibEntries, bibDatabase.getEntries());

        dbmsProcessor.removeEntry(expectedBibEntries.get(0));
        dbmsProcessor.removeEntry(expectedBibEntries.get(1));

        expectedBibEntries = new ArrayList<>();

        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(expectedBibEntries, bibDatabase.getEntries());
    }

    @Test
    public void testSynchronizeLocalDatabaseWithEntryUpdate() throws OfflineLockException, SQLException {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);
        assertEquals(1, bibDatabase.getEntries().size());

        BibEntry modifiedBibEntry = getBibEntryExample(1);
        modifiedBibEntry.setField("custom", "custom value");
        modifiedBibEntry.clearField("title");
        modifiedBibEntry.setType("article");

        dbmsProcessor.updateEntry(modifiedBibEntry);
        //testing point
        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(bibDatabase.getEntries(), dbmsProcessor.getSharedEntries());
    }

    @Test
    public void testApplyMetaData() {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        MetaData testMetaData = new MetaData();
        testMetaData.setSaveActions(new FieldFormatterCleanups(true,
                Collections.singletonList(new FieldFormatterCleanup("author", new LowerCaseFormatter()))));
        dbmsSynchronizer.setMetaData(testMetaData);

        dbmsSynchronizer.applyMetaData();

        assertEquals("wirthlin, michael j1", bibEntry.getField("author").get());

    }

    private BibEntry getBibEntryExample(int index) {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType("book");
        bibEntry.setField("author", "Wirthlin, Michael J" + index);
        bibEntry.setField("title", "The nano processor" + index);
        bibEntry.getSharedBibEntryData().setSharedID(index);
        return bibEntry;
    }

    @AfterEach
    public void clear() throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }

}
