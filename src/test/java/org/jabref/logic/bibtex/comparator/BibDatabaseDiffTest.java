package org.jabref.logic.bibtex.comparator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BibDatabaseDiffTest {

    @Test
    void compareOfEmptyDatabasesReportsNoDifferences() throws Exception {
        BibDatabaseDiff diff = BibDatabaseDiff.compare(new BibDatabaseContext(), new BibDatabaseContext());

        assertEquals(Optional.empty(), diff.getPreambleDifferences());
        assertEquals(Optional.empty(), diff.getMetaDataDifferences());
        assertEquals(Collections.emptyList(), diff.getBibStringDifferences());
        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
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

    @Test
    void compareOfTwoEntriesWithSameContentAndLfEndingsReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryOne)));

        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryTwo)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndCrLfEndingsReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryOne)));

        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryTwo)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndMixedLineEndingsReportsNoDifferences() throws Exception {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryOne)));

        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryTwo)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        assertEquals(Collections.emptyList(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoDifferentEntriesWithDifferentDataReportsDifferences() throws Exception {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "another test");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryOne)));
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryTwo)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        // two different entries between the databases
        assertEquals(2, diff.getEntryDifferences().size(), "incorrect amount of different entries");

        assertEquals(entryOne, diff.getEntryDifferences().get(0).getOriginalEntry(), "there is another value as originalEntry");
        assertNull(diff.getEntryDifferences().get(0).getNewEntry(), "newEntry is not null");
        assertEquals(entryTwo, diff.getEntryDifferences().get(1).getNewEntry(), "there is another value as newEntry");
        assertNull(diff.getEntryDifferences().get(1).getOriginalEntry(), "originalEntry is not null");

    }

    @Test
    void compareOfThreeDifferentEntriesWithDifferentDataReportsDifferences() throws Exception {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "another test");
        BibEntry entryThree = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "again another test");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(Collections.singletonList(entryOne)));
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Arrays.asList(entryTwo, entryThree)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo);

        // three different entries between the databases
        assertEquals(3, diff.getEntryDifferences().size(), "incorrect amount of different entries");

        assertEquals(entryOne, diff.getEntryDifferences().get(0).getOriginalEntry(), "there is another value as originalEntry");
        assertNull(diff.getEntryDifferences().get(0).getNewEntry(), "newEntry is not null");
        assertEquals(entryTwo, diff.getEntryDifferences().get(1).getNewEntry(), "there is another value as newEntry");
        assertNull(diff.getEntryDifferences().get(1).getOriginalEntry(), "originalEntry is not null");
        assertEquals(entryThree, diff.getEntryDifferences().get(2).getNewEntry(), "there is another value as newEntry [2]");
        assertNull(diff.getEntryDifferences().get(2).getOriginalEntry(), "originalEntry is not null [2]");
    }
}
