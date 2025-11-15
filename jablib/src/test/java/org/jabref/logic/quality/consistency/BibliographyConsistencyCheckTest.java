package org.jabref.logic.quality.consistency;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibliographyConsistencyCheckTest {

    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        // TODO: add some custom entry types for this manager and test with it
        entryTypesManager = new BibEntryTypesManager();
    }

    @Test
    void checkSimpleLibrary(@TempDir Path tempDir) {
        BibEntry first = new BibEntry(StandardEntryType.Article, "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(StandardEntryType.Article, "second")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PUBLISHER, "publisher");
        BibDatabase database = new BibDatabase(List.of(first, second));
        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (count, total) -> {
        });

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
        BibDatabase bibDatabase = new BibDatabase(List.of(first, second));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult entryTypeResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.TITLE, customField), List.of(first, second));
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

        BibDatabase bibDatabase = new BibDatabase(List.of(first, second, third, fourth, fifth));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult articleResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        BibliographyConsistencyCheck.EntryTypeResult inProceedingsResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER, StandardField.LOCATION), List.of(fifth, fourth, third));
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
        BibDatabase bibDatabase = new BibDatabase(List.of(first, second));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

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

        BibDatabase bibDatabase = new BibDatabase(List.of(a, b));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, entryTypesManager, (_, _) -> {
                });

        assertEquals(Map.of(), result.entryTypeToResultMap(),
                "Differences only in filtered fields must be ignored");
    }

    @Test
    void nonFilteredFieldDifferenceIsReported() {
        BibEntry withAuthor = new BibEntry(StandardEntryType.Misc, "1")
                .withField(StandardField.AUTHOR, "Knuth");
        BibEntry withoutAuthor = new BibEntry(StandardEntryType.Misc, "2");

        BibDatabase bibDatabase = new BibDatabase(List.of(withAuthor, withoutAuthor));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, entryTypesManager, (_, _) -> {
                });

        BibliographyConsistencyCheck.EntryTypeResult typeResult =
                result.entryTypeToResultMap().get(StandardEntryType.Misc);

        assertEquals(Set.of(StandardField.AUTHOR), typeResult.fields());
    }

    @Test
    void unsetRequriedFieldsReported() {
        BibEntry withDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withDate")
                .withField(StandardField.DATE, "date") // Required in BibLaTeX
                .withField(StandardField.URLDATE, "urldate");
        BibEntry withoutDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withoutDate")
                .withField(StandardField.URLDATE, "urldate");

        BibDatabase bibDatabase = new BibDatabase(List.of(withDate, withoutDate));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBLATEX);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, entryTypesManager, (_, _) -> {
                });

        BibliographyConsistencyCheck.EntryTypeResult typeResult =
                result.entryTypeToResultMap().get(StandardEntryType.Online);

        assertEquals(List.of(withDate, withoutDate), typeResult.sortedEntries().stream().toList());
    }

    @Test
    void unsetFieldsReportedInBibtexMode() {
        // "Online" is unknown in BibTeX, thus "date" should be reported as inconsistent (set only in one entry)
        BibEntry withDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withDate")
                .withField(StandardField.DATE, "date")
                .withField(StandardField.URLDATE, "urldate");
        BibEntry withoutDate = new BibEntry(StandardEntryType.Online)
                .withCitationKey("withoutDate")
                .withField(StandardField.URLDATE, "urldate");

        BibDatabase bibDatabase = new BibDatabase(List.of(withDate, withoutDate));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBTEX);
        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck()
                .check(bibContext, entryTypesManager, (_, _) -> {
                });
        BibliographyConsistencyCheck.EntryTypeResult typeResult =
                result.entryTypeToResultMap().get(StandardEntryType.Online);

        assertEquals(List.of(withDate), typeResult.sortedEntries().stream().toList());
    }

    @Test
    void checkFieldEntriesWithFieldDifferences() {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article, "id1")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.TITLE, "Title ")
                .withField(StandardField.PAGES, "1-10");

        BibEntry entry2 = new BibEntry(StandardEntryType.Article, "id2")
                .withField(StandardField.AUTHOR, "Author Two");

        BibEntry entry3 = new BibEntry(StandardEntryType.Article, "id3")
                .withField(StandardField.AUTHOR, "Author Three")
                .withField(new UnknownField("customField"), "valore custom");

        BibEntry entry4 = new BibEntry(StandardEntryType.Article, "id4")
                .withField(StandardField.AUTHOR, "Author Four")
                .withField(StandardField.PDF, "file.pdf");

        BibEntry entry5 = new BibEntry(StandardEntryType.Article, "id5")
                .withField(StandardField.AUTHOR, "Author Five")
                .withField(StandardField.PUBLISHER, "Editor");

        Set<Field> differingFields = Set.of(
                StandardField.TITLE,
                StandardField.PAGES,
                new UnknownField("customField"),
                StandardField.PUBLISHER
        );
        List<BibEntry> result = new BibliographyConsistencyCheck().filterAndSortEntriesWithFieldDifferences(
                Set.of(entry1, entry2, entry3, entry4, entry5),
                differingFields,
                Set.of(StandardField.AUTHOR, StandardField.TITLE, StandardField.PAGES, StandardField.PDF));

        assertEquals(List.of(entry1, entry2, entry3, entry4, entry5), result);
    }

    @Test
    void checkComplexLibraryWithAdditionalEntry(@TempDir Path tempDir) {
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
        BibEntry sixth = new BibEntry(StandardEntryType.InProceedings, "sixth")
                .withField(StandardField.AUTHOR, "Author One");

        BibDatabase bibDatabase = new BibDatabase(List.of(first, second, third, fourth, fifth, sixth));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);

        BibliographyConsistencyCheck.Result actualResult = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult articleResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        BibliographyConsistencyCheck.EntryTypeResult expectedInProceedings = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER, StandardField.LOCATION, StandardField.YEAR), List.of(fifth, fourth, sixth, third));
        BibliographyConsistencyCheck.Result expectedResult = new BibliographyConsistencyCheck.Result(Map.of(
                StandardEntryType.Article, articleResult,
                StandardEntryType.InProceedings, expectedInProceedings
        ));
        assertEquals(expectedResult, actualResult);
    }
}
