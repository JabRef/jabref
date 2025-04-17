package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSLStyleUtilsTest {

    private static final String MODIFIED_IEEE = "ieee-bold-author.csl";
    private static final String MODIFIED_APA = "modified-apa.csl";
    private static final String LITERATURA = "literatura.csl";

    @ParameterizedTest
    @MethodSource("styleTestData")
    void parseStyleInfo(String styleName, String expectedTitle, boolean expectedIsNumeric) throws IOException {
        String styleContent;
        try (InputStream inputStream = CSLStyleUtilsTest.class.getResourceAsStream(styleName)) {
            styleContent = new String(inputStream.readAllBytes());
        }

        Optional<CSLStyleUtils.StyleInfo> styleInfo = CSLStyleUtils.parseStyleInfo(styleName, styleContent);

        assertTrue(styleInfo.isPresent());
        assertEquals(expectedTitle, styleInfo.get().title());
        assertEquals(expectedIsNumeric, styleInfo.get().isNumericStyle());
    }

    @ParameterizedTest
    @MethodSource("styleTestData")
    void createCitationStyleFromFileReturnsValidCitationStyle(String styleName, String expectedTitle, boolean expectedIsNumeric) {
        // use absolute path to test csl so that it is treated as external file
        Path resourcePath = Path.of("").toAbsolutePath()
                                .resolve("src/test/resources/org/jabref/logic/citationstyle")
                                .resolve(styleName);

        Optional<CitationStyle> citationStyle = CSLStyleUtils.createCitationStyleFromFile(resourcePath.toString());

        assertTrue(citationStyle.isPresent());
        assertEquals(expectedTitle, citationStyle.get().getTitle());
        assertEquals(expectedIsNumeric, citationStyle.get().isNumericStyle());
        assertNotNull(citationStyle.get().getSource());
        assertFalse(citationStyle.get().isInternalStyle());
    }

    private static Stream<Arguments> styleTestData() {
        return Stream.of(
                Arguments.of(MODIFIED_IEEE, "IEEE - Bold Author", true),
                Arguments.of(MODIFIED_APA, "Modified American Psychological Association 7th edition", false),
                Arguments.of(LITERATURA, "Literatūra", false) // Literatūra uses author-date format, so non-numeric
        );
    }
}
