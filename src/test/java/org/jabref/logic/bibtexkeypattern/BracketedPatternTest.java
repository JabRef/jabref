package org.jabref.logic.bibtexkeypattern;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BracketedPatternTest {
    private static final String TITLE_STRING_THE_INTERESTING_TITLE = "The Interesting Title";

    /**
     * Test the [:truncate] modifier
     */
    @Test
    void applyTruncate4Modifier() {
        assertEquals("The",
                BracketedPattern.applyModifiers(TITLE_STRING_THE_INTERESTING_TITLE, Collections.singletonList("truncate4"), 0));
    }

    @Test
    void applyTruncate5Modifier() {
        assertEquals("The I",
                BracketedPattern.applyModifiers(TITLE_STRING_THE_INTERESTING_TITLE, Collections.singletonList("truncate5"), 0));
    }

    @Test
    void applyShortTruncateModifier() {
        assertEquals("",
                BracketedPattern.applyModifiers(TITLE_STRING_THE_INTERESTING_TITLE, Collections.singletonList("truncate0"), 0));
    }

    @Test
    void applyLongTruncateModifier() {
        assertEquals(TITLE_STRING_THE_INTERESTING_TITLE,
                BracketedPattern.applyModifiers(TITLE_STRING_THE_INTERESTING_TITLE, Collections.singletonList("truncate99"), 0));
    }

    @Test
    void applyTwoTruncateModifiers() {
        assertEquals("The",
                BracketedPattern.applyModifiers(TITLE_STRING_THE_INTERESTING_TITLE, Arrays.asList("truncate5", "truncate4"), 0));
    }

    @Test
    void applyEmptyTruncateModifiers() {
        assertEquals(TITLE_STRING_THE_INTERESTING_TITLE,
                BracketedPattern.applyModifiers(TITLE_STRING_THE_INTERESTING_TITLE, Collections.singletonList("truncate"), 0));
    }
}
