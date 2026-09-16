package org.jabref.gui.util.component;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [utest->feat~ai.chat.json-highlighting~1]
class JsonHighlighterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "[1, 2, 3]",
            "  {\"a\": [true, null, -1.5e3]}\n",
            "{\"a\": {\"b\": \"c\"}}"
    })
    void recognizesJson(String text) {
        assertTrue(JsonHighlighter.prettyPrint(text).isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "The answer is 42.",
            "{\"a\": }",
            "{\"a\": 1} trailing text",
            "42",
            "\"just a string\"",
            ""
    })
    void rejectsNonJson(String text) {
        assertEquals(Optional.empty(), JsonHighlighter.prettyPrint(text));
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
                JsonHighlighter.prettyPrint("{\"a\":[1,2],\"b\":{\"c\":true}}").orElseThrow());
    }

    @Test
    void tokenizeStylesKeysValuesAndPunctuation() {
        assertEquals(List.of(
                        new JsonHighlighter.Segment("{", "json-punctuation"),
                        new JsonHighlighter.Segment("\"a\"", "json-key"),
                        new JsonHighlighter.Segment(":", "json-punctuation"),
                        new JsonHighlighter.Segment(" ", ""),
                        new JsonHighlighter.Segment("\"b\"", "json-string"),
                        new JsonHighlighter.Segment(",", "json-punctuation"),
                        new JsonHighlighter.Segment(" ", ""),
                        new JsonHighlighter.Segment("\"n\"", "json-key"),
                        new JsonHighlighter.Segment(":", "json-punctuation"),
                        new JsonHighlighter.Segment(" ", ""),
                        new JsonHighlighter.Segment("1.5", "json-number"),
                        new JsonHighlighter.Segment(",", "json-punctuation"),
                        new JsonHighlighter.Segment(" ", ""),
                        new JsonHighlighter.Segment("\"t\"", "json-key"),
                        new JsonHighlighter.Segment(":", "json-punctuation"),
                        new JsonHighlighter.Segment(" ", ""),
                        new JsonHighlighter.Segment("true", "json-literal"),
                        new JsonHighlighter.Segment("}", "json-punctuation")),
                JsonHighlighter.tokenize("{\"a\": \"b\", \"n\": 1.5, \"t\": true}"));
    }

    @Test
    void tokenizeKeepsTheTextComplete() {
        String json = "{\n  \"a\": [1, null]\n}";
        assertEquals(json, JsonHighlighter.tokenize(json).stream().map(JsonHighlighter.Segment::text).reduce("", String::concat));
    }
}
