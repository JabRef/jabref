package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class HtmlToLatexFormatterTest {

    private HtmlToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new HtmlToLatexFormatter();
    }

    @ParameterizedTest
    @CsvSource({
            // Return the same string
            "abc, abc", "aaa, aaa", "(p < 0.01), (p < 0.01)",

            // IEEE-style HTML entity for em dash
            "Towards situation-aware adaptive workflows: SitOPT --- A general purpose situation-aware workflow management system, Towards situation-aware adaptive workflows: SitOPT &amp;#x2014; A general purpose situation-aware workflow management system",

            // Format multiple HTML characters
            "{{\\aa}}{\\\"{a}}{\\\"{o}}, &aring;&auml;&ouml;",

            // Format combined accents
            "{\\'{\\i}}, i&#x301;", "{\\\"{a}}, a&#776;", "{\\\"{a}}, a&#x308;", "{\\\"{a}}b, a&#776;b", "{\\\"{a}}b, a&#x308;b",

            // Format HTML entities
            "{\\\"{a}}, &auml;", "{\\\"{a}}, &#228;", "{\\\"{a}}, &#xe4;", "{{$\\Epsilon$}}, &Epsilon;",

            // Strip tags
            "aaa, <b>aaa</b>"
    })
    void html(String expected, String text) {
        assertEquals(expected, formatter.format(text));
    }

    @Test
    void formatExample() {
        assertEquals("JabRef", formatter.format(formatter.getExampleInput()));
    }
}
