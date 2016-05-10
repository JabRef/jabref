package net.sf.jabref.logic.search.rules;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SentenceAnalyzerTest {

    @Test
    public void testGetWords() {
        assertEquals(Arrays.asList("a","b"), new SentenceAnalyzer("a b").getWords());
        assertEquals(Arrays.asList("a","b"), new SentenceAnalyzer(" a b ").getWords());
        assertEquals(Collections.singletonList("b "), new SentenceAnalyzer("\"b \" ").getWords());
        assertEquals(Collections.singletonList(" a"), new SentenceAnalyzer(" \\ a").getWords());
    }

}