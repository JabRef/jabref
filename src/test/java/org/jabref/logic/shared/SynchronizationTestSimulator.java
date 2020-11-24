package org.jabref.logic.shared;

import java.sql.SQLException;
import java.util.List;

import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
public class SynchronizationTestSimulator {

    private BibDatabaseContext clientContextA;
    private BibDatabaseContext clientContextB;
    private SynchronizationTestEventListener eventListenerB; // used to monitor occurring events
    private final GlobalCitationKeyPattern pattern = GlobalCitationKeyPattern.fromPattern("[auth][year]");

    private BibEntry getBibEntryExample(int index) {
        return new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L " + index)
                .withField(StandardField.TITLE, "The nano processor: a low resource reconfigurable processor " + index)
                .withField(StandardField.BOOKTITLE, "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on " + index)
                .withField(StandardField.YEAR, "199" + index)
                .withCitationKey("nanoproc199" + index);
    }

    @BeforeEach
    public void setup() throws Exception {
        DBMSConnection dbmsConnection = TestConnector.getTestDBMSConnection(TestManager.getDBMSTypeTestParameter());
        TestManager.clearTables(dbmsConnection);

        clientContextA = new BibDatabaseContext();
        DBMSSynchronizer synchronizerA = new DBMSSynchronizer(clientContextA, ',', pattern, new DummyFileUpdateMonitor());
        clientContextA.convertToSharedDatabase(synchronizerA);
        clientContextA.getDBMSSynchronizer().openSharedDatabase(dbmsConnection);

        clientContextB = new BibDatabaseContext();
        DBMSSynchronizer synchronizerB = new DBMSSynchronizer(clientContextB, ',', pattern, new DummyFileUpdateMonitor());
        clientContextB.convertToSharedDatabase(synchronizerB);
        // use a second connection, because this is another client (typically on another machine)
        clientContextB.getDBMSSynchronizer().openSharedDatabase(TestConnector.getTestDBMSConnection(TestManager.getDBMSTypeTestParameter()));
        eventListenerB = new SynchronizationTestEventListener();
        clientContextB.getDBMSSynchronizer().registerListener(eventListenerB);
    }

    @AfterEach
    public void clear() throws SQLException {
        clientContextA.getDBMSSynchronizer().closeSharedDatabase();
        clientContextB.getDBMSSynchronizer().closeSharedDatabase();
    }

    @Test
    public void simulateEntryInsertionAndManualPull() throws Exception {
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1));
        // client A inserts another entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(2));
        // client B pulls the changes
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    public void simulateEntryUpdateAndManualPull() throws Exception {
        BibEntry bibEntry = getBibEntryExample(1);
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntry);
        // client A changes the entry
        bibEntry.setField(new UnknownField("custom"), "custom value");
        // client B pulls the changes
        bibEntry.clearField(StandardField.AUTHOR);

        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    public void simulateEntryDelitionAndManualPull() throws Exception {
        BibEntry bibEntry = getBibEntryExample(1);
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntry);
        // client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        // client A removes the entry
        clientContextA.getDatabase().removeEntry(bibEntry);
        // client B pulls the change
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertTrue(clientContextA.getDatabase().getEntries().isEmpty());
        assertTrue(clientContextB.getDatabase().getEntries().isEmpty());
    }

    @Test
    public void simulateUpdateOnNoLongerExistingEntry() throws Exception {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        // client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        // client A removes the entry
        clientContextA.getDatabase().removeEntry(bibEntryOfClientA);

        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertNull(eventListenerB.getSharedEntriesNotPresentEvent());
        // client B tries to update the entry
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        bibEntryOfClientB.setField(StandardField.YEAR, "2009");

        // here a new SharedEntryNotPresentEvent has been thrown. In this case the user B would get an pop-up window.
        assertNotNull(eventListenerB.getSharedEntriesNotPresentEvent());
        assertEquals(List.of(bibEntryOfClientB), eventListenerB.getSharedEntriesNotPresentEvent().getBibEntries());
    }

    @Test
    public void simulateEntryChangeConflicts() {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        // client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        // A now increases the version number
        bibEntryOfClientA.setField(StandardField.YEAR, "2001");

        // B does nothing here, so there is no event occurrence
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertNull(eventListenerB.getUpdateRefusedEvent());

        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        // B also tries to change something
        bibEntryOfClientB.setField(StandardField.YEAR, "2016");

        // B now cannot update the shared entry, due to optimistic offline lock.
        // In this case an BibEntry merge dialog pops up.
        assertNotNull(eventListenerB.getUpdateRefusedEvent());
    }

}
