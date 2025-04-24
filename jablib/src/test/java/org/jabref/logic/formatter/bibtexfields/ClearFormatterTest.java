package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class ClearFormatterTest {

    private ClearFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ClearFormatter();
    }

    /**
     * Check whether the clear formatter really returns the empty string for the empty string
     */
    @Test
    void formatReturnsEmptyForEmptyString() throws Exception {
        assertEquals("", formatter.format(""));
    }

    /**
     * Check whether the clear formatter really returns the empty string for some string
     */
    @Test
    void formatReturnsEmptyForSomeString() throws Exception {
        assertEquals("", formatter.format("test"));
    }

    @Test
    void formatExample() {
        assertEquals("", formatter.format(formatter.getExampleInput()));
    }
}
