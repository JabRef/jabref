package net.sf.jabref.exporter.layout.format;

import static org.junit.Assert.*;

import org.junit.Test;


public class RisMonthTest {

    @Test
    public void testEmpty() {
        assertEquals("", new RisKeywords().format(""));
    }

    @Test
    public void testNull() {
        assertEquals("", new RisKeywords().format(null));
    }

    @Test
    public void testSingleKeyword() {
        assertEquals("KW  - abcd", new RisKeywords().format("abcd"));
    }

    @Test
    public void testTwoKeywords() {
        assertEquals("KW  - abcd\nKW  - efg", new RisKeywords().format("abcd, efg"));
    }

    @Test
    public void testMultipleKeywords() {
        assertEquals("KW  - abcd\nKW  - efg\nKW  - hij\nKW  - klm", new RisKeywords().format("abcd, efg, hij, klm"));
    }
}
