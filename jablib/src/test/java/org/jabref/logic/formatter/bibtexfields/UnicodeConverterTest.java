package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class UnicodeConverterTest {

    private UnicodeToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnicodeToLatexFormatter();
    }

    @Test
    void basic() {
        assertEquals("aaa", formatter.format("aaa"));
    }

    @ParameterizedTest
    @CsvSource({
            // combining accents
            "{\\\"{a}}, a\u0308",
            "{\\\"{a}}b, a\u0308b",

            // plain unicode letters
            "{\\\"{a}}, ä",
            "{{$\\Epsilon$}}, \u0395"
    })
    void unicode(String expected, String text) {
        assertEquals(expected, formatter.format(text));
    }

    @Test
    void unicodeSingle() {
        assertEquals("a", formatter.format("a"));
    }
}
