package net.sf.jabref.logic.formatter.bibtexfields;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
import org.junit.Test;

import static org.junit.Assert.*;

public class ClearFormatterTest {

    /**
     * Check whether the clear formatter really returns the empty string for the empty string
     */
    @Test
    public void formatReturnsEmptyForEmptyString() throws Exception {
        assertEquals("", new ClearFormatter().format(""));
    }

    /**
     * Check whether the clear formatter really returns the empty string for some string
     */
    @Test
    public void formatReturnsEmptyForSomeString() throws Exception {
        assertEquals("", new ClearFormatter().format("test"));
    }
}