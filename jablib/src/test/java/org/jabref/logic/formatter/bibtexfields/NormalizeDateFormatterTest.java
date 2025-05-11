package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class NormalizeDateFormatterTest {

    private NormalizeDateFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new NormalizeDateFormatter();
    }

    @Test
    void formatDateYYYYMM0D() {
        assertEquals("2015-11-08", formatter.format("2015-11-08"));
    }

    @Test
    void formatDateYYYYM0D() {
        assertEquals("2015-01-08", formatter.format("2015-1-08"));
    }

    @Test
    void formatDateYYYYMD() {
        assertEquals("2015-01-08", formatter.format("2015-1-8"));
    }

    @Test
    void formatDateYYYYMM() {
        assertEquals("2015-11", formatter.format("2015-11"));
    }

    @Test
    void formatDateYYYYM() {
        assertEquals("2015-01", formatter.format("2015-1"));
    }

    @Test
    void formatDateMMYY() {
        assertEquals("2015-11", formatter.format("11/15"));
    }

    @Test
    void formatDateMYY() {
        assertEquals("2015-01", formatter.format("1/15"));
    }

    @Test
    void formatDate0MYY() {
        assertEquals("2015-01", formatter.format("01/15"));
    }

    @Test
    void formatDateMMYYYY() {
        assertEquals("2015-11", formatter.format("11/2015"));
    }

    @Test
    void formatDateMYYYY() {
        assertEquals("2015-01", formatter.format("1/2015"));
    }

    @Test
    void formatDate0MYYYY() {
        assertEquals("2015-01", formatter.format("01/2015"));
    }

    @Test
    void formatDateMMMDDCommaYYYY() {
        assertEquals("2015-11-08", formatter.format("November 08, 2015"));
    }

    @Test
    void formatDateMMMDCommaYYYY() {
        assertEquals("2015-11-08", formatter.format("November 8, 2015"));
    }

    @Test
    void formatDateMMMCommaYYYY() {
        assertEquals("2015-11", formatter.format("November, 2015"));
    }

    @Test
    void formatDate0DdotMMdotYYYY() {
        assertEquals("2015-11-08", formatter.format("08.11.2015"));
    }

    @Test
    void formatDateDdotMMdotYYYY() {
        assertEquals("2015-11-08", formatter.format("8.11.2015"));
    }

    @Test
    void formatDateDDdotMMdotYYYY() {
        assertEquals("2015-11-15", formatter.format("15.11.2015"));
    }

    @Test
    void formatDate0Ddot0MdotYYYY() {
        assertEquals("2015-01-08", formatter.format("08.01.2015"));
    }

    @Test
    void formatDateDdot0MdotYYYY() {
        assertEquals("2015-01-08", formatter.format("8.01.2015"));
    }

    @Test
    void formatDateDDdot0MdotYYYY() {
        assertEquals("2015-01-15", formatter.format("15.01.2015"));
    }

    @Test
    void formatDate0DdotMdotYYYY() {
        assertEquals("2015-01-08", formatter.format("08.1.2015"));
    }

    @Test
    void formatDateDdotMdotYYYY() {
        assertEquals("2015-01-08", formatter.format("8.1.2015"));
    }

    @Test
    void formatDateDDdotMdotYYYY() {
        assertEquals("2015-01-15", formatter.format("15.1.2015"));
    }

    @Test
    void formatExample() {
        assertEquals("2003-11-29", formatter.format(formatter.getExampleInput()));
    }
}
