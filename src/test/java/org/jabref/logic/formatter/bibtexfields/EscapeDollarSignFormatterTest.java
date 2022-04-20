package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeDollarSignFormatterTest {

    private EscapeDollarSignFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new EscapeDollarSignFormatter();
    }

    @Test
    void formatReturnsSameTextIfNoDollarSignPresent() throws Exception {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    @Test
    void formatEscapesDollarSignIfPresent() throws Exception {
        assertEquals("Lorem\\$ipsum", formatter.format("Lorem$ipsum"));
    }

    @Test
    void formatExample() {
        assertEquals("Text\\$with\\$dollar\\$sign", formatter.format(formatter.getExampleInput()));
    }
}
