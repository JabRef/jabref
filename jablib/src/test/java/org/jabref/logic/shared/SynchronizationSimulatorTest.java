package org.jabref.logic.shared;

import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DatabaseTest
@Execution(ExecutionMode.SAME_THREAD)
class SynchronizationSimulatorTest {

    private BibDatabaseContext clientContextA;
    private BibDatabaseContext clientContextB;
    private SynchronizationEventListenerTest eventListenerB; // used to monitor occurring events
    private final GlobalCitationKeyPatterns pattern = GlobalCitationKeyPatterns.fromPattern("[auth][year]");
    private ConnectorTest connectorTest;

    private BibEntry getBibEntryExample(int index) {
        return new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Wirthlin, Michael J and Hutchings, Brad L and Gilson, Kent L " + index)
                .withField(StandardField.TITLE, "The nano processor: a low resource reconfigurable processor " + index)
                .withField(StandardField.BOOKTITLE, "FPGAs for Custom Computing Machines, 1994. Proceedings. IEEE Workshop on " + index)
                .withField(StandardField.YEAR, "199" + index)
                .withCitationKey("nanoproc199" + index);
    }

    @BeforeEach
    void setup() throws Exception {
        this.connectorTest = new ConnectorTest();
        DBMSConnection dbmsConnection = connectorTest.getTestDBMSConnection();
        TestManager.clearTables(dbmsConnection);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());

        clientContextA = new BibDatabaseContext();
        DBMSSynchronizer synchronizerA = new DBMSSynchronizer(clientContextA, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost");
        clientContextA.convertToSharedDatabase(synchronizerA);
        clientContextA.getDBMSSynchronizer().openSharedDatabase(dbmsConnection);

        clientContextB = new BibDatabaseContext();
        DBMSSynchronizer synchronizerB = new DBMSSynchronizer(clientContextB, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost");
        clientContextB.convertToSharedDatabase(synchronizerB);
        // use a second connection, because this is another client (typically on another machine)
        clientContextB.getDBMSSynchronizer().openSharedDatabase(connectorTest.getTestDBMSConnection());
        eventListenerB = new SynchronizationEventListenerTest();
        clientContextB.getDBMSSynchronizer().registerListener(eventListenerB);
    }

    @AfterEach
    void clear() throws Exception {
        clientContextA.getDBMSSynchronizer().closeSharedDatabase();
        clientContextB.getDBMSSynchronizer().closeSharedDatabase();
        connectorTest.close();
    }

    @Test
    void simulateLiveFieldChangePropagation() throws Exception {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        // client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();

        // client A changes a field; the notification carries the change and client B's listener applies it without pulling
        bibEntryOfClientA.setField(StandardField.YEAR, "2026");

        Optional<String> expected = Optional.of("2026");
        for (int i = 0; (i < 100) && !expected.equals(bibEntryOfClientB.getField(StandardField.YEAR)); i++) {
            Thread.sleep(100);
        }
        assertEquals(expected, bibEntryOfClientB.getField(StandardField.YEAR));
        assertEquals(bibEntryOfClientA.getSharedBibEntryData().getVersion(), bibEntryOfClientB.getSharedBibEntryData().getVersion());
    }

    @Test
    void simulateLiveMetaDataPropagation() throws Exception {
        MetaData metaDataOfClientA = clientContextA.getMetaData();
        metaDataOfClientA.registerListener(clientContextA.getDBMSSynchronizer());

        // client A changes the library mode; client B's listener re-reads the shared metadata
        metaDataOfClientA.setMode(BibDatabaseMode.BIBLATEX);

        Optional<BibDatabaseMode> expected = Optional.of(BibDatabaseMode.BIBLATEX);
        for (int i = 0; (i < 100) && !expected.equals(clientContextB.getMetaData().getMode()); i++) {
            Thread.sleep(100);
        }
        assertEquals(expected, clientContextB.getMetaData().getMode());
    }

    @Test
    void simulateFlushedMicroEditsPropagation() throws Exception {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        clientContextB.getDBMSSynchronizer().pullChanges();
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();

        // Simulate a micro-edit: change the value silently and hand the synchronizer the
        // filtered event, as CoarseChangeFilter would
        bibEntryOfClientA.setField(StandardField.YEAR, "2030", EntriesEventSource.SHARED);
        FieldChangedEvent filteredEvent = new FieldChangedEvent(bibEntryOfClientA, StandardField.YEAR, "1991", "2030");
        filteredEvent.setFiltered(true);
        ((DBMSSynchronizer) clientContextA.getDBMSSynchronizer()).listen(filteredEvent);

        // The next major change flushes the buffered entry and asks other clients to pull
        bibEntryOfClientA.setField(StandardField.TITLE, "Flush trigger");

        Optional<String> expectedYear = Optional.of("2030");
        Optional<String> expectedTitle = Optional.of("Flush trigger");
        for (int i = 0; (i < 100) && !(expectedYear.equals(bibEntryOfClientB.getField(StandardField.YEAR))
                && expectedTitle.equals(bibEntryOfClientB.getField(StandardField.TITLE))); i++) {
            Thread.sleep(100);
        }
        assertEquals(expectedYear, bibEntryOfClientB.getField(StandardField.YEAR));
        assertEquals(expectedTitle, bibEntryOfClientB.getField(StandardField.TITLE));
    }

    @Test
    void simulateLiveEntryInsertionAndRemovalPropagation() throws Exception {
        // client A inserts an entry; client B's listener pulls it
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1));
        for (int i = 0; (i < 100) && clientContextB.getDatabase().getEntries().isEmpty(); i++) {
            Thread.sleep(100);
        }
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        // client A removes the entry again; client B follows
        clientContextA.getDatabase().removeEntry(clientContextA.getDatabase().getEntries().getFirst());
        for (int i = 0; (i < 100) && !clientContextB.getDatabase().getEntries().isEmpty(); i++) {
            Thread.sleep(100);
        }
        assertEquals(List.of(), clientContextB.getDatabase().getEntries());
    }

    @Test
    void simulateEntryInsertionAndManualPull() {
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1));
        // client A inserts another entry
        clientContextA.getDatabase().insertEntry(getBibEntryExample(2));
        // client B pulls the changes
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());
    }

    @Test
    void simulateEntryUpdateAndManualPull() {
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
    void simulateEntryDelitionAndManualPull() {
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
    void simulateUpdateOnNoLongerExistingEntry() {
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
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();
        bibEntryOfClientB.setField(StandardField.YEAR, "2009");

        // here a new SharedEntryNotPresentEvent has been thrown. In this case the user B would get an pop-up window.
        assertNotNull(eventListenerB.getSharedEntriesNotPresentEvent());
        assertEquals(List.of(bibEntryOfClientB), eventListenerB.getSharedEntriesNotPresentEvent().bibEntries());
    }

    @Test
    void simulateEntryChangeConflicts() {
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

        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();
        // B also tries to change something
        bibEntryOfClientB.setField(StandardField.YEAR, "2016");

        // B now cannot update the shared entry, due to optimistic offline lock.
        // In this case an BibEntry merge dialog pops up.
        assertNotNull(eventListenerB.getUpdateRefusedEvent());
    }
}
