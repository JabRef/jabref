package org.jabref.logic.ai.chatting.util;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->feat~ai.chat.json-highlighting~1]
@NullMarked
class JsonAnswerFormatterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "[1, 2, 3]",
            "  {\"a\": [true, null, -1.5e3]}\n",
            "{\"a\": {\"b\": \"c\"}}"
    })
    void recognizesJson(String text) {
        assertTrue(JsonAnswerFormatter.leadingJson(text).isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "The answer is 42.",
            "{\"a\": }",
            "42",
            "\"just a string\"",
            ""
    })
    void rejectsNonJson(String text) {
        assertEquals(Optional.empty(), JsonAnswerFormatter.leadingJson(text));
    }

    @Test
    void leadingJsonSeparatesTheExplanationFollowingTheJson() {
        JsonAnswerFormatter.LeadingJson leadingJson = JsonAnswerFormatter.leadingJson("{\"a\": 1}\n\nThe answer is *one*.").orElseThrow();

        assertEquals("{\n  \"a\": 1\n}", leadingJson.json());
        assertEquals("The answer is *one*.", leadingJson.rest());
    }

    @Test
    void leadingJsonKeepsTheIndentationOfTheExplanation() {
        JsonAnswerFormatter.LeadingJson leadingJson = JsonAnswerFormatter.leadingJson("{\"a\": 1}\n\n    code line\n").orElseThrow();

        assertEquals("    code line", leadingJson.rest());
    }

    @Test
    void fenceJsonFencesJsonFollowedByAnExplanation() {
        assertEquals("```json\n{\n  \"a\": 1\n}\n```\n\nThe answer is *one*.",
                JsonAnswerFormatter.fenceJson("{\"a\": 1}\n\nThe answer is *one*."));
    }

    @Test
    void fenceJsonFencesJsonFollowingAnExplanation() {
        assertEquals("The result:\n\n```json\n{\n  \"a\": 1\n}\n```",
                JsonAnswerFormatter.fenceJson("The result:\n{\"a\": 1}\n"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Plain text with a [link](entries/Key).",
            "[@Key](entries/Key) starts this line.",
            "```json\n{\"a\": 1}\n```",
            "Explained first.\n{\"a\": 1}\nExplained last."
    })
    void fenceJsonLeavesOtherTextAlone(String markdown) {
        assertEquals(markdown, JsonAnswerFormatter.fenceJson(markdown));
    }

    @Test
    void prettyPrintIndentsObjectsAndArrays() {
        assertEquals("""
                        {
                          "a": [
                            1,
                            2
                          ],
                          "b": {
                            "c": true
                          }
                        }""",
                JsonAnswerFormatter.leadingJson("{\"a\":[1,2],\"b\":{\"c\":true}}").orElseThrow().json());
    }

    @Test
    void prettyPrintKeepsAllDigitsOfDecimals() {
        assertEquals("{\n  \"a\": 0.123456789012345678901234567890\n}",
                JsonAnswerFormatter.leadingJson("{\"a\": 0.123456789012345678901234567890}").orElseThrow().json());
    }

    @Test
    void prettyPrintKeepsHugeExponentsShort() {
        assertEquals("{\n  \"n\": 1E+100000000\n}",
                JsonAnswerFormatter.leadingJson("{\"n\": 1e100000000}").orElseThrow().json());
    }

    @Test
    void leadingJsonAcceptsAnUnescapedLineBreakInAString() {
        assertEquals("{\n  \"a\": \"first\\nsecond\"\n}",
                JsonAnswerFormatter.leadingJson("{\"a\": \"first\nsecond\"}").orElseThrow().json());
    }

    @Test
    void prettyPrintRejectsDuplicateNamesInsteadOfDroppingThem() {
        assertEquals(Optional.empty(), JsonAnswerFormatter.leadingJson("{\"a\": 1, \"a\": 2}"));
    }
}
