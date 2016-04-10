package net.sf.jabref.logic.formatter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class CaseChangersTest {

    @Test
    public void testChangeCaseLower() {
        Assert.assertEquals("lower", CaseChangers.TO_LOWER_CASE.format("LOWER"));
        Assert.assertEquals("lower {UPPER}", CaseChangers.TO_LOWER_CASE.format("LOWER {UPPER}"));
        Assert.assertEquals("lower {U}pper", CaseChangers.TO_LOWER_CASE.format("LOWER {U}PPER"));
    }

    @Test
    public void testChangeCaseUpper() {
        Assert.assertEquals("LOWER", CaseChangers.TO_UPPER_CASE.format("LOWER"));
        Assert.assertEquals("UPPER", CaseChangers.TO_UPPER_CASE.format("upper"));
        Assert.assertEquals("UPPER", CaseChangers.TO_UPPER_CASE.format("UPPER"));
        Assert.assertEquals("UPPER {lower}", CaseChangers.TO_UPPER_CASE.format("upper {lower}"));
        Assert.assertEquals("UPPER {l}OWER", CaseChangers.TO_UPPER_CASE.format("upper {l}ower"));
    }

    @Test
    public void testChangeCaseUpperFirst() {
        Assert.assertEquals("Upper first", CaseChangers.TO_SENTENCE_CASE.format("upper First"));
        Assert.assertEquals("Upper first", CaseChangers.TO_SENTENCE_CASE.format("uPPER FIRST"));
        Assert.assertEquals("Upper {NOT} first", CaseChangers.TO_SENTENCE_CASE.format("upper {NOT} FIRST"));
        Assert.assertEquals("Upper {N}ot first", CaseChangers.TO_SENTENCE_CASE.format("upper {N}OT FIRST"));
    }

    @Test
    public void testChangeCaseUpperEachFirst() {
        Assert.assertEquals("Upper Each First", CaseChangers.CAPITALIZE.format("upper each First"));
        Assert.assertEquals("Upper Each First {NOT} {this}", CaseChangers.CAPITALIZE.format("upper each first {NOT} {this}"));
        Assert.assertEquals("Upper Each First {N}ot {t}his", CaseChangers.CAPITALIZE.format("upper each first {N}OT {t}his"));
    }

    @Test
    public void testChangeCaseTitle() {
        Assert.assertEquals("Upper Each First", CaseChangers.TO_TITLE_CASE.format("upper each first"));
        Assert.assertEquals("An Upper Each First And", CaseChangers.TO_TITLE_CASE.format("an upper each first and"));
        Assert.assertEquals("An Upper Each of the and First And", CaseChangers.TO_TITLE_CASE.format("an upper each of the and first and"));
        Assert.assertEquals("An Upper Each of: The and First And", CaseChangers.TO_TITLE_CASE.format("an upper each of: the and first and"));
        Assert.assertEquals("An Upper First with and without {CURLY} {brackets}", CaseChangers.TO_TITLE_CASE.format("AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"));
        Assert.assertEquals("An Upper First with {A}nd without {C}urly {b}rackets", CaseChangers.TO_TITLE_CASE.format("AN UPPER FIRST WITH {A}ND WITHOUT {C}URLY {b}rackets"));
    }
}
