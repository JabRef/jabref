package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CSLStyleLoader;
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

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.logic.openoffice.oocsltext.CSLFormatUtils.generateAlphanumericCitation;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.SAME_THREAD)
class CSLFormatUtilsTest {

    private static final BibEntry TEST_ENTRY = TestEntry.getTestEntry();
    private static final BibDatabaseContext TEST_ENTRY_CONTEXT = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
    private static final BibEntryTypesManager ENTRY_TYPES_MANAGER = new BibEntryTypesManager();

    private static final CitationStyleOutputFormat HTML_OUTPUT_FORMAT = CitationStyleOutputFormat.HTML;

    private static final List<CitationStyle> STYLE_LIST = CSLStyleLoader.getInternalStyles();

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
     *
     * @implSpec Assumes that {@link CitationStyleGenerator#generateBibliography(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateBibliography} works as expected.
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromRawBibliography(String expected, CitationStyle style) {
        String citation = CitationStyleGenerator.generateBibliography(List.of(TEST_ENTRY), style.getSource(), HTML_OUTPUT_FORMAT, TEST_ENTRY_CONTEXT, ENTRY_TYPES_MANAGER).getFirst();
        String actual = CSLFormatUtils.transformHTML(citation);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> ooHTMLTransformFromRawBibliography() {
        return Stream.of(

                // Non-numeric, parentheses, commas, full stops, slashes, hyphens, colons, italics
                Arguments.of(
                        "Smith, B., Jones, B., & Williams, J. (2016). Title of the test entry. <i>BibTeX Journal</i>, <i>34</i>(3), 45–67. https://doi.org/10.1001/bla.blubb<p></p>",
                        STYLE_LIST.stream().filter(e -> "American Psychological Association 7th edition".equals(e.getTitle())).findAny().get()
                ),

                // Numeric type "[1]", brackets, newlines
                Arguments.of(
                        "[1] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, July 2016, doi: 10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "IEEE Reference Guide version 11.29.2023".equals(e.getTitle())).findAny().get()
                ),

                // Numeric type "1."
                Arguments.of(
                        "1. Smith, B., Jones, B., Williams, J.: Title of the test entry. BibTeX Journal. 34, 45–67 (2016). https://doi.org/10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Springer - Lecture Notes in Computer Science".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "Smith, Bill, Bob Jones, and Jeff Williams. 2016. “Title of the Test Entry.” <i>BibTeX Journal</i> 34 (3): 45–67. https://doi.org/10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Chicago Manual of Style 17th edition (author-date)".equals(e.getTitle())).findAny().get()
                ),

                // Semicolons
                Arguments.of(
                        "1. Smith B, Jones B, Williams J. Title of the test entry. Taylor P, editor. BibTeX Journal [Internet]. 2016 July;34(3):45–67. Available from: https://github.com/JabRef<p></p>",
                        STYLE_LIST.stream().filter(e -> "Vancouver".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "1. Smith, B., Jones, B. & Williams, J. Title of the test entry. <i>BibTeX Journal</i> <b>34</b>, 45–67 (2016).<p></p>",
                        STYLE_LIST.stream().filter(e -> "Nature".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "1. Smith B, Jones B, Williams J. Title of the test entry. Taylor P, ed. <i>BibTeX Journal</i>. 2016;34(3):45-67. doi:10.1001/bla.blubb<p></p>",
                        STYLE_LIST.stream().filter(e -> "AMA Manual of Style 11th edition".equals(e.getTitle())).findAny().get()
                ),

                // Small-caps
                Arguments.of(
                        "<smallcaps>Smith</smallcaps>, <smallcaps>B.</smallcaps>, <smallcaps>Jones</smallcaps>, <smallcaps>B.</smallcaps> and <smallcaps>Williams</smallcaps>, <smallcaps>J.</smallcaps> (2016) Title of the test entry <smallcaps>Taylor</smallcaps>, <smallcaps>P.</smallcaps> (ed.). <i>BibTeX Journal</i>, 34(3), pp. 45–67.<p></p>",
                        STYLE_LIST.stream().filter(e -> "De Montfort University (author-date/Harvard)".equals(e.getTitle())).findAny().get()
                ),

                // Non-breaking spaces
                Arguments.of(
                        "Smith, Bill, Bob Jones, & Jeff Williams, “Title of the test entry,” <i>BibTeX Journal</i>, 2016, vol. 34, no. 3, pp. 45–67.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Histoire & Mesure (Français)".equals(e.getTitle())).findAny().get()
                ),

                // Numeric with a full stop - "1."
                Arguments.of(
                        "1. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.<p></p>",
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().get()
                ),

                // Bold text, bold numeric with a full stop - "<BOLD>1."
                Arguments.of(
                        "<b>1</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().get()
                ),

                // Naked numeric - "1"
                Arguments.of(
                        "1 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().get()
                ),

                // Numeric in parentheses - "(1)"
                Arguments.of(
                        "(1) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "ACS Guide 2022 revision".equals(e.getTitle())).findAny().get()
                ),

                // Numeric with right parenthesis - "1)"
                Arguments.of(
                        "1) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).<p></p>",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                // Numeric in superscript - "<SUPERSCRIPT>1"
                Arguments.of(
                        "<sup>1</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).<p></p>",
                        STYLE_LIST.stream().filter(e -> "AIP Style Manual 4th edition".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Test to check correct transformation of raw CSL citation with a single entry generated by citeproc-java methods into OO-ready text.
     *
     * @implSpec Assumes that {@link CitationStyleGenerator#generateCitation(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateCitation} works as expected.
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromCitationWithSingleEntry(String expected, CitationStyle style) {
        String citation = CitationStyleGenerator.generateCitation(List.of(TEST_ENTRY), style.getSource(), HTML_OUTPUT_FORMAT, TEST_ENTRY_CONTEXT, ENTRY_TYPES_MANAGER);
        String actual = CSLFormatUtils.transformHTML(citation);
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
                        STYLE_LIST.stream().filter(e -> "IEEE Reference Guide version 11.29.2023".equals(e.getTitle())).findAny().get()
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
                        STYLE_LIST.stream().filter(e -> "AMA Manual of Style 11th edition".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Smith, Jones and Williams, 2016)",
                        STYLE_LIST.stream().filter(e -> "De Montfort University (author-date/Harvard)".equals(e.getTitle())).findAny().get()
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
                        STYLE_LIST.stream().filter(e -> "ACS Guide 2022 revision".equals(e.getTitle())).findAny().get()
                ),

                // Note: not sure if the right parenthesis outside the superscript is correct, but that's how citeproc-java generates it in raw form as well.
                Arguments.of(
                        "<sup>1</sup>)",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1</sup>",
                        STYLE_LIST.stream().filter(e -> "AIP Style Manual 4th edition".equals(e.getTitle())).findAny().get()
                ),

                // Non-bibliographic style (citation-format="note")
                Arguments.of(
                        "B. Smith, B. Jones and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i> 34, no. 3 (July 2016): 45–67.",
                        STYLE_LIST.stream().filter(e -> "The Journal of Clinical Ethics".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Test to check correct transformation of raw CSL citations with multiple entries generated by citeproc-java methods into OO-ready text.
     *
     * @implSpec Assumes that {@link CitationStyleGenerator#generateCitation(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateCitation} works as expected.
     */
    @ParameterizedTest
    @MethodSource
    void ooHTMLTransformFromCitationWithMultipleEntries(String expected, CitationStyle style) {
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
        String citation = CitationStyleGenerator.generateCitation(entries, style.getSource(), HTML_OUTPUT_FORMAT, context, ENTRY_TYPES_MANAGER);
        String actual = CSLFormatUtils.transformHTML(citation);
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
                        STYLE_LIST.stream().filter(e -> "IEEE Reference Guide version 11.29.2023".equals(e.getTitle())).findAny().get()
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
                        STYLE_LIST.stream().filter(e -> "AMA Manual of Style 11th edition".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "(Garcia and Lee, 2021; Smith and Johnson, 2020)",
                        STYLE_LIST.stream().filter(e -> "De Montfort University (author-date/Harvard)".equals(e.getTitle())).findAny().get()
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
                        STYLE_LIST.stream().filter(e -> "ACS Guide 2022 revision".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>)",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                Arguments.of(
                        "<sup>1,2</sup>",
                        STYLE_LIST.stream().filter(e -> "AIP Style Manual 4th edition".equals(e.getTitle())).findAny().get()
                ),

                // Non-bibliographic style (citation-format="note")
                Arguments.of(
                        "M. Garcia and D. Lee, “Quantum Entanglement in Superconductors,” <i>International Review of Physics</i> 28, no. 6 (2021): 789–810; J. Smith and E. Johnson, “A Study on Machine Learning Algorithms,” <i>Journal of Computer Science</i> 15, no. 4 (2020): 101–20.",
                        STYLE_LIST.stream().filter(e -> "The Journal of Clinical Ethics".equals(e.getTitle())).findAny().get()
                )
        );
    }

    /**
     * Test for modifying the number (index) of a numeric citation.
     * The numeric index should change to the provided "current number".
     * The rest of the citation should stay as it is (other numbers in the body shouldn't be affected).
     *
     * @implSpec <ol>
     * <li>Assumes that {@link CitationStyleGenerator#generateBibliography(List, String, CitationStyleOutputFormat, BibDatabaseContext, BibEntryTypesManager) generateBibliography} works as expected.</li>
     * <li>Assumes that the method {@link CSLFormatUtils#transformHTML(String) transformHTML} works as expected.</li>
     * <li>Run this test ONLY on numeric Citation Styles.</li>
     * </ol>
     */
    @ParameterizedTest
    @MethodSource
    void updateSingleNumericBibliography(String expected, CitationStyle style) {
        String citation = CitationStyleGenerator.generateBibliography(List.of(TEST_ENTRY), style.getSource(), HTML_OUTPUT_FORMAT, TEST_ENTRY_CONTEXT, ENTRY_TYPES_MANAGER).getFirst();
        String transformedCitation = CSLFormatUtils.transformHTML(citation);
        String actual = CSLFormatUtils.updateSingleBibliographyNumber(transformedCitation, 3);
        assertEquals(expected, actual);
    }

    static Stream<Arguments> updateSingleNumericBibliography() {
        return Stream.of(

                // Type: "[1]"
                Arguments.of(
                        "[3] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016, doi: 10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "IEEE Reference Guide version 11.29.2023".equals(e.getTitle())).findAny().get()
                ),

                // Type: "1."
                Arguments.of(
                        "3. Smith, B., Jones, B. and Williams, J. 2016. Title of the test entry. <i>BibTeX Journal</i>. <b>34</b>: 45–67.<p></p>",
                        STYLE_LIST.stream().filter(e -> "The Journal of Veterinary Medical Science".equals(e.getTitle())).findAny().get()
                ),

                // Type: "<BOLD>1."
                Arguments.of(
                        "<b>3</b>. <b>Smith  B, Jones  B, Williams  J</b>. Title of the test entry. <i>BibTeX Journal</i> 2016 ; 34 : 45–67.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Acta Orthopædica Belgica".equals(e.getTitle())).findAny().get()
                ),

                // Type: "1"
                Arguments.of(
                        "3 Smith Bill, Jones Bob, Williams Jeff. Title of the test entry. <i>BibTeX Journal</i> 2016;<b>34</b>(3):45–67. Doi: 10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "Acta Anaesthesiologica Taiwanica".equals(e.getTitle())).findAny().get()
                ),

                // Type: "(1)"
                Arguments.of(
                        "(3) Smith, B.; Jones, B.; Williams, J. Title of the Test Entry. <i>BibTeX Journal</i> <b>2016</b>, <i>34</i> (3), 45–67. https://doi.org/10.1001/bla.blubb.<p></p>",
                        STYLE_LIST.stream().filter(e -> "ACS Guide 2022 revision".equals(e.getTitle())).findAny().get()
                ),

                // Type: "1)"
                Arguments.of(
                        "3) Smith B., Jones B., Williams J., <i>BibTeX Journal</i>, <b>34</b>, 45–67 (2016).<p></p>",
                        STYLE_LIST.stream().filter(e -> "Chemical and Pharmaceutical Bulletin".equals(e.getTitle())).findAny().get()
                ),

                // Type: "<SUPERSCRIPT>1"
                Arguments.of(
                        "<sup>3</sup> B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal <b>34</b>(3), 45–67 (2016).<p></p>",
                        STYLE_LIST.stream().filter(e -> "AIP Style Manual 4th edition".equals(e.getTitle())).findAny().get()
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
     *
     * @implSpec Assumes that the method {@link org.jabref.logic.citationkeypattern.BracketedPattern#authorsAlpha authorsAlpha} works as expected.</p>
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

    /**
     * Test for proper generation of author prefix for in-text citations
     */
    @ParameterizedTest
    @MethodSource
    void generateAuthorPrefixTest(String expected, BibEntry entry) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String actual = CSLFormatUtils.generateAuthorPrefix(entry, context);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> generateAuthorPrefixTest() {
        return Stream.of(
                // Single author
                Arguments.of(
                        "Garcia ",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Garcia, Maria")
                                .withField(StandardField.YEAR, "2021")
                ),

                // Two authors
                Arguments.of(
                        "Smith et al. ",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily")
                                .withField(StandardField.YEAR, "2020")
                ),

                // Three authors
                Arguments.of(
                        "Johnson et al. ",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Johnson, Emily and Williams, Jessica and Lee, David")
                                .withField(StandardField.YEAR, "2019")
                ),

                // Four or more authors
                Arguments.of(
                        "Smith et al. ",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily and Lee, David and Williams, Jessica")
                                .withField(StandardField.YEAR, "2018")
                ),

                // Four or more authors with first author beginning with "and"
                Arguments.of(
                        "Smith et al. ",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith and James, John and Johnson, Emily and Lee, David and Williams, Jessica")
                                .withField(StandardField.YEAR, "2018")
                ),

                // Missing author
                Arguments.of(
                        "",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.TITLE, "No Author Paper")
                                .withField(StandardField.YEAR, "2020")
                )
        );
    }

    /**
     * Test for proper generation of in-text citations for alphanumeric styles (currently supported: DIN 1505-2)
     */
    @ParameterizedTest
    @MethodSource
    void generateAlphanumericInTextCitationTest(String expected, BibEntry entry) {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        String actual = CSLFormatUtils.generateAlphanumericInTextCitation(entry, context);
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> generateAlphanumericInTextCitationTest() {
        return Stream.of(
                // Single author
                Arguments.of(
                        "Garcia [Ga21]",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Garcia, Maria")
                                .withField(StandardField.YEAR, "2021")
                ),

                // Two authors
                Arguments.of(
                        "Smith et al. [SJ20]",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily")
                                .withField(StandardField.YEAR, "2020")
                ),

                // Three authors
                Arguments.of(
                        "Johnson et al. [JWL19]",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Johnson, Emily and Williams, Jessica and Lee, David")
                                .withField(StandardField.YEAR, "2019")
                ),

                // Four or more authors
                Arguments.of(
                        "Smith et al. [SJLW18]",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith, John and Johnson, Emily and Lee, David and Williams, Jessica")
                                .withField(StandardField.YEAR, "2018")
                ),

                // Four or more authors, with first author containing "and"
                Arguments.of(
                        "Smith et al. [SJJL18]",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.AUTHOR, "Smith and James, John and Johnson, Emily and Lee, David and Williams, Jessica")
                                .withField(StandardField.YEAR, "2018")
                ),

                // Missing author (should fall back to citation key)
                Arguments.of(
                        " [missing_key]",
                        new BibEntry(StandardEntryType.Article)
                                .withField(StandardField.TITLE, "No Author Paper")
                                .withField(StandardField.YEAR, "2020")
                                .withCitationKey("missing_key")
                )
        );
    }
}
