package net.sf.jabref.logic.formatter;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

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
    public void formatPageNumbers() {
        String value = "1-2";
        String result = formatter.format(value);

        Assert.assertEquals("1--2", result);
    }

    @Test
    public void formatPageNumbersCommaSeparated() {
        String value = "1,2,3";
        String result = formatter.format(value);

        Assert.assertEquals("1,2,3", result);
    }

    @Test
    public void ignoreWhitespaceInPageNumbers() {
        String value = "   1  - 2 ";
        String result = formatter.format(value);

        Assert.assertEquals("1--2", result);
    }

    @Test
    public void keepCorrectlyFormattedPageNumbers() {
        String value = "1--2";
        String result = formatter.format(value);

        Assert.assertEquals("1--2", result);
    }

    @Test
    public void formatPageNumbersEmptyFields() {
        String value = "";
        String result = formatter.format(value);

        Assert.assertEquals("", result);

        value = null;
        result = formatter.format(value);

        Assert.assertEquals(null, result);
    }

    @Test
    public void formatPageNumbersRemoveUnexpectedLiterals() {
        String value = "{1}-{2}";
        String result = formatter.format(value);

        Assert.assertEquals("1--2", result);
    }

    @Test
    public void formatPageNumbersRegexNotMatching() {
        String value = "12";
        String result = formatter.format(value);

        Assert.assertEquals("12", result);
    }
}