package net.sf.jabref.shared;

import java.sql.SQLException;
import java.util.Collection;

import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.bibtexkeypattern.AbstractBibtexKeyPattern;
import net.sf.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.DatabaseLocation;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.shared.exception.DatabaseNotSupportedException;
import net.sf.jabref.shared.exception.InvalidDBMSConnectionPropertiesException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class SynchronizationTestSimulator {

    private BibDatabaseContext clientContextA;
    private BibDatabaseContext clientContextB;

    private SynchronizationTestEventListener eventListenerB; // used to monitor occurring events

    private DBMSConnection dbmsConnection;

    @Parameter
    public DBMSType dbmsType;


    @Before
    public void setUp() throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException {
        this.dbmsConnection = TestConnector.getTestDBMSConnection(dbmsType);

        GlobalBibtexKeyPattern pattern = new GlobalBibtexKeyPattern(AbstractBibtexKeyPattern.split("[auth][year]"));
        clientContextA = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX), DatabaseLocation.SHARED, ',',
                pattern);
        clientContextA.getDBMSSynchronizer().openSharedDatabase(dbmsConnection);

        clientContextB = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX), DatabaseLocation.SHARED, ',',
                pattern);
        clientContextB.getDBMSSynchronizer().openSharedDatabase(dbmsConnection);
        eventListenerB = new SynchronizationTestEventListener();
        clientContextB.getDBMSSynchronizer().registerListener(eventListenerB);
    }

    @Parameters(name = "Test with {0} database system")
    public static Collection<DBMSType> getTestingDatabaseSystems() {
        return TestManager.getDBMSTypeTestParameter();
    }

    @Test
    public void simulateEntryInsertionAndManualPull() {
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1)); // client A inserts an entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(2)); // client A inserts another entry
        clientContextB.getDBMSSynchronizer().pullChanges(); // client B pulls the changes

        Assert.assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    public void simulateEntryUpdateAndManualPull() {
        BibEntry bibEntry = getBibEntryExample(1);
        clientContextA.getDatabase().insertEntry(bibEntry); // client A inserts an entry
        bibEntry.setField("custom", "custom value"); // client A changes the entry
        bibEntry.clearField("author");

        clientContextB.getDBMSSynchronizer().pullChanges(); // client B pulls the changes

        Assert.assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    public void simulateEntryDelitionAndManualPull() {
        BibEntry bibEntry = getBibEntryExample(1);
        clientContextA.getDatabase().insertEntry(bibEntry); // client A inserts an entry
        clientContextB.getDBMSSynchronizer().pullChanges(); // client B pulls the entry

        Assert.assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        Assert.assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        Assert.assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        clientContextA.getDatabase().removeEntry(bibEntry); // client A removes the entry
        clientContextB.getDBMSSynchronizer().pullChanges(); // client B pulls the change

        Assert.assertTrue(clientContextA.getDatabase().getEntries().isEmpty());
        Assert.assertTrue(clientContextB.getDatabase().getEntries().isEmpty());
    }

    @Test
    public void simulateUpdateOnNoLongerExistingEntry() {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA); // client A inserts an entry
        clientContextB.getDBMSSynchronizer().pullChanges(); // client B pulls the entry

        Assert.assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        Assert.assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        Assert.assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        clientContextA.getDatabase().removeEntry(bibEntryOfClientA); // client A removes the entry

        Assert.assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        Assert.assertNull(eventListenerB.getSharedEntryNotPresentEvent());

        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0); // client B tries to update the entry
        bibEntryOfClientB.setField("year", "2009");

        // here a new SharedEntryNotPresentEvent has been thrown. In this case the user B would get an pop-up window.
        Assert.assertNotNull(eventListenerB.getSharedEntryNotPresentEvent());
        Assert.assertEquals(bibEntryOfClientB, eventListenerB.getSharedEntryNotPresentEvent().getBibEntry());
    }

    @Test
    public void simulateEntryChangeConflicts() {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA); // client A inserts an entry
        clientContextB.getDBMSSynchronizer().pullChanges(); // client B pulls the entry

        bibEntryOfClientA.setField("year", "2001"); // A now increases the version number

        // B does nothing here, so there is no event occurrence

        // B now tries to update the entry
        Assert.assertFalse(clientContextB.getDatabase().getEntries().isEmpty());

        Assert.assertNull(eventListenerB.getUpdateRefusedEvent());

        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        bibEntryOfClientB.setField("year", "2016"); // B also tries to change something

        // B now can not update the shared entry, due to optimistic offline lock.
        // In this case an BibEntry merge dialog pops up.
        Assert.assertNotNull(eventListenerB.getUpdateRefusedEvent());
    }

    private BibEntry getBibEntryExample(int index) {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setType("inproceedings");
        bibEntry.setField("author", "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L " + index);
        bibEntry.setField("title", "The nano processor: a low resource reconfigurable processor " + index);
        bibEntry.setField("booktitle", "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on " + index);
        bibEntry.setField("year", "199" + index);
        bibEntry.setCiteKey("nanoproc199" + index);
        return bibEntry;
    }

    @After
    public void clear() throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
