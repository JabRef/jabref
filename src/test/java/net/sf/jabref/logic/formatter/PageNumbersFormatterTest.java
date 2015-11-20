package net.sf.jabref.logic.formatter;

import net.sf.jabref.logic.formatter.bibtexfields.PageNumbersFormatter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PageNumbersFormatterTest {
    private PageNumbersFormatter formatter;

    @Before
    public void setUp() {
        formatter = new PageNumbersFormatter();
    }

    @After
    public void teardown() {
        formatter = null;
    }

    @Test
    public void returnsFormatterName() {
        Assert.assertNotNull(formatter.getName());
        Assert.assertNotEquals("", formatter.getName());
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
    public void keepCorrectlyFormattedPageNumbers() {
        expectCorrect("1--2", "1--2");
    }

    @Test
    public void formatPageNumbersEmptyFields() {
        expectCorrect("", "");
        expectCorrect(null, null);
    }

    @Test
    public void formatPageNumbersRemoveUnexpectedLiterals() {
        expectCorrect("{1}-{2}", "1--2");
    }

    @Test
    public void formatPageNumbersRegexNotMatching() {
        expectCorrect("12", "12");
    }

    private void expectCorrect(String input, String expected) {
        Assert.assertEquals(expected, formatter.format(input));
    }
}