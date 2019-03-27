
package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TrimWhitespaceFormatterTest {

    private TrimWhitespaceFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new TrimWhitespaceFormatter();
    }

    @Test
    public void removeHorizontalTabulations() {
        assertEquals("whitespace", formatter.format("\twhitespace"));
        assertEquals("whitespace", formatter.format("whitespace\t"));
        assertEquals("whitespace", formatter.format("\twhitespace\t\t"));
    }

    @Test
    public void removeLineFeeds() {
        assertEquals("whitespace", formatter.format("\nwhitespace"));
        assertEquals("whitespace", formatter.format("whitespace\n"));
        assertEquals("whitespace", formatter.format("\nwhitespace\n\n"));
    }

    @Test
    public void removeFormFeeds() {
        assertEquals("whitespace", formatter.format("\fwhitespace"));
        assertEquals("whitespace", formatter.format("whitespace\f"));
        assertEquals("whitespace", formatter.format("\fwhitespace\f\f"));
    }

    @Test
    public void removeCarriageReturnFeeds() {
        assertEquals("whitespace", formatter.format("\rwhitespace"));
        assertEquals("whitespace", formatter.format("whitespace\r"));
        assertEquals("whitespace", formatter.format("\rwhitespace\r\r"));
    }

    @Test
    public void removeSeparatorSpaces() {
        assertEquals("whitespace", formatter.format(" whitespace"));
        assertEquals("whitespace", formatter.format("whitespace "));
        assertEquals("whitespace", formatter.format(" whitespace  "));
    }

    @Test
    public void removeMixedWhitespaceChars() {
        assertEquals("whitespace", formatter.format(" \r\t\fwhitespace"));
        assertEquals("whitespace", formatter.format("whitespace \n \r"));
        assertEquals("whitespace", formatter.format("   \f\t whitespace  \r \n"));
    }
}
