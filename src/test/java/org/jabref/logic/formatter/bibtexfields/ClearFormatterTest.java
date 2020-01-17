package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class ClearFormatterTest {

    private ClearFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ClearFormatter();
    }

    /**
     * Check whether the clear formatter really returns the empty string for the empty string
     */
    @Test
    public void formatReturnsEmptyForEmptyString() throws Exception {
        assertEquals("", formatter.format(""));
    }

    /**
     * Check whether the clear formatter really returns the empty string for some string
     */
    @Test
    public void formatReturnsEmptyForSomeString() throws Exception {
        assertEquals("", formatter.format("test"));
    }

    @Test
    public void formatExample() {
        assertEquals("", formatter.format(formatter.getExampleInput()));
    }
}
