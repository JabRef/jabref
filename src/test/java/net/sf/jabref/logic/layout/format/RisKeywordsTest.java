package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.util.strings.StringUtil;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


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
        assertEquals("KW  - abcd" + StringUtil.NEWLINE + "KW  - efg", new RisKeywords().format("abcd, efg"));
    }

    @Test
    public void testMultipleKeywords() {
        assertEquals("KW  - abcd" + StringUtil.NEWLINE + "KW  - efg" + StringUtil.NEWLINE + "KW  - hij" + StringUtil.NEWLINE
                + "KW  - klm", new RisKeywords().format("abcd, efg, hij, klm"));
    }
}
