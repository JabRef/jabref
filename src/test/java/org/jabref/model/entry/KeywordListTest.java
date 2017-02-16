package org.jabref.model.entry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeywordListTest {

    private KeywordList keywords;

    @Before
    public void setUp() throws Exception {
        keywords = new KeywordList();
        keywords.add("keywordOne");
        keywords.add("keywordTwo");
    }

    @Test
    public void parseEmptyStringReturnsEmptyList() throws Exception {
        assertEquals(new KeywordList(), KeywordList.parse("", ','));
    }

    @Test
    public void parseOneWordReturnsOneKeyword() throws Exception {
        assertEquals(new KeywordList("keywordOne"),
                KeywordList.parse("keywordOne", ','));
    }

    @Test
    public void parseTwoWordReturnsTwoKeywords() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo", ','));
    }

    @Test
    public void parseTwoWordReturnsTwoKeywordsWithoutSpace() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne,keywordTwo", ','));
    }

    @Test
    public void parseTwoWordReturnsTwoKeywordsWithDifferentDelimiter() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne| keywordTwo", '|'));
    }

    @Test
    public void parseWordsWithWhitespaceReturnsOneKeyword() throws Exception {
        assertEquals(new KeywordList("keyword and one"),
                KeywordList.parse("keyword and one", ','));
    }

    @Test
    public void parseWordsWithWhitespaceAndCommaReturnsTwoKeyword() throws Exception {
        assertEquals(new KeywordList("keyword and one", "and two"),
                KeywordList.parse("keyword and one, and two", ','));
    }

    @Test
    public void parseIgnoresDuplicates() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo, keywordOne", ','));
    }

    @Test
    public void parseWordsWithBracketsReturnsOneKeyword() throws Exception {
        assertEquals(new KeywordList("[a] keyword"), KeywordList.parse("[a] keyword", ','));
    }

    @Test
    public void asStringAddsSpaceAfterDelimiter() throws Exception {
        assertEquals("keywordOne, keywordTwo", keywords.getAsString(','));
    }
}
