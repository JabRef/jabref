package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeUnderscoresFormatterTest {

    private EscapeUnderscoresFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new EscapeUnderscoresFormatter();
    }

    @Test
    void formatReturnsSameTextIfNoUnderscoresPresent() {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    @Test
    void formatEscapesUnderscoresIfPresent() {
        assertEquals("Lorem\\_ipsum", formatter.format("Lorem_ipsum"));
    }

    @Test
    void formatExample() {
        assertEquals("Text\\_with\\_underscores", formatter.format(formatter.getExampleInput()));
    }
}
