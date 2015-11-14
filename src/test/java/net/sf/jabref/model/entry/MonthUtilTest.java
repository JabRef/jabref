package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

public class MonthUtilTest {

    @Test
    public void testToMonthNumber() {
        Assert.assertEquals(0, MonthUtil.getMonth("jan").index);
        Assert.assertEquals(1, MonthUtil.getMonth("feb").index);
        Assert.assertEquals(2, MonthUtil.getMonth("mar").index);
        Assert.assertEquals(3, MonthUtil.getMonth("apr").index);
        Assert.assertEquals(4, MonthUtil.getMonth("may").index);
        Assert.assertEquals(5, MonthUtil.getMonth("jun").index);
        Assert.assertEquals(6, MonthUtil.getMonth("jul").index);
        Assert.assertEquals(7, MonthUtil.getMonth("aug").index);
        Assert.assertEquals(8, MonthUtil.getMonth("sep").index);
        Assert.assertEquals(9, MonthUtil.getMonth("oct").index);
        Assert.assertEquals(10, MonthUtil.getMonth("nov").index);
        Assert.assertEquals(11, MonthUtil.getMonth("dec").index);

        Assert.assertEquals(0, MonthUtil.getMonth("#jan#").index);
        Assert.assertEquals(1, MonthUtil.getMonth("#feb#").index);
        Assert.assertEquals(2, MonthUtil.getMonth("#mar#").index);
        Assert.assertEquals(3, MonthUtil.getMonth("#apr#").index);
        Assert.assertEquals(4, MonthUtil.getMonth("#may#").index);
        Assert.assertEquals(5, MonthUtil.getMonth("#jun#").index);
        Assert.assertEquals(6, MonthUtil.getMonth("#jul#").index);
        Assert.assertEquals(7, MonthUtil.getMonth("#aug#").index);
        Assert.assertEquals(8, MonthUtil.getMonth("#sep#").index);
        Assert.assertEquals(9, MonthUtil.getMonth("#oct#").index);
        Assert.assertEquals(10, MonthUtil.getMonth("#nov#").index);
        Assert.assertEquals(11, MonthUtil.getMonth("#dec#").index);

        Assert.assertEquals(0, MonthUtil.getMonth("January").index);
        Assert.assertEquals(1, MonthUtil.getMonth("February").index);
        Assert.assertEquals(2, MonthUtil.getMonth("March").index);
        Assert.assertEquals(3, MonthUtil.getMonth("April").index);
        Assert.assertEquals(4, MonthUtil.getMonth("May").index);
        Assert.assertEquals(5, MonthUtil.getMonth("June").index);
        Assert.assertEquals(6, MonthUtil.getMonth("July").index);
        Assert.assertEquals(7, MonthUtil.getMonth("August").index);
        Assert.assertEquals(8, MonthUtil.getMonth("September").index);
        Assert.assertEquals(9, MonthUtil.getMonth("October").index);
        Assert.assertEquals(10, MonthUtil.getMonth("November").index);
        Assert.assertEquals(11, MonthUtil.getMonth("December").index);

        Assert.assertEquals(0, MonthUtil.getMonth("01").index);
        Assert.assertEquals(1, MonthUtil.getMonth("02").index);
        Assert.assertEquals(2, MonthUtil.getMonth("03").index);
        Assert.assertEquals(3, MonthUtil.getMonth("04").index);
        Assert.assertEquals(4, MonthUtil.getMonth("05").index);
        Assert.assertEquals(5, MonthUtil.getMonth("06").index);
        Assert.assertEquals(6, MonthUtil.getMonth("07").index);
        Assert.assertEquals(7, MonthUtil.getMonth("08").index);
        Assert.assertEquals(8, MonthUtil.getMonth("09").index);
        Assert.assertEquals(9, MonthUtil.getMonth("10").index);

        Assert.assertEquals(0, MonthUtil.getMonth("1").index);
        Assert.assertEquals(1, MonthUtil.getMonth("2").index);
        Assert.assertEquals(2, MonthUtil.getMonth("3").index);
        Assert.assertEquals(3, MonthUtil.getMonth("4").index);
        Assert.assertEquals(4, MonthUtil.getMonth("5").index);
        Assert.assertEquals(5, MonthUtil.getMonth("6").index);
        Assert.assertEquals(6, MonthUtil.getMonth("7").index);
        Assert.assertEquals(7, MonthUtil.getMonth("8").index);
        Assert.assertEquals(8, MonthUtil.getMonth("9").index);

        Assert.assertEquals(10, MonthUtil.getMonth("11").index);
        Assert.assertEquals(11, MonthUtil.getMonth("12").index);

        Assert.assertEquals(-1, MonthUtil.getMonth(";lkjasdf").index);
        Assert.assertEquals(-1, MonthUtil.getMonth("3.2").index);
        Assert.assertEquals(-1, MonthUtil.getMonth("#test#").index);
        Assert.assertEquals(-1, MonthUtil.getMonth("").index);
        Assert.assertFalse(MonthUtil.getMonth("8,").isValid());
    }
}