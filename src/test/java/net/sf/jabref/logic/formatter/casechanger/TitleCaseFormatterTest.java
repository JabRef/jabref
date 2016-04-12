package net.sf.jabref.logic.formatter.casechanger;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class TitleCaseFormatterTest {

    private final TitleCaseFormatter formatter = new TitleCaseFormatter();

    @Test
    public void test() {
        Assert.assertEquals("Upper Each First", formatter.format("upper each first"));
        Assert.assertEquals("An Upper Each First And", formatter.format("an upper each first and"));
        Assert.assertEquals("An Upper Each of the and First And",
                formatter.format("an upper each of the and first and"));
        Assert.assertEquals("An Upper Each of: The and First And",
                formatter.format("an upper each of: the and first and"));
        Assert.assertEquals("An Upper First with and without {CURLY} {brackets}",
                formatter.format("AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"));
        Assert.assertEquals("An Upper First with {A}nd without {C}urly {b}rackets",
                formatter.format("AN UPPER FIRST WITH {A}ND WITHOUT {C}URLY {b}rackets"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("{BPMN} Conformance in Open Source Engines", formatter.format(formatter.getExampleInput()));
    }
}
