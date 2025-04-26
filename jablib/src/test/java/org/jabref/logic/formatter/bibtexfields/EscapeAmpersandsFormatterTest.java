package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeAmpersandsFormatterTest {

    private EscapeAmpersandsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new EscapeAmpersandsFormatter();
    }

    @Test
    void formatReturnsSameTextIfNoAmpersandsPresent() {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    @Test
    void formatEscapesAmpersandsIfPresent() {
        assertEquals("Lorem\\&ipsum", formatter.format("Lorem&ipsum"));
    }

    @Test
    void formatExample() {
        assertEquals("Text \\& with \\&ampersands", formatter.format(formatter.getExampleInput()));
    }

    @Test
    void formatReturnsSameTextInNewUserDefinedLatexCommandIfNoAmpersandsPresent() {
        assertEquals("\\newcommand[1]{Lorem ipsum}", formatter.format("\\newcommand[1]{Lorem ipsum}"));
    }

    @Test
    void formatReturnsSameTextInLatexCommandIfOneAmpersandPresent() {
        assertEquals("\\textbf{Lorem\\&ipsum}", formatter.format("\\textbf{Lorem\\&ipsum}"));
    }
}
