package net.sf.jabref.logic.formatter.casechanger;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class LowerCaseFormatterTest {

    private final LowerCaseFormatter formatter = new LowerCaseFormatter();

    @Test
    public void test() {
        Assert.assertEquals("lower", formatter.format("LOWER"));
        Assert.assertEquals("lower {UPPER}", formatter.format("LOWER {UPPER}"));
        Assert.assertEquals("lower {U}pper", formatter.format("LOWER {U}PPER"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("kde {Amarok}", formatter.format(formatter.getExampleInput()));
    }

}
