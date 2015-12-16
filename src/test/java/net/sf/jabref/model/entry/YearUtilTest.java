package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

public class YearUtilTest {

    @Test
    public void test2to4DigitsYear() {
        Assert.assertEquals("1990", YearUtil.toFourDigitYear("1990"));
        Assert.assertEquals("190", YearUtil.toFourDigitYear("190"));
        Assert.assertEquals("1990", YearUtil.toFourDigitYear("90", 1990));
        Assert.assertEquals("1990", YearUtil.toFourDigitYear("90", 1991));
        Assert.assertEquals("2020", YearUtil.toFourDigitYear("20", 1990));
        Assert.assertEquals("1921", YearUtil.toFourDigitYear("21", 1990));
        Assert.assertEquals("1922", YearUtil.toFourDigitYear("22", 1990));
        Assert.assertEquals("2022", YearUtil.toFourDigitYear("22", 1992));
        Assert.assertEquals("1999", YearUtil.toFourDigitYear("99", 2001));
        Assert.assertEquals("1931", YearUtil.toFourDigitYear("1931", 2001));
        Assert.assertEquals("2031", YearUtil.toFourDigitYear("31", 2001));
        Assert.assertEquals("1932", YearUtil.toFourDigitYear("32", 2001));
        Assert.assertEquals("1944", YearUtil.toFourDigitYear("44", 2001));
        Assert.assertEquals("2011", YearUtil.toFourDigitYear("11", 2001));
        Assert.assertEquals("2005a", YearUtil.toFourDigitYear("2005a"));
        Assert.assertEquals("2005b", YearUtil.toFourDigitYear("2005b"));

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        int d2 = thisYear % 100;

        NumberFormat f = new DecimalFormat("00");

        for (int i = 0; i <= 30; i++) {
            Assert.assertTrue("" + i, thisYear <= Integer.parseInt(YearUtil.toFourDigitYear(f.format((d2 + i) % 100))));
        }
        for (int i = 0; i < 70; i++) {
            Assert.assertTrue("" + i, thisYear >= Integer.parseInt(YearUtil.toFourDigitYear(f.format((d2 - i + 100) % 100))));
        }
    }

}