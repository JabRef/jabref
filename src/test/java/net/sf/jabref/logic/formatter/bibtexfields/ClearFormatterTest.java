package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;

public class ClearFormatterTest {

    @Test
    public void formatReturnsEmptyForEmptyString() throws Exception {
        assertEquals("", new ClearFormatter().format(""));
    }

    @Test
    public void formatReturnsEmptyForSomeString() throws Exception {
        assertEquals("", new ClearFormatter().format("test"));
    }
}