package net.sf.jabref.logic.formatter;

import org.junit.Assert;
import org.junit.Test;

public class CaseChangersTest {

    @Test
    public void testNumberOfModes() {
        Assert.assertEquals("lower", CaseChangers.LOWER.getName()); // equals: FORMAT_MODE.ALL_LOWERS
        Assert.assertEquals("UPPER", CaseChangers.UPPER.getName()); // equals: FORMAT_MODE.ALL_UPPERS
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.getName()); // equals: FORMAT_MODE.TITLE_LOWERS
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.getName()); // equals: FORMAT_MODE.EACH_FIRST_UPPERS
        Assert.assertEquals("Title", CaseChangers.TITLE.getName()); // equals: FORMAT_MODE.TITLE_UPPERS
    }

    @Test
    public void testChangeCaseLower() {
        Assert.assertEquals("", CaseChangers.LOWER.format(""));
        Assert.assertEquals("lower", CaseChangers.LOWER.format("LOWER"));
        Assert.assertEquals("lower {UPPER}", CaseChangers.LOWER.format("LOWER {UPPER}"));
        Assert.assertEquals("lower {U}pper", CaseChangers.LOWER.format("LOWER {U}PPER"));
    }

    @Test
    public void testChangeCaseUpper() {
        Assert.assertEquals("", CaseChangers.UPPER.format(""));
        Assert.assertEquals("LOWER", CaseChangers.UPPER.format("LOWER"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.format("upper"));
        Assert.assertEquals("UPPER", CaseChangers.UPPER.format("UPPER"));
        Assert.assertEquals("UPPER {lower}", CaseChangers.UPPER.format("upper {lower}"));
        Assert.assertEquals("UPPER {l}OWER", CaseChangers.UPPER.format("upper {l}ower"));
    }

    @Test
    public void testChangeCaseUpperFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_FIRST.format(""));
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.format("upper First"));
        Assert.assertEquals("Upper first", CaseChangers.UPPER_FIRST.format("uPPER FIRST"));
        Assert.assertEquals("Upper {NOT} first", CaseChangers.UPPER_FIRST.format("upper {NOT} FIRST"));
        Assert.assertEquals("Upper {N}ot first", CaseChangers.UPPER_FIRST.format("upper {N}OT FIRST"));
    }

    @Test
    public void testChangeCaseUpperEachFirst() {
        Assert.assertEquals("", CaseChangers.UPPER_EACH_FIRST.format(""));
        Assert.assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.format("upper each First"));
        Assert.assertEquals("Upper Each First {NOT} {this}", CaseChangers.UPPER_EACH_FIRST.format("upper each first {NOT} {this}"));
        Assert.assertEquals("Upper Each First {N}ot {t}his", CaseChangers.UPPER_EACH_FIRST.format("upper each first {N}OT {t}his"));
    }

    @Test
    public void testChangeCaseTitle() {
        Assert.assertEquals("", CaseChangers.TITLE.format(""));
        Assert.assertEquals("Upper Each First", CaseChangers.TITLE.format("upper each first"));
        Assert.assertEquals("An Upper Each First And", CaseChangers.TITLE.format("an upper each first and"));
        Assert.assertEquals("An Upper Each of the and First And", CaseChangers.TITLE.format("an upper each of the and first and"));
        Assert.assertEquals("An Upper Each of: The and First And", CaseChangers.TITLE.format("an upper each of: the and first and"));
        Assert.assertEquals("An Upper First with and without {CURLY} {brackets}", CaseChangers.TITLE.format("AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"));
        Assert.assertEquals("An Upper First with {A}nd without {C}urly {b}rackets", CaseChangers.TITLE.format("AN UPPER FIRST WITH {A}ND WITHOUT {C}URLY {b}rackets"));
    }
}
