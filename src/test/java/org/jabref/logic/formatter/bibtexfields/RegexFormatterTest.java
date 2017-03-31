package org.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class RegexFormatterTest {

    private RegexFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RegexFormatter();
    }

    @Test
    public void test() {
        String regexInput = "(\" \",\"-\")";
        formatter.setRegex(regexInput);
        Assert.assertEquals("replace-all-spaces", formatter.format("replace all spaces"));
        Assert.assertEquals("replace-spaces-{not these ones}", formatter.format("replace spaces {not these ones}"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("Please-replace-the-spaces", formatter.format(formatter.getExampleInput()));
    }

}
