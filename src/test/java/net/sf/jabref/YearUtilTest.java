package net.sf.jabref;

import org.junit.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class YearUtilTest {

    @Test
    public void test2to4DigitsYear() {
        assertEquals("1990", YearUtil.toFourDigitYear("1990"));
        assertEquals("190", YearUtil.toFourDigitYear("190"));
        assertEquals("1990", YearUtil.toFourDigitYear("90", 1990));
        assertEquals("1990", YearUtil.toFourDigitYear("90", 1991));
        assertEquals("2020", YearUtil.toFourDigitYear("20", 1990));
        assertEquals("1921", YearUtil.toFourDigitYear("21", 1990));
        assertEquals("1922", YearUtil.toFourDigitYear("22", 1990));
        assertEquals("2022", YearUtil.toFourDigitYear("22", 1992));
        assertEquals("1999", YearUtil.toFourDigitYear("99", 2001));
        assertEquals("1931", YearUtil.toFourDigitYear("1931", 2001));
        assertEquals("2031", YearUtil.toFourDigitYear("31", 2001));
        assertEquals("1932", YearUtil.toFourDigitYear("32", 2001));
        assertEquals("1944", YearUtil.toFourDigitYear("44", 2001));
        assertEquals("2011", YearUtil.toFourDigitYear("11", 2001));

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        int d2 = thisYear % 100;

        NumberFormat f = new DecimalFormat("00");

        for (int i = 0; i <= 30; i++) {
            assertTrue("" + i, thisYear <= Integer.parseInt(YearUtil.toFourDigitYear(f.format((d2 + i) % 100))));
        }
        for (int i = 0; i < 70; i++) {
            assertTrue("" + i, thisYear >= Integer.parseInt(YearUtil.toFourDigitYear(f.format((d2 - i + 100) % 100))));
        }
    }

}