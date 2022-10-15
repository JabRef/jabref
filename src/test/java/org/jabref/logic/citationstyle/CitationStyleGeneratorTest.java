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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationStyleGeneratorTest {

    private final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();

    @Test
    void testACMCitation() {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
        context.setMode(BibDatabaseMode.BIBLATEX);
        List<CitationStyle> styleList = CitationStyle.discoverCitationStyles();
        CitationStyle style = styleList.stream().filter(e -> "ACM SIGGRAPH".equals(e.getTitle())).findAny().orElse(null);
        String citation = CitationStyleGenerator.generateCitation(TestEntry.getTestEntry(), style.getSource(), CitationStyleOutputFormat.HTML, context, new BibEntryTypesManager());

        // if the acm-siggraph.csl citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">"
                + "<span style=\"font-variant: small-caps\">Smith, B., Jones, B., and Williams, J.</span> 2016-07. Title of the test entry. <span style=\"font-style: italic\">BibTeX Journal</span> <span style=\"font-style: italic\">34</span>, 3, 45&ndash;67."
                + "</div>\n"
                + "";

        assertEquals(expected, citation);
    }

    @Test
    void testAPACitation() {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
        context.setMode(BibDatabaseMode.BIBLATEX);
        List<CitationStyle> styleList = CitationStyle.discoverCitationStyles();
        CitationStyle style = styleList.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().orElse(null);
        String citation = CitationStyleGenerator.generateCitation(TestEntry.getTestEntry(), style.getSource(), CitationStyleOutputFormat.HTML, context, new BibEntryTypesManager());

        // if the apa-7th-citation.csl citation style changes this has to be modified
        String expected = "  <div class=\"csl-entry\">"
                + "Smith, B., Jones, B., &amp; Williams, J. (2016-07). Title of the test entry. <span style=\"font-style: italic\">BibTeX Journal</span>, <span style=\"font-style: italic\">34</span>(3), 45&ndash;67. https://doi.org/10.1001/bla.blubb"
                + "</div>\n"
                + "";

        assertEquals(expected, citation);
    }

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
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, &ldquo;Title of the test entry,&rdquo; <span style=\"font-style: italic\">BibTeX Journal</span>, vol. 34, no. 3, pp. 45&ndash;67, 2016-07, doi: 10.1001/bla.blubb.</div>\n" +
                "  </div>\n";

        BibEntry entry = TestEntry.getTestEntry();
        String style = CitationStyle.getDefault().getSource();
        CitationStyleOutputFormat format = CitationStyleOutputFormat.HTML;

        String actualCitation = CitationStyleGenerator.generateCitation(entry, style, format, new BibDatabaseContext(), bibEntryTypesManager);
        assertEquals(expectedCitation, actualCitation);
    }

    @Test
    void testTextFormat() {
        String expectedCitation = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, 2016-07, doi: 10.1001/bla.blubb.\n";

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
    void testHandleAmpersand() {
        String expectedCitation = "[1]B. Smith, B. Jones, and J. Williams, “Famous quote: “&TitleTest&” - that is it,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, 2016-07, doi: 10.1001/bla.blubb.\n";
        BibEntry entry = TestEntry.getTestEntry();
        entry.setField(StandardField.TITLE, "Famous quote: “&TitleTest&” - that is it");
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
                        "[1]F. Last and J. Doe, no. 28.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.NUMBER, "28"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 7.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.ISSUE, "7"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 28.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and Doe, Jane")
                                .withField(StandardField.NUMBER, "28"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 28.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.ISSUE, "7")
                                .withField(StandardField.NUMBER, "28"),
                        "ieee.csl"),
                Arguments.of(
                        "[1]F. Last and J. Doe, no. 7, Art. no. e0270533.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.ISSUE, "7")
                                .withField(StandardField.EID, "e0270533"),
                        "ieee.csl"),
                         Arguments.of(
                        "[1]F. Last and J. Doe, no. 33, pp. 7–8.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Last, First and\nDoe, Jane")
                                .withField(StandardField.PAGES, "7--8")
                                .withField(StandardField.ISSUE, "33"),
                        "ieee.csl"),

                Arguments.of(
                        "Foo, B. (n.d.). volume + issue + number + pages. Bib(La)TeX Journal, 1(3number), 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "volume + issue + number + pages")
                                .withField(StandardField.VOLUME, "1")
                                .withField(StandardField.COMMENT, "The issue field does not exist in Bibtex standard, therefore there is no need to render it (The issue field exists in biblatex standard though)")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

                Arguments.of(
                        "Foo, B. (n.d.). volume + issue + pages. Bib(La)TeX Journal, 1(9), 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "volume + issue + pages")
                                .withField(StandardField.VOLUME, "1")
                                .withField(StandardField.COMMENT, "The issue field does not exist in Bibtex standard, therefore there is no need to render it (The issue field exists in biblatex standard though.). Since, for this entry, there is no number field present and therefore no data will be overwriten, enabling the user to be able to move the data within the issue field to the number field via cleanup action is something worth pursuing.")
                                .withField(StandardField.ISSUE, "9"),
                        "apa.csl"),

                Arguments.of(
                        "Foo, B. (n.d.). volume + pages. Bib(La)TeX Journal, 1, 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "volume + pages")
                                .withField(StandardField.VOLUME, "1"),
                        "apa.csl"),

                Arguments.of(
                        "Foo, B. (n.d.). number. Bib(La)TeX Journal, (3number).\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.TITLE, "number"),
                        "apa.csl"),

                // The issue field does not exist in bibtex standard, therefore there is no need to render it (it exists in biblatex standard though). Since, for this entry, there is no number field present and therefore no data will be overwriten, enabling the user to be able to move the data within the issue field to the number field via cleanup action is something worth pursuing.
                Arguments.of(
                        "Foo, B. (n.d.). issue + pages. Bib(La)TeX Journal, (9), 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "issue + pages")
                                .withField(StandardField.ISSUE, "9"),
                        "apa.csl"),

                // The issue field does not exist in bibtex standard, therefore there is no need to render it (it exists in biblatex standard though)
                Arguments.of(
                        "Foo, B. (n.d.). issue + number. Bib(La)TeX Journal, (3number).\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.TITLE, "issue + number")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

                // The issue field does not exist in bibtex standard, therefore there is no need to render it (it exists in biblatex standard though)
                Arguments.of(
                        "Foo, B. (n.d.). issue + number + pages. Bib(La)TeX Journal, (3number), 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "issue + number + pages")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

                // "Article number" is not a field that exists in Bibtex standard. Printing the article number in the pages field is a workaround. Some Journals have opted to put the article number into the pages field. APA 7th Style recommends following procedure: If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range. Question: Should it be rendered WITH Article or WITHOUT the Word Article in Front? I guess without?
                Arguments.of(
                        "Foo, B. (n.d.). number + pages. Bib(La)TeX Journal, (3number), Article 777.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "Article 777")
                                .withField(StandardField.TITLE, "number + pages")
                                .withField(StandardField.COMMENT, "number + article-number WITH the word article instead of pagerange"),
                        "apa.csl"),

                // "The issue field does not exist in bibtex standard, therefore there is no need to render it (it exists in biblatex standard though). Since, for this entry, there is no number field present and therefore no data will be overwriten, enabling the user to be able to move the data within the issue field to the number field via cleanup action is something worth pursuing."
                Arguments.of(
                        "Foo, B. (n.d.). issue. Bib(La)TeX Journal, (9issue).\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "issue")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

                Arguments.of(
                        "Foo, B. (n.d.). number + pages. Bib(La)TeX Journal, (3number), 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "Bib(La)TeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "number + pages"),
                        "apa.csl"),

                // "Article number" is not a field that exists in Bibtex standard. Printing the article number in the pages field is a workaround. Some Journals have opted to put the article number into the pages field. APA 7th Style recommends following procedure: If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range. Question: Should it be rendered WITH Article or WITHOUT the Word Article in Front? I guess without?
                Arguments.of(
                        "Foo, B. (n.d.). number + pages. BibTeX Journal, (3number), 777e23.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "BibTeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "777e23")
                                .withField(StandardField.TITLE, "number + pages")
                                .withField(StandardField.COMMENT, "number + article-number WITHOUT the word article instead of pagerange"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is correct. The "eid" field(short for: electronic identifier) does not exist in Bibtex standard. It exists in Biblatex standard though and is the field, in which the "article number" should be entered into. "Article number" is a field that does not exist in Bibtex and also not in Biblatex standard. As a workaround, some Journals have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue (= the "number" field" in Biblatex)  whenever it is in the data and the number (= the "eid" field in Biblatex) when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280). I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition. Not rendering the "issue" field here is sufficient for APA 7th edition. Under current circumstances the "number" field takes priority over the "issue" field (see https://github.com/JabRef/jabref/issues/8372#issuecomment-1023768144). [Keyword: IS RENDERING BOTH VIABLE?]. Ideally, they would coexist: "Roughly speaking number subdivides volume and issue is much closer to subdividing year. I don't think I would want to say that issue is subordinate to number or vice versa. They sort of operate on a similar level." (Source: https://github.com/plk/biblatex/issues/726#issuecomment-1010264258)
                Arguments.of(
                        "Foo, B. (n.d.). eid + issue + number + pages. BibTeX Journal, (3number), 45–67.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "BibTeX Journal")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.TITLE, "eid + issue + number + pages")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

                /*
                Not rendering the "eid" field here, is correct. The "eid" field(= electronic identifier) does not exist in Bibtex standard.
                                 Since, for this entry, there is no pages field present and therefore no data will be overwritten, enabling the user to be able to move the data within the eid scala.annotation.meta.field to the pages scala.annotation.meta.field via cleanup action is something worth pursuing.

                Not rendering the "issue" field here is correct. The "issue" field does not exist in Bibtex standard.
                        Since, for this entry, there is no number field present and therefore no data will be overwritten, enabling the user to be able to move the data within the issue field to the number field via cleanup action is something worth pursuing.
                 */
                Arguments.of(
                        "Foo, B. (n.d.). eid + issue. BibTeX Journal, (9issue), Article 6eid.\n",
                        BibDatabaseMode.BIBTEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNAL, "BibTeX Journal")
                                .withField(StandardField.TITLE, "eid + issue")
                                .withField(StandardField.COMMENT, "")
                                .withField(StandardField.EID, "Article 6eid")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

        /*
        Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.

        Not rendering the "issue" field here is sufficient for APA 7th edition. Under current circumstances the "number" field takes priority over the "issue" field (see https://github.com/JabRef/jabref/issues/8372#issuecomment-1023768144). [Keyword: IS RENDERING BOTH VIABLE?]. Ideally, they would coexist: "Roughly speaking number subdivides volume and issue is much closer to subdividing year. I don't think I would want to say that issue is subordinate to number or vice versa. They sort of operate on a similar level." (Source: https://github.com/plk/biblatex/issues/726#issuecomment-1010264258)
         */
                Arguments.of(
                        "Foo, B. (n.d.). volume + issue + number + pages + eid. Bib(La)TeX Journal, 1(3number), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "volume + issue + number + pages + eid")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.VOLUME, "1"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.
                Arguments.of(
                        "Foo, B. (n.d.). volume + issue + pages + eid. Bib(La)TeX Journal, 1(9issue), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "volume + issue + pages + eid")
                                .withField(StandardField.EID, "Article 6eid")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.VOLUME, "1"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.
                Arguments.of(
                        "Foo, B. (n.d.). volume + number + pages + eid. Bib(La)TeX Journal, 1(3number), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "volume + number + pages + eid")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.VOLUME, "1"),
                        "apa.csl"),

                Arguments.of(
                        /*
                         * Change test once apa.csl supports the CSL "number" field.
                         * Tracked in https://github.com/citation-style-language/styles/issues/5827
                         * Ideal test: "Foo, B. (n.d.). eid + issue. Bib(La)TeX Journal, (9issue), Article 6eid.\n",
                         * Because of https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2
                         */
                        "Foo, B. (n.d.). eid + issue. Bib(La)TeX Journal, (9issue).\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + issue")
                                .withField(StandardField.EID, "Article 6eid")
                                .withField(StandardField.ISSUE, "9issue"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.
                Arguments.of(
                        "Foo, B. (n.d.). eid + issue + pages. Bib(La)TeX Journal, (9issue), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + issue + pages")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.PAGES, "45--67"),
                        "apa.csl"),

                // Not rendering the "issue" field here is sufficient for APA 7th edition. Under current circumstances the "number" field takes priority over the "issue" field (see https://github.com/JabRef/jabref/issues/8372#issuecomment-1023768144). [Keyword: IS RENDERING BOTH VIABLE?]. Ideally, they would coexist: "Roughly speaking number subdivides volume and issue is much closer to subdividing year. I don't think I would want to say that issue is subordinate to number or vice versa. They sort of operate on a similar level." (Source: https://github.com/plk/biblatex/issues/726#issuecomment-1010264258)
                /*
                  Change test once apa.csl supports the CSL "number" field.
                  Tracked in https://github.com/citation-style-language/styles/issues/5827
                  Ideal test: "Foo, B. (n.d.). eid + issue + number. Bib(La)TeX Journal, (3number), Article 6eid.\n",
                  Because of https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2
                 */
                Arguments.of(
                        "Foo, B. (n.d.). eid + issue + number. Bib(La)TeX Journal, (3number).\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + issue + number")
                                .withField(StandardField.EID, "Article 6eid")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.NUMBER, "3number"),
                        "apa.csl"),

                /*
                    Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.

                    Not rendering the "issue" field here is sufficient for APA 7th edition. Under current circumstances the "number" field takes priority over the "issue" field (see https://github.com/JabRef/jabref/issues/8372#issuecomment-1023768144). [Keyword: IS RENDERING BOTH VIABLE?]. Ideally, they would coexist: "Roughly speaking number subdivides volume and issue is much closer to subdividing year. I don't think I would want to say that issue is subordinate to number or vice versa. They sort of operate on a similar level." (Source: https://github.com/plk/biblatex/issues/726#issuecomment-1010264258)
                */
                Arguments.of(
                        "Foo, B. (n.d.). eid + issue + number + pages. Bib(La)TeX Journal, (3number), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + issue + number + pages")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is sufficient, but contested practice. My personal opinion is that the "eid" (article number) will probably be more helpful to users for finding this article than the page range, if the "number" and "issue" and "volume" are unknown.
                Arguments.of(
                        "Foo, B. (n.d.). eid + pages. Bib(La)TeX Journal, 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + pages")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.PAGES, "45--67"),
                        "apa.csl"),

                Arguments.of(
                       /*
                         Change test once apa.csl supports the CSL "number" field.
                         Tracked in https://github.com/citation-style-language/styles/issues/5827
                         Ideal test: "Foo, B. (n.d.). eid. Bib(La)TeX Journal, Article 6eid.\n",
                         Because of https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2
                       */
                        "Foo, B. (n.d.). eid. Bib(La)TeX Journal.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid")
                                .withField(StandardField.EID, "Article 6eid"),
                        "apa.csl"),

                // All correct and not controversial, because APA Style (7th edition) recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." (Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2)
                /*
                 Change test once apa.csl supports the CSL "number" field.
                 Tracked in https://github.com/citation-style-language/styles/issues/5827
                 Ideal test: "Foo, B. (n.d.). volume + number + eid. Bib(La)TeX Journal, 1(3number), Article 6eid.\n",
                 Because of https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2
                */
                Arguments.of(
                        "Foo, B. (n.d.). volume + number + eid. Bib(La)TeX Journal, 1(3number).\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "volume + number + eid")
                                .withField(StandardField.EID, "Article 6eid")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.VOLUME, "1"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.
                Arguments.of(
                        "Foo, B. (n.d.). volume + pages + eid. Bib(La)TeX Journal, 1, 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "volume + pages + eid")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.PAGES, "45--67")
                                .withField(StandardField.VOLUME, "1"),
                        "apa.csl"),

                // APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." (Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2)
                /*
                 Change test once apa.csl supports the CSL "number" field.
                 Tracked in https://github.com/citation-style-language/styles/issues/5827
                 Ideal test: "Foo, B. (n.d.). eid + number. Bib(La)TeX Journal, (3number), Article 6eid.\n",
                 Because of https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2
                */
                Arguments.of(
                        "Foo, B. (n.d.). eid + number. Bib(La)TeX Journal, (3number).\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + number")
                                .withField(StandardField.EID, "Article 6eid")
                                .withField(StandardField.NUMBER, "3number"),
                        "apa.csl"),

                // Not rendering the "eid" field here, is sufficient, but contested practice. This statement is based on the following reasoning: "eid" (in long: electronic identifier) is the field, which should be used to enter the "article number", when using the Biblatex standard. Both "eid" and "article number" do not exist in the older Bibtex standard. As a workaround, some Journals and publishers have opted to put the article number into the pages field. APA 7th Style recommends following procedure: "If the journal article has an article number instead of a page range, include the word "Article" and then the article number instead of the page range." - Source: https://apastyle.apa.org/style-grammar-guidelines/references/examples/journal-article-references#2. Additionally the APA style (7th edition) created by the CSL community "prints the issue [= the "number" field" in both Biblatex and Bibtex]  whenever it is in the data and the number [= the "eid" field in Biblatex] when no page range is present, entirely independent of the issue number" - Source: https://github.com/citation-style-language/styles/issues/5827#issuecomment-1006011280. I personally think the "eid" field SHOULD be rendered here SOMEWHERE, maybe even IN ADDITION to the page range, because we have the data, right? Why not show it? - But this is just my humble opinion and may not be coherent with the current APA Style 7th edition.
                Arguments.of(
                        "Foo, B. (n.d.). eid + number + pages. Bib(La)TeX Journal, (3number), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "eid + number + pages")
                                .withField(StandardField.EID, "6eid")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67"),
                        "apa.csl"),

                // Not rendering the "issue" field here is sufficient for APA Style (7th edition). Under current circumstances the "number" field takes priority over the "issue" field (see https://github.com/JabRef/jabref/issues/8372#issuecomment-1023768144). [Keyword: IS RENDERING BOTH VIABLE?]. Ideally, they would coexist: "Roughly speaking number subdivides volume and issue is much closer to subdividing year. I don't think I would want to say that issue is subordinate to number or vice versa. They sort of operate on a similar level." (Source: https://github.com/plk/biblatex/issues/726#issuecomment-1010264258)
                Arguments.of(
                        "Foo, B. (n.d.). issue + number. Bib(La)TeX Journal, (3number).\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "issue + number")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.NUMBER, "3number"),
                        "apa.csl"),

                // Not rendering the "issue" field here is sufficient for APA 7th edition. Under current circumstances the "number" field takes priority over the "issue" field (see https://github.com/JabRef/jabref/issues/8372#issuecomment-1023768144). [Keyword: IS RENDERING BOTH VIABLE?]. Ideally, they would coexist: "Roughly speaking number subdivides volume and issue is much closer to subdividing year. I don't think I would want to say that issue is subordinate to number or vice versa. They sort of operate on a similar level." (Source: https://github.com/plk/biblatex/issues/726#issuecomment-1010264258)
                Arguments.of(
                        "Foo, B. (n.d.). issue + number + pages. Bib(La)TeX Journal, (3number), 45–67.\n",
                        BibDatabaseMode.BIBLATEX,
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Foo, Bar")
                                .withField(StandardField.JOURNALTITLE, "Bib(La)TeX Journal")
                                .withField(StandardField.TITLE, "issue + number + pages")
                                .withField(StandardField.ISSUE, "9issue")
                                .withField(StandardField.NUMBER, "3number")
                                .withField(StandardField.PAGES, "45--67"),
                        "apa.csl")
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCslMapping(String expected, BibDatabaseMode mode, BibEntry entry, String cslFileName) throws Exception {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        bibDatabaseContext.setMode(mode);

        String citation = CitationStyleGenerator.generateCitation(
                entry,
                CitationStyle.createCitationStyleFromFile(cslFileName).orElseThrow().getSource(),
                CitationStyleOutputFormat.TEXT,
                bibDatabaseContext,
                bibEntryTypesManager);
        assertEquals(expected, citation);
    }
}
