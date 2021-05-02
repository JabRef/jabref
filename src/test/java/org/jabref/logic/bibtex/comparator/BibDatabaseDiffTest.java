package org.jabref.logic.bibtex.comparator;

import java.util.Collections;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibDatabaseDiffTest {

    @Test
    void compareOfEmptyDatabasesReportsNoDifferences() throws Exception {
        BibDatabaseDiff diff = BibDatabaseDiff.compare(new BibDatabaseContext(), new BibDatabaseContext());

        assertEquals(Optional.empty(), diff.getPreambleDifferences(), "Expected preamble differences to be Optional.empty()");
        assertEquals(Optional.empty(), diff.getMetaDataDifferences(), "Expected metadata differences to be Optional.empty()");
        assertEquals(Collections.emptyList(), diff.getBibStringDifferences(), "Expected bibstring differences to be Collections.emptyList()");
        assertEquals(Collections.emptyList(), diff.getEntryDifferences(), "Expected entry differences to be Collections.emptyList()");
    }

    @Test
    void compareOfSameEntryReportsNoDifferences() throws Exception {
        BibEntry entry = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entry)));
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entry)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
    }

    @Test
    void compareOfDifferentEntriesWithSameDataReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryOne)));
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryTwo)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
    }
}
