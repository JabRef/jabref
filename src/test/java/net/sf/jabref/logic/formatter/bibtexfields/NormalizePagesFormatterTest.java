package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class NormalizePagesFormatterTest {

    private NormalizePagesFormatter formatter;

    @Before
    public void setUp() {
        formatter = new NormalizePagesFormatter();
    }

    @Test
    public void formatSinglePageResultsInNoChange() {
        expectCorrect("1", "1");
    }

    @Test
    public void formatPageNumbers() {
        expectCorrect("1-2", "1--2");
    }

    @Test
    public void formatPageNumbersCommaSeparated() {
        expectCorrect("1,2,3", "1,2,3");
    }

    @Test
    public void formatPageNumbersPlusRange() {
        expectCorrect("43+", "43+");
    }

    @Test
    public void ignoreWhitespaceInPageNumbers() {
        expectCorrect("   1  - 2 ", "1--2");
    }

    @Test
    public void removeWhitespace() {
        expectCorrect("   1  ", "1");
        expectCorrect("   1 -- 2  ", "1--2");
    }

    @Test
    public void ignoreWhitespaceInPageNumbersWithDoubleDash() {
        expectCorrect("43 -- 103", "43--103");
    }

    @Test
    public void keepCorrectlyFormattedPageNumbers() {
        expectCorrect("1--2", "1--2");
    }

    @Test
    public void formatPageNumbersRemoveUnexpectedLiterals() {
        expectCorrect("{1}-{2}", "1--2");
    }

    @Test
    public void formatPageNumbersRegexNotMatching() {
        expectCorrect("12", "12");
    }

    @Test
    public void doNotRemoveLetters() {
        expectCorrect("R1-R50", "R1-R50");
    }

    @Test
    public void replaceLongDashWithDoubleDash() {
        expectCorrect("1 \u2014 50", "1--50");
    }

    @Test
    public void removePagePrefix() {
        expectCorrect("p.50", "50");
    }

    @Test
    public void removePagesPrefix() {
        expectCorrect("pp.50", "50");
    }

    @Test
    public void formatExample() {
        expectCorrect(formatter.getExampleInput(), "1--2");
    }

    private void expectCorrect(String input, String expected) {
        Assert.assertEquals(expected, formatter.format(input));
    }
}
