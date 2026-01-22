package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.SAME_THREAD)
class CSLStyleUtilsTest {

    // internal styles
    private static final String APA = "apa.csl";
    private static final String IEEE = "ieee.csl";
    private static final String VANCOUVER = "vancouver.csl";
    private static final String CHICAGO_AUTHOR_DATE = "chicago-author-date.csl";
    private static final String NATURE = "nature.csl";
    private static final String MLA = "modern-language-association.csl";
    private static final String JOURNAL_OF_CLINICAL_ETHICS = "the-journal-of-clinical-ethics.csl";

    // external styles
    private static final String MODIFIED_IEEE = "ieee-bold-author.csl";
    private static final String MODIFIED_APA = "modified-apa.csl";
    private static final String LITERATURA = "literatura.csl";

    @ParameterizedTest
    @ValueSource(strings = {
            "ieee.csl",
            "apa.csl",
            "harvard.csl",
            "vancouver.csl",
            "ieee.modified.csl",
            "apa.v7.csl",
            "/path/to/style/nature.csl",
            "C:\\Users\\username\\Documents\\styles\\chicago.csl"
    })
    void acceptsCslExtension(String filename) {
        assertTrue(CSLStyleUtils.isCitationStyleFile(filename));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ieee.txt",
            "apa.xml",
            "harvard.css",
            "vancouver",
            "nature.",
            "",
            "chicago.CSL" // case sensitivity - should reject
    })
    void rejectsNonCslExtension(String filename) {
        assertFalse(CSLStyleUtils.isCitationStyleFile(filename));
    }

    @ParameterizedTest
    @MethodSource("styleTestData")
    void parseStyleInfo(String styleName, String expectedTitle, String expectedShortTitle, boolean expectedNumericNature, boolean expectedBibliographicNature, boolean expectedUsesHangingIndent) throws IOException {
        String styleContent;
        try (InputStream inputStream = CSLStyleUtilsTest.class.getResourceAsStream(styleName)) {
            styleContent = new String(inputStream.readAllBytes());
        }

        Optional<CSLStyleUtils.StyleInfo> styleInfo = CSLStyleUtils.parseStyleInfo(styleName, styleContent);

        assertTrue(styleInfo.isPresent());
        assertEquals(expectedTitle, styleInfo.get().title());
        assertEquals(expectedShortTitle, styleInfo.get().shortTitle());
        assertEquals(expectedNumericNature, styleInfo.get().isNumericStyle());
        assertEquals(expectedBibliographicNature, styleInfo.get().hasBibliography());
        assertEquals(expectedUsesHangingIndent, styleInfo.get().usesHangingIndent());
    }

    @ParameterizedTest
    @MethodSource("styleTestData")
    void createCitationStyleFromFileReturnsValidCitationStyle(String styleName, String expectedTitle, String expectedShortTitle, boolean expectedNumericNature, boolean expectedBibliographicNature, boolean expectedUsesHangingIndent) {
        // use absolute path to test csl so that it is treated as external file
        Path resourcePath = Path.of("").toAbsolutePath()
                                .resolve("src/test/resources/org/jabref/logic/citationstyle")
                                .resolve(styleName);

        Optional<CitationStyle> citationStyle = CSLStyleUtils.createCitationStyleFromFile(resourcePath.toString());

        assertTrue(citationStyle.isPresent());
        assertEquals(expectedTitle, citationStyle.get().getTitle());
        assertEquals(expectedShortTitle, citationStyle.get().getShortTitle());
        assertEquals(expectedNumericNature, citationStyle.get().isNumericStyle());
        assertEquals(expectedBibliographicNature, citationStyle.get().hasBibliography());
        assertEquals(expectedUsesHangingIndent, citationStyle.get().usesHangingIndent());
        assertNotNull(citationStyle.get().getSource());
        assertFalse(citationStyle.get().isInternalStyle());
    }

    private static Stream<Arguments> styleTestData() {
        return Stream.of(
                Arguments.of(MODIFIED_IEEE, "IEEE - Bold Author", "", true, true, false),
                Arguments.of(MODIFIED_APA, "Modified American Psychological Association 7th edition", "APA", false, true, true),
                Arguments.of(LITERATURA, "Literatūra", "Literatūra", false, true, true) // Literatūra uses author-date format, so non-numeric
        );
    }

    @ParameterizedTest
    @MethodSource
    void internalCitationStylePresent(String cslFileName) {
        Optional<CitationStyle> citationStyle = CSLStyleUtils.createCitationStyleFromFile(cslFileName);
        assertTrue(citationStyle.isPresent());
    }

    static Stream<Arguments> internalCitationStylePresent() {
        return Stream.of(
                Arguments.of(IEEE),
                Arguments.of(APA),
                Arguments.of(VANCOUVER),
                Arguments.of(CHICAGO_AUTHOR_DATE),
                Arguments.of(NATURE),
                Arguments.of(MLA),
                Arguments.of(JOURNAL_OF_CLINICAL_ETHICS)
        );
    }

    @ParameterizedTest
    @MethodSource
    void titleMatches(String expectedTitle, String cslFileName) {
        CitationStyle citationStyle = CSLStyleUtils.createCitationStyleFromFile(cslFileName).get();
        assertEquals(expectedTitle, citationStyle.getTitle());
    }

    static Stream<Arguments> titleMatches() {
        return Stream.of(
                Arguments.of("IEEE Reference Guide version 11.29.2023", IEEE),
                Arguments.of("American Psychological Association 7th edition", APA),
                Arguments.of("Vancouver", VANCOUVER),
                Arguments.of("Chicago Manual of Style 18th edition (author-date)", CHICAGO_AUTHOR_DATE),
                Arguments.of("Nature", NATURE),
                Arguments.of("MLA Handbook 9th edition (in-text citations)", MLA),
                Arguments.of("The Journal of Clinical Ethics", JOURNAL_OF_CLINICAL_ETHICS)
        );
    }

    @ParameterizedTest
    @MethodSource
    void numericPropertyMatches(boolean expectedNumericNature, String cslFileName) {
        CitationStyle citationStyle = CSLStyleUtils.createCitationStyleFromFile(cslFileName).get();
        assertEquals(expectedNumericNature, citationStyle.isNumericStyle());
    }

    private static Stream<Arguments> numericPropertyMatches() {
        return Stream.of(
                Arguments.of(true, IEEE),
                Arguments.of(false, APA),
                Arguments.of(true, VANCOUVER),
                Arguments.of(false, CHICAGO_AUTHOR_DATE),
                Arguments.of(true, NATURE),
                Arguments.of(false, MLA),
                Arguments.of(false, JOURNAL_OF_CLINICAL_ETHICS)
        );
    }

    @ParameterizedTest
    @MethodSource
    void bibliographicPropertyMatches(boolean expectedBibliographicNature, String cslFileName) {
        CitationStyle citationStyle = CSLStyleUtils.createCitationStyleFromFile(cslFileName).get();
        assertEquals(expectedBibliographicNature, citationStyle.hasBibliography());
    }

    private static Stream<Arguments> bibliographicPropertyMatches() {
        return Stream.of(
                Arguments.of(true, IEEE),
                Arguments.of(true, APA),
                Arguments.of(true, VANCOUVER),
                Arguments.of(true, CHICAGO_AUTHOR_DATE),
                Arguments.of(true, NATURE),
                Arguments.of(true, MLA),
                Arguments.of(false, JOURNAL_OF_CLINICAL_ETHICS)
        );
    }

    @ParameterizedTest
    @MethodSource
    void hangingIndentPropertyMatches(boolean expectedUsesHangingIndent, String cslFileName) {
        CitationStyle citationStyle = CSLStyleUtils.createCitationStyleFromFile(cslFileName).get();
        assertEquals(expectedUsesHangingIndent, citationStyle.usesHangingIndent());
    }

    private static Stream<Arguments> hangingIndentPropertyMatches() {
        return Stream.of(
                Arguments.of(false, IEEE),
                Arguments.of(true, APA),
                Arguments.of(false, VANCOUVER),
                Arguments.of(true, CHICAGO_AUTHOR_DATE),
                Arguments.of(false, NATURE),
                Arguments.of(true, MLA),
                Arguments.of(false, JOURNAL_OF_CLINICAL_ETHICS)
        );
    }
}
