package net.sf.jabref.logic.formatter.casechanger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class TitleCaseFormatterTest {

    private TitleCaseFormatter formatter;

    @Before
    public void setUp() {
        formatter = new TitleCaseFormatter();
    }

    @Test
    public void upperEachFirst() {
        Assert.assertEquals("Upper Each First", formatter.format("upper each first"));
    }

    @Test
    public void anUpperEachFirstAnd() {
        Assert.assertEquals("An Upper Each First And", formatter.format("an upper each first and"));
    }

    @Test
    public void anUpperEachOfTheAndFirstAnd1(){
        Assert.assertEquals("An Upper Each of the and First And",
                formatter.format("an upper each of the and first and"));
    }

    @Test
    public void anUpperEachOfTheAndFirstAnd2() {
        Assert.assertEquals("An Upper Each of: The and First And",
                formatter.format("an upper each of: the and first and"));
    }

    @Test
    public void anUpperEachFirstWithAndWithoutCurlyBrackets1() {
        Assert.assertEquals("An Upper First with and without {CURLY} {brackets}",
                formatter.format("AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"));
    }

    @Test
    public void anUpperEachFirstWithAndWithoutCurlyBrackets2() {
        Assert.assertEquals("An Upper First with {A}nd without {C}urly {b}rackets",
                formatter.format("AN UPPER FIRST WITH {A}ND WITHOUT {C}URLY {b}rackets"));
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
