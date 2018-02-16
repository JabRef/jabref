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
    public void formatExample() {
        assertEquals("I have {Aa} dream", formatter.format(formatter.getExampleInput()));
    }

}
