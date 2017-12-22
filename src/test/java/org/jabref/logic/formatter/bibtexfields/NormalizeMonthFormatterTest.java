package org.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeMonthFormatterTest {

    private NormalizeMonthFormatter formatter;

    @Before
    public void setUp() {
        formatter = new NormalizeMonthFormatter();
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("#dec#", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void formatGermanMarch() {
        Assert.assertEquals("#mar#", formatter.format("Maerz"));
    }

    @Test
    public void formatGermanMarchInUtf8() {
        Assert.assertEquals("#mar#", formatter.format("März"));
    }

    @Test
    public void formatGermanMarchInLaTeX() {
        Assert.assertEquals("#mar#", formatter.format("M\\\"arz"));
    }

    @Test
    public void formatGermanMarchInLaTeXWithBraces() {
        Assert.assertEquals("#mar#", formatter.format("M{\\\"a}rz"));
    }

}
