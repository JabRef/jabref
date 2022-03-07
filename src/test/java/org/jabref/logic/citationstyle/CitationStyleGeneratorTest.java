package org.jabref.logic.citationstyle;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationStyleGeneratorTest {

    private final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();

    @Test
    void testIgnoreNewLine() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Last, First and\nDoe, Jane");

        // if the default citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">F. Last and J. Doe, </div>\n" +
                "  </div>\n";
        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.getDefault(), bibEntryTypesManager);
        assertEquals(expected, citation);
    }

    @Test
    void testIgnoreCarriageReturnNewLine() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Last, First and\r\nDoe, Jane");

        // if the default citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">F. Last and J. Doe, </div>\n" +
                "  </div>\n";
        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.getDefault(), bibEntryTypesManager);
        assertEquals(expected, citation);
    }

    @Test
    void testMissingCitationStyle() {
        String expected = Localization.lang("Cannot generate preview based on selected citation style.");
        String citation = CitationStyleGenerator.generateCitation(new BibEntry(), "faulty citation style", bibEntryTypesManager);
        assertEquals(expected, citation);
    }

    @Test
    void testHtmlFormat() {
        String expectedCitation = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, &ldquo;Title of the test entry,&rdquo; <span style=\"font-style: italic\">BibTeX Journal</span>, vol. 34, no. 7, Art. no. 3, 2016-07, doi: 10.1001/bla.blubb.</div>\n" +
                "  </div>\n";

        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format, new BibDatabaseContext(), bibEntryTypesManager);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testTextFormat() {
        String expectedCitation = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 7, Art. no. 3, 2016-07, doi: 10.1001/bla.blubb.\n";

        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.TEXT;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format, new BibDatabaseContext(new BibDatabase(List.of(entry))), bibEntryTypesManager);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testHandleDiacritics() {
        BibEntry entry = new BibEntry();
        // We need to escape the backslash as well, because the slash is part of the LaTeX expression
        entry.setField(StandardField.AUTHOR, "L{\\\"a}st, First and Doe, Jane");
        // if the default citation style changes this has to be modified.
        // in this case ä was added to check if it is formatted appropriately
        String expected = "  <div class=\"csl-entry\">\n" +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">F. L&auml;st and J. Doe, </div>\n" +
                "  </div>\n";
        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.getDefault(), bibEntryTypesManager);
        assertEquals(expected, citation);
    }

    @Test
    @Disabled("Currently citeproc does not handler number field correctly")
    void testHandleAmpersand() {
        String expectedCitation = "[1]B. Smith, B. Jones, and J. Williams, “&TitleTest&” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016.\n";
        BibEntry entry = TestEntry.getTestEntry();
        entry.setField(StandardField.TITLE, "“&TitleTest&”");
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.TEXT;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format, new BibDatabaseContext(), bibEntryTypesManager);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testHandleCrossRefFields() {
        BibEntry firstEntry = new BibEntry(StandardEntryType.InCollection)
                .withCitationKey("smit2021")
                .withField(StandardField.AUTHOR, "Smith, Bob")
                .withField(StandardField.TITLE, "An article")
                .withField(StandardField.PAGES, "1-10")
                .withField(StandardField.CROSSREF, "jone2021");

        BibEntry secondEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("jone2021")
                .withField(StandardField.EDITOR, "Jones, John")
                .withField(StandardField.PUBLISHER, "Great Publisher")
                .withField(StandardField.TITLE, "A book")
                .withField(StandardField.YEAR, "2021")
                .withField(StandardField.ADDRESS, "Somewhere");

        String expectedCitation = "[1]B. Smith, “An article,” J. Jones, Ed. Somewhere: Great Publisher, 2021, pp. 1–10.\n";
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(firstEntry, secondEntry)));
        String style = CitationStyle.getDefault().getSource();

        String actualCitation = CitationStyleGenerator.generateCitation(firstEntry, style, CitationStyleOutputFormat.TEXT, bibDatabaseContext, bibEntryTypesManager);
        assertEquals(expectedCitation, actualCitation);
    }

    static Stream<Arguments> testCslMapping() {
        // if the default citation style changes this has to be modified
        return Stream.of(
                Arguments.of(
                        "[1]F. Last and J. Doe, Art. no. 7.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.NUMBER, "7"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 7.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.ISSUE, "7"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, Art. no. 7.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and Doe, Jane")
                                .withField(StandardField.NUMBER, "7"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 7, Art. no. 28.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.ISSUE, "7")
                                .withField(StandardField.NUMBER, "28"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 28, pp. 7–8.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.PAGES, "7--8")
                                .withField(StandardField.ISSUE, "28"),
                        "ieee.csl")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCslMapping(String expected, BibDatabaseMode mode, BibEntry entry, String cslFileName) throws Exception {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        bibDatabaseContext.setMode(mode);

        String citation = CitationStyleGenerator.generateCitation(entry, CitationStyle.createCitationStyleFromFile(cslFileName).orElseThrow().getSource(), CitationStyleOutputFormat.TEXT, bibDatabaseContext, bibEntryTypesManager);
        assertEquals(expected, citation);
    }
}
