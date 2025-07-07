package org.jabref.logic.quality.consistency;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibliographyConsistencyCheckTest {

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second), (_, _) -> { });

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
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second), (_, _) -> { });

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

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second, third, fourth, fifth), (_, _) -> { });

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
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(List.of(first, second), (_, _) -> { });

        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of());
        assertEquals(expected, result);
    }

    @Test
    void filteredFieldsAreIgnored() {
        BibEntry a = new BibEntry(StandardEntryType.Misc, "a")
                .withField(StandardField.COMMENT, "note")
                .withField(StandardField.PDF, "file.pdf")
                .withField(new UserSpecificCommentField("XYZ"), "foo")
                .withField(SpecialField.PRIORITY, "high");
        BibEntry b = new BibEntry(StandardEntryType.Misc, "b")
                .withField(StandardField.COMMENT, "another note")
                .withField(StandardField.PDF, "other.pdf");

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(List.of(a, b), (_, _) -> { });

        assertEquals(Map.of(), result.entryTypeToResultMap(),
                "Differences only in filtered fields must be ignored");
    }

    @Test
    void nonFilteredFieldDifferenceIsReported() {
        BibEntry withAuthor = new BibEntry(StandardEntryType.Misc, "1")
                .withField(StandardField.AUTHOR, "Knuth");
        BibEntry withoutAuthor = new BibEntry(StandardEntryType.Misc, "2");

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(List.of(withAuthor, withoutAuthor), (_, _) -> { });

        BibliographyConsistencyCheck.EntryTypeResult typeResult =
                result.entryTypeToResultMap().get(StandardEntryType.Misc);

        assertEquals(Set.of(StandardField.AUTHOR), typeResult.fields());
    }

    @Test
    @Disabled("Fixed when https://github.com/JabRef/jabref/issues/13467 is resolved")
    void unsetFieldsReported() {
        BibEntry withDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withDate")
                .withField(StandardField.DATE, "date")
                .withField(StandardField.URLDATE, "urldate");
        BibEntry withoutDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withoutDate")
                .withField(StandardField.URLDATE, "urldate");

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(List.of(withDate, withoutDate), (_, _) -> { });

        BibliographyConsistencyCheck.EntryTypeResult typeResult =
                result.entryTypeToResultMap().get(StandardEntryType.Online);

        assertEquals(List.of(withDate, withoutDate), typeResult.sortedEntries().stream().toList());
    }
}
