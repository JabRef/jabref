package org.jabref.model.entry;

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

    private static Stream<Arguments> provideParseKeywordCases() {
        return Stream.of(
                Arguments.of("keyword\\,one, keywordTwo", new KeywordList("keyword,one", "keywordTwo")),
                Arguments.of("keywordOne\\,, keywordTwo", new KeywordList("keywordOne,", "keywordTwo")),
                Arguments.of("keyword\\\\, keywordTwo", new KeywordList("keyword\\", "keywordTwo")),
                Arguments.of("keyword\\,one > sub", new KeywordList(Keyword.of("keyword,one", "sub"))),
                Arguments.of("one\\,two\\,three, four", new KeywordList("one,two,three", "four")),
                Arguments.of("keywordOne\\\\", new KeywordList("keywordOne\\"))
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
        Keyword expected = Keyword.of("Parent", "Node", "Child");

        assertEquals(new KeywordList(expected), KeywordList.parse("Parent > Node > Child", ',', '>'));
    }

    @Test
    void parseTwoHierarchicalChains() {
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

    @ParameterizedTest
    @MethodSource("provideParseKeywordCases")
    void parseKeywordWithEscapedDelimiterDoesNotSplitKeyword(String input, KeywordList expected) {
        assertEquals(expected, KeywordList.parse(input, ',', '>'));
    }

    // TODO: We need to redefine the roundtrip test depending on the context GUI or BibTex,
    //  we want the user to type in escaping character but see the "clean" String as in:
    //  keyword1\,keyword2, keyword3 --> "keyword1,keyword2", "keyword3"
    // how is the .bib parser handling this? will there be escaping characters at all?
    // @ParameterizedTest
    @MethodSource("provideParseKeywordCases")
    void roundTripPreservesStructure(String original) {
        KeywordList parsed = KeywordList.parse(original, ',', '>');
        // We need to test the toString() functionality
        assertEquals(original, parsed.toString());
    }
}
