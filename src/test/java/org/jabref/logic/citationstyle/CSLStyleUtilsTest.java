package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSLStyleUtilsTest {

    private static final String MODIFIED_IEEE = "ieee-bold-author.csl";
    private static final String MODIFIED_APA = "modified-apa.csl";

    @Test
    void parseStyleInfo() throws IOException {
        String styleContent;
        try (InputStream inputStream = CSLStyleUtilsTest.class.getResourceAsStream(MODIFIED_IEEE)) {
            styleContent = new String(inputStream.readAllBytes());
        }

        Optional<CSLStyleUtils.StyleInfo> styleInfo = CSLStyleUtils.parseStyleInfo(MODIFIED_IEEE, styleContent);

        assertTrue(styleInfo.isPresent());
        assertEquals("IEEE - Bold Author", styleInfo.get().title());
        assertTrue(styleInfo.get().isNumericStyle());
    }

    @Test
    void createCitationStyleFromFileReturnsValidCitationStyle() {
        // use absolute path to test csl so that it is treated as external file
        Path resourcePath = Path.of("").toAbsolutePath()
                                .resolve("src/test/resources/org/jabref/logic/citationstyle")
                                .resolve(MODIFIED_IEEE);

        Optional<CitationStyle> citationStyle = CSLStyleUtils.createCitationStyleFromFile(resourcePath.toString());

        assertTrue(citationStyle.isPresent());
        assertEquals("IEEE - Bold Author", citationStyle.get().getTitle());
        assertTrue(citationStyle.get().isNumericStyle());
        assertNotNull(citationStyle.get().getSource());
        assertFalse(citationStyle.get().isInternalStyle());
    }

    @Test
    void parseStyleInfo2() throws IOException {
        String styleContent;
        try (InputStream inputStream = CSLStyleUtilsTest.class.getResourceAsStream(MODIFIED_APA)) {
            styleContent = new String(inputStream.readAllBytes());
        }

        Optional<CSLStyleUtils.StyleInfo> styleInfo = CSLStyleUtils.parseStyleInfo(MODIFIED_APA, styleContent);

        assertTrue(styleInfo.isPresent());
        assertEquals("Modified American Psychological Association 7th edition", styleInfo.get().title());
        assertFalse(styleInfo.get().isNumericStyle());
    }

    @Test
    void createCitationStyleFromFileReturnsValidCitationStyle2() {
        // use absolute path to test csl so that it is treated as external file
        Path resourcePath = Path.of("").toAbsolutePath()
                                .resolve("src/test/resources/org/jabref/logic/citationstyle")
                                .resolve(MODIFIED_APA);

        Optional<CitationStyle> citationStyle = CSLStyleUtils.createCitationStyleFromFile(resourcePath.toString());

        assertTrue(citationStyle.isPresent());
        assertEquals("Modified American Psychological Association 7th edition", citationStyle.get().getTitle());
        assertFalse(citationStyle.get().isNumericStyle());
        assertNotNull(citationStyle.get().getSource());
        assertFalse(citationStyle.get().isInternalStyle());
    }
}
