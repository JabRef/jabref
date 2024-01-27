package org.jabref.logic.quality.consistency;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class PaperConsistencyCheckTest {

    private BibtexImporter importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        PaperConsistencyCheck.Result result = new PaperConsistencyCheck().check(List.of(first, second));

        PaperConsistencyCheck.EntryTypeResult entryTypeResult = new PaperConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        PaperConsistencyCheck.Result expected = new PaperConsistencyCheck.Result(Map.of(StandardEntryType.Article, entryTypeResult));
        assertEquals(expected, result);
    }

    @Test
    void checkDifferentOutputSymbols(@TempDir Path tempDir) throws Exception {
        UnknownField customField = new UnknownField("custom");
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One") // required
                .withField(StandardField.TITLE, "Title") // required
                .withField(StandardField.PAGES, "some pages") // optional
                .withField(customField, "custom"); // unknown
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One");
        PaperConsistencyCheck.Result result = new PaperConsistencyCheck().check(List.of(first, second));

        PaperConsistencyCheck.EntryTypeResult entryTypeResult = new PaperConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.TITLE, customField), List.of(first));
        PaperConsistencyCheck.Result expected = new PaperConsistencyCheck.Result(Map.of(StandardEntryType.Article, entryTypeResult));
        assertEquals(expected, result);
    }

    @Test
    void checkComplexLibrary(@TempDir Path tempDir) throws Exception {
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

        PaperConsistencyCheck.Result result = new PaperConsistencyCheck().check(List.of(first, second, third, fourth, fifth));

        PaperConsistencyCheck.EntryTypeResult articleResult = new PaperConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        PaperConsistencyCheck.EntryTypeResult inProceedingsResult = new PaperConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER, StandardField.LOCATION), List.of(fourth, third));
        PaperConsistencyCheck.Result expected = new PaperConsistencyCheck.Result(Map.of(
                StandardEntryType.Article, articleResult,
                StandardEntryType.InProceedings, inProceedingsResult
        ));
        assertEquals(expected, result);
    }

    @Test
    void checkLibraryWithoutIssues(@TempDir Path tempDir) throws Exception {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        PaperConsistencyCheck.Result result = new PaperConsistencyCheck().check(List.of(first, second));

        PaperConsistencyCheck.Result expected = new PaperConsistencyCheck.Result(Map.of());
        assertEquals(expected, result);
    }
}
