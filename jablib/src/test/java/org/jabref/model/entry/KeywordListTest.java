package org.jabref.model.entry;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordListTest {

    private KeywordList keywords;

    @BeforeEach
    void setUp() {
        keywords = new KeywordList();
        keywords.add("keywordOne");
        keywords.add("keywordTwo");
    }

    private static Stream<Arguments> provideWithEscapedDelimiterCases() {
        return Stream.of(
                Arguments.of("keyword\\,one, keywordTwo", new KeywordList("keyword,one", "keywordTwo")),
                Arguments.of("keywordOne\\,, keywordTwo", new KeywordList("keywordOne,", "keywordTwo")),
                Arguments.of("keyword\\\\, keywordTwo", new KeywordList("keyword\\", "keywordTwo")),
                Arguments.of("keyword\\,one > sub", new KeywordList(Keyword.of(List.of("keyword,one", "sub")))),
                Arguments.of("one\\,two\\,three, four", new KeywordList("one,two,three", "four")),
                Arguments.of("keywordOne\\\\", new KeywordList("keywordOne\\"))
        );
    }

    private static Stream<Arguments> provideSerializeWithNonEscapedDelimiterCases() {
        return Stream.of(
                Arguments.of(List.of(new Keyword("keyword,one"), new Keyword("keywordTwo")), "keyword\\,one, keywordTwo"),
                Arguments.of(List.of(new Keyword("keywordOne,"), new Keyword("keywordTwo")), "keywordOne\\,, keywordTwo"),
                Arguments.of(List.of(Keyword.of(List.of("keyword\\")), Keyword.of(List.of("keywordTwo"))), "keyword\\\\, keywordTwo"),
                Arguments.of(List.of(Keyword.of(List.of("keyword,one", "sub"))), "keyword\\,one > sub"),
                Arguments.of(List.of(new Keyword("one,two,three"), new Keyword("four")), "one\\,two\\,three, four"),
                Arguments.of(List.of(new Keyword("keywordOne\\")), "keywordOne\\\\")
        );
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
    void parseTwoWordReturnsTwoKeywords() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo", ','));
    }

    @Test
    void parseTwoWordReturnsTwoKeywordsWithoutSpace() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne,keywordTwo", ','));
    }

    @Test
    void parseTwoWordReturnsTwoKeywordsWithDifferentDelimiter() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne| keywordTwo", '|'));
    }

    @Test
    void parseWordsWithWhitespaceReturnsOneKeyword() {
        assertEquals(new KeywordList("keyword and one"),
                KeywordList.parse("keyword and one", ','));
    }

    @Test
    void parseWordsWithWhitespaceAndCommaReturnsTwoKeyword() {
        assertEquals(new KeywordList("keyword and one", "and two"),
                KeywordList.parse("keyword and one, and two", ','));
    }

    @Test
    void parseIgnoresDuplicates() {
        assertEquals(new KeywordList("keywordOne", "keywordTwo"),
                KeywordList.parse("keywordOne, keywordTwo, keywordOne", ','));
    }

    @Test
    void parseTakeDelimiterNotRegexWhite() {
        assertEquals(new KeywordList("keywordOne keywordTwo", "keywordThree"),
                KeywordList.parse("keywordOne keywordTwoskeywordThree", 's'));
    }

    @Test
    void parseWordsWithBracketsReturnsOneKeyword() {
        assertEquals(new KeywordList("[a] keyword"), KeywordList.parse("[a] keyword", ','));
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

    @ParameterizedTest
    @MethodSource("provideWithEscapedDelimiterCases")
    void parseKeywordWithEscapedDelimiterDoesNotSplitKeyword(String input, KeywordList expected) {
        assertEquals(expected, KeywordList.parse(input, ','));
    }

    @ParameterizedTest
    @MethodSource("provideSerializeWithNonEscapedDelimiterCases")
    void serializeKeywordWithNonEscapedDelimiterJoinsKeywordsCorrectly(List<Keyword> input, String expected) {
        assertEquals(expected, KeywordList.serialize(input, ','));
    }

    @ParameterizedTest
    @MethodSource("provideWithEscapedDelimiterCases")
    void afterFirstParsingNoChangesShouldBeDoneToKeywords(String input, KeywordList expected) {
        char delimiter = ',';
        KeywordList firstParse = KeywordList.parse(input, delimiter);
        String firstSerialize = KeywordList.serialize(firstParse.stream().toList(), delimiter);
        KeywordList secondParse = KeywordList.parse(firstSerialize, delimiter);
        String secondSerialize = KeywordList.serialize(secondParse.stream().toList(), delimiter);
        assertEquals(firstSerialize, secondSerialize);
        assertEquals(expected, secondParse);
    }
}
