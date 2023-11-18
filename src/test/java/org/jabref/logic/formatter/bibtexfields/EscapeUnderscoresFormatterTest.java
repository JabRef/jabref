package org.jabref.logic.formatter.bibtexfields;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EscapeUnderscoresFormatterTest {

    private EscapeUnderscoresFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new EscapeUnderscoresFormatter();
    }

    @Test
    void formatReturnsSameTextIfNoUnderscoresPresent() throws Exception {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    @Test
    void formatEscapesUnderscoresIfPresent() throws Exception {
        assertEquals("Lorem\\_ipsum", formatter.format("Lorem_ipsum"));
    }

    @Test
    void formatExample() {
        assertEquals("Text\\_with\\_underscores", formatter.format(formatter.getExampleInput()));
    }
}
