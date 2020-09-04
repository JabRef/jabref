package org.jabref.logic.formatter.casechanger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class SentenceCaseFormatterTest {

    private SentenceCaseFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new SentenceCaseFormatter();
    }

    @Test
    public void test() {
        assertEquals("Upper first", formatter.format("upper First"));
        assertEquals("Upper first", formatter.format("uPPER FIRST"));
        assertEquals("Upper {NOT} first", formatter.format("upper {NOT} FIRST"));
        assertEquals("Upper {N}ot first", formatter.format("upper {N}OT FIRST"));
    }

    @Test
    public void secondSentenceAfterQuestionMarkGetsCapitalized() {
        assertEquals("Whose music? A sociology of musical language",
                     formatter.format("Whose music? a sociology of musical language"));
    }

    @Test
    public void testSimpleTwoSentenceTitle() {
        assertEquals("Bibliographic software. A comparison.",
                     formatter.format("bibliographic software. a comparison."));
    }

    @Test
    public void sentenceAfterSemicolonGetsCapitalized() {
        assertEquals("England’s monitor; The history of the separation",
                     formatter.format("England’s Monitor; the History of the Separation"));
    }

    @Test
    public void commonAbbreviationIsNotTreatedAsSeperateSentence() {
        assertEquals("Dr. schultz: a dentist turned bounty hunter.",
                     formatter.format("Dr. schultz: a dentist turned bounty hunter."));
    }

    @Test
    public void secondSentenceInBracketsIsLeftUnchanged() {
        assertEquals("Example case. {EXCLUDED SENTENCE.}",
                     formatter.format("Example case. {EXCLUDED SENTENCE.}"));
    }

    @Test
    public void formatExample() {
        assertEquals("I have {Aa} dream", formatter.format(formatter.getExampleInput()));
    }
}
