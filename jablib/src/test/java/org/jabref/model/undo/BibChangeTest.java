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
    void applyingAMetaDataChangeKeepsTheMetaDataInstance() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        MetaData before = databaseContext.getMetaData();
        UndoableMetaDataChange change = new UndoableMetaDataChange(databaseContext, before, metaDataWithMode());

        change.apply();
        assertSame(before, databaseContext.getMetaData());

        change.inverted().apply();
        assertSame(before, databaseContext.getMetaData());
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

        assertTrue(changeSet.apply().isComplete());
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

    @Test
    void replacingAGroupSubtreeCanBeUndoneAndRedone() {
        GroupTreeNode root = group("root");
        root.addChild(group("original"));

        GroupTreeNode before = root.copySubtree();
        root.removeAllChildren();
        root.addChild(group("replacement"));
        UndoableModifySubtree change = new UndoableModifySubtree(root, root.getIndexedPathFromRoot(), before, root.copySubtree());

        change.inverted().apply();
        assertEquals(List.of("original"), childNames(root));

        change.apply();
        assertEquals(List.of("replacement"), childNames(root));
    }

    @Test
    void redoingASubtreeReplacementWithoutUndoingFirstIsHarmless() {
        GroupTreeNode root = group("root");
        GroupTreeNode before = root.copySubtree();
        root.addChild(group("replacement"));
        UndoableModifySubtree change = new UndoableModifySubtree(root, root.getIndexedPathFromRoot(), before, root.copySubtree());

        change.apply();

        assertEquals(List.of("replacement"), childNames(root));
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
