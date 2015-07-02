package net.sf.jabref.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CaseChangersTest {

    @Test
    public void testNumberOfModes() {
        Assert.assertEquals("lower", CaseChangers.LOWER.getName());
        Assert.assertEquals("UPPER", CaseChangers.UPPER.getName());
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.getName());
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.getName());
        Assert.assertEquals("Title", CaseChangers.TITLE.getName());
    }

    @Test
    public void testChangeCaseLower() {
        Assert.assertEquals("", CaseChangers.LOWER.changeCase(""));
        Assert.assertEquals("lower", CaseChangers.LOWER.changeCase("LOWER"));
    }

    @Test
    public void testChangeCaseUpper() {
        Assert.assertEquals("", CaseChangers.UPPER.changeCase(""));
        Assert.assertEquals("LOWER", CaseChangers.UPPER.changeCase("LOWER"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.changeCase("upper"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.changeCase("UPPER"));
    }

    @Test
    public void testChangeCaseUpperFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_FIRST.changeCase(""));
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.changeCase("upper First"));
    }

    @Test
    public void testChangeCaseUpperEachFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_EACH_FIRST.changeCase(""));
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.changeCase("upper each First"));
    }

    @Test
    public void testChangeCaseTitle() {
        Assert.assertEquals("", CaseChangers.TITLE.changeCase(""));
        Assert.assertEquals("Upper Each First", CaseChangers.TITLE.changeCase("upper each first"));
        Assert.assertEquals("An Upper Each First And", CaseChangers.TITLE.changeCase("an upper each first and"));
        Assert.assertEquals("An Upper Each of the and First And", CaseChangers.TITLE.changeCase("an upper each of the and first and"));
    }
}
