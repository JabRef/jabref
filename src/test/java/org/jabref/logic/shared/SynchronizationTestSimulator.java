package org.jabref.logic.shared;

import java.sql.SQLException;

import org.jabref.model.Defaults;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.testutils.category.DatabaseTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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
    private final GlobalBibtexKeyPattern pattern = GlobalBibtexKeyPattern.fromPattern("[auth][year]");

    public void setUp(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
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

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void simulateEntryInsertionAndManualPull(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsType, dbmsConnection, dbmsProcessor);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1));
        //client A inserts another entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(2));
        //client B pulls the changes
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void simulateEntryUpdateAndManualPull(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsType, dbmsConnection, dbmsProcessor);

        BibEntry bibEntry = getBibEntryExample(1);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntry);
        //client A changes the entry
        bibEntry.setField(new UnknownField("custom"), "custom value");
        //client B pulls the changes
        bibEntry.clearField(StandardField.AUTHOR);

        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void simulateEntryDelitionAndManualPull(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsType, dbmsConnection, dbmsProcessor);

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

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void simulateUpdateOnNoLongerExistingEntry(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsType, dbmsConnection, dbmsProcessor);

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
        assertNull(eventListenerB.getSharedEntriesNotPresentEvent());
        //client B tries to update the entry
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        bibEntryOfClientB.setField(StandardField.YEAR, "2009");

        // here a new SharedEntryNotPresentEvent has been thrown. In this case the user B would get an pop-up window.
        assertNotNull(eventListenerB.getSharedEntriesNotPresentEvent());
        assertEquals(bibEntryOfClientB, eventListenerB.getSharedEntriesNotPresentEvent().getBibEntries());

        clear(dbmsConnection);
    }

    @ParameterizedTest
    @MethodSource("org.jabref.logic.shared.TestManager#getTestingDatabaseSystems")
    public void simulateEntryChangeConflicts(DBMSType dbmsType, DBMSConnection dbmsConnection, DBMSProcessor dbmsProcessor) throws Exception {
        setUp(dbmsType, dbmsConnection, dbmsProcessor);

        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        //client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        //client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        //A now increases the version number
        bibEntryOfClientA.setField(StandardField.YEAR, "2001");

        // B does nothing here, so there is no event occurrence
        // B now tries to update the entry
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());

        assertNull(eventListenerB.getUpdateRefusedEvent());

        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().get(0);
        //B also tries to change something
        bibEntryOfClientB.setField(StandardField.YEAR, "2016");

        // B now cannot update the shared entry, due to optimistic offline lock.
        // In this case an BibEntry merge dialog pops up.
        assertNotNull(eventListenerB.getUpdateRefusedEvent());

        clear(dbmsConnection);
    }

    private BibEntry getBibEntryExample(int index) {
        BibEntry bibEntry = new BibEntry(StandardEntryType.InProceedings);
        bibEntry.setField(StandardField.AUTHOR, "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L " + index);
        bibEntry.setField(StandardField.TITLE, "The nano processor: a low resource reconfigurable processor " + index);
        bibEntry.setField(StandardField.BOOKTITLE, "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on " + index);
        bibEntry.setField(StandardField.YEAR, "199" + index);
        bibEntry.setCiteKey("nanoproc199" + index);
        return bibEntry;
    }

    public void clear(DBMSConnection dbmsConnection) throws SQLException {
        TestManager.clearTables(dbmsConnection);
    }
}
