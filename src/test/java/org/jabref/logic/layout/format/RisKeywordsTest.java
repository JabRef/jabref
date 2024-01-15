package org.jabref.logic.layout.format;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RisKeywordsTest {

    @Test
    void empty() {
        assertEquals("", new RisKeywords().format(""));
    }

    @Test
    void testNull() {
        assertEquals("", new RisKeywords().format(null));
    }

    @Test
    void singleKeyword() {
        assertEquals("KW  - abcd", new RisKeywords().format("abcd"));
    }

    @Test
    void twoKeywords() {
        assertEquals("KW  - abcd" + OS.NEWLINE + "KW  - efg", new RisKeywords().format("abcd, efg"));
    }

    @Test
    void multipleKeywords() {
        assertEquals("KW  - abcd" + OS.NEWLINE + "KW  - efg" + OS.NEWLINE + "KW  - hij" + OS.NEWLINE
                + "KW  - klm", new RisKeywords().format("abcd, efg, hij, klm"));
    }
}
