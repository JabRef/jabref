package org.jabref.logic.ai.util;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmResponseParserTest {

    static Stream<Arguments> emptyInputCases() {
        return Stream.of(
                Arguments.of("empty string", ""),
                Arguments.of("whitespace only", "   \n\t  ")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyInputCases")
    void emptyInputs_returnEmptyList(String description, String input) {
        assertTrue(LlmResponseParser.extractNumberedList(input).isEmpty());
    }

    static Stream<Arguments> numberedListCases() {
        return Stream.of(
                Arguments.of("simple three items",
                        """
                                1. First question
                                2. Second question
                                3. Third question
                                """,
                        List.of("First question", "Second question", "Third question")),

                Arguments.of("with leading whitespace",
                        """
                                   1. Question one
                                   2. Question two
                                """,
                        List.of("Question one", "Question two")),

                Arguments.of("with surrounding prose",
                        """
                                Here are some questions:

                                1. First question
                                2. Second question

                                Hope this helps!
                                """,
                        List.of("First question", "Second question")),

                Arguments.of("with double quotes stripped",
                        """
                                1. "What is the main topic?"
                                2. "How does it work?"
                                """,
                        List.of("What is the main topic?", "How does it work?")),

                Arguments.of("with single quotes stripped",
                        """
                                1. 'First question'
                                2. 'Second question'
                                """,
                        List.of("First question", "Second question")),

                Arguments.of("mixed quoted and unquoted items",
                        """
                                1. "Question with double quotes"
                                2. 'Question with single quotes'
                                3. Question without quotes
                                """,
                        List.of("Question with double quotes", "Question with single quotes", "Question without quotes")),

                Arguments.of("blank items filtered out",
                        """
                                1. Valid question
                                2.
                                3. Another valid question
                                """,
                        List.of("Valid question", "Another valid question")),

                Arguments.of("extra spacing after number",
                        """
                                1.    Question with extra spaces
                                2.  Another one
                                """,
                        List.of("Question with extra spaces", "Another one")),

                Arguments.of("large numbers parsed correctly",
                        """
                                100. Question one hundred
                                101. Question one hundred one
                                """,
                        List.of("Question one hundred", "Question one hundred one")),

                Arguments.of("numbered takes priority over bulleted",
                        """
                                Here are questions:
                                1. Numbered question
                                - Bulleted item (should not be extracted in numbered mode)
                                2. Another numbered question
                                """,
                        List.of("Numbered question", "Another numbered question"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("numberedListCases")
    void numberedList(String description, String input, List<String> expected) {
        assertEquals(expected, LlmResponseParser.extractNumberedList(input));
    }

    static Stream<Arguments> bulletedListCases() {
        return Stream.of(
                Arguments.of("hyphen bullets",
                        """
                                - First question
                                - Second question
                                - Third question
                                """,
                        List.of("First question", "Second question", "Third question")),

                Arguments.of("asterisk bullets",
                        """
                                * What is the main idea?
                                * How does it apply?
                                * Why is it important?
                                """,
                        List.of("What is the main idea?", "How does it apply?", "Why is it important?")),

                Arguments.of("unicode bullet character",
                        """
                                • First item
                                • Second item
                                • Third item
                                """,
                        List.of("First item", "Second item", "Third item")),

                Arguments.of("mixed quote styles stripped",
                        """
                                - "Question one"
                                - 'Question two'
                                - Question three
                                """,
                        List.of("Question one", "Question two", "Question three"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("bulletedListCases")
    void bulletedList(String description, String input, List<String> expected) {
        assertEquals(expected, LlmResponseParser.extractNumberedList(input));
    }

    static Stream<Arguments> fallbackModeCases() {
        return Stream.of(
                Arguments.of("plain lines, blank lines filtered",
                        """
                                First question
                                Second question

                                Third question
                                """,
                        List.of("First question", "Second question", "Third question")),

                Arguments.of("mixed bullet types all extracted",
                        """
                                - Question with hyphen
                                * Question with asterisk
                                • Question with bullet
                                Plain question
                                """,
                        List.of("Question with hyphen", "Question with asterisk", "Question with bullet", "Plain question")),

                Arguments.of("quotes stripped in fallback",
                        """
                                "First question"
                                'Second question'
                                Third question
                                """,
                        List.of("First question", "Second question", "Third question")),

                Arguments.of("multiline item treated as separate lines",
                        """
                                Question one
                                that spans multiple lines
                                Question two
                                """,
                        List.of("Question one", "that spans multiple lines", "Question two"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("fallbackModeCases")
    void fallbackMode(String description, String input, List<String> expected) {
        assertEquals(expected, LlmResponseParser.extractNumberedList(input));
    }

    static Stream<Arguments> emptyResultCases() {
        return Stream.of(
                Arguments.of("only empty bullet points", """
                        -
                        *
                        •
                        """),
                Arguments.of("numbers without content", """
                        1.
                        2.
                        3.
                        """)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyResultCases")
    void edgeCases_returnEmptyList(String description, String input) {
        assertTrue(LlmResponseParser.extractNumberedList(input).isEmpty());
    }

    static Stream<Arguments> singleItemCases() {
        return Stream.of(
                Arguments.of("single numbered item", "1. Single question", List.of("Single question")),
                Arguments.of("single bulleted item", "- Single question", List.of("Single question"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleItemCases")
    void singleItem(String description, String input, List<String> expected) {
        assertEquals(expected, LlmResponseParser.extractNumberedList(input));
    }

    static Stream<Arguments> realWorldCases() {
        return Stream.of(
                Arguments.of("ChatGPT-style numbered with intro",
                        """
                                Here are 3 follow-up questions based on the conversation:

                                1. What are the key differences between the approaches?
                                2. How can this be applied in practice?
                                3. What are the potential limitations?
                                """,
                        List.of(
                                "What are the key differences between the approaches?",
                                "How can this be applied in practice?",
                                "What are the potential limitations?")),

                Arguments.of("bulleted with intro and outro prose",
                        """
                                Based on the discussion, here are some relevant questions:

                                - How does this compare to traditional methods?
                                - What are the performance implications?
                                - Are there any security considerations?

                                Feel free to explore these topics further.
                                """,
                        List.of(
                                "How does this compare to traditional methods?",
                                "What are the performance implications?",
                                "Are there any security considerations?")),

                Arguments.of("numbered with all items quoted",
                        """
                                1. "What is the main hypothesis?"
                                2. "How was the data collected?"
                                3. "What are the key findings?"
                                """,
                        List.of(
                                "What is the main hypothesis?",
                                "How was the data collected?",
                                "What are the key findings?"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("realWorldCases")
    void realWorld(String description, String input, List<String> expected) {
        assertEquals(expected, LlmResponseParser.extractNumberedList(input));
    }

    @Test
    void noLimit_extractsAllTenItems() {
        String input = """
                1. Question 1
                2. Question 2
                3. Question 3
                4. Question 4
                5. Question 5
                6. Question 6
                7. Question 7
                8. Question 8
                9. Question 9
                10. Question 10
                """;
        List<String> result = LlmResponseParser.extractNumberedList(input);
        assertEquals(10, result.size());
        assertEquals("Question 1", result.getFirst());
        assertEquals("Question 10", result.getLast());
    }
}
