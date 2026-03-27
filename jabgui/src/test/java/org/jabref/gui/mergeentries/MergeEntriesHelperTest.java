package org.jabref.gui.mergeentries;

import org.jabref.gui.undo.NamedCompoundEdit;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MergeEntriesHelperTest {

    private static final char KEYWORD_SEPARATOR = ',';

    private NamedCompoundEdit compoundEdit;

    @BeforeEach
    void setup() {
        compoundEdit = new NamedCompoundEdit("test");
    }

    @Test
    void groupsFieldIsNotRemovedWhenFetcherHasNoGroups() {
        BibEntry entryFromFetcher = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title");
        BibEntry entryFromLibrary = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.GROUPS, "IEEE");

        MergeEntriesHelper.mergeEntries(entryFromFetcher, entryFromLibrary, compoundEdit, KEYWORD_SEPARATOR);

        assertEquals("IEEE", entryFromLibrary.getField(StandardField.GROUPS).orElse(""));
    }

    @Test
    void groupsAreUnionMergedWhenBothEntriesHaveGroups() {
        BibEntry entryFromFetcher = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.GROUPS, "Springer");
        BibEntry entryFromLibrary = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.GROUPS, "IEEE");

        MergeEntriesHelper.mergeEntries(entryFromFetcher, entryFromLibrary, compoundEdit, KEYWORD_SEPARATOR);

        String groups = entryFromLibrary.getField(StandardField.GROUPS).orElse("");
        assertEquals("IEEE, Springer", entryFromLibrary.getField(StandardField.GROUPS).orElse(""));
    }

    @Test
    void groupsAreNotDuplicatedOnRepeatedMerge() {
        BibEntry entryFromFetcher = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.GROUPS, "IEEE");
        BibEntry entryFromLibrary = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.GROUPS, "IEEE");

        MergeEntriesHelper.mergeEntries(entryFromFetcher, entryFromLibrary, compoundEdit, KEYWORD_SEPARATOR);

        assertEquals("IEEE", entryFromLibrary.getField(StandardField.GROUPS).orElse(""));
    }

    @Test
    void groupsFromFetcherAreAddedWhenLibraryHasNoGroups() {
        BibEntry entryFromFetcher = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.GROUPS, "Springer");
        BibEntry entryFromLibrary = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title");

        MergeEntriesHelper.mergeEntries(entryFromFetcher, entryFromLibrary, compoundEdit, KEYWORD_SEPARATOR);

        assertEquals("Springer", entryFromLibrary.getField(StandardField.GROUPS).orElse(""));
    }

    @Test
    void regularObsoleteFieldIsRemovedWhenNotInFetcher() {
        BibEntry entryFromFetcher = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title");
        BibEntry entryFromLibrary = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Some Title")
                .withField(StandardField.NOTE, "Obsolete note");

        MergeEntriesHelper.mergeEntries(entryFromFetcher, entryFromLibrary, compoundEdit, KEYWORD_SEPARATOR);

        assertFalse(entryFromLibrary.hasField(StandardField.NOTE));
    }
}
