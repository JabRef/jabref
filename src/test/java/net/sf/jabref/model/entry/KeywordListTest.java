package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.Collections;

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
        assertEquals(Collections.singletonList(new Keyword("keywordOne")), KeywordList.parse("keywordOne", ','));
    }

    @Test
    public void parseTwoWordReturnsTwoKeywords() throws Exception {
        assertEquals(Arrays.asList(new Keyword("keywordOne"), new Keyword("keywordTwo")),
                KeywordList.parse("keywordOne, keywordTwo", ','));
    }

    @Test
    public void parseTwoWordReturnsTwoKeywordsWithoutSpace() throws Exception {
        assertEquals(Arrays.asList(new Keyword("keywordOne"), new Keyword("keywordTwo")),
                KeywordList.parse("keywordOne,keywordTwo", ','));
    }

    @Test
    public void parseTwoWordReturnsTwoKeywordsWithDifferentDelimiter() throws Exception {
        assertEquals(Arrays.asList(new Keyword("keywordOne"), new Keyword("keywordTwo")),
                KeywordList.parse("keywordOne| keywordTwo", '|'));
    }

    @Test
    public void parseWordsWithWhitespaceReturnsOneKeyword() throws Exception {
        assertEquals(Collections.singletonList(new Keyword("keyword and one")),
                KeywordList.parse("keyword and one", ','));
    }

    @Test
    public void parseWordsWithWhitespaceAndCommaReturnsTwoKeyword() throws Exception {
        assertEquals(Arrays.asList(new Keyword("keyword and one"), new Keyword("and two")),
                KeywordList.parse("keyword and one, and two", ','));
    }

    @Test
    public void asStringAddsSpaceAfterDelimiter() throws Exception {
        assertEquals("keywordOne, keywordTwo", keywords.getAsString(','));
    }
}
