package org.jabref.gui.util.component;

import java.util.List;

import org.jabref.logic.ai.chatting.util.JsonAnswerFormatter;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->feat~ai.chat.json-highlighting~1]
@NullMarked
class JsonHighlighterTest {

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
    void tokenizeKeepsTheTextCompleteWithSupplementaryCharacters() {
        String json = JsonAnswerFormatter.leadingJson("{\"a\": \"\uD83D\uDE00\", \"b\": 1}").orElseThrow().json();
        assertEquals(json, JsonHighlighter.tokenize(json).stream().map(JsonHighlighter.Segment::text).reduce("", String::concat));
    }

    @Test
    void tokenizeStylesQuotationsInsideStrings() {
        assertEquals(List.of(
                        new JsonHighlighter.Segment("\"a ", "json-string"),
                        new JsonHighlighter.Segment("\\\"b\\\"", "json-string-quotation"),
                        new JsonHighlighter.Segment(" \\\\ c\"", "json-string")),
                JsonHighlighter.tokenize("\"a \\\"b\\\" \\\\ c\""));
    }

    @Test
    void tokenizeKeepsTheTextComplete() {
        String json = "{\n  \"a\": [1, null]\n}";
        assertEquals(json, JsonHighlighter.tokenize(json).stream().map(JsonHighlighter.Segment::text).reduce("", String::concat));
    }
}
