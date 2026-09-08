package org.jabref.model.undo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibChangeTest {

    private static BibEntry entry() {
        return new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Einstein")
                .withField(StandardField.TITLE, "Relativity");
    }

    static Stream<BibChange> changes() {
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("name", "content");
        return Stream.of(
                new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"),
                new UndoableFieldChange(entry, StandardField.YEAR, null, "1905"),
                new UndoableFieldChange(entry, StandardField.YEAR, "1905", null),
                new UndoableChangeType(entry, StandardEntryType.Article, StandardEntryType.Book),
                new UndoableInsertEntries(database, entry),
                new UndoableRemoveEntries(database, entry),
                new UndoablePreambleChange(database, null, "preamble"),
                new UndoableInsertString(database, string),
                new UndoableRemoveString(database, string),
                new UndoableStringChange(string, UndoableStringChange.Part.CONTENT, "content", "other"),
                new UndoableMetaDataChange(new BibDatabaseContext(), new MetaData(), metaDataWithMode()),
                new UndoableGroupTreeChange(new MetaData(), Optional.of(groupTree("Books")), Optional.of(groupTree("Articles"))),
                new ChangeSet("group", List.of(
                        new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"),
                        new UndoableChangeType(entry, StandardEntryType.Article, StandardEntryType.Book))));
    }

    @ParameterizedTest
    @MethodSource("changes")
    void invertingTwiceIsIdentity(BibChange change) {
        assertEquals(change, change.inverted().inverted());
    }

    @ParameterizedTest
    @MethodSource("changes")
    void invertingOnceIsNotIdentity(BibChange change) {
        assertNotEquals(change, change.inverted());
    }

    private static GroupTreeNode groupTree(String childName) {
        GroupTreeNode root = GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new ExplicitGroup(childName, GroupHierarchyType.INDEPENDENT, ','));
        return root;
    }

    private static MetaData metaDataWithMode() {
        MetaData metaData = new MetaData();
        metaData.setMode(BibDatabaseMode.BIBLATEX);
        return metaData;
    }

    @Test
    void undoingAMetaDataChangePutsTheLibrarySettingsBack() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        MetaData before = databaseContext.getMetaData();
        UndoableMetaDataChange change = new UndoableMetaDataChange(databaseContext, before, metaDataWithMode());

        change.apply();
        assertEquals(BibDatabaseMode.BIBLATEX, databaseContext.getMetaData().getMode().orElseThrow());

        change.inverted().apply();
        assertEquals(Optional.empty(), databaseContext.getMetaData().getMode());
    }

    /// The change holds copies, so a group edit made after it was recorded cannot rewrite what
    /// undoing it restores.
    @Test
    void aMetaDataChangeIsNotRewrittenByALaterGroupEdit() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        MetaData live = databaseContext.getMetaData();
        live.setGroups(GroupTreeNode.fromGroup(new ExplicitGroup("All", GroupHierarchyType.INDEPENDENT, ',')));
        UndoableMetaDataChange change = new UndoableMetaDataChange(databaseContext, live, metaDataWithMode());

        // The user edits the groups after the change was recorded, then takes the change back.
        live.getGroups().orElseThrow().addSubgroup(new ExplicitGroup("Added later", GroupHierarchyType.INDEPENDENT, ','));
        change.inverted().apply();

        assertEquals(List.of(), live.getGroups().orElseThrow().getChildren(),
                "undo restored a group tree the recorded state never held");
    }

    @Test
    void undoingAGroupTreeChangePutsTheEarlierTreeBack() {
        MetaData metaData = new MetaData();
        metaData.setGroups(groupTree("Books"));
        UndoableGroupTreeChange change = new UndoableGroupTreeChange(
                metaData, metaData.getGroups(), Optional.of(groupTree("Articles")));

        change.apply();
        assertEquals(List.of("Articles"), childNames(metaData));

        change.inverted().apply();
        assertEquals(List.of("Books"), childNames(metaData));
    }

    /// A library that had no groups has to end up with none again, not with an empty root that a
    /// save would write out.
    @Test
    void undoingTheFirstGroupLeavesTheLibraryWithoutGroups() {
        MetaData metaData = new MetaData();
        UndoableGroupTreeChange change = new UndoableGroupTreeChange(
                metaData, Optional.empty(), Optional.of(groupTree("Books")));

        change.apply();
        assertEquals(List.of("Books"), childNames(metaData));

        change.inverted().apply();
        assertEquals(Optional.empty(), metaData.getGroups());
    }

    /// The tree the library holds must not be the one the change kept, or the next edit would
    /// rewrite what undoing it restores.
    @Test
    void applyingAGroupTreeChangeInstallsACopy() {
        MetaData metaData = new MetaData();
        UndoableGroupTreeChange change = new UndoableGroupTreeChange(
                metaData, Optional.empty(), Optional.of(groupTree("Books")));

        change.apply();
        metaData.getGroups().orElseThrow().addSubgroup(new ExplicitGroup("Added later", GroupHierarchyType.INDEPENDENT, ','));
        change.apply();

        assertEquals(List.of("Books"), childNames(metaData));
    }

    private static List<String> childNames(MetaData metaData) {
        return metaData.getGroups().orElseThrow().getChildren().stream()
                       .map(node -> node.getGroup().getName())
                       .toList();
    }

    @Test
    void applyingAMetaDataChangeKeepsTheMetaDataInstance() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        MetaData before = databaseContext.getMetaData();
        UndoableMetaDataChange change = new UndoableMetaDataChange(databaseContext, before, metaDataWithMode());

        change.apply();
        assertSame(before, databaseContext.getMetaData());

        change.inverted().apply();
        assertSame(before, databaseContext.getMetaData());
    }

    /// The case a suspension cannot cover: a command writes on a background thread, the user edits
    /// the same field meanwhile, and the step recorded for one of them no longer describes what the
    /// library holds. Writing over it would produce a state no step on the stack describes.
    @Test
    // [utest->req~logic.undo.stale-change-refused~1]
    void aChangeRefusesWhenTheLibraryMovedOnUnderIt() {
        BibEntry entry = entry();
        UndoableFieldChange change = new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr");
        entry.setField(StandardField.AUTHOR, "Planck");

        ApplyResult result = change.apply();

        assertEquals(List.of(change), result.failures().stream().map(ApplyResult.Failure::change).toList());
        assertEquals("Planck", entry.getField(StandardField.AUTHOR).orElseThrow(), "the change wrote over the newer value");
    }

    @Test
    void aTypeChangeRefusesWhenTheEntryIsNoLongerWhatItRecorded() {
        BibEntry entry = entry();
        UndoableChangeType change = new UndoableChangeType(entry, StandardEntryType.Article, StandardEntryType.Book);
        entry.setType(StandardEntryType.Thesis);

        assertFalse(change.apply().complete());
        assertEquals(StandardEntryType.Thesis, entry.getType());
    }

    @Test
    void insertingAStringRefusesWhenOneOfThatNameIsAlreadyThere() {
        BibDatabase database = new BibDatabase();
        UndoableInsertString change = new UndoableInsertString(database, new BibtexString("name", "content"));
        database.addString(new BibtexString("name", "something else"));

        assertFalse(change.apply().complete());
        assertEquals(1, database.getStringCount(), "the insert went ahead over the string already there");
    }

    @Test
    void removingAStringRefusesWhenItIsNoLongerThere() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("name", "content");
        UndoableRemoveString change = new UndoableRemoveString(database, string);

        assertFalse(change.apply().complete(), "removing a string that is not there reported success");
    }

    /// Same name, different string: the one this change recorded is gone, and removing by its id
    /// would quietly remove nothing while reporting success.
    @Test
    void removingAStringRefusesWhenAnotherStringTookItsName() {
        BibDatabase database = new BibDatabase();
        BibtexString recorded = new BibtexString("name", "content");
        database.addString(recorded);
        UndoableRemoveString change = new UndoableRemoveString(database, recorded);
        change.apply();
        database.addString(new BibtexString("name", "something else"));

        assertFalse(change.apply().complete());
        assertEquals(1, database.getStringCount(), "the other string was removed");
    }

    /// A set keeps going: one element being stale says nothing about the others.
    @Test
    void aSetAppliesWhatStillFitsAndReportsWhatDoesNot() {
        BibEntry moved = entry();
        BibEntry untouched = entry();
        UndoableFieldChange stale = new UndoableFieldChange(moved, StandardField.AUTHOR, "Einstein", "Bohr");
        ChangeSet changeSet = new ChangeSet("edit", List.of(
                stale,
                new UndoableFieldChange(untouched, StandardField.AUTHOR, "Einstein", "Curie")));
        moved.setField(StandardField.AUTHOR, "Planck");

        ApplyResult result = changeSet.apply();

        assertEquals(List.of(stale), result.failures().stream().map(ApplyResult.Failure::change).toList());
        assertEquals("Curie", untouched.getField(StandardField.AUTHOR).orElseThrow());
    }

    /// Undo and redo are the ordinary case, and must not be mistaken for staleness.
    @Test
    void aChangeAppliesInBothDirectionsWhenNothingMovedOn() {
        BibEntry entry = entry();
        UndoableFieldChange change = new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr");

        assertTrue(change.apply().complete());
        assertTrue(change.inverted().apply().complete());
        assertTrue(change.apply().complete(), "redo refused although the library was where the change left it");
    }

    @Test
    void applyingThenUndoingRestoresFieldValue() {
        BibEntry entry = entry();
        UndoableFieldChange change = new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr");

        change.apply();
        assertEquals("Bohr", entry.getField(StandardField.AUTHOR).orElseThrow());

        change.inverted().apply();
        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void undoingAnInsertRemovesTheEntryAgain() {
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase();
        UndoableInsertEntries change = new UndoableInsertEntries(database, entry);

        change.apply();
        assertEquals(List.of(entry), database.getEntries());

        change.inverted().apply();
        assertEquals(List.of(), database.getEntries());
    }

    @Test
    void aSetReportsTheChangesItCouldNotApplyAndKeepsGoing() {
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("name", "content");
        database.addString(string);
        UndoableInsertString collides = new UndoableInsertString(database, new BibtexString("name", "other"));
        ChangeSet changeSet = new ChangeSet("edit", List.of(
                collides,
                new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr")));

        ApplyResult result = changeSet.apply();

        assertEquals(List.of(collides), result.failures().stream().map(ApplyResult.Failure::change).toList());
        assertEquals("Bohr", entry.getField(StandardField.AUTHOR).orElseThrow(), "stopped at the failing change");
    }

    @Test
    void aSetThatAppliedEverythingReportsSuccess() {
        BibEntry entry = entry();
        ChangeSet changeSet = new ChangeSet("edit", List.of(
                new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr")));

        assertTrue(changeSet.apply().complete());
    }

    @Test
    void aNestedSetsFailuresTravelUp() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("name", "content");
        database.addString(string);
        UndoableInsertString collides = new UndoableInsertString(database, new BibtexString("name", "other"));
        ChangeSet changeSet = new ChangeSet("outer", List.of(new ChangeSet("inner", List.of(collides))));

        ApplyResult result = changeSet.apply();

        assertEquals(List.of(collides), result.failures().stream().map(ApplyResult.Failure::change).toList());
    }

    @Test
    void undoingAGroupRevertsItsChangesInReverseOrder() {
        BibEntry entry = entry();
        ChangeSet changeSet = new ChangeSet("edit", List.of(
                new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr"),
                new UndoableFieldChange(entry, StandardField.AUTHOR, "Bohr", "Planck")));

        changeSet.apply();
        assertEquals("Planck", entry.getField(StandardField.AUTHOR).orElseThrow());

        changeSet.inverted().apply();
        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void undoingATypeChangeRestoresTheExactPreviousType() {
        BibEntry entry = new BibEntry(new UnknownEntryType("customtype"));
        UndoableChangeType change = new UndoableChangeType(entry, entry.getType(), StandardEntryType.Article);

        change.apply();
        assertEquals(StandardEntryType.Article, entry.getType());

        change.inverted().apply();
        assertEquals(new UnknownEntryType("customtype"), entry.getType());
    }

    @Test
    void undoingARemovalReinsertsWithTheUndoEventSource() {
        BibDatabase database = new BibDatabase();
        UndoableRemoveEntries removal = new UndoableRemoveEntries(database, entry());

        assertEquals(EntriesEventSource.UNDO, removal.inverted().source());
    }

    @Test
    void redoingAnInsertionKeepsTheLocalEventSource() {
        BibDatabase database = new BibDatabase();
        UndoableInsertEntries insertion = new UndoableInsertEntries(database, entry());

        assertEquals(EntriesEventSource.LOCAL, insertion.source());
        assertEquals(EntriesEventSource.LOCAL, insertion.inverted().inverted().source());
    }

    /// A record in the undo stack must keep the hash it was created with. BibDatabase hashes
    /// its entry list, so content-based hashing would move the record's hash whenever the
    /// library changes.
    @Test
    void hashIsStableWhileTheDatabaseChanges() {
        BibDatabase database = new BibDatabase();
        UndoablePreambleChange change = new UndoablePreambleChange(database, null, "preamble");
        int before = change.hashCode();

        database.insertEntries(List.of(entry()));

        assertEquals(before, change.hashCode());
    }

    private static GroupTreeNode group(String name) {
        return GroupTreeNode.fromGroup(new ExplicitGroup(name, GroupHierarchyType.INDEPENDENT, ','));
    }

    /// Both were recorded against the same library: the first by the external-changes dialog, the
    /// second by the group panel. Undoing them in turn has to undo both, which a record holding
    /// nodes could not do once the second one installed a fresh tree.
    @Test
    void anExternalGroupChangeStaysUndoableAfterALaterLocalOperation() {
        MetaData metaData = new MetaData();
        metaData.setGroups(group("All"));

        Optional<GroupTreeNode> beforeExternal = metaData.getGroups().map(GroupTreeNode::copySubtree);
        metaData.getGroups().orElseThrow().addChild(group("FromRemote"));
        UndoableGroupTreeChange external = new UndoableGroupTreeChange(metaData, beforeExternal, metaData.getGroups());

        Optional<GroupTreeNode> beforeLocal = metaData.getGroups().map(GroupTreeNode::copySubtree);
        metaData.getGroups().orElseThrow().addChild(group("Local"));
        UndoableGroupTreeChange local = new UndoableGroupTreeChange(metaData, beforeLocal, metaData.getGroups());

        local.inverted().apply();
        assertEquals(List.of("FromRemote"), childNames(metaData));

        external.inverted().apply();
        assertEquals(List.of(), childNames(metaData), "the earlier change was undone silently");
    }

    private static List<String> childNames(GroupTreeNode node) {
        return node.getChildren().stream().map(child -> child.getGroup().getName()).toList();
    }

    @Test
    void changesAgainstDistinctEntriesWithEqualContentAreNotEqual() {
        UndoableFieldChange onFirst = new UndoableFieldChange(entry(), StandardField.AUTHOR, "Einstein", "Bohr");
        UndoableFieldChange onSecond = new UndoableFieldChange(entry(), StandardField.AUTHOR, "Einstein", "Bohr");

        assertNotEquals(onFirst, onSecond);
    }
}
