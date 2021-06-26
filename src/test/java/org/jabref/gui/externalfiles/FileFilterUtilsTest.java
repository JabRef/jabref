package org.jabref.gui.externalfiles;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileFilterUtilsTest {

    private final FileFilterUtils fileFilterUtils = new FileFilterUtils();
    private final LocalDateTime time = LocalDateTime.now();
   
    @Test
    public void isDuringLastDayNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastDay(time.minusHours(24)), false);
    }

    @Test
    public void isDuringLastDayPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastDay(time.minusHours(23)), true);
    }

    @Test
    public void isDuringLastWeekNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastWeek(time.minusDays(7)), false);
    }

    @Test
    public void isDuringLastWeekPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastWeek(time.minusDays(6).minusHours(23)), true);
    }

    @Test
    public void isDuringLastMonthNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastMonth(time.minusMonths(1)), false);
    }

    @Test
    public void isDuringLastMonthPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastMonth(time.minusDays(29).minusHours(23)), true);
    }

    @Test
    public void isDuringLastYearNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastYear(time.minusYears(1)), false);
    }

    @Test
    public void isDuringLastYearPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastYear(time.minusMonths(11).minusDays(29).minusHours(23)), true);
    }
}
