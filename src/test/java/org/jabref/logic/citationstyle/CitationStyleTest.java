package org.jabref.logic.citationstyle;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.util.TestEntry;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CitationStyleTest {

    @Test
    void getDefault() {
        assertNotNull(CitationStyle.getDefault());
    }

    @Test
    void defaultCitation() {
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(TestEntry.getTestEntry())));
        context.setMode(BibDatabaseMode.BIBLATEX);
        String citation = CitationStyleGenerator.generateCitation(List.of(TestEntry.getTestEntry()), CitationStyle.getDefault().getSource(), CitationStyleOutputFormat.HTML, context, new BibEntryTypesManager()).getFirst();

        // if the default citation style changes this has to be modified
        String expected = """
                  <div class="csl-entry">
                    <div class="csl-left-margin">[1]</div><div class="csl-right-inline">B. Smith, B. Jones, and J. Williams, &ldquo;Title of the test entry,&rdquo; <span style="font-style: italic">BibTeX Journal</span>, vol. 34, no. 3, pp. 45&ndash;67, Jul. 2016, doi: 10.1001/bla.blubb.</div>
                  </div>
                """;

        assertEquals(expected, citation);
    }

    @Test
    void discoverCitationStylesNotNull() {
        List<CitationStyle> styleList = CitationStyle.discoverCitationStyles();
        assertNotNull(styleList);
    }

    @ParameterizedTest
    @MethodSource
    void citationStylePresent(String cslFileName) {
        Optional<CitationStyle> citationStyle = CitationStyle.createCitationStyleFromFile(cslFileName);
        assertTrue(citationStyle.isPresent());
    }

    static Stream<Arguments> citationStylePresent() {
        return java.util.stream.Stream.of(
                Arguments.of("ieee.csl"),
                Arguments.of("apa.csl"),
                Arguments.of("vancouver.csl"),
                Arguments.of("chicago-author-date.csl"),
                Arguments.of("nature.csl")
        );
    }

    @ParameterizedTest
    @MethodSource
    void titleMatches(String cslFileName, String expectedTitle) {
        Optional<CitationStyle> citationStyle = CitationStyle.createCitationStyleFromFile(cslFileName);
        CitationStyle.StyleInfo styleInfo = new CitationStyle.StyleInfo(citationStyle.get().getTitle(), citationStyle.get().isNumericStyle());
        assertEquals(expectedTitle, styleInfo.title());
    }

    static Stream<Arguments> titleMatches() {
        return Stream.of(
                Arguments.of("ieee.csl", "IEEE"),
                Arguments.of("apa.csl", "American Psychological Association 7th edition"),
                Arguments.of("vancouver.csl", "Vancouver"),
                Arguments.of("chicago-author-date.csl", "Chicago Manual of Style 17th edition (author-date)"),
                Arguments.of("nature.csl", "Nature")
        );
    }

    @ParameterizedTest
    @MethodSource
    void numericPropertyMatches(String cslFileName, boolean expectedNumericNature) {
        Optional<CitationStyle> citationStyle = CitationStyle.createCitationStyleFromFile(cslFileName);
        CitationStyle.StyleInfo styleInfo = new CitationStyle.StyleInfo(citationStyle.get().getTitle(), citationStyle.get().isNumericStyle());
        assertEquals(expectedNumericNature, styleInfo.isNumericStyle());
    }

    private static Stream<Arguments> numericPropertyMatches() {
        return java.util.stream.Stream.of(
                Arguments.of("ieee.csl", true),
                Arguments.of("apa.csl", false),
                Arguments.of("vancouver.csl", true),
                Arguments.of("chicago-author-date.csl", false),
                Arguments.of("nature.csl", true)
        );
    }
}
