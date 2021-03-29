package org.jabref.model.search.rules;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SentenceAnalyzerTest {

    @Test
    public void testGetWords() {
        assertEquals(Arrays.asList("a", "b"), new SentenceAnalyzer("a b").getWords(), "Sentence was not parsed correctly.");
        assertEquals(Arrays.asList("a", "b"), new SentenceAnalyzer(" a b ").getWords(), "Sentence was not parsed correctly. Check leading and trailing spaces.");
        assertEquals(Collections.singletonList("b "), new SentenceAnalyzer("\"b \" ").getWords(), "Sentence was not parsed correctly. Check escaped characters and trailing spaces.");
        assertEquals(Collections.singletonList(" a"), new SentenceAnalyzer(" \\ a").getWords(), "Sentence was not parsed correctly. Check escaped characters and leading spaces.");
    }
}
