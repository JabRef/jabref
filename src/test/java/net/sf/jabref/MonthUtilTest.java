package net.sf.jabref;

import org.junit.Test;

import static org.junit.Assert.*;

public class MonthUtilTest {

    @Test
    public void testToMonthNumber() {

        assertEquals(0, MonthUtil.getMonth("jan").index);
        assertEquals(1, MonthUtil.getMonth("feb").index);
        assertEquals(2, MonthUtil.getMonth("mar").index);
        assertEquals(3, MonthUtil.getMonth("apr").index);
        assertEquals(4, MonthUtil.getMonth("may").index);
        assertEquals(5, MonthUtil.getMonth("jun").index);
        assertEquals(6, MonthUtil.getMonth("jul").index);
        assertEquals(7, MonthUtil.getMonth("aug").index);
        assertEquals(8, MonthUtil.getMonth("sep").index);
        assertEquals(9, MonthUtil.getMonth("oct").index);
        assertEquals(10, MonthUtil.getMonth("nov").index);
        assertEquals(11, MonthUtil.getMonth("dec").index);

        assertEquals(0, MonthUtil.getMonth("#jan#").index);
        assertEquals(1, MonthUtil.getMonth("#feb#").index);
        assertEquals(2, MonthUtil.getMonth("#mar#").index);
        assertEquals(3, MonthUtil.getMonth("#apr#").index);
        assertEquals(4, MonthUtil.getMonth("#may#").index);
        assertEquals(5, MonthUtil.getMonth("#jun#").index);
        assertEquals(6, MonthUtil.getMonth("#jul#").index);
        assertEquals(7, MonthUtil.getMonth("#aug#").index);
        assertEquals(8, MonthUtil.getMonth("#sep#").index);
        assertEquals(9, MonthUtil.getMonth("#oct#").index);
        assertEquals(10, MonthUtil.getMonth("#nov#").index);
        assertEquals(11, MonthUtil.getMonth("#dec#").index);

        assertEquals(0, MonthUtil.getMonth("January").index);
        assertEquals(1, MonthUtil.getMonth("February").index);
        assertEquals(2, MonthUtil.getMonth("March").index);
        assertEquals(3, MonthUtil.getMonth("April").index);
        assertEquals(4, MonthUtil.getMonth("May").index);
        assertEquals(5, MonthUtil.getMonth("June").index);
        assertEquals(6, MonthUtil.getMonth("July").index);
        assertEquals(7, MonthUtil.getMonth("August").index);
        assertEquals(8, MonthUtil.getMonth("September").index);
        assertEquals(9, MonthUtil.getMonth("October").index);
        assertEquals(10, MonthUtil.getMonth("November").index);
        assertEquals(11, MonthUtil.getMonth("Decembre").index);

        assertEquals(0, MonthUtil.getMonth("01").index);
        assertEquals(1, MonthUtil.getMonth("02").index);
        assertEquals(2, MonthUtil.getMonth("03").index);
        assertEquals(3, MonthUtil.getMonth("04").index);
        assertEquals(4, MonthUtil.getMonth("05").index);
        assertEquals(5, MonthUtil.getMonth("06").index);
        assertEquals(6, MonthUtil.getMonth("07").index);
        assertEquals(7, MonthUtil.getMonth("08").index);
        assertEquals(8, MonthUtil.getMonth("09").index);
        assertEquals(9, MonthUtil.getMonth("10").index);

        assertEquals(0, MonthUtil.getMonth("1").index);
        assertEquals(1, MonthUtil.getMonth("2").index);
        assertEquals(2, MonthUtil.getMonth("3").index);
        assertEquals(3, MonthUtil.getMonth("4").index);
        assertEquals(4, MonthUtil.getMonth("5").index);
        assertEquals(5, MonthUtil.getMonth("6").index);
        assertEquals(6, MonthUtil.getMonth("7").index);
        assertEquals(7, MonthUtil.getMonth("8").index);
        assertEquals(8, MonthUtil.getMonth("9").index);

        assertEquals(10, MonthUtil.getMonth("11").index);
        assertEquals(11, MonthUtil.getMonth("12").index);

        assertEquals(-1, MonthUtil.getMonth(";lkjasdf").index);
        assertEquals(-1, MonthUtil.getMonth("3.2").index);
        assertEquals(-1, MonthUtil.getMonth("#test#").index);
        assertEquals(-1, MonthUtil.getMonth("").index);
    }

}