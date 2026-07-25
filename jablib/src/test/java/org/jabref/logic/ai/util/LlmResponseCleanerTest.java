package org.jabref.logic.ai.util;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmResponseCleanerTest {

    static Stream<Arguments> noFenceCases() {
        return Stream.of(
                Arguments.of("plain text with surrounding spaces", "  Hello, world!  ", "Hello, world!"),
                Arguments.of("already trimmed text", "Just a sentence.", "Just a sentence."),
                Arguments.of("multiline with leading/trailing newlines", "\n  line1\n  line2\n", "line1\n  line2"),
                Arguments.of("empty string", "", ""),
                Arguments.of("whitespace only", "   \n\t  ", "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noFenceCases")
    void noFences(String description, String input, String expected) {
        assertEquals(expected, LlmResponseCleaner.clean(input));
    }

    @Test
    void nofences_null_returnsEmpty() {
        assertEquals("", LlmResponseCleaner.clean(null));
    }

    static Stream<Arguments> singleBlockCases() {
        return Stream.of(
                Arguments.of("no label", "```\nHello\n```", "Hello"),
                Arguments.of("no label with internal newlines", "```\nline1\nline2\nline3\n```", "line1\nline2\nline3"),
                Arguments.of("with leading text", "Here is the result:\n```\ncontent\n```", "content"),
                Arguments.of("with trailing text", "```\ncontent\n```\nSome trailing note.", "content"),
                Arguments.of("json label stripped", "```json\n{\"key\": \"value\"}\n```", "{\"key\": \"value\"}"),
                Arguments.of("markdown label stripped", "```markdown\n# Title\nBody text.\n```", "# Title\nBody text."),
                Arguments.of("java label stripped", "```java\npublic class Foo {}\n```", "public class Foo {}"),
                Arguments.of("xml label stripped", "```xml\n<root/>\n```", "<root/>"),
                Arguments.of("label with surrounding spaces stripped", "```  json  \n{}\n```", "{}"),
                Arguments.of("empty block", "```\n```", ""),
                Arguments.of("empty block with label", "```json\n```", ""),
                Arguments.of("internal indentation preserved", "```\n  indented line\n    more indent\n```", "  indented line\n    more indent"),
                Arguments.of("fence on same line as content", "```json```", ""),
                Arguments.of("surrounding whitespace stripped", "  ```\n  content  \n```  ", "content")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleBlockCases")
    void singleBlock(String description, String input, String expected) {
        assertEquals(expected, LlmResponseCleaner.clean(input));
    }

    static Stream<Arguments> multipleBlockCases() {
        return Stream.of(
                Arguments.of("returns last of two blocks",
                        "```\nfirst block\n```\nsome text\n```\nsecond block\n```",
                        "second block"),
                Arguments.of("returns last of three blocks",
                        "```\nA\n```\n```\nB\n```\n```\nC\n```",
                        "C"),
                Arguments.of("last block has label, label stripped",
                        "```\nfirst\n```\n```json\n{\"x\":1}\n```",
                        "{\"x\":1}"),
                Arguments.of("first has label, last does not",
                        "```json\n{}\n```\n```\nplain content\n```",
                        "plain content")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multipleBlockCases")
    void multipleBlocks(String description, String input, String expected) {
        assertEquals(expected, LlmResponseCleaner.clean(input));
    }

    static Stream<Arguments> unclosedFenceCases() {
        return Stream.of(
                Arguments.of("no label, returns content after fence line",
                        "```\nsome content without closing",
                        "some content without closing"),
                Arguments.of("with label, returns content after fence line",
                        "```json\n{\"partial\": true",
                        "{\"partial\": true"),
                Arguments.of("after a closed block, unclosed treated as last opener",
                        "```\nfirst\n```\n```\nunclosed content",
                        "unclosed content")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unclosedFenceCases")
    void unclosedFence(String description, String input, String expected) {
        assertEquals(expected, LlmResponseCleaner.clean(input));
    }

    static Stream<Arguments> realWorldCases() {
        return Stream.of(
                Arguments.of("JSON response with prose around it",
                        "Sure! Here is the JSON you requested:\n\n```json\n{\n  \"name\": \"Alice\",\n  \"age\": 30\n}\n```\n\nLet me know if you need anything else.\n",
                        "{\n  \"name\": \"Alice\",\n  \"age\": 30\n}"),
                Arguments.of("markdown response with prose before it",
                        "Here's a summary:\n\n```markdown\n# Summary\n\n- Point A\n- Point B\n```",
                        "# Summary\n\n- Point A\n- Point B")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("realWorldCases")
    void realWorld(String description, String input, String expected) {
        assertEquals(expected, LlmResponseCleaner.clean(input));
    }
}
