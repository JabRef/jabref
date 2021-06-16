package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeDateFormatterTest {

    private NormalizeDateFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizeDateFormatter();
    }

    @Test
    public void formatDateYYYYMM0D() {
        assertEquals("2015-11-08", formatter.format("2015-11-08"));
    }

    @Test
    public void formatDateYYYYM0D() {
        assertEquals("2015-01-08", formatter.format("2015-1-08"));
    }

    @Test
    public void formatDateYYYYMD() {
        assertEquals("2015-01-08", formatter.format("2015-1-8"));
    }

    @Test
    public void formatDateYYYYMM() {
        assertEquals("2015-11", formatter.format("2015-11"));
    }

    @Test
    public void formatDateYYYYM() {
        assertEquals("2015-01", formatter.format("2015-1"));
    }

    @Test
    public void formatDateMMYY() {
        assertEquals("2015-11", formatter.format("11/15"));
    }

    @Test
    public void formatDateMYY() {
        assertEquals("2015-01", formatter.format("1/15"));
    }

    @Test
    public void formatDate0MYY() {
        assertEquals("2015-01", formatter.format("01/15"));
    }

    @Test
    public void formatDateMMYYYY() {
        assertEquals("2015-11", formatter.format("11/2015"));
    }

    @Test
    public void formatDateMYYYY() {
        assertEquals("2015-01", formatter.format("1/2015"));
    }

    @Test
    public void formatDate0MYYYY() {
        assertEquals("2015-01", formatter.format("01/2015"));
    }

    @Test
    public void formatDateMMMDDCommaYYYY() {
        assertEquals("2015-11-08", formatter.format("November 08, 2015"));
    }

    @Test
    public void formatDateMMMDCommaYYYY() {
        assertEquals("2015-11-08", formatter.format("November 8, 2015"));
    }

    @Test
    public void formatDateMMMCommaYYYY() {
        assertEquals("2015-11", formatter.format("November, 2015"));
    }

    @Test
    public void formatDate0DdotMMdotYYYY() {
        assertEquals("2015-11-08", formatter.format("08.11.2015"));
    }

    @Test
    public void formatDateDdotMMdotYYYY() {
        assertEquals("2015-11-08", formatter.format("8.11.2015"));
    }

    @Test
    public void formatDateDDdotMMdotYYYY() {
        assertEquals("2015-11-15", formatter.format("15.11.2015"));
    }

    @Test
    public void formatDate0Ddot0MdotYYYY() {
        assertEquals("2015-01-08", formatter.format("08.01.2015"));
    }

    @Test
    public void formatDateDdot0MdotYYYY() {
        assertEquals("2015-01-08", formatter.format("8.01.2015"));
    }

    @Test
    public void formatDateDDdot0MdotYYYY() {
        assertEquals("2015-01-15", formatter.format("15.01.2015"));
    }

    @Test
    public void formatDate0DdotMdotYYYY() {
        assertEquals("2015-01-08", formatter.format("08.1.2015"));
    }

    @Test
    public void formatDateDdotMdotYYYY() {
        assertEquals("2015-01-08", formatter.format("8.1.2015"));
    }

    @Test
    public void formatDateDDdotMdotYYYY() {
        assertEquals("2015-01-15", formatter.format("15.1.2015"));
    }

    @Test
    public void formatExample() {
        assertEquals("2003-11-29", formatter.format(formatter.getExampleInput()));
    }
}
