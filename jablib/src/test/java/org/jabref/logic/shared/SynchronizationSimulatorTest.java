package org.jabref.logic.shared;

import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import javafx.collections.FXCollections;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.util.VirtualThreadTaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.support.DatabaseTest;

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

    /// Awaits an asynchronously arriving state change (listener-thread work) with a bounded deadline.
    /// The assertion afterwards reports the actual state if the deadline was missed.
    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
        while (!condition.getAsBoolean() && (System.currentTimeMillis() < deadline)) {
            Thread.sleep(50);
        }
    }

    @BeforeEach
    void setup() throws Exception {
        this.connectorTest = new ConnectorTest();
        DBMSConnection dbmsConnection = connectorTest.getTestDBMSConnection();
        TestManager.clearTables(dbmsConnection);

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());

        clientContextA = new BibDatabaseContext();
        DBMSSynchronizer synchronizerA = new DBMSSynchronizer(clientContextA, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost", new VirtualThreadTaskExecutor());
        clientContextA.convertToSharedDatabase(synchronizerA);
        clientContextA.getDBMSSynchronizer().openSharedDatabase(dbmsConnection);

        clientContextB = new BibDatabaseContext();
        DBMSSynchronizer synchronizerB = new DBMSSynchronizer(clientContextB, ',', fieldPreferences, pattern, new DummyFileUpdateMonitor(), "UserAndHost", new VirtualThreadTaskExecutor());
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
        waitUntil(() -> expected.equals(bibEntryOfClientB.getField(StandardField.YEAR)));
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
        waitUntil(() -> expected.equals(clientContextB.getMetaData().getMode()));
        assertEquals(expected, clientContextB.getMetaData().getMode());
    }

    /// [Issue 9452](https://github.com/JabRef/jabref/issues/9452): groups created by one client have to
    /// show up at the other client without reconnecting
    // [utest->req~shared-database.live-propagation~1]
    @Test
    void simulateLiveGroupCreationPropagation() throws Exception {
        // client A creates the group tree; the group panel writes it back via MetaData.setGroups
        GroupTreeNode rootOfClientA = new GroupTreeNode(new ExplicitGroup("All entries", GroupHierarchyType.INDEPENDENT, ','));
        rootOfClientA.addSubgroup(new ExplicitGroup("Group A", GroupHierarchyType.INDEPENDENT, ','));
        clientContextA.getMetaData().setGroups(rootOfClientA);

        waitUntil(() -> clientContextB.getMetaData().getGroups().isPresent());
        assertEquals(Optional.of(rootOfClientA), clientContextB.getMetaData().getGroups());
    }

    // [utest->req~shared-database.live-propagation~1]
    @Test
    void simulateLiveSubgroupAdditionPropagation() throws Exception {
        // A root without children is not serialized at all, so the initial tree needs one group
        GroupTreeNode rootOfClientA = new GroupTreeNode(new ExplicitGroup("All entries", GroupHierarchyType.INDEPENDENT, ','));
        rootOfClientA.addSubgroup(new ExplicitGroup("Group A", GroupHierarchyType.INDEPENDENT, ','));
        clientContextA.getMetaData().setGroups(rootOfClientA);
        waitUntil(() -> clientContextB.getMetaData().getGroups().isPresent());
        assertEquals(Optional.of(rootOfClientA), clientContextB.getMetaData().getGroups());

        // client A adds a subgroup to the existing tree; the group panel writes the (same) root back
        rootOfClientA.addSubgroup(new ExplicitGroup("Group B", GroupHierarchyType.INDEPENDENT, ','));
        clientContextA.getMetaData().setGroups(rootOfClientA);

        waitUntil(() -> clientContextB.getMetaData().getGroups().map(root -> root.getNumberOfChildren() == 2).orElse(false));
        assertEquals(Optional.of(rootOfClientA), clientContextB.getMetaData().getGroups());
    }

    // [utest->req~shared-database.live-propagation~1]
    @Test
    void simulateLiveGroupEditPropagation() throws Exception {
        GroupTreeNode rootOfClientA = new GroupTreeNode(new ExplicitGroup("All entries", GroupHierarchyType.INDEPENDENT, ','));
        GroupTreeNode groupNodeOfClientA = rootOfClientA.addSubgroup(new ExplicitGroup("Group A", GroupHierarchyType.INDEPENDENT, ','));
        clientContextA.getMetaData().setGroups(rootOfClientA);
        waitUntil(() -> clientContextB.getMetaData().getGroups().isPresent());
        assertEquals(Optional.of(rootOfClientA), clientContextB.getMetaData().getGroups());

        // client A edits name, icon, color and hierarchy of the group; the edit dialog replaces the
        // node's group and the group panel writes the (same) root back
        ExplicitGroup editedGroup = new ExplicitGroup("Renamed group", GroupHierarchyType.INCLUDING, ',');
        editedGroup.setIconName("star");
        editedGroup.setColor("#ff0000");
        groupNodeOfClientA.setGroup(editedGroup);
        clientContextA.getMetaData().setGroups(rootOfClientA);

        waitUntil(() -> Optional.of(editedGroup).equals(groupOfClientB()));
        assertEquals(Optional.of(editedGroup), groupOfClientB());

        // client A changes the group type
        WordKeywordGroup keywordGroup = new WordKeywordGroup("Keyword group", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "fpga", false, ',', false);
        groupNodeOfClientA.setGroup(keywordGroup);
        clientContextA.getMetaData().setGroups(rootOfClientA);

        waitUntil(() -> Optional.of(keywordGroup).equals(groupOfClientB()));
        assertEquals(Optional.of(keywordGroup), groupOfClientB());
    }

    /// The group of the first (and only) child of client B's group tree
    private Optional<AbstractGroup> groupOfClientB() {
        return clientContextB.getMetaData().getGroups()
                             .filter(root -> root.getNumberOfChildren() == 1)
                             .map(root -> root.getChildAt(0).orElseThrow().getGroup());
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
        waitUntil(() -> expectedYear.equals(bibEntryOfClientB.getField(StandardField.YEAR))
                && expectedTitle.equals(bibEntryOfClientB.getField(StandardField.TITLE)));
        assertEquals(expectedYear, bibEntryOfClientB.getField(StandardField.YEAR));
        assertEquals(expectedTitle, bibEntryOfClientB.getField(StandardField.TITLE));
    }

    @Test
    void simulateLiveEntryInsertionAndRemovalPropagation() throws Exception {
        // client A inserts an entry; client B's listener pulls it
        clientContextA.getDatabase().insertEntry(getBibEntryExample(1));
        waitUntil(() -> !clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        // client A removes the entry again; client B follows
        clientContextA.getDatabase().removeEntry(clientContextA.getDatabase().getEntries().getFirst());
        waitUntil(() -> clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(List.of(), clientContextB.getDatabase().getEntries());
    }

    @Test
    void simulateBulkInsertionPropagation() throws Exception {
        // Pasting many entries at once arrives as a single EntriesAddedEvent - all of them
        // have to reach the other client through one pull notification
        List<BibEntry> pasted = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            pasted.add(new BibEntry(StandardEntryType.Article)
                    .withField(StandardField.TITLE, "Title " + i)
                    .withCitationKey("bulk" + i));
        }
        clientContextA.getDatabase().insertEntries(pasted);

        waitUntil(() -> clientContextB.getDatabase().getEntries().size() == 1000);
        assertEquals(1000, clientContextB.getDatabase().getEntries().size());
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
    void simulateUpdateOnNoLongerExistingEntry() throws Exception {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        // client A inserts an entry
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        // client B pulls the entry
        clientContextB.getDBMSSynchronizer().pullChanges();

        assertFalse(clientContextA.getDatabase().getEntries().isEmpty());
        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertEquals(clientContextA.getDatabase().getEntries(), clientContextB.getDatabase().getEntries());

        // The entry disappears on the shared side without any notification
        // (a removal through JabRef would notify client B thanks to live synchronization)
        try (Statement statement = connectorTest.getTestDBMSConnection().getConnection().createStatement()) {
            statement.executeUpdate("DELETE FROM jabref.entry");
        }

        assertFalse(clientContextB.getDatabase().getEntries().isEmpty());
        assertNull(eventListenerB.getSharedEntriesNotPresentEvent());
        // client B tries to update the entry
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();
        bibEntryOfClientB.setField(StandardField.YEAR, "2009");

        // here a new SharedEntryNotPresentEvent has been thrown. In this case the user B would get an pop-up window.
        waitUntil(() -> eventListenerB.getSharedEntriesNotPresentEvent() != null);
        assertNotNull(eventListenerB.getSharedEntriesNotPresentEvent());
        assertEquals(List.of(bibEntryOfClientB), eventListenerB.getSharedEntriesNotPresentEvent().bibEntries());
    }

    @Test
    void simulateEntryChangeConflicts() throws Exception {
        // Inserted without a notification, so that no pull triggered by it can interfere below
        DBMSProcessor otherClient = new DBMSProcessor(connectorTest.getTestDBMSConnection());
        BibEntry sharedEntry = getBibEntryExample(1);
        otherClient.insertEntry(sharedEntry);
        clientContextB.getDBMSSynchronizer().pullChanges();
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();

        // The entry changes on the shared side without client B learning about it
        // (e.g. the notification was lost while B was briefly disconnected)
        sharedEntry.setField(StandardField.YEAR, "2001");
        otherClient.updateEntry(sharedEntry);
        assertNull(eventListenerB.getUpdateRefusedEvent());

        // B changes its stale copy; a major change is written at once
        bibEntryOfClientB.setField(StandardField.YEAR, "2016 (in press)");

        // B cannot update the shared entry, due to optimistic offline lock: a merge dialog pops up
        waitUntil(() -> eventListenerB.getUpdateRefusedEvent() != null);
        assertNotNull(eventListenerB.getUpdateRefusedEvent());
        assertEquals(Optional.of("2001"), eventListenerB.getUpdateRefusedEvent().sharedBibEntry().getField(StandardField.YEAR));
        assertEquals(Optional.of("2016 (in press)"), bibEntryOfClientB.getField(StandardField.YEAR));
    }

    @Test
    void simulateRemoteChangeDuringMicroEditIsReportedAsConflict() throws Exception {
        // Inserted without a notification, so that no pull triggered by it flushes B's buffered edit early
        DBMSProcessor otherClient = new DBMSProcessor(connectorTest.getTestDBMSConnection());
        otherClient.insertEntry(getBibEntryExample(1));
        clientContextA.getDBMSSynchronizer().pullChanges();
        clientContextB.getDBMSSynchronizer().pullChanges();
        BibEntry bibEntryOfClientA = clientContextA.getDatabase().getEntries().getFirst();
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();

        // Client B is typing: the edit is buffered, not yet written
        bibEntryOfClientB.setField(StandardField.YEAR, "2030", EntriesEventSource.SHARED);
        FieldChangedEvent filteredEvent = new FieldChangedEvent(bibEntryOfClientB, StandardField.YEAR, "1991", "2030");
        filteredEvent.setFiltered(true);
        ((DBMSSynchronizer) clientContextB.getDBMSSynchronizer()).listen(filteredEvent);

        // Client A changes the same field meanwhile; the notification makes B write its buffered edit first
        bibEntryOfClientA.setField(StandardField.YEAR, "2001");

        // Client B's buffered edit conflicts: reported for merging instead of silently overwritten
        waitUntil(() -> eventListenerB.getUpdateRefusedEvent() != null);
        assertNotNull(eventListenerB.getUpdateRefusedEvent());
        assertEquals(Optional.of("2030"), bibEntryOfClientB.getField(StandardField.YEAR));
        assertEquals(Optional.of("2001"), eventListenerB.getUpdateRefusedEvent().sharedBibEntry().getField(StandardField.YEAR));
    }

    /// Types the text into the field character by character, as a user does: every keystroke is
    /// a one-character change, which [org.jabref.logic.util.CoarseChangeFilter] marks as filtered
    private static void typeInto(BibEntry bibEntry, StandardField field, String text) {
        StringBuilder typed = new StringBuilder();
        for (char character : text.toCharArray()) {
            typed.append(character);
            bibEntry.setField(field, typed.toString());
        }
    }

    /// https://github.com/JabRef/jabref/issues/9738: text typed character by character arrived truncated on the shared side
    @Test
    void simulateTypedTextReachesSharedSideCompletely() throws Exception {
        BibEntry bibEntryOfClientA = getBibEntryExample(1);
        clientContextA.getDatabase().insertEntry(bibEntryOfClientA);
        clientContextB.getDBMSSynchronizer().pullChanges();
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();

        String comment = "je ne sais pas quoi en dire en ce moment";
        typeInto(bibEntryOfClientA, StandardField.COMMENT, comment);
        // Nothing is written while typing; the user then moves on to another field
        bibEntryOfClientA.setField(StandardField.TITLE, "my very short title");

        Optional<String> expectedComment = Optional.of(comment);
        waitUntil(() -> expectedComment.equals(bibEntryOfClientB.getField(StandardField.COMMENT)));
        assertEquals(expectedComment, bibEntryOfClientB.getField(StandardField.COMMENT));
        assertEquals(Optional.of("my very short title"), bibEntryOfClientB.getField(StandardField.TITLE));
        assertEquals(bibEntryOfClientA.getSharedBibEntryData().getVersion(), bibEntryOfClientB.getSharedBibEntryData().getVersion());

        // Typing is the last thing the user does before closing JabRef
        typeInto(bibEntryOfClientA, StandardField.ABSTRACT, "my comment on this issue");
        clientContextA.getDBMSSynchronizer().closeSharedDatabase();

        DBMSProcessor reader = new DBMSProcessor(connectorTest.getTestDBMSConnection());
        BibEntry sharedEntry = reader.getSharedEntry(bibEntryOfClientA.getSharedBibEntryData().getSharedIdAsInt()).orElseThrow();
        assertEquals(expectedComment, sharedEntry.getField(StandardField.COMMENT));
        assertEquals(Optional.of("my comment on this issue"), sharedEntry.getField(StandardField.ABSTRACT));
        assertEquals(bibEntryOfClientA.getSharedBibEntryData().getVersion(), sharedEntry.getSharedBibEntryData().getVersion());
    }

    /// https://github.com/JabRef/jabref/issues/9738: two users typing into the same field must end with one complete text on the
    /// shared side and a conflict for the other user - not with a mix of both or a truncation
    @Test
    void simulateConcurrentTypingIntoSameField() throws Exception {
        // Inserted without a notification, so that no pull triggered by it flushes B's buffered typing early
        DBMSProcessor otherClient = new DBMSProcessor(connectorTest.getTestDBMSConnection());
        otherClient.insertEntry(getBibEntryExample(1));
        clientContextA.getDBMSSynchronizer().pullChanges();
        clientContextB.getDBMSSynchronizer().pullChanges();
        BibEntry bibEntryOfClientA = clientContextA.getDatabase().getEntries().getFirst();
        BibEntry bibEntryOfClientB = clientContextB.getDatabase().getEntries().getFirst();

        typeInto(bibEntryOfClientA, StandardField.COMMENT, "comment of asterix");
        typeInto(bibEntryOfClientB, StandardField.COMMENT, "comment of obelix");

        // A leaves the field first: A's text is written; B's typing is still buffered
        bibEntryOfClientA.setField(StandardField.TITLE, "title of asterix");
        Optional<String> expectedTitle = Optional.of("title of asterix");
        waitUntil(() -> eventListenerB.getUpdateRefusedEvent() != null);

        // A's write asked B to pull; the pull flushes B's buffered typing first, which is refused
        assertNotNull(eventListenerB.getUpdateRefusedEvent());
        assertEquals(Optional.of("comment of asterix"), eventListenerB.getUpdateRefusedEvent().sharedBibEntry().getField(StandardField.COMMENT));
        assertEquals(expectedTitle, eventListenerB.getUpdateRefusedEvent().sharedBibEntry().getField(StandardField.TITLE));
        // B keeps the local text for the merge dialog; the shared side is untouched by B
        assertEquals(Optional.of("comment of obelix"), bibEntryOfClientB.getField(StandardField.COMMENT));
        DBMSProcessor reader = new DBMSProcessor(connectorTest.getTestDBMSConnection());
        BibEntry sharedEntry = reader.getSharedEntry(bibEntryOfClientA.getSharedBibEntryData().getSharedIdAsInt()).orElseThrow();
        assertEquals(Optional.of("comment of asterix"), sharedEntry.getField(StandardField.COMMENT));
        assertEquals(expectedTitle, sharedEntry.getField(StandardField.TITLE));
    }
}
