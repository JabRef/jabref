package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;

public class EraseFormatterTest {

    @Test
    public void formatReturnsEmptyForEmptyString() throws Exception {
        assertEquals("", new EraseFormatter().format(""));
    }

    @Test
    public void formatReturnsEmptyForSomeString() throws Exception {
        assertEquals("", new EraseFormatter().format("test"));
    }
}