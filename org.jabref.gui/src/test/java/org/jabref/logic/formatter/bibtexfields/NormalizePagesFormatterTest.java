package org.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizePagesFormatterTest {

    private NormalizePagesFormatter formatter;

    @Before
    public void setUp() {
        formatter = new NormalizePagesFormatter();
    }

    @Test
    public void formatSinglePageResultsInNoChange() {
        Assert.assertEquals("1", formatter.format("1"));
    }

    @Test
    public void formatPageNumbers() {
        Assert.assertEquals("1--2", formatter.format("1-2"));
    }

    @Test
    public void formatPageNumbersCommaSeparated() {
        Assert.assertEquals("1,2,3", formatter.format("1,2,3"));
    }

    @Test
    public void formatPageNumbersPlusRange() {
        Assert.assertEquals("43+", formatter.format("43+"));
    }

    @Test
    public void ignoreWhitespaceInPageNumbers() {
        Assert.assertEquals("1--2", formatter.format("   1  - 2 "));
    }

    @Test
    public void removeWhitespaceSinglePage() {
        Assert.assertEquals("1", formatter.format("   1  "));
    }

    @Test
    public void removeWhitespacePageRange() {
        Assert.assertEquals("1--2", formatter.format("   1 -- 2  "));
    }

    @Test
    public void ignoreWhitespaceInPageNumbersWithDoubleDash() {
        Assert.assertEquals("43--103", formatter.format("43 -- 103"));
    }

    @Test
    public void keepCorrectlyFormattedPageNumbers() {
        Assert.assertEquals("1--2", formatter.format("1--2"));
    }

    @Test
    public void formatPageNumbersRemoveUnexpectedLiterals() {
        Assert.assertEquals("1--2", formatter.format("{1}-{2}"));
    }

    @Test
    public void formatPageNumbersRegexNotMatching() {
        Assert.assertEquals("12", formatter.format("12"));
    }

    @Test
    public void doNotRemoveLetters() {
        Assert.assertEquals("R1-R50", formatter.format("R1-R50"));
    }

    @Test
    public void replaceLongDashWithDoubleDash() {
        Assert.assertEquals("1--50", formatter.format("1 \u2014 50"));
    }

    @Test
    public void removePagePrefix() {
        Assert.assertEquals("50", formatter.format("p.50"));
    }

    @Test
    public void removePagesPrefix() {
        Assert.assertEquals("50", formatter.format("pp.50"));
    }

    @Test
    public void formatACMPages() {
        // This appears in https://doi.org/10.1145/1658373.1658375
        Assert.assertEquals("2:1--2:33", formatter.format("2:1-2:33"));
    }

    @Test
    public void keepFormattedACMPages() {
        // This appears in https://doi.org/10.1145/1658373.1658375
        Assert.assertEquals("2:1--2:33", formatter.format("2:1--2:33"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("1--2", formatter.format(formatter.getExampleInput()));
    }

}
