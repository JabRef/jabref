package org.jabref.model.entry;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordListTest {

    private KeywordList keywords;

    @BeforeEach
    void setUp() {
        keywords = new KeywordList();
        keywords.add("keywordOne");
        keywords.add("keywordTwo");
    }

    @Test
    void parseEmptyStringReturnsEmptyList() {
        assertEquals(new KeywordList(), KeywordList.parse("", ','));
    }

    @Test
    void parseOneWordReturnsOneKeyword() {
        assertEquals(new KeywordList("keywordOne"),
                KeywordList.parse("keywordOne", ','));
    }

    @Test
    void parseTwoWordsReturnsTwoKeywords() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo", ','));
    }

    @Test
    void parseTwoWordsWithoutSpace() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne,keywordTwo", ','));
    }

    @Test
    void parseWithDifferentDelimiter() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne| keywordTwo", '|'));
    }

    @Test
    void parseSingleKeywordWithWhitespace() {
        assertEquals(new KeywordList("keyword and one"),
                KeywordList.parse("keyword and one", ','));
    }

    @Test
    void parseTwoKeywordsWithWhitespaceAndComma() {
        assertEquals(new KeywordList("keyword and one", "and two"),
                KeywordList.parse("keyword and one, and two", ','));
    }

    @Test
    void parseIgnoresDuplicates() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo, keywordOne", ','));
    }

    @Test
    void parseKeywordWithEscapedSeparator() {
        String input = "network\\,security, AI";
        KeywordList list = KeywordList.parse(input, ',');
        assertEquals(2, list.size());
        assertEquals("network,security", list.get(0).get());
        assertEquals("AI", list.get(1).get());
    }

    @Test
    void serializeEscapedKeyword() {
        KeywordList list = new KeywordList("network,security", "AI");
        String serialized = KeywordList.serialize(list.stream().toList(), ',');
        assertEquals("network\\,security,AI", serialized);
    }

    @Test
    void asStringAddsSpaceAfterDelimiter() {
        assertEquals("keywordOne, keywordTwo", keywords.getAsString(','));
    }

    @Test
    void parseHierarchicalChain() {
        Keyword expected = Keyword.of(List.of("Parent", "Node", "Child"));

        assertEquals(new KeywordList(expected), KeywordList.parse("Parent > Node > Child", ','));
    }

    @Test
    void parseTwoHierarchicalChains() {
        Keyword expectedOne = Keyword.of(List.of("Parent1", "Node1", "Child1"));
        Keyword expectedTwo = Keyword.of(List.of("Parent2", "Node2", "Child2"));

        assertEquals(new KeywordList(expectedOne, expectedTwo),
                KeywordList.parse("Parent1 > Node1 > Child1, Parent2 > Node2 > Child2", ','));
    }

    @Test
    void mergeTwoIdenticalKeywordsShouldReturnOneKeyword() {
        assertEquals(new KeywordList("JabRef"), KeywordList.merge("JabRef", "JabRef", ','));
    }

    @Test
    void mergeOneEmptyKeywordAndAnotherNonEmptyShouldReturnTheNonEmptyKeyword() {
        assertEquals(new KeywordList("JabRef"), KeywordList.merge("", "JabRef", ','));
    }

    @Test
    void mergeTwoDistinctKeywordsShouldReturnBothKeywords() {
        assertEquals(new KeywordList("Figma", "JabRef"), KeywordList.merge("Figma", "JabRef", ','));
    }

    @Test
    void mergeTwoListsOfKeywordsShouldReturnMergedKeywords() {
        assertEquals(new KeywordList("Figma", "Adobe", "JabRef", "Eclipse", "JetBrains"),
                KeywordList.merge("Figma, Adobe, JetBrains, Eclipse", "Adobe, JabRef", ','));
    }

    @Test
    void parseMultipleEscapedSeparators() {
        String input = "a\\,b\\,c, d";
        KeywordList list = KeywordList.parse(input, ',');
        assertEquals(2, list.size());
        assertEquals("a,b,c", list.get(0).get());
        assertEquals("d", list.get(1).get());
    }
}
