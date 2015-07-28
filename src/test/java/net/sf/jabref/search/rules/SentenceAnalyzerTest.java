package net.sf.jabref.search.rules;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class SentenceAnalyzerTest {

    @Test
    public void testGetWords() throws Exception {
        assertEquals(Arrays.asList("a","b"), new BasicSearchRule.SentenceAnalyzer("a b").getWords());
        assertEquals(Arrays.asList("a","b"), new BasicSearchRule.SentenceAnalyzer(" a b ").getWords());
        assertEquals(Collections.singletonList("b "), new BasicSearchRule.SentenceAnalyzer("\"b \" ").getWords());
        assertEquals(Collections.singletonList(" a"), new BasicSearchRule.SentenceAnalyzer(" \\ a").getWords());
    }
    
}