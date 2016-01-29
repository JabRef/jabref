package net.sf.jabref.exporter.layout.format;

import static org.junit.Assert.*;

import org.junit.Test;


public class RisKeywordsTest {

    @Test
    public void testEmpty() {
        assertEquals("", new RisMonth().format(""));
    }

    @Test
    public void testNull() {
        assertEquals("", new RisMonth().format(null));
    }

    @Test
    public void testMonth() {
        assertEquals("12", new RisMonth().format("dec"));
    }

    @Test
    public void testInvalidMonth() {
        assertEquals("abcd", new RisMonth().format("abcd"));
    }

}
