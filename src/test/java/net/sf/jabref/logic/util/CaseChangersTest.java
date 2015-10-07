package net.sf.jabref.logic.util;

import net.sf.jabref.logic.util.strings.CaseChangers;
import org.junit.Assert;
import org.junit.Test;

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
        Assert.assertEquals("lower {UPPER}", CaseChangers.LOWER.changeCase("LOWER {UPPER}"));
    }

    @Test
    public void testChangeCaseUpper() {
        Assert.assertEquals("", CaseChangers.UPPER.changeCase(""));
        Assert.assertEquals("LOWER", CaseChangers.UPPER.changeCase("LOWER"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.changeCase("upper"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.changeCase("UPPER"));
        Assert.assertEquals("UPPER {lower}", CaseChangers.UPPER.changeCase("upper {lower}"));
        }

    @Test
    public void testChangeCaseUpperFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_FIRST.changeCase(""));
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.changeCase("upper First"));
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.changeCase("uPPER FIRST"));
        Assert.assertEquals("Upper {NOT} first", CaseChangers.UPPER_FIRST.changeCase("upper {NOT} FIRST"));

    }

    @Test
    public void testChangeCaseUpperEachFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_EACH_FIRST.changeCase(""));
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.changeCase("upper each First"));
        Assert.assertEquals("Upper Each First {NOT} {this}", CaseChangers.UPPER_EACH_FIRST.changeCase("upper each first {NOT} {this}"));
    }

    @Test
    public void testChangeCaseTitle() {
        Assert.assertEquals("", CaseChangers.TITLE.changeCase(""));
        Assert.assertEquals("Upper Each First", CaseChangers.TITLE.changeCase("upper each first"));
        Assert.assertEquals("An Upper Each First And", CaseChangers.TITLE.changeCase("an upper each first and"));
        Assert.assertEquals("An Upper Each of the and First And", CaseChangers.TITLE.changeCase("an upper each of the and first and"));
        Assert.assertEquals("An Upper First with and without {CURLY} {brackets}", CaseChangers.TITLE.changeCase("AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"));
    }
}
