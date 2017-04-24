package org.jabref.logic.formatter.casechanger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class CapitalizeFormatterTest {

    private CapitalizeFormatter formatter;

    @Before
    public void setUp() {
        formatter = new CapitalizeFormatter();
    }

    @Test
    public void test() {
        Assert.assertEquals("Upper Each First", formatter.format("upper each First"));
        Assert.assertEquals("Upper Each First {NOT} {this}", formatter.format("upper each first {NOT} {this}"));
        Assert.assertEquals("Upper Each First {N}ot {t}his", formatter.format("upper each first {N}OT {t}his"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("I Have {a} Dream", formatter.format(formatter.getExampleInput()));
    }

}
