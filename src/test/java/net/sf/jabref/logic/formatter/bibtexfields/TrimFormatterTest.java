package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Test;

import static org.junit.Assert.*;

public class TrimFormatterTest {

    @Test
    public void formatTrimsWhitespaceBefore() throws Exception {
        TrimFormatter formatter = new TrimFormatter();
        assertEquals("nonspace", formatter.format("  nonspace"));
    }

    @Test
    public void formatTrimsWhitespaceAfter() throws Exception {
        TrimFormatter formatter = new TrimFormatter();
        assertEquals("nonspace", formatter.format("nonspace   "));
    }

    @Test
    public void formatTrimsWhitespaceBeforeAndAfter() throws Exception {
        TrimFormatter formatter = new TrimFormatter();
        assertEquals("nonspace", formatter.format("  nonspace   "));
    }
}
