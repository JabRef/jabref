package org.jabref.model.search.rules;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SentenceAnalyzerTest {

    @Test
    public void testGetWords() {
        assertEquals(Arrays.asList("a","b"), new SentenceAnalyzer("a b").getWords());
        assertEquals(Arrays.asList("a","b"), new SentenceAnalyzer(" a b ").getWords());
        assertEquals(Collections.singletonList("b "), new SentenceAnalyzer("\"b \" ").getWords());
        assertEquals(Collections.singletonList(" a"), new SentenceAnalyzer(" \\ a").getWords());
    }

}
