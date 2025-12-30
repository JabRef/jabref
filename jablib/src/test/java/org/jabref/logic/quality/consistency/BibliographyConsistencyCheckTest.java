package org.jabref.logic.quality.consistency;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.field.UserSpecificCommentField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BibliographyConsistencyCheckTest {

    private static final EntryType CUSTOM_TYPE = new UnknownEntryType("customType");

    private BibEntryType newCustomType;
    private BibEntryType overwrittenStandardTypeWithCustomFields;
    private UnknownField bibUrl;
    private UnknownField bibSource;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                List.of(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT)),
                Set.of());

        bibUrl = new UnknownField("biburl");
        bibSource = new UnknownField("bibsource");

        overwrittenStandardTypeWithCustomFields = new BibEntryTypeBuilder()
                .withType(StandardEntryType.Article)
                .withRequiredFields(
                        StandardField.AUTHOR, StandardField.TITLE)
                .withImportantFields(
                        StandardField.SUBTITLE, StandardField.EDITOR, StandardField.SERIES, StandardField.VOLUME, StandardField.NUMBER,
                        StandardField.EID, StandardField.ISSUE, StandardField.PAGES, StandardField.NOTE, StandardField.ISSN, StandardField.DOI,
                        StandardField.EPRINT, StandardField.EPRINTCLASS, StandardField.EPRINTTYPE, StandardField.URL, StandardField.URLDATE, StandardField.LANGUAGEID,
                        bibUrl, bibSource)
                .withDetailFields(
                        StandardField.TRANSLATOR, StandardField.ANNOTATOR, StandardField.COMMENTATOR,
                        StandardField.TITLEADDON, StandardField.EDITORA, StandardField.EDITORB, StandardField.EDITORC,
                        StandardField.JOURNALSUBTITLE, StandardField.ISSUETITLE, StandardField.ISSUESUBTITLE, StandardField.LANGUAGE,
                        StandardField.ORIGLANGUAGE, StandardField.VERSION,
                        StandardField.ADDENDUM, StandardField.PUBSTATE)
                .build();

        entryTypesManager = new BibEntryTypesManager();
    }

    @Test
    void checkEntriesWithNonTrivialRequiredOrFields() {
        BibEntry one = new BibEntry(StandardEntryType.Booklet, "1")
                .withField(StandardField.EDITOR, "editor one") // Booklet requires editor or author
                .withField(StandardField.TITLE, "the first entry")
                .withField(StandardField.DATE, "date");
        BibEntry two = new BibEntry(StandardEntryType.Booklet, "2")
                .withField(StandardField.EDITOR, "editor two")
                .withField(StandardField.TITLE, "the second entry")
                .withField(StandardField.DATE, "date")
                .withField(StandardField.PAGES, "17");
        BibEntry three = new BibEntry(StandardEntryType.Booklet, "3")
                .withField(StandardField.EDITOR, "editor three")
                .withField(StandardField.TITLE, "the third entry")
                .withField(StandardField.DATE, "date");

        BibEntry four = new BibEntry(StandardEntryType.Manual, "4")
                .withField(StandardField.AUTHOR, "author four") // Manual also requires editor or author
                .withField(StandardField.TITLE, "the fourth entry")
                .withField(StandardField.DATE, "date");
        BibEntry five = new BibEntry(StandardEntryType.Manual, "5")
                .withField(StandardField.AUTHOR, "author five")
                .withField(StandardField.TITLE, "the fifth entry")
                .withField(StandardField.DATE, "date")
                .withField(StandardField.SUBTITLE, "some subtitle");
        BibEntry six = new BibEntry(StandardEntryType.Manual, "6")
                .withField(StandardField.AUTHOR, "author six")
                .withField(StandardField.TITLE, "the sixth entry")
                .withField(StandardField.DATE, "date")
                .withField(StandardField.PAGES, "59");

        BibDatabase bibDatabase = new BibDatabase(List.of(one, two, three, four, five, six));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBLATEX);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult bookletResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES), List.of(two));

        BibliographyConsistencyCheck.EntryTypeResult manualResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.SUBTITLE), List.of(five, six));

        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(
                StandardEntryType.Booklet, bookletResult,
                StandardEntryType.Manual, manualResult
        ));
        assertEquals(expected, result);
    }

    @Test
    void checkEntryWithOverwrittenStandardTypeWithCustomFields() {
        BibEntry one = new BibEntry(StandardEntryType.Article, "DBLP:journals/sqj/IftikharBAK25")
                .withField(StandardField.AUTHOR,
                        "Umar Iftikhar and J{\"u}rgen B{\"o}rstler and Nauman Bin Ali and Oliver Kopp")
                .withField(StandardField.TITLE,
                        "Supporting the identification of prevalent quality issues in code changes " +
                                "by analyzing reviewers' feedback")
                .withField(StandardField.JOURNAL, "Softw. Qual. J.")
                .withField(StandardField.VOLUME, "33")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.PAGES, "22")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.URL,
                        "https://doi.org/10.1007/s11219-025-09720-9")
                .withField(StandardField.DOI,
                        "10.1007/S11219-025-09720-9")
                .withField(StandardField.TIMESTAMP,
                        "Mon, 12 May 2025 21:02:32 +0200")
                .withField(bibUrl,
                        "https://dblp.org/rec/journals/sqj/IftikharBAK25.bib")
                .withField(bibSource,
                        "dblp computer science bibliography, https://dblp.org");

        BibEntry two = new BibEntry(StandardEntryType.Article, "DBLP:journals/sqj/Aldalur25")
                .withField(StandardField.AUTHOR,
                        "Iñigo Aldalur")
                .withField(StandardField.TITLE,
                        "Enhancing software development education through gamification and " +
                                "experiential learning with genially")
                .withField(StandardField.JOURNAL, "Softw. Qual. J.")
                .withField(StandardField.VOLUME, "33")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "1")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.URL,
                        "https://doi.org/10.1007/s11219-024-09699-9")
                .withField(StandardField.DOI,
                        "10.1007/S11219-024-09699-9")
                .withField(StandardField.TIMESTAMP,
                        "Thu, 02 Jan 2025 12:39:52 +0100")
                .withField(bibUrl,
                        "https://dblp.org/rec/journals/sqj/Aldalur25.bib");

        BibEntry three = new BibEntry(StandardEntryType.Article, "DBLP:journals/sqj/PhungOA25")
                .withField(StandardField.AUTHOR,
                        "Khoa Phung and Emmanuel Ogunshile and Mehmet Emin Aydin")
                .withField(StandardField.TITLE,
                        "Domain-specific implications of error-type metrics in risk-based software " +
                                "fault prediction")
                .withField(StandardField.JOURNAL, "Softw. Qual. J.")
                .withField(StandardField.VOLUME, "33")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "7")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.URL,
                        "https://doi.org/10.1007/s11219-024-09704-1")
                .withField(StandardField.DOI,
                        "10.1007/S11219-024-09704-1")
                .withField(StandardField.TIMESTAMP,
                        "Mon, 03 Mar 2025 22:23:25 +0100")
                .withField(bibUrl,
                        "https://dblp.org/rec/journals/sqj/PhungOA25.bib");

        BibEntry four = new BibEntry(StandardEntryType.Article, "DBLP:journals/sqj/WuWWCD25")
                .withField(StandardField.AUTHOR,
                        "Qikai Wu and Xingqi Wang and Dan Wei and Bin Chen and Qingguo Dang")
                .withField(StandardField.TITLE,
                        "Just-in-time software defect prediction method for non-stationary " +
                                "and imbalanced data streams")
                .withField(StandardField.JOURNAL, "Softw. Qual. J.")
                .withField(StandardField.VOLUME, "33")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "14")
                .withField(StandardField.YEAR, "2025")
                .withField(StandardField.URL,
                        "https://doi.org/10.1007/s11219-025-09711-w")
                .withField(StandardField.DOI,
                        "10.1007/S11219-025-09711-W")
                .withField(StandardField.TIMESTAMP,
                        "Fri, 25 Jul 2025 18:22:04 +0200");

        BibDatabase bibDatabase = new BibDatabase(List.of(one, two, three, four));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBTEX);

        entryTypesManager.addCustomOrModifiedType(overwrittenStandardTypeWithCustomFields, BibDatabaseMode.BIBTEX);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult overwrittenStandardTypeWithCustomFieldsResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(bibUrl, bibSource), List.of(two, one, three));

        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(
                StandardEntryType.Article, overwrittenStandardTypeWithCustomFieldsResult
        ));
        assertEquals(expected, result);
    }

    @Test
    void checkComplexLibraryWithCustomEntryTypes(@TempDir Path tempDir) {
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
        BibEntry sixth = new BibEntry(newCustomType.getType(), "sixth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages")
                .withField(StandardField.ABSTRACT, "some abstract");
        BibEntry seventh = new BibEntry(newCustomType.getType(), "seventh")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry eighth = new BibEntry(newCustomType.getType(), "eighth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages")
                .withField(StandardField.ABSTRACT, "some abstract")
                .withField(StandardField.YEAR, "2025");
        BibEntry ninth = new BibEntry(newCustomType.getType(), "ninth")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages")
                .withField(StandardField.YEAR, "2025");

        BibDatabase bibDatabase = new BibDatabase(List.of(first, second, third, fourth, fifth, sixth, seventh, eighth, ninth));
        BibDatabaseContext bibContext = new BibDatabaseContext(bibDatabase);
        bibContext.setMode(BibDatabaseMode.BIBTEX);

        entryTypesManager.addCustomOrModifiedType(newCustomType, BibDatabaseMode.BIBTEX);

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (_, _) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult articleResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER), List.of(first, second));
        BibliographyConsistencyCheck.EntryTypeResult inProceedingsResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.PAGES, StandardField.PUBLISHER, StandardField.LOCATION), List.of(fifth, fourth, third));
        BibliographyConsistencyCheck.EntryTypeResult customResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.ABSTRACT, StandardField.YEAR), List.of(eighth, ninth, sixth));
        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(
                StandardEntryType.Article, articleResult,
                StandardEntryType.InProceedings, inProceedingsResult,
                CUSTOM_TYPE, customResult
        ));
        assertEquals(expected, result);
    }

    @Test
    void checkSimpleLibraryWithCustomEntryTypes() {
        BibEntry first = new BibEntry(newCustomType.getType(), "first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry(newCustomType.getType(), "second")
                .withField(StandardField.AUTHOR, "Author Two")
                .withField(StandardField.PAGES, "some pages");
        BibEntry third = new BibEntry(newCustomType.getType(), "third")
                .withField(StandardField.AUTHOR, "Author Three")
                .withField(StandardField.PAGES, "some pages")
                .withField(StandardField.ABSTRACT, "some abstract");

        BibDatabase database = new BibDatabase(List.of(first, second, third));
        BibDatabaseContext bibContext = new BibDatabaseContext(database);
        bibContext.setMode(BibDatabaseMode.BIBTEX);

        entryTypesManager.addCustomOrModifiedType(newCustomType, bibContext.getMode());

        BibliographyConsistencyCheck.Result result = new BibliographyConsistencyCheck().check(bibContext, entryTypesManager, (count, total) -> {
        });

        BibliographyConsistencyCheck.EntryTypeResult entryTypeResult = new BibliographyConsistencyCheck.EntryTypeResult(Set.of(StandardField.ABSTRACT), List.of(third));
        BibliographyConsistencyCheck.Result expected = new BibliographyConsistencyCheck.Result(Map.of(CUSTOM_TYPE, entryTypeResult));
        assertEquals(expected, result);
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

        Set<OrFields> requiredFields = Set.of(StandardField.AUTHOR, StandardField.TITLE, StandardField.PAGES, StandardField.PDF).stream()
                                          .map(OrFields::new)
                                          .collect(Collectors.toSet());

        List<BibEntry> result = new BibliographyConsistencyCheck().filterAndSortEntriesWithFieldDifferences(
                Set.of(entry1, entry2, entry3, entry4, entry5),
                differingFields,
                requiredFields
        );

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
