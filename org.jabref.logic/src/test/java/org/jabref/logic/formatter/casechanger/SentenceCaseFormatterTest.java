package org.jabref.logic.formatter.casechanger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class SentenceCaseFormatterTest {

    private SentenceCaseFormatter formatter;

    @Before
    public void setUp() {
        formatter = new SentenceCaseFormatter();
    }

    @Test
    public void test() {
        Assert.assertEquals("Upper first", formatter.format("upper First"));
        Assert.assertEquals("Upper first", formatter.format("uPPER FIRST"));
        Assert.assertEquals("Upper {NOT} first", formatter.format("upper {NOT} FIRST"));
        Assert.assertEquals("Upper {N}ot first", formatter.format("upper {N}OT FIRST"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("I have {Aa} dream", formatter.format(formatter.getExampleInput()));
    }

}
