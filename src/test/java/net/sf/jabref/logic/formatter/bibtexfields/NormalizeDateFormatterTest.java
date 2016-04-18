package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeDateFormatterTest {

    private final NormalizeDateFormatter formatter = new NormalizeDateFormatter();

    @Test
    public void formatDateYYYYMM0D() {
        Assert.assertEquals("2015-11-08", formatter.format("2015-11-08"));
    }

    @Test
    public void formatDateYYYYM0D() {
        Assert.assertEquals("2015-01-08", formatter.format("2015-1-08"));
    }

    @Test
    public void formatDateYYYYMD() {
        Assert.assertEquals("2015-01-08", formatter.format("2015-1-8"));
    }

    @Test
    public void formatDateYYYYMM() {
        Assert.assertEquals("2015-11", formatter.format("2015-11"));
    }

    @Test
    public void formatDateYYYYM() {
        Assert.assertEquals("2015-01", formatter.format("2015-1"));
    }

    @Test
    public void formatDateMMYY() {
        Assert.assertEquals("2015-11", formatter.format("11/15"));
    }

    @Test
    public void formatDateMYY() {
        Assert.assertEquals("2015-01", formatter.format("1/15"));
    }

    @Test
    public void formatDate0MYY() {
        Assert.assertEquals("2015-01", formatter.format("01/15"));
    }

    @Test
    public void formatDateMMYYYY() {
        Assert.assertEquals("2015-11", formatter.format("11/2015"));
    }

    @Test
    public void formatDateMYYYY() {
        Assert.assertEquals("2015-01", formatter.format("1/2015"));
    }

    @Test
    public void formatDate0MYYYY() {
        Assert.assertEquals("2015-01", formatter.format("01/2015"));
    }

    @Test
    public void formatDateMMMDDCommaYYYY() {
        Assert.assertEquals("2015-11-08", formatter.format("November 08, 2015"));
    }

    @Test
    public void formatDateMMMDCommaYYYY() {
        Assert.assertEquals("2015-11-08", formatter.format("November 8, 2015"));
    }

    @Test
    public void formatDateMMMCommaYYYY() {
        Assert.assertEquals("2015-11", formatter.format("November, 2015"));
    }

    @Test
    public void formatDate0DdotMMdotYYYY() {
        Assert.assertEquals("2015-11-08", formatter.format("08.11.2015"));
    }

    @Test
    public void formatDateDdotMMdotYYYY() {
        Assert.assertEquals("2015-11-08", formatter.format("8.11.2015"));
    }

    @Test
    public void formatDateDDdotMMdotYYYY() {
        Assert.assertEquals("2015-11-15", formatter.format("15.11.2015"));
    }

    @Test
    public void formatDate0Ddot0MdotYYYY() {
        Assert.assertEquals("2015-01-08", formatter.format("08.01.2015"));
    }

    @Test
    public void formatDateDdot0MdotYYYY() {
        Assert.assertEquals("2015-01-08", formatter.format("8.01.2015"));
    }

    @Test
    public void formatDateDDdot0MdotYYYY() {
        Assert.assertEquals("2015-01-15", formatter.format("15.01.2015"));
    }

    @Test
    public void formatDate0DdotMdotYYYY() {
        Assert.assertEquals("2015-01-08", formatter.format("08.1.2015"));
    }

    @Test
    public void formatDateDdotMdotYYYY() {
        Assert.assertEquals("2015-01-08", formatter.format("8.1.2015"));
    }

    @Test
    public void formatDateDDdotMdotYYYY() {
        Assert.assertEquals("2015-01-15", formatter.format("15.1.2015"));
    }

    @Test
    public void formatExample() {
        Assert.assertEquals("2003-11-29", formatter.format(formatter.getExampleInput()));
    }
}