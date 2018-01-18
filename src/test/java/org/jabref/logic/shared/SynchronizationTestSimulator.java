package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.Collection;

import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.model.Defaults;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseTest
public class SynchronizationTestSimulator {

    private BibDatabaseContext clientContextA;
    private BibDatabaseContext clientContextB;

    private SynchronizationTestEventListener eventListenerB; // used to monitor occurring events

    private DBMSConnection dbmsConnection;

    @Parameter
    public DBMSType dbmsType;

    @BeforeEach
    public void setUp() throws SQLException, DatabaseNotSupportedException, InvalidDBMSConnectionPropertiesException {
        this.dbmsConnection = TestConnector.getTestDBMSConnection(dbmsType);

        GlobalBibtexKeyPattern pattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");
        clientContextA = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));
        DBMSSynchronizer synchronizerA = new DBMSSynchronizer(clientContextA, ',', pattern, new DummyFileUpdateMonitor());
        clientContextA.convertToSharedDatabase(synchronizerA);
        clientContextA.getDBMSSynchronizer().openSharedDatabase(dbmsConnection);

        clientContextB = new BibDatabaseContext(new Defaults(BibDatabaseMode.BIBTEX));
        DBMSSynchronizer synchronizerB = new DBMSSynchronizer(clientContextA, ',', pattern, new DummyFileUpdateMonitor());
        clientContextB.convertToSharedDatabase(synchronizerB);
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
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1));
        //client A inserts another entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(2));
        //client B pulls the changes
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    public void simulateEntryUpdateAndManualPull() {
        BibEntry bibEntry = getBibEntryExample(1);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntry);
        //client A changes the entry
        bibEntry.setField("custom", "custom value");
        //client B pulls the changes
        bibEntry.clearField("author");

        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    public void simulateEntryDelitionAndManualPull() {
        BibEntry bibEntry = getBibEntryExample(1);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntry);
        //client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        //client A removes the entry
        clientContextA.getDatabase().removeEntry(bibEntry);
        //client B pulls the change
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertTrue(clientContextA.getDatabase().getEntries().isEmpty());
        assertTrue(clientContextB.getDatabase().getEntries().isEmpty());
    }

    @Test
    public void simulateUpdateOnNoLongerExistingEntry() {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        //client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        //client A removes the entry
        clientContextA.getDatabase().removeEntry(bibEntryOfClientA);

        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertNull(eventListenerB.getSharedEntryNotPresentEvent());
        //client B tries to update the entry
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        bibEntryOfClientB.setField("year", "2009");

        // here a new SharedEntryNotPresentEvent has been thrown. In this case the user B would get an pop-up window.
        assertNotNull(eventListenerB.getSharedEntryNotPresentEvent());
        assertEquals(bibEntryOfClientB, eventListenerB.getSharedEntryNotPresentEvent().getBibEntry());
    }

    @Test
    public void simulateEntryChangeConflicts() {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        //client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        //A now increases the version number
        bibEntryOfClientA.setField("year", "2001");

        // B does nothing here, so there is no event occurrence
        // B now tries to update the entry
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());

        assertNull(eventListenerB.getUpdateRefusedEvent());

        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        //B also tries to change something
        bibEntryOfClientB.setField("year", "2016");

        // B now cannot update the shared entry, due to optimistic offline lock.
        // In this case an BibEntry merge dialog pops up.
        assertNotNull(eventListenerB.getUpdateRefusedEvent());
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

    @AfterEach
    public void clear() throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
