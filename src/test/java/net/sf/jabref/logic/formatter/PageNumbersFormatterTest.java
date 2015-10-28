package net.sf.jabref.logic.formatter;

import junit.framework.Assert;
import net.sf.jabref.model.entry.BibtexEntry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PageNumbersFormatterTest {
    private BibtexEntry entry;

    @Before
    public void setUp() {
        entry = new BibtexEntry();
    }

    @After
    public void teardown() {
        entry = null;
    }

    @Test
    public void formatPageNumbers() {
        entry.setField("pages", "1-2");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void formatPageNumbersCommaSeparated() {
        entry.setField("pages", "1,2,3");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("1,2,3", entry.getField("pages"));
    }

    @Test
    public void ignoreWhitespaceInPageNumbers() {
        entry.setField("pages", "   1  - 2 ");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void keepCorrectlyFormattedPageNumbers() {
        entry.setField("pages", "1--2");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void onlyFormatPageNumbersField() {
        entry.setField("otherfield", "1-2");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("1-2", entry.getField("otherfield"));
    }

    @Test
    public void formatPageNumbersEmptyFields() {
        entry.setField("pages", "");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("", entry.getField("pages"));

        entry.setField("pages", null);
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals(null, entry.getField("pages"));
    }

    @Test
    public void formatPageNumbersRemoveUnexpectedLiterals() {
        entry.setField("pages", "{1}-{2}");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("1--2", entry.getField("pages"));
    }

    @Test
    public void formatPageNumbersRegexNotMatching() {
        entry.setField("pages", "12");
        new PageNumbersFormatter(entry).format();

        Assert.assertEquals("12", entry.getField("pages"));
    }
}