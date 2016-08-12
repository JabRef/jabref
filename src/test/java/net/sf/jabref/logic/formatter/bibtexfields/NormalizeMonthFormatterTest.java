package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
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

}