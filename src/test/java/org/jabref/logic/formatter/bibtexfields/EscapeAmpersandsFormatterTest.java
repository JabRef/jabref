package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeAmpersandsFormatterTest {

    private static final EscapeAmpersandsFormatter formatter = new EscapeAmpersandsFormatter();

    @Test
    void formatReturnsSameTextIfNoAmpersandsPresent() throws Exception {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    @Test
    void formatEscapesAmpersandsIfPresent() throws Exception {
        assertEquals("Lorem\\&ipsum", formatter.format("Lorem&ipsum"));
    }

    @Test
    void formatExample() {
        assertEquals("Text \\& with \\&ampersands", formatter.format(formatter.getExampleInput()));
    }
}
