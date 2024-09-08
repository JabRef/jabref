package org.jabref.logic.layout.format;

import org.jabref.gui.desktop.os.NativeDesktop;

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
        assertEquals("KW  - abcd" + NativeDesktop.NEWLINE + "KW  - efg", new RisKeywords().format("abcd, efg"));
    }

    @Test
    void multipleKeywords() {
        assertEquals("KW  - abcd" + NativeDesktop.NEWLINE + "KW  - efg" + NativeDesktop.NEWLINE + "KW  - hij" + NativeDesktop.NEWLINE
                + "KW  - klm", new RisKeywords().format("abcd, efg, hij, klm"));
    }
}
