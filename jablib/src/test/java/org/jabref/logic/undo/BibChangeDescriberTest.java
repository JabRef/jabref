package org.jabref.logic.undo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.undo.BibChange;
import org.jabref.model.undo.ChangeSet;
import org.jabref.model.undo.UndoableChangeType;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableGroupChange;
import org.jabref.model.undo.UndoableInsertEntries;
import org.jabref.model.undo.UndoableInsertString;
import org.jabref.model.undo.UndoableKeywordSeparatorChange;
import org.jabref.model.undo.UndoableMetaDataChange;
import org.jabref.model.undo.UndoablePreambleChange;
import org.jabref.model.undo.UndoableRemoveEntries;
import org.jabref.model.undo.UndoableRemoveString;
import org.jabref.model.undo.UndoableStringChange;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class BibChangeDescriberTest {

    private static BibEntry entry() {
        return new BibEntry(StandardEntryType.Article).withField(StandardField.AUTHOR, "Einstein");
    }

    static Stream<Arguments> descriptions() {
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("name", "content");
        return Stream.of(
                Arguments.of("Change field Author", new UndoableFieldChange(entry, StandardField.AUTHOR, "Einstein", "Bohr")),
                Arguments.of("Change entry type", new UndoableChangeType(entry, StandardEntryType.Article, StandardEntryType.Book)),
                Arguments.of("Insert entry", new UndoableInsertEntries(database, List.of(entry), EntriesEventSource.LOCAL)),
                Arguments.of("Insert entries", new UndoableInsertEntries(database, List.of(entry, entry()), EntriesEventSource.LOCAL)),
                Arguments.of("Remove entry", new UndoableRemoveEntries(database, List.of(entry), EntriesEventSource.LOCAL)),
                Arguments.of("Remove entries", new UndoableRemoveEntries(database, List.of(entry, entry()), EntriesEventSource.LOCAL)),
                Arguments.of("Edit group Books", new UndoableGroupChange(
                        GroupTreeNode.fromGroup(new ExplicitGroup("Articles", GroupHierarchyType.INDEPENDENT, ',')),
                        new ExplicitGroup("Articles", GroupHierarchyType.INDEPENDENT, ','),
                        new ExplicitGroup("Books", GroupHierarchyType.INDEPENDENT, ','))),
                Arguments.of("Change keyword separator", new UndoableKeywordSeparatorChange(new MetaData(), Optional.of(','), Optional.of(';'))),
                Arguments.of("Change library settings", new UndoableMetaDataChange(new BibDatabaseContext(), new MetaData(), new MetaData())),
                Arguments.of("Change preamble", new UndoablePreambleChange(database, null, "preamble")),
                Arguments.of("Change string name", new UndoableStringChange(string, UndoableStringChange.Part.CONTENT, "content", "other")),
                Arguments.of("Insert string name", new UndoableInsertString(database, string)),
                Arguments.of("Remove string name", new UndoableRemoveString(database, string)));
    }

    @ParameterizedTest
    @MethodSource("descriptions")
    void changeRecordedOnItsOwnIsDescribedByWhatItDoes(String expected, BibChange change) {
        assertEquals(expected, BibChangeDescriber.describe(change));
    }

    /// The name of a set is the label of the control the user activated, so no derived text beats it.
    @Test
    void changeSetIsDescribedByItsOwnName() {
        ChangeSet changeSet = new ChangeSet("Generate citation keys", List.of(
                new UndoableFieldChange(entry(), StandardField.AUTHOR, "Einstein", "Bohr")));

        assertEquals("Generate citation keys", BibChangeDescriber.describe(changeSet));
    }
}
