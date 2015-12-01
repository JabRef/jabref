package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Test;

public class PageNumbersFormatterTest {

    @Test
    public void formatPageNumbers() {
        String formatted = new PageNumbersFormatter().format("1-2");
        Assert.assertEquals("1--2", formatted);
    }
}