package org.jabref.logic.formatter.casechanger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class TitleCaseFormatterTest {

    private TitleCaseFormatter formatter;

    @Before
    public void setUp() {
        formatter = new TitleCaseFormatter();
    }

    @Test
    public void eachFirstLetterIsUppercased() {
        Assert.assertEquals("Upper Each First", formatter.format("upper each first"));
    }

    @Test
    public void eachFirstLetterIsUppercasedAndOthersLowercased() {
        Assert.assertEquals("Upper Each First", formatter.format("upper eACH first"));
    }

    @Test
    public void eachFirstLetterIsUppercasedAndATralingAndIsAlsoUppercased() {
        Assert.assertEquals("An Upper Each First And", formatter.format("an upper each first and"));
    }

    @Test
    public void eachFirstLetterIsUppercasedAndATralingAndIsAlsoCorrectlyCased() {
        Assert.assertEquals("An Upper Each First And", formatter.format("an upper each first AND"));
    }

    @Test
    public void eachFirstLetterIsUppercasedButIntermediateAndsAreKeptLowercase(){
        Assert.assertEquals("An Upper Each of the and First And",
                formatter.format("an upper each of the and first and"));
    }

    @Test
    public void eachFirstLetterIsUppercasedButIntermediateAndsArePutLowercase(){
        Assert.assertEquals("An Upper Each of the and First And",
                formatter.format("an upper each of the AND first and"));
    }

    @Test
    public void theAfterColonGetsCapitalized() {
        Assert.assertEquals("An Upper Each of: The and First And",
                formatter.format("an upper each of: the and first and"));
    }

    @Test
    public void completeWordsInCurlyBracketsIsLeftUnchanged() {
        Assert.assertEquals("An Upper First with and without {CURLY} {brackets}",
                formatter.format("AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"));
    }

    @Test
    public void lettersInCurlyBracketsIsLeftUnchanged() {
        Assert.assertEquals("An Upper First with {A}nd without {C}urly {b}rackets",
                formatter.format("AN UPPER FIRST WITH {A}ND WITHOUT {C}URLY {b}rackets"));
    }

    @Test
    public void intraWordLettersInCurlyBracketsIsLeftUnchanged() {
        Assert.assertEquals("{b}rackets {b}rac{K}ets Brack{E}ts",
                formatter.format("{b}RaCKeTS {b}RaC{K}eTS bRaCK{E}ts"));
    }

    @Test
    public void testTwoExperiencesTitle() {
        Assert.assertEquals(
            "Two Experiences Designing for Effective Security",
            formatter.format("Two experiences designing for effective security"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("{BPMN} Conformance in Open Source Engines", formatter.format(formatter.getExampleInput()));
    }

}
