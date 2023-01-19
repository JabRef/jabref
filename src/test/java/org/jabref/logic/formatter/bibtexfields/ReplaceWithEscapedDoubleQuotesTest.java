package org.jabref.logic.formatter.bibtexfields;

import org.jabref.logic.layout.format.ReplaceWithEscapedDoubleQuotes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReplaceWithEscapedDoubleQuotesTest {

    private ReplaceWithEscapedDoubleQuotes formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ReplaceWithEscapedDoubleQuotes();
    }

    @Test
    public void replacingSingleDoubleQuote() {
        assertEquals("single \"\"double quote", formatter.format("single \"double quote"));
    }

    @Test
    public void replacingMultipleDoubleQuote() {
        assertEquals("multiple \"\"double\"\" quote", formatter.format("multiple \"double\" quote"));
    }
    
    @Test
    public void replacingSingleDoubleQuoteHavingCommas() {
        assertEquals("this \"\"is\"\", a test", formatter.format("this \"is\", a test"));
    }

    @Test
    public void doNothing() {
        assertEquals("this is a test", formatter.format("this is a test"));
    }
}
