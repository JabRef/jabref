package org.jabref.logic.formatter.bibtexfields;

import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class ShortenDOIFormatterTest {

    private ShortenDOIFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ShortenDOIFormatter();
    }

    @Test
    public void formatDoi() {
        assertEquals("10/adc", formatter.format("10.1006/jmbi.1998.2354"));
    }

    @Test
    public void invalidDoiIsKept() {
        assertEquals("invalid-doi", formatter.format("invalid-doi"));
    }
}
