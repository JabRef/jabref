package org.jabref.logic.formatter.bibtexfields;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClearFormatterTest {

    private ClearFormatter formatter;

    @Before
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
