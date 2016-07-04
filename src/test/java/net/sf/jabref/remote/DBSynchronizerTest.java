package net.sf.jabref.remote;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.event.location.EntryEventTargetScope;
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
public class DBSynchronizerTest {

    private DBSynchronizer dbSynchronizer;
    private Connection connection;
    private DBProcessor dbProcessor;
    private BibDatabase bibDatabase;

    @Parameter
    public DBType dbType;


    @Before
    public void setUp() {

        Globals.prefs = JabRefPreferences.getInstance();

        try {
            connection = TestConnector.getTestConnection(dbType);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        bibDatabase = new BibDatabase();
        dbSynchronizer = new DBSynchronizer(bibDatabase, new MetaData());
        dbProcessor = new DBProcessor(connection, dbType);

        bibDatabase.registerListener(dbSynchronizer);

        dbSynchronizer.openRemoteDatabase(connection, dbType, "TEST");
    }

    @Parameters(name = "Test with {0} database system")
    public static Collection<DBType> getTestingDatabaseSystems() {
        Set<DBType> dbTypes = new HashSet<>();
        dbTypes.add(DBType.MYSQL);
        dbTypes.add(DBType.POSTGRESQL);

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            dbTypes.add(DBType.ORACLE);
        } catch (ClassNotFoundException e) {
            // In case that Oracle interface is not available do not perform tests for this system.
            System.out.println("Oracle driver not available. Skipping tests for this system...");
        }
        return dbTypes;
    }

    @Test
    public void testEntryAddedEventListener() {
        BibEntry expectedEntry = getBibEntryExample(1);
        BibEntry furtherEntry = getBibEntryExample(1);

        bibDatabase.insertEntry(expectedEntry);
        // should not add remotely.
        bibDatabase.insertEntry(furtherEntry, EntryEventTargetScope.LOCAL);

        List<BibEntry> actualEntries = dbProcessor.getRemoteEntries();

        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(expectedEntry, actualEntries.get(0));
    }

    @Test
    public void testFieldChangedEventListener() {
        BibEntry expectedEntry = getBibEntryExample(1);
        expectedEntry.registerListener(dbSynchronizer);

        bibDatabase.insertEntry(expectedEntry);
        expectedEntry.setField("author", "Brad L and Gilson");
        expectedEntry.setField("title", "The micro multiplexer", EntryEventTargetScope.LOCAL);

        List<BibEntry> actualEntries = dbProcessor.getRemoteEntries();
        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(expectedEntry.getField("author"), actualEntries.get(0).getField("author"));
        Assert.assertEquals("The nano processor1", actualEntries.get(0).getField("title"));

    }

    @Test
    public void testEntryRemovedEventListener() {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        List<BibEntry> actualEntries = dbProcessor.getRemoteEntries();
        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(bibEntry, actualEntries.get(0));

        bibDatabase.removeEntry(bibEntry);
        actualEntries = dbProcessor.getRemoteEntries();

        Assert.assertEquals(0, actualEntries.size());

        bibDatabase.insertEntry(bibEntry);
        bibDatabase.removeEntry(bibEntry, EntryEventTargetScope.LOCAL);

        actualEntries = dbProcessor.getRemoteEntries();
        Assert.assertEquals(1, actualEntries.size());
        Assert.assertEquals(bibEntry, actualEntries.get(0));
    }

    @Test
    public void testMetaDataChangedEventListener() {
        MetaData testMetaData = new MetaData();
        testMetaData.registerListener(dbSynchronizer);
        dbSynchronizer.setMetaData(testMetaData);
        testMetaData.putData("databaseType", Arrays.asList("bibtex"));

        Map<String, List<String>> expectedMap = testMetaData.getMetaData();
        Map<String, List<String>> actualMap = dbProcessor.getRemoteMetaData();

        Assert.assertEquals(expectedMap, actualMap);
    }

    @Test
    public void testInitializeDatabases() {
        clear();
        dbSynchronizer.initializeDatabases();
        Assert.assertTrue(dbProcessor.checkBaseIntegrity());
        dbSynchronizer.initializeDatabases();
        Assert.assertTrue(dbProcessor.checkBaseIntegrity());
    }

    @Test
    public void testSynchronizeLocalDatabase() {
        List<BibEntry> expectedBibEntries = Arrays.asList(getBibEntryExample(1), getBibEntryExample(2));

        dbProcessor.insertEntry(expectedBibEntries.get(0));
        dbProcessor.insertEntry(expectedBibEntries.get(1));

        Assert.assertTrue(bibDatabase.getEntries().isEmpty());

        dbSynchronizer.synchronizeLocalDatabase();

        Assert.assertEquals(expectedBibEntries, bibDatabase.getEntries());

        dbProcessor.removeEntry(expectedBibEntries.get(0));
        dbProcessor.removeEntry(expectedBibEntries.get(1));
        expectedBibEntries = new ArrayList<>();

        dbSynchronizer.synchronizeLocalDatabase();

        Assert.assertEquals(expectedBibEntries, bibDatabase.getEntries());
    }

    @Test
    public void testApplyMetaData() {
        BibEntry bibEntry = getBibEntryExample(1);
        bibDatabase.insertEntry(bibEntry);

        MetaData testMetaData = new MetaData();
        testMetaData.putData("saveActions", Arrays.asList("enabled", "author[lower_case]"));
        dbSynchronizer.setMetaData(testMetaData);

        dbSynchronizer.applyMetaData();

        Assert.assertEquals("wirthlin, michael j1", bibEntry.getField("author"));

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
        return dbProcessor.escape(expression);
    }

    @After
    public void clear() {
        try {
            if ((dbType == DBType.MYSQL) || (dbType == DBType.POSTGRESQL)) {
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + escape(DBProcessor.ENTRY));
                connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + escape(DBProcessor.METADATA));
            } else if (dbType == DBType.ORACLE) {
                connection.createStatement()
                        .executeUpdate("BEGIN\n" + "EXECUTE IMMEDIATE 'DROP TABLE " + escape(DBProcessor.ENTRY) + "';\n"
                                + "EXECUTE IMMEDIATE 'DROP TABLE " + escape(DBProcessor.METADATA) + "';\n"
                                + "EXECUTE IMMEDIATE 'DROP SEQUENCE " + escape(DBProcessor.ENTRY + "_SEQ") + "';\n"
                                + "EXCEPTION\n" + "WHEN OTHERS THEN\n" + "IF SQLCODE != -942 THEN\n" + "RAISE;\n"
                                + "END IF;\n" + "END;");
            }
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        }
    }

}
