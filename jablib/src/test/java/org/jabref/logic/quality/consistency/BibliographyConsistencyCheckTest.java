package org.jabref.logic.quality.consistency;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class BibliographyConsistencyCheckTest {

    private BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        BibliographyConsistencyCheck.EntryTypeResult entryTypeResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(StandardEntryType.Article, entryTypeResult));
        assertEquals(expected, result);
    }

    @Test
    void checkDifferentOutputSymbols(@TempDir Path tempDir) {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title") // required
                .withField(StandardField.PAGES, "some pages") // optional
                .withField(customField, "custom"); // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        BibliographyConsistencyCheck.EntryTypeResult entryTypeResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.TITLE, customField), List.of(first));
        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(StandardEntryType.Article, entryTypeResult));
        assertEquals(expected, result);
    }

    @Test
    void checkComplexLibrary(@TempDir Path tempDir) {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");

        BibEntry third = new BibEntry(StandardEntryType.InProceedings, "third")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.LOCATION, "location")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.PAGES, "some pages");
        BibEntry fourth = new BibEntry(StandardEntryType.InProceedings, "fourth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024")
                .withField(StandardField.PUBLISHER, "publisher");
        BibEntry fifth = new BibEntry(StandardEntryType.InProceedings, "fifth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.YEAR, "2024");

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second, third, fourth, fifth));

        BibliographyConsistencyCheck.EntryTypeResult articleResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        BibliographyConsistencyCheck.EntryTypeResult inProceedingsResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER, StandardField.LOCATION), List.of(fourth, third));
        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(
                StandardEntryType.Article, articleResult,
                StandardEntryType.InProceedings, inProceedingsResult
        ));
        assertEquals(expected, result);
    }

    @Test
    void checkLibraryWithoutIssues(@TempDir Path tempDir) {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second));

        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of());
        assertEquals(expected, result);
    }

    /**
     * Entries that differ only by automatic, comment, PDF, special or user-specific comment fields
     * should produce no consistency results.
     */
    @Test
    void filteredFieldsAreIgnored() {
        // MISC entries differing only in filtered fields:
        BibEntry a = new BibEntry(StandardEntryType.Misc, "a")
                .withField(StandardField.COMMENT, "note")
                .withField(StandardField.PDF, "file.pdf")
                .withField(new UserSpecificCommentField("XYZ"), "foo")
                .withField(SpecialField.PRIORITY, "high");
        BibEntry b = new BibEntry(StandardEntryType.Misc, "b")
                .withField(StandardField.COMMENT, "another note")
                .withField(StandardField.PDF, "other.pdf");

        var result = new BibliographyConsistencyCheck().check(List.of(a, b));

        // Since all differences are in filtered fields, there should be no entries reported:
        assertTrue(result.entryTypeToResultMap().isEmpty(),
                "Differences only in filtered fields must be ignored");
    }

    @SuppressWarnings("checkstyle:RegexpMultiline")
    @Test
    void nonFilteredFieldDifferenceIsReported() {
        // Two MISC entries differing in AUTHOR (not filtered)
        BibEntry withAuthor = new BibEntry(StandardEntryType.Misc, "1")
                .withField(StandardField.AUTHOR, "Knuth");
        BibEntry withoutAuthor = new BibEntry(StandardEntryType.Misc, "2");

        var result = new BibliographyConsistencyCheck().check(List.of(withAuthor, withoutAuthor));

        // AUTHOR should be reported as a unique field
        var typeResult = result.entryTypeToResultMap().get(StandardEntryType.Misc);
        assertNotNull(typeResult);
        assertTrue(typeResult.fields().contains(StandardField.AUTHOR));
    }
}
