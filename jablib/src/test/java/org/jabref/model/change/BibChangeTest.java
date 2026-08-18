package org.jabref.model.change;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
                new FieldEdit(entry, StandardField.AUTHOR, "Einstein", "Bohr"),
                new FieldEdit(entry, StandardField.YEAR, null, "1905"),
                new FieldEdit(entry, StandardField.YEAR, "1905", null),
                new EntryTypeEdit(entry, StandardEntryType.Article, StandardEntryType.Book),
                new EntriesInserted(database, entry),
                new EntriesRemoved(database, entry),
                new PreambleEdit(database, null, "preamble"),
                new StringInserted(database, string),
                new StringRemoved(database, string),
                new StringEdit(string, StringEdit.Part.CONTENT, "content", "other"),
                new ChangeSet("group", List.of(
                        new FieldEdit(entry, StandardField.AUTHOR, "Einstein", "Bohr"),
                        new EntryTypeEdit(entry, StandardEntryType.Article, StandardEntryType.Book))));
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

    @Test
    void applyingThenUndoingRestoresFieldValue() {
        BibEntry entry = entry();
        FieldEdit change = new FieldEdit(entry, StandardField.AUTHOR, "Einstein", "Bohr");

        change.apply();
        assertEquals("Bohr", entry.getField(StandardField.AUTHOR).orElseThrow());

        change.inverted().apply();
        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void undoingAnInsertRemovesTheEntryAgain() {
        BibEntry entry = entry();
        BibDatabase database = new BibDatabase();
        EntriesInserted change = new EntriesInserted(database, entry);

        change.apply();
        assertEquals(List.of(entry), database.getEntries());

        change.inverted().apply();
        assertEquals(List.of(), database.getEntries());
    }

    @Test
    void undoingAGroupRevertsItsChangesInReverseOrder() {
        BibEntry entry = entry();
        ChangeSet changeSet = new ChangeSet("edit", List.of(
                new FieldEdit(entry, StandardField.AUTHOR, "Einstein", "Bohr"),
                new FieldEdit(entry, StandardField.AUTHOR, "Bohr", "Planck")));

        changeSet.apply();
        assertEquals("Planck", entry.getField(StandardField.AUTHOR).orElseThrow());

        changeSet.inverted().apply();
        assertEquals("Einstein", entry.getField(StandardField.AUTHOR).orElseThrow());
    }

    @Test
    void changesAgainstDistinctEntriesWithEqualContentAreNotEqual() {
        FieldEdit onFirst = new FieldEdit(entry(), StandardField.AUTHOR, "Einstein", "Bohr");
        FieldEdit onSecond = new FieldEdit(entry(), StandardField.AUTHOR, "Einstein", "Bohr");

        assertNotEquals(onFirst, onSecond);
    }
}
