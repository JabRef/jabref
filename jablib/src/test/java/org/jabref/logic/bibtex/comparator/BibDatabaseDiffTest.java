package org.jabref.logic.bibtex.comparator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BibDatabaseDiffTest {

    private static final EntryType MEASUREMENT = new UnknownEntryType("measurement");

    private final BibEntryTypesManager entryTypesManager = new BibEntryTypesManager();

    @Test
    void compareOfEmptyDatabasesReportsNoDifferences() {
        BibDatabaseDiff diff = BibDatabaseDiff.compare(new BibDatabaseContext(), new BibDatabaseContext(), entryTypesManager);

        assertEquals(Optional.empty(), diff.getPreambleDifferences());
        assertEquals(Optional.empty(), diff.getMetaDataDifferences());
        assertEquals(List.of(), diff.getBibStringDifferences());
        assertEquals(List.of(), diff.getEntryDifferences());
    }

    @Test
    void compareOfSameEntryReportsNoDifferences() {
        BibEntry entry = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");

        BibDatabaseDiff diff = compareEntries(entry, entry);

        assertEquals(List.of(), diff.getEntryDifferences());
    }

    @Test
    void compareOfDifferentEntriesWithSameDataReportsNoDifferences() {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        assertEquals(List.of(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndLfEndingsReportsNoDifferences() {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        assertEquals(List.of(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndCrLfEndingsReportsNoDifferences() {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");
        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        assertEquals(List.of(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoEntriesWithSameContentAndMixedLineEndingsReportsNoDifferences() {
        BibEntry entryOne = new BibEntry().withField(StandardField.COMMENT, "line1\n\nline3\n\nline5");
        BibEntry entryTwo = new BibEntry().withField(StandardField.COMMENT, "line1\r\n\r\nline3\r\n\r\nline5");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        assertEquals(List.of(), diff.getEntryDifferences());
    }

    @Test
    void compareOfTwoDifferentEntriesWithDifferentDataReportsDifferences() {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "another test");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        // two different entries between the databases
        assertEquals(2, diff.getEntryDifferences().size(), "incorrect amount of different entries");

        assertEquals(entryOne, diff.getEntryDifferences().getFirst().originalEntry(), "there is another value as originalEntry");
        assertNull(diff.getEntryDifferences().getFirst().newEntry(), "newEntry is not null");
        assertEquals(entryTwo, diff.getEntryDifferences().get(1).newEntry(), "there is another value as newEntry");
        assertNull(diff.getEntryDifferences().get(1).originalEntry(), "originalEntry is not null");
    }

    @Test
    void compareOfThreeDifferentEntriesWithDifferentDataReportsDifferences() {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "test");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "another test");
        BibEntry entryThree = new BibEntry(BibEntry.DEFAULT_TYPE).withField(StandardField.TITLE, "again another test");
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(List.of(entryOne)));
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(Arrays.asList(entryTwo, entryThree)));

        BibDatabaseDiff diff = BibDatabaseDiff.compare(databaseOne, databaseTwo, entryTypesManager);

        // three different entries between the databases
        assertEquals(3, diff.getEntryDifferences().size(), "incorrect amount of different entries");

        assertEquals(entryOne, diff.getEntryDifferences().getFirst().originalEntry(), "there is another value as originalEntry");
        assertNull(diff.getEntryDifferences().getFirst().newEntry(), "newEntry is not null");
        assertEquals(entryTwo, diff.getEntryDifferences().get(1).newEntry(), "there is another value as newEntry");
        assertNull(diff.getEntryDifferences().get(1).originalEntry(), "originalEntry is not null");
        assertEquals(entryThree, diff.getEntryDifferences().get(2).newEntry(), "there is another value as newEntry [2]");
        assertNull(diff.getEntryDifferences().get(2).originalEntry(), "originalEntry is not null [2]");
    }

    @Test
    void compareOfTwoEntriesWithEqualCitationKeysShouldReportsOneDifference() {
        BibEntry entryOne = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.TITLE, "test")
                .withField(StandardField.AUTHOR, "author")
                .withField(StandardField.YEAR, "2001")
                .withCitationKey("key");
        BibEntry entryTwo = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.TITLE, "test1")
                .withField(StandardField.AUTHOR, "writer")
                .withField(StandardField.YEAR, "1899")
                .withCitationKey("key");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        // two different entries between the databases
        assertEquals(1, diff.getEntryDifferences().size(), "incorrect amount of different entries");

        assertEquals(entryOne, diff.getEntryDifferences().getFirst().originalEntry(), "there is another value as originalEntry");
        assertEquals(entryTwo, diff.getEntryDifferences().getFirst().newEntry(), "there is another value as newEntry");
    }

    @Test
    void compareOfEntriesOfUnknownTypeWithOnlyOneEqualFieldReportsRemovalAndAddition() {
        BibEntry entryOne = createMeasurement("first reading", "lab A");
        BibEntry entryTwo = createMeasurement("totally other", "lab B");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        assertEquals(List.of(new BibEntryDiff(entryOne, null), new BibEntryDiff(null, entryTwo)), diff.getEntryDifferences());
    }

    // [utest->req~ux.external-library-changes.custom-entry-types~1]
    @Test
    void compareOfEntriesOfCustomTypeWithEqualRequiredFieldReportsOneModification() {
        entryTypesManager.addCustomOrModifiedType(
                new BibEntryTypeBuilder().withType(MEASUREMENT)
                                         .withRequiredFields(StandardField.NUMBER)
                                         .build(),
                BibDatabaseMode.BIBLATEX);
        BibEntry entryOne = createMeasurement("first reading", "lab A");
        BibEntry entryTwo = createMeasurement("totally other", "lab B");

        BibDatabaseDiff diff = compareEntries(entryOne, entryTwo);

        assertEquals(List.of(new BibEntryDiff(entryOne, entryTwo)), diff.getEntryDifferences());
    }

    private BibEntry createMeasurement(String title, String note) {
        return new BibEntry(MEASUREMENT)
                .withField(StandardField.NUMBER, "M-42")
                .withField(StandardField.TITLE, title)
                .withField(StandardField.NOTE, note);
    }

    private BibDatabaseDiff compareEntries(BibEntry entryOne, BibEntry entryTwo) {
        BibDatabaseContext databaseOne = new BibDatabaseContext(new BibDatabase(List.of(entryOne)));
        BibDatabaseContext databaseTwo = new BibDatabaseContext(new BibDatabase(List.of(entryTwo)));

        return BibDatabaseDiff.compare(databaseOne, databaseTwo, entryTypesManager);
    }
}
