package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EscapeUnderscoresFormatterTest {

    private EscapeUnderscoresFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new EscapeUnderscoresFormatter();
    }

    /**
     * Check whether the clear formatter really returns the empty string for the empty string
     */
    @Test
    public void formatReturnsSameTextIfNoUnderscoresPresent() throws Exception {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    /**
     * Check whether the clear formatter really returns the empty string for some string
     */
    @Test
    public void formatEscapesUnderscoresIfPresent() throws Exception {
        assertEquals("Lorem\\_ipsum", formatter.format("Lorem_ipsum"));
    }

    @Test
    public void formatExample() {
        assertEquals("Text\\_with\\_underscores", formatter.format(formatter.getExampleInput()));
    }
}
