package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeDateFormatterTest {

    @Test
    public void formatDateYYYYMM0D() {
        String formatted = new NormalizeDateFormatter().format("2015-11-08");
        Assert.assertEquals("2015-11-08", formatted);
    }

    @Test
    public void formatDateYYYYM0D() {
        String formatted = new NormalizeDateFormatter().format("2015-1-08");
        Assert.assertEquals("2015-01-08", formatted);
    }

    @Test
    public void formatDateYYYYMD() {
        String formatted = new NormalizeDateFormatter().format("2015-1-8");
        Assert.assertEquals("2015-01-08", formatted);
    }

    @Test
    public void formatDateYYYYMM() {
        String formatted = new NormalizeDateFormatter().format("2015-11");
        Assert.assertEquals("2015-11", formatted);
    }

    @Test
    public void formatDateYYYYM() {
        String formatted = new NormalizeDateFormatter().format("2015-1");
        Assert.assertEquals("2015-01", formatted);
    }

    @Test
    public void formatDateMMYY() {
        String formatted = new NormalizeDateFormatter().format("11/15");
        Assert.assertEquals("2015-11", formatted);
    }

    @Test
    public void formatDateMYY() {
        String formatted = new NormalizeDateFormatter().format("1/15");
        Assert.assertEquals("2015-01", formatted);
    }

    @Test
    public void formatDate0MYY() {
        String formatted = new NormalizeDateFormatter().format("01/15");
        Assert.assertEquals("2015-01", formatted);
    }

    @Test
    public void formatDateMMYYYY() {
        String formatted = new NormalizeDateFormatter().format("11/2015");
        Assert.assertEquals("2015-11", formatted);
    }

    @Test
    public void formatDateMYYYY() {
        String formatted = new NormalizeDateFormatter().format("1/2015");
        Assert.assertEquals("2015-01", formatted);
    }

    @Test
    public void formatDate0MYYYY() {
        String formatted = new NormalizeDateFormatter().format("01/2015");
        Assert.assertEquals("2015-01", formatted);
    }

    @Test
    public void formatDateMMMDDCommaYYYY() {
        String formatted = new NormalizeDateFormatter().format("November 08, 2015");
        Assert.assertEquals("2015-11-08", formatted);
    }

    @Test
    public void formatDateMMMDCommaYYYY() {
        String formatted = new NormalizeDateFormatter().format("November 8, 2015");
        Assert.assertEquals("2015-11-08", formatted);
    }

    @Test
    public void formatDateMMMCommaYYYY() {
        String formatted = new NormalizeDateFormatter().format("November, 2015");
        Assert.assertEquals("2015-11", formatted);
    }

    @Test
    public void formatDate0DdotMMdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("08.11.2015");
        Assert.assertEquals("2015-11-08", formatted);
    }

    @Test
    public void formatDateDdotMMdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("8.11.2015");
        Assert.assertEquals("2015-11-08", formatted);
    }

    @Test
    public void formatDateDDdotMMdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("15.11.2015");
        Assert.assertEquals("2015-11-15", formatted);
    }

    @Test
    public void formatDate0Ddot0MdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("08.01.2015");
        Assert.assertEquals("2015-01-08", formatted);
    }

    @Test
    public void formatDateDdot0MdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("8.01.2015");
        Assert.assertEquals("2015-01-08", formatted);
    }

    @Test
    public void formatDateDDdot0MdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("15.01.2015");
        Assert.assertEquals("2015-01-15", formatted);
    }

    @Test
    public void formatDate0DdotMdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("08.1.2015");
        Assert.assertEquals("2015-01-08", formatted);
    }

    @Test
    public void formatDateDdotMdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("8.1.2015");
        Assert.assertEquals("2015-01-08", formatted);
    }

    @Test
    public void formatDateDDdotMdotYYYY() {
        String formatted = new NormalizeDateFormatter().format("15.1.2015");
        Assert.assertEquals("2015-01-15", formatted);
    }
}