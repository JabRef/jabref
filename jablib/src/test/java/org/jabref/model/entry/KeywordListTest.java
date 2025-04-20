package org.jabref.model.entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordListTest {

    private KeywordList keywords;

    @BeforeEach
    void setUp() throws Exception {
        keywords = new KeywordList();
        keywords.add("keywordOne");
        keywords.add("keywordTwo");
    }

    @Test
    void parseEmptyStringReturnsEmptyList() throws Exception {
        assertEquals(new KeywordList(), KeywordList.parse("", ','));
    }

    @Test
    void parseOneWordReturnsOneKeyword() throws Exception {
        assertEquals(new KeywordList("keywordOne"),
                KeywordList.parse("keywordOne", ','));
    }

    @Test
    void parseTwoWordReturnsTwoKeywords() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo", ','));
    }

    @Test
    void parseTwoWordReturnsTwoKeywordsWithoutSpace() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne,keywordTwo", ','));
    }

    @Test
    void parseTwoWordReturnsTwoKeywordsWithDifferentDelimiter() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne| keywordTwo", '|'));
    }

    @Test
    void parseWordsWithWhitespaceReturnsOneKeyword() throws Exception {
        assertEquals(new KeywordList("keyword and one"),
                KeywordList.parse("keyword and one", ','));
    }

    @Test
    void parseWordsWithWhitespaceAndCommaReturnsTwoKeyword() throws Exception {
        assertEquals(new KeywordList("keyword and one", "and two"),
                KeywordList.parse("keyword and one, and two", ','));
    }

    @Test
    void parseIgnoresDuplicates() throws Exception {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo, keywordOne", ','));
    }

    @Test
    void parseTakeDelimiterNotRegexWhite() throws Exception {
        assertEquals(new KeywordList("keywordOne keywordTwo", "keywordThree"),
                KeywordList.parse("keywordOne keywordTwoskeywordThree", 's'));
    }

    @Test
    void parseWordsWithBracketsReturnsOneKeyword() throws Exception {
        assertEquals(new KeywordList("[a] keyword"), KeywordList.parse("[a] keyword", ','));
    }

    @Test
    void asStringAddsSpaceAfterDelimiter() throws Exception {
        assertEquals("keywordOne, keywordTwo", keywords.getAsString(','));
    }

    @Test
    void parseHierarchicalChain() throws Exception {
        Keyword expected = Keyword.of("Parent", "Node", "Child");

        assertEquals(new KeywordList(expected), KeywordList.parse("Parent > Node > Child", ',', '>'));
    }

    @Test
    void parseTwoHierarchicalChains() throws Exception {
        Keyword expectedOne = Keyword.of("Parent1", "Node1", "Child1");
        Keyword expectedTwo = Keyword.of("Parent2", "Node2", "Child2");

        assertEquals(new KeywordList(expectedOne, expectedTwo),
                KeywordList.parse("Parent1 > Node1 > Child1, Parent2 > Node2 > Child2", ',', '>'));
    }

    @Test
    void mergeTwoIdenticalKeywordsShouldReturnOnKeyword() {
        assertEquals(new KeywordList("JabRef"), KeywordList.merge("JabRef", "JabRef", ','));
    }

    @Test
    void mergeOneEmptyKeywordAnAnotherNonEmptyShouldReturnTheNonEmptyKeyword() {
        assertEquals(new KeywordList("JabRef"), KeywordList.merge("", "JabRef", ','));
    }

    @Test
    void mergeTwoDistinctKeywordsShouldReturnTheTwoKeywordsMerged() {
        assertEquals(new KeywordList("Figma", "JabRef"), KeywordList.merge("Figma", "JabRef", ','));
        assertEquals(new KeywordList("JabRef", "Figma"), KeywordList.merge("Figma", "JabRef", ','));
    }

    @Test
    void mergeTwoListsOfKeywordsShouldReturnTheKeywordsMerged() {
        assertEquals(new KeywordList("Figma", "Adobe", "JabRef", "Eclipse", "JetBrains"), KeywordList.merge("Figma, Adobe, JetBrains, Eclipse", "Adobe, JabRef", ','));
    }
}
