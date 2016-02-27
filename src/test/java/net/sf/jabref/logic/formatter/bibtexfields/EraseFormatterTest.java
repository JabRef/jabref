package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;

public class EraseFormatterTest {

    @Test
    public void formatReturnsNullForEmptyString() throws Exception {
        assertNull(new EraseFormatter().format(""));
    }

    @Test
    public void formatReturnsNullForSomeString() throws Exception {
        assertNull(new EraseFormatter().format("test"));
    }
}