package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.openoffice.ootext.OOText;

import de.undercouch.citeproc.output.Citation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.openoffice.oocsltext.CSLFormatUtils.generateAlphanumericCitation;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CSLFormatUtilsTest {

    private static final List<CitationStyle> STYLE_LIST = CitationStyle.discoverCitationStyles();

    private final BibEntry testEntry = TestEntry.getTestEntry();
    private final BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
    private final BibEntryTypesManager bibEntryTypesManager = new BibEntryTypesManager();

    /**
     * Test to check transformation of raw, unsupported HTML into OO-ready HTML.
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromRawHTML(String expected, String rawHtml) {
        String actual = CSLFormatUtils.transformHTML(rawHtml);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> ooHTMLTransformFromRawHTML() {
        return Stream.of(

                // region: general test cases for unescaping HTML entities

                // Ampersand (&amp entity)
                Arguments.of(
                        "Smith & Jones",
                        "Smith &amp; Jones"
                ),

                // Non-breaking space (&nbsp; entity)
                Arguments.of(
                        "Text with non-breaking spaces",
                        "Text with&nbsp;non-breaking&nbsp;spaces"
                ),

                // Bold formatting, less than, greater than symbols (&lt, &gt entities)
                Arguments.of(
                        "<b>Bold Text</b>",
                        "&lt;b&gt;Bold Text&lt;/b&gt;"
                ),

                // endregion

                // Handling margins
                Arguments.of(
                        "[1] Citation text",
                        "<div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">Citation text</div>"
                ),

                // Removing unsupported div tags
                Arguments.of(
                        "Aligned text",
                        "<div style=\"text-align:left;\">Aligned text</div>"
                ),

                // Removing unsupported links
                Arguments.of(
                        "Text with link",
                        "Text with <a href=\"http://example.com\">link</a>"
                ),

                // Replacing span tags with inline styles for bold
                Arguments.of(
                        "Text with <b>bold</b>",
                        "Text with <span style=\"font-weight:bold;\">bold</span>"
                ),

                // Replacing span tags with inline styles for italic
                Arguments.of(
                        "Text with <i>italic</i>",
                        "Text with <span style=\"font-style:italic;\">italic</span>"
                ),

                // Replacing span tags with inline styles for underline
                Arguments.of(
                        "Text with <u>underline</u>",
                        "Text with <span style=\"text-decoration:underline;\">underline</span>"
                ),

                // Replacing span tags with inline styles for small-caps
                Arguments.of(
                        "Text with <smallcaps>small caps</smallcaps>",
                        "Text with <span style=\"font-variant:small-caps;\">small caps</span>"
                ),

                // Test case for cleaning up remaining span tags
                Arguments.of(
                        "Text with unnecessary span",
                        "Text with <span>unnecessary span</span>"
                ),

                // Test case combining multiple transformations
                Arguments.of(
                        "[1] <b>Author</b>, \"Title,\" <i>Journal</i>, vol. 1, no. 1, pp. 1-10, 2023.",
                        "<div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\"><span style=\"font-weight:bold;\">Author</span>, &quot;Title,&quot; <span style=\"font-style:italic;\">Journal</span>, vol. 1, no. 1, pp. 1-10, 2023.</div>"
                ),

                // Comprehensive test
                Arguments.of(
                        "[1] <b>Smith & Jones</b>, " +
                                "\"<i>Analysis of <code> in HTML</i>,\" " +
                                "<smallcaps>Journal of Web Development</smallcaps>, " +
                                "vol. 1, no. 1, pp. 1-10, 2023. " +
                                "https://doi.org/10.1000/example",

                        "<div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">" +
                                "<span style=\"font-weight:bold;\">Smith&nbsp;&amp;&nbsp;Jones</span>, " +
                                "&quot;<span style=\"font-style:italic;\">Analysis of &lt;code&gt; in HTML</span>,&quot; " +
                                "<span style=\"font-variant:small-caps;\">Journal of Web&nbsp;Development</span>, " +
                                "vol. 1, no. 1, pp. 1-10, 2023. " +
                                "<a href=\"https://doi.org/10.1000/example\">https://doi.org/10.1000/example</a></div>"
                )
        );
    }

    /**
     * Test to check correct transformation of raw CSL bibliography generated by citeproc-java methods into OO-ready text.
     * <p>
     * <b>Precondition:</b> This test assumes that {@link CitationStyleGenerator#generateCitation(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateCitation} works as expected.
     * </p>
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromRawBibliography(String expected, CitationStyle style) {
        String citation = CitationStyleGenerator.generateCitation(List.of(testEntry), style.getSource(), CSLFormatUtils.OUTPUT_FORMAT, context, bibEntryTypesManager).getFirst();
        String actual = CSLFormatUtils.transformHTML(citation);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> ooHTMLTransformFromRawBibliography() {
        return Stream.of(

                // Non-numeric, parentheses, commas, full stops, slashes, hyphens, colons, italics
                Arguments.of(
                        "  Smith, B., Jones, B., & Williams, J. (2016). Title of the test entry. <i>BibTeX Journal</i>, <i>34</i>(3), 45–67. https://doi.org/10.1001/bla.blubb\n",
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().get()
                ),

                // Numeric type "[1]", brackets, newlines
                Arguments.of(
                        "  \n" +
                                "    [1] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016, doi: 10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().get()
                ),

                // Numeric type "1."
                Arguments.of(
                        "  \n" +
                                "    1. Smith, B., Jones, B., Williams, J.: Title of the test entry. BibTeX Journal. 34, 45–67 (2016). https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "  Smith, Bill, Bob Jones, and Jeff Williams. 2016. “Title of the Test Entry.” Edited by Phil Taylor. <i>BibTeX Journal</i> 34 (3): 45–67. https://doi.org/10.1001/bla.blubb.\n",
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().get()
                ),

                // Semicolons
                Arguments.of(
                        "  \n" +
                                "    1. Smith B, Jones B, Williams J. Title of the test entry. Taylor P, editor. BibTeX Journal [Internet]. 2016 Jul;34(3):45–67. Available from: https://github.com/JabRef\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "  \n" +
                                "    1. Smith, B., Jones, B. & Williams, J. Title of the test entry. <i>BibTeX Journal</i> <b>34</b>, 45–67 (2016).\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "  \n" +
                                "    1. Smith B, Jones B, Williams J. Title of the test entry. Taylor P, ed. <i>BibTeX Journal</i>. 2016;34(3):45-67. doi:10.1001/bla.blubb\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "American Medical Association 11th edition".equals(e.getTitle())).findAny().get()
                ),

                // Small-caps
                Arguments.of(
                        "  <smallcaps>Smith</smallcaps>, <smallcaps>B.</smallcaps>, <smallcaps>Jones</smallcaps>, <smallcaps>B.</smallcaps>, <smallcaps>Williams</smallcaps>, <smallcaps>J.</smallcaps> (2016) Title of the test entry <smallcaps>Taylor</smallcaps>, <smallcaps>P.</smallcaps> (ed.). <i>BibTeX Journal</i>, 34(3), pp. 45–67.\n",
                        STYLE_LIST.stream().filter(e -> "De Montfort University - Harvard".equals(e.getTitle())).findAny().get()
                ),

                // Underlines
                Arguments.of(
                        "  Smith, Bill, Bob Jones, and Jeff Williams. “Title of the test entry.” Ed. Phil Taylor. <u>BibTeX Journal</u> 34.3 (2016): 45–67. <https://github.com/JabRef>.\n",
                        STYLE_LIST.stream().filter(e -> "Modern Language Association 7th edition (underline)".equals(e.getTitle())).findAny().get()
                ),

                // Non-breaking spaces
                Arguments.of(
                        "  Smith, Bill, Bob Jones, & Jeff Williams, “Title of the test entry,” <i>BibTeX Journal</i>, 2016, vol. 34, no. 3, pp. 45–67.\n",
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().get()
                ),

                // Numeric with a full stop - "1."
                Arguments.of(
                        "  \n" +
                                "    1. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().get()
                ),

                // Bold text, bold numeric with a full stop - "<BOLD>1."
                Arguments.of(
                        "  \n" +
                                "    <b>1</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().get()
                ),

                // Naked numeric - "1"
                Arguments.of(
                        "  \n" +
                                "    1 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().get()
                ),

                // Numeric in parentheses - "(1)"
                Arguments.of(
                        "  \n" +
                                "    (1) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().get()
                ),

                // Numeric with right parenthesis - "1)"
                Arguments.of(
                        "  \n" +
                                "    1) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                // Numeric in superscript - "<SUPERSCRIPT>1"
                Arguments.of(
                        "  <sup>1</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).\n",
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Test to check correct transformation of raw CSL citation with a single entry generated by citeproc-java methods into OO-ready text.
     * <p>
     * <b>Precondition:</b> This test assumes that {@link CitationStyleGenerator#generateInText(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateInText} works as expected.
     * </p>
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromCitationWithSingleEntry(String expected, CitationStyle style) throws IOException {
        Citation citation = CitationStyleGenerator.generateInText(List.of(testEntry), style.getSource(), CSLFormatUtils.OUTPUT_FORMAT, context, bibEntryTypesManager);
        String inTextCitationText = citation.getText();
        String actual = CSLFormatUtils.transformHTML(inTextCitationText);
        OOText ooText = OOText.fromString(actual);
        assertEquals(OOText.fromString(expected), ooText);
    }

    static Stream<Arguments> ooHTMLTransformFromCitationWithSingleEntry() {
        return Stream.of(

                Arguments.of(
                        "(Smith et al., 2016)",
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "[1]",
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "[1]",
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Smith, Jones, and Williams 2016)",
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(1)",
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1</sup>",
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1</sup>",
                        STYLE_LIST.stream().filter(e -> "American Medical Association 11th edition".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Smith, Jones, Williams, 2016)",
                        STYLE_LIST.stream().filter(e -> "De Montfort University - Harvard".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Smith, Jones, & Williams)",
                        STYLE_LIST.stream().filter(e -> "Modern Language Association 7th edition (underline)".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "Smith, B., B. Jones, and J. Williams, 2016.",
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "[1]",
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(<i>1</i>)",
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1</sup>",
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1</sup>",
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().get()
                ),

                // Note: not sure if the right parenthesis outside the superscript is correct, but that's how citeproc-java generates it in raw form as well.
                Arguments.of(
                        "<sup>1</sup>)",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1</sup>",
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Test to check correct transformation of raw CSL citations with multiple entries generated by citeproc-java methods into OO-ready text.
     * <p>
     * <b>Precondition:</b> This test assumes that {@link CitationStyleGenerator#generateInText(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateInText} works as expected.
     * </p>
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromCitationWithMultipleEntries(String expected, CitationStyle style) throws IOException {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria and Lee, David")
                .withField(StandardField.JOURNAL, "International Review of Physics")
                .withField(StandardField.NUMBER, "6")
                .withField(StandardField.PAGES, "789--810")
                .withField(StandardField.TITLE, "Quantum Entanglement in Superconductors")
                .withField(StandardField.VOLUME, "28")
                .withField(StandardField.ISSUE, "3")
                .withField(StandardField.YEAR, "2021")
                .withCitationKey("Garcia_2021");

        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily")
                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "101--120")
                .withField(StandardField.TITLE, "A Study on Machine Learning Algorithms")
                .withField(StandardField.VOLUME, "15")
                .withField(StandardField.ISSUE, "2")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Smith_2020");

        List<BibEntry> entries = List.of(entry1, entry2);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
        context.setMode(BibDatabaseMode.BIBLATEX);
        Citation citation = CitationStyleGenerator.generateInText(entries, style.getSource(), CSLFormatUtils.OUTPUT_FORMAT, context, bibEntryTypesManager);
        String inTextCitationText = citation.getText();
        String actual = CSLFormatUtils.transformHTML(inTextCitationText);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> ooHTMLTransformFromCitationWithMultipleEntries() {
        return Stream.of(
                Arguments.of(
                        "(Garcia & Lee, 2021; Smith & Johnson, 2020)",
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "[1], [2]",
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "[1, 2]",
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Garcia and Lee 2021; Smith and Johnson 2020)",
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(1,2)",
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>",
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>",
                        STYLE_LIST.stream().filter(e -> "American Medical Association 11th edition".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Garcia, Lee, 2021; Smith, Johnson, 2020)",
                        STYLE_LIST.stream().filter(e -> "De Montfort University - Harvard".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Garcia & Lee; Smith & Johnson)",
                        STYLE_LIST.stream().filter(e -> "Modern Language Association 7th edition (underline)".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "Garcia, M. and D. Lee, 2021 ; Smith, J. and E. Johnson, 2020.",
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "[1, 2]",
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(<i>1,2</i>)",
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>",
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>",
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>)",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>",
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Test for modifying the number (index) of a numeric citation.
     * The numeric index should change to the provided "current number".
     * The rest of the citation should stay as it is (other numbers in the body shouldn't be affected).
     * <p>
     * <b>Precondition 1:</b> This test assumes that {@link CitationStyleGenerator#generateCitation(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateCitation} works as expected.<br>
     * <b>Precondition 2:</b> This test assumes that the method {@link CSLFormatUtils#transformHTML(String) transformHTML} works as expected.<br>
     * <b>Precondition 3:</b> Run this test ONLY on numeric Citation Styles.</p>
     */
    @ParameterizedTest
    @MethodSource
    void updateSingleNumericCitation(String expected, CitationStyle style) {
        String citation = CitationStyleGenerator.generateCitation(List.of(testEntry), style.getSource(), CSLFormatUtils.OUTPUT_FORMAT, context, bibEntryTypesManager).getFirst();
        String transformedCitation = CSLFormatUtils.transformHTML(citation);
        String actual = CSLFormatUtils.updateSingleBibliographyNumber(transformedCitation, 3);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> updateSingleNumericCitation() {
        return Stream.of(

                // Type: "[1]"
                Arguments.of(
                        "  \n" +
                                "    [3] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016, doi: 10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "IEEE".equals(e.getTitle())).findAny().get()
                ),

                // Type: "1."
                Arguments.of(
                        "  \n" +
                                "    3. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().get()
                ),

                // Type:"<BOLD>1."
                Arguments.of(
                        "  \n" +
                                "    <b>3</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().get()
                ),

                // Type: "1"
                Arguments.of(
                        "  \n" +
                                "    3 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().get()
                ),

                // Type: "(1)"
                Arguments.of(
                        "  \n" +
                                "    (3) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "American Chemical Society".equals(e.getTitle())).findAny().get()
                ),

                // Type: "1)"
                Arguments.of(
                        "  \n" +
                                "    3) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).\n" +
                                "  \n",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                // Type: "<SUPERSCRIPT>1"
                Arguments.of(
                        "  <sup>3</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).\n",
                        STYLE_LIST.stream().filter(e -> "American Institute of Physics 4th edition".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Tests if a citation (LaTeX "\cite") is converted into an in-text citation (LaTeX "\citet") as expected.
     */
    @ParameterizedTest
    @MethodSource
    void ChangeToInText(String expected, String input) {
        String actual = CSLFormatUtils.changeToInText(input);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> ChangeToInText() {
        return Stream.of(

                // APA Style
                Arguments.of("Smith (2020)", "(Smith, 2020)"),
                Arguments.of("Johnson & Brown (2018)", "(Johnson & Brown, 2018)"),
                Arguments.of("Williams et al. (2019)", "(Williams et al., 2019)"),

                // MLA Style
                Arguments.of("(Smith 20)", "(Smith 20)"),
                Arguments.of("(Johnson and Brown 18)", "(Johnson and Brown 18)"),
                Arguments.of("(Williams et al. 19)", "(Williams et al. 19)"),

                // Chicago Style (Author-Date)
                Arguments.of("(Smith 2020)", "(Smith 2020)"),
                Arguments.of("(Johnson and Brown 2018)", "(Johnson and Brown 2018)"),
                Arguments.of("(Williams et al. 2019)", "(Williams et al. 2019)"),

                // Harvard Style
                Arguments.of("Smith (2020)", "(Smith, 2020)"),
                Arguments.of("Johnson and Brown (2018)", "(Johnson and Brown, 2018)"),
                Arguments.of("Williams et al. (2019)", "(Williams et al., 2019)"),

                // IEEE Style
                Arguments.of("[1]", "[1]"),
                Arguments.of("[2], [3]", "[2], [3]"),

                // Vancouver Style
                Arguments.of("(1)", "(1)"),
                Arguments.of("(1,2)", "(1,2)"),

                // Nature Style
                Arguments.of("1", "1"),
                Arguments.of("1,2", "1,2")

        );
    }

    /**
     * Test for proper generation of alphanumeric citations (currently supported: DIN 1505-2).
     * <p>
     * <b>Precondition:</b> This test assumes that the method {@link org.jabref.logic.citationkeypattern.BracketedPattern#authorsAlpha authorsAlpha} works as expected.</p>
     */
    @ParameterizedTest
    @MethodSource
    void generateAlphanumericCitationTest(String expected, List<BibEntry> entries) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(entries));
        String actual = generateAlphanumericCitation(entries, context);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> generateAlphanumericCitationTest() {
        BibEntry entry1 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria")
                .withField(StandardField.TITLE, "Quantum Entanglement in Superconductors")
                .withField(StandardField.JOURNAL, "International Review of Physics")
                .withField(StandardField.VOLUME, "28")
                .withField(StandardField.NUMBER, "6")
                .withField(StandardField.PAGES, "789--810")
                .withField(StandardField.YEAR, "2021")
                .withCitationKey("Garcia_2021");

        BibEntry entry2 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily")
                .withField(StandardField.TITLE, "A Study on Machine Learning Algorithms")
                .withField(StandardField.JOURNAL, "Journal of Computer Science")
                .withField(StandardField.VOLUME, "15")
                .withField(StandardField.NUMBER, "4")
                .withField(StandardField.PAGES, "101--120")
                .withField(StandardField.YEAR, "2020")
                .withCitationKey("Smith_2020");

        BibEntry entry3 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Johnson, Emily; Williams, Jessica; Lee, David")
                .withField(StandardField.TITLE, "Trends in Artificial Intelligence")
                .withField(StandardField.JOURNAL, "AI Magazine")
                .withField(StandardField.VOLUME, "41")
                .withField(StandardField.NUMBER, "2")
                .withField(StandardField.PAGES, "45--60")
                .withField(StandardField.YEAR, "2019")
                .withCitationKey("Johnson_2019");

        BibEntry entry4 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Smith, John; Johnson, Emily; Lee, David; Williams, Jessica")
                .withField(StandardField.TITLE, "Big Data Analytics in Healthcare")
                .withField(StandardField.JOURNAL, "Journal of Medical Informatics")
                .withField(StandardField.VOLUME, "23")
                .withField(StandardField.NUMBER, "1")
                .withField(StandardField.PAGES, "11--25")
                .withField(StandardField.YEAR, "2018")
                .withCitationKey("Smith_2018");

        BibEntry entry5 = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Garcia, Maria; Smith, John; Johnson, Emily; Lee, David; Williams, Jessica")
                .withField(StandardField.TITLE, "Advances in Renewable Energy Technologies")
                .withField(StandardField.JOURNAL, "Energy Policy")
                .withField(StandardField.VOLUME, "52")
                .withField(StandardField.NUMBER, "3")
                .withField(StandardField.PAGES, "120--135")
                .withField(StandardField.YEAR, "2017")
                .withCitationKey("Garcia_2017");

        return Stream.of(

                // Entry with single author
                Arguments.of("[Ga21]", List.of(entry1)),

                // Entry with two authors
                Arguments.of("[SJ20]", List.of(entry2)),

                // Entry with three authors
                Arguments.of("[JWL19]", List.of(entry3)),

                // Entry with four authors
                Arguments.of("[SJLW18]", List.of(entry4)),

                // Entry with five authors
                Arguments.of("[GSJL17]", List.of(entry5)),

                // Multiple entries with varying number of authors
                Arguments.of("[Ga21; SJ20; JWL19; SJLW18; GSJL17]", List.of(entry1, entry2, entry3, entry4, entry5))

        );
    }
}
