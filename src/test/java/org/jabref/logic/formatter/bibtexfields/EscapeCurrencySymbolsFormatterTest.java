package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EscapeCurrencySymbolsFormatterTest {

    private EscapeCurrencySymbolsFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new EscapeCurrencySymbolsFormatter();
    }

    @Test
    void formatReturnsSameTextIfNoCurrencySymbolsPresent() throws Exception {
        assertEquals("Lorem ipsum", formatter.format("Lorem ipsum"));
    }

    @Test
    void formatEscapesCurrencySymbolsIfPresent() throws Exception {
        assertEquals("Lorem\\\\$ipsum", formatter.format("Lorem$ipsum"));
    }

    @Test
    void formatExample() {
        assertEquals("Text\\\\$with\\\\$currency\\\\$symbols", formatter.format(formatter.getExampleInput()));
    }
}
