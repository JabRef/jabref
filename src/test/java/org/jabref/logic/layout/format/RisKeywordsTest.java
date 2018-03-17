package org.jabref.logic.layout.format;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RisKeywordsTest {

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
        assertEquals("KW  - abcd" + OS.NEWLINE + "KW  - efg", new RisKeywords().format("abcd, efg"));
    }

    @Test
    public void testMultipleKeywords() {
        assertEquals("KW  - abcd" + OS.NEWLINE + "KW  - efg" + OS.NEWLINE + "KW  - hij" + OS.NEWLINE
                + "KW  - klm", new RisKeywords().format("abcd, efg, hij, klm"));
    }
}
