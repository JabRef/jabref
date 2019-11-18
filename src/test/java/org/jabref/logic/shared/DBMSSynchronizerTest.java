package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseTest
public class DBMSSynchronizerTest {

    private DBMSSynchronizer dbmsSynchronizer;
    private BibDatabase bibDatabase;
    private final GlobalBibtexKeyPattern pattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");

    public void setUp(DBMSConnection dbmsConnection) throws Exception {
        clear(dbmsConnection);

        bibDatabase = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);
        dbmsSynchronizer = new DBMSSynchronizer(context, ',', pattern, new DummyFileUpdateMonitor());

        bibDatabase.registerListener(dbmsSynchronizer);

        dbmsSynchronizer.openSharedDatabase(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testEntryAddedEventListener(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

        BibEntry expectedEntry = getBibEntryExample(1);
        BibEntry furtherEntry = getBibEntryExample(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add into shared database.
        bibDatabase.insertEntry(furtherEntry, EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();

        assertEquals(1, actualEntries.size());
        assertEquals(expectedEntry, actualEntries.get(0));

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testFieldChangedEventListener(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

        BibEntry expectedEntry = getBibEntryExample(1);
        expectedEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(expectedEntry);
        expectedEntry.setField(StandardField.AUTHOR, "Brad L and Gilson");
        expectedEntry.setField(StandardField.TITLE, "The micro multiplexer", EntriesEventSource.SHARED);

        List<BibEntry> actualEntries = dbmsProcessor.getSharedEntries();
        assertEquals(Collections.singletonList(expectedEntry), actualEntries);

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testEntriesRemovedEventListener(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

>>>>>>> master
        BibEntry bibEntry = getBibEntryExample(1);
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

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testMetaDataChangedEventListener(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

        MetaData testMetaData = new MetaData();
        testMetaData.registerListener(dbmsSynchronizer);
        dbmsSynchronizer.setMetaData(testMetaData);
        testMetaData.setMode(BibDatabaseMode.BIBTEX);

        Map<String, String> expectedMap = MetaDataSerializer.getSerializedStringMap(testMetaData, pattern);
        Map<String, String> actualMap = dbmsProcessor.getSharedMetaData();

        assertEquals(expectedMap, actualMap);

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testInitializeDatabases(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

        clear(dbmsConnection);
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        dbmsSynchronizer.initializeDatabases();
        assertTrue(dbmsProcessor.checkBaseIntegrity());
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testSynchronizeLocalDatabaseWithEntryRemoval(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

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

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testSynchronizeLocalDatabaseWithEntryUpdate(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);
        assertEquals(1, bibDatabase.getEntries().size());

        BibEntry modifiedBibEntry = getBibEntryExample(1);
        modifiedBibEntry.setField(new UnknownField("custom"), "custom value");
        modifiedBibEntry.clearField(StandardField.TITLE);
        modifiedBibEntry.setType(StandardEntryType.Article);

        dbmsProcessor.updateEntry(modifiedBibEntry);
        //testing point
        dbmsSynchronizer.synchronizeLocalDatabase();

        assertEquals(bibDatabase.getEntries(), dbmsProcessor.getSharedEntries());
        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void testApplyMetaData(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsConnection);

        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        MetaData testMetaData = new MetaData();
        testMetaData.setSaveActions(new FieldFormatterCleanups(true, Collections.singletonList(new FieldFormatterCleanup(StandardField.AUTHOR, new LowerCaseFormatter()))));
        dbmsSynchronizer.setMetaData(testMetaData);

        dbmsSynchronizer.applyMetaData();

        assertEquals("wirthlin, michael j1", bibEntry.getField(StandardField.AUTHOR).get());

        clear(dbmsConnection);
    }

    private BibEntry getBibEntryExample(int index) {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType(StandardEntryType.Book);
        bibEntry.setField(StandardField.AUTHOR, "Wirthlin, Michael J" + index);
        bibEntry.setField(StandardField.TITLE, "The nano processor" + index);
        bibEntry.getSharedBibEntryData().setSharedID(index);
        return bibEntry;
    }

    public void clear(DBMSConnection dbmsConnection) throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
