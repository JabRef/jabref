package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.event.source.EntryEventSource;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DBMSSynchronizerTest {

    private DBMSSynchronizer dbmsSynchronizer;
    private Connection connection;
    private DBMSProcessor dbmsProcessor;
    private BibDatabase bibDatabase;

    @Parameter
    public DBMSType dbmsType;


    @Before
    public void setUp() {

        Globals.prefs = JabRefPreferences.getInstance();

        try {
            connection = TestConnector.getTestConnection(dbmsType);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        bibDatabase = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(bibDatabase);


        dbmsSynchronizer = new DBMSSynchronizer(context);
        dbmsProcessor = new DBMSProcessor(new DBMSHelper(connection), dbmsType);

        bibDatabase.registerListener(dbmsSynchronizer);

        dbmsSynchronizer.openRemoteDatabase(connection, dbmsType, "TEST");
    }

    @Parameters(name = "Test with {0} database system")
    public static Collection<DBMSType> getTestingDatabaseSystems() {
        return DBMSConnector.getAvailableDBMSTypes();
    }

    @Test
    public void testEntryAddedEventListener() {
        BibEntry expectedEntry = getBibEntryExample(1);
        BibEntry furtherEntry = getBibEntryExample(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add remotely.
        bibDatabase.insertEntry(furtherEntry, EntryEventSource.REMOTE);

        List<BibEntry> actualEntries = dbmsProcessor.getRemoteEntries();

        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(expectedEntry, actualEntries.get(0));
    }

    @Test
    public void testFieldChangedEventListener() {
        BibEntry expectedEntry = getBibEntryExample(1);
        expectedEntry.registerListener(dbmsSynchronizer);

        bibDatabase.insertEntry(expectedEntry);
        expectedEntry.setField("author", "Brad L and Gilson");
        expectedEntry.setField("title", "The micro multiplexer", EntryEventSource.REMOTE);

        List<BibEntry> actualEntries = dbmsProcessor.getRemoteEntries();
        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(expectedEntry.getFieldOptional("author"), actualEntries.get(0).getFieldOptional("author"));
        Assert.assertEquals("The nano processor1", actualEntries.get(0).getFieldOptional("title").get());

    }

    @Test
    public void testEntryRemovedEventListener() {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        List<BibEntry> actualEntries = dbmsProcessor.getRemoteEntries();
        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(bibEntry, actualEntries.get(0));

        bibDatabase.removeEntry(bibEntry);
        actualEntries = dbmsProcessor.getRemoteEntries();

        Assert.assertEquals(0, actualEntries.size());

        bibDatabase.insertEntry(bibEntry);
        bibDatabase.removeEntry(bibEntry, EntryEventSource.REMOTE);

        actualEntries = dbmsProcessor.getRemoteEntries();
        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(bibEntry, actualEntries.get(0));
    }

    @Test
    public void testMetaDataChangedEventListener() {
        MetaData testMetaData = new MetaData();
        testMetaData.registerListener(dbmsSynchronizer);
        dbmsSynchronizer.setMetaData(testMetaData);
        testMetaData.putData("databaseType", Arrays.asList("bibtex"));

        Map<String, String> expectedMap = testMetaData.getAsStringMap();
        Map<String, String> actualMap = dbmsProcessor.getRemoteMetaData();

        Assert.assertEquals(expectedMap, actualMap);
    }

    @Test
    public void testInitializeDatabases() {
        clear();
        dbmsSynchronizer.initializeDatabases();
        Assert.assertTrue(dbmsProcessor.checkBaseIntegrity());
        dbmsSynchronizer.initializeDatabases();
        Assert.assertTrue(dbmsProcessor.checkBaseIntegrity());
    }

    @Test
    public void testSynchronizeLocalDatabase() {
        List<BibEntry> expectedBibEntries = Arrays.asList(getBibEntryExample(1), getBibEntryExample(2));

        dbmsProcessor.insertEntry(expectedBibEntries.get(0));
        dbmsProcessor.insertEntry(expectedBibEntries.get(1));

        Assert.assertTrue(bibDatabase.getEntries().isEmpty());

        dbmsSynchronizer.synchronizeLocalDatabase();

        Assert.assertEquals(expectedBibEntries, bibDatabase.getEntries());

        dbmsProcessor.removeEntry(expectedBibEntries.get(0));
        dbmsProcessor.removeEntry(expectedBibEntries.get(1));
        expectedBibEntries = new ArrayList<>();

        dbmsSynchronizer.synchronizeLocalDatabase();

        Assert.assertEquals(expectedBibEntries, bibDatabase.getEntries());
    }

    @Test
    public void testApplyMetaData() {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        MetaData testMetaData = new MetaData();
        testMetaData.putData("saveActions", Arrays.asList("enabled", "author[lower_case]"));
        dbmsSynchronizer.setMetaData(testMetaData);

        dbmsSynchronizer.applyMetaData();

        Assert.assertEquals("wirthlin, michael j1", bibEntry.getFieldOptional("author").get());

    }

    private BibEntry getBibEntryExample(int index) {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType("book");
        bibEntry.setField("author", "Wirthlin, Michael J" + index);
        bibEntry.setField("title", "The nano processor" + index);
        bibEntry.setRemoteId(index);
        return bibEntry;
    }

    private String escape(String expression) {
        return dbmsProcessor.escape(expression);
    }

    @After
    public void clear() {
        try {
            if ((dbmsType == DBMSType.MYSQL) || (dbmsType == DBMSType.POSTGRESQL)) {
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + escape(DBMSProcessor.ENTRY));
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + escape(DBMSProcessor.METADATA));
            } else if (dbmsType == DBMSType.ORACLE) {
                connection.createStatement()
                        .executeUpdate("BEGIN\n" + "EXECUTE IMMEDIATE 'DROP TABLE " + escape(DBMSProcessor.ENTRY) + "';\n"
                                + "EXECUTE IMMEDIATE 'DROP TABLE " + escape(DBMSProcessor.METADATA) + "';\n"
                                + "EXECUTE IMMEDIATE 'DROP SEQUENCE " + escape(DBMSProcessor.ENTRY + "_SEQ") + "';\n"
                                + "EXCEPTION\n" + "WHEN OTHERS THEN\n" + "IF SQLCODE != -942 THEN\n" + "RAISE;\n"
                                + "END IF;\n" + "END;");
            }
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

}
