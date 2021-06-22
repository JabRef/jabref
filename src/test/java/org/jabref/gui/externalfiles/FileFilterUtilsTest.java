package org.jabref.gui.externalfiles;

import java.time.LocalDateTime;
import org.jabref.gui.externalfiles.FileFilterUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileFilterUtilsTest {

    @Test
    public void isDuringLastDayNegativeTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusHours(24);
        assertEquals(fileFilterUtils.isDuringLastDay(time), false);
    }

    @Test
    public void isDuringLastDayPositiveTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusHours(23);
        assertEquals(fileFilterUtils.isDuringLastDay(time), true);
    }

    @Test
    public void isDuringLastWeekNegativeTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusDays(7);
        assertEquals(fileFilterUtils.isDuringLastWeek(time), false);
    }

    @Test
    public void isDuringLastWeekPositiveTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusDays(6).minusHours(23);
        assertEquals(fileFilterUtils.isDuringLastWeek(time), true);
    }

    @Test
    public void isDuringLastMonthNegativeTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusMonths(1);
        assertEquals(fileFilterUtils.isDuringLastMonth(time), false);
    }

    @Test
    public void isDuringLastMonthPositiveTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusDays(29).minusHours(23);
        assertEquals(fileFilterUtils.isDuringLastMonth(time), true);
    }

    @Test
    public void isDuringLastYearNegativeTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusYears(1);
        assertEquals(fileFilterUtils.isDuringLastYear(time), false);
    }

    @Test
    public void isDuringLastYearPositiveTest() {
        FileFilterUtils fileFilterUtils = new FileFilterUtils();
        LocalDateTime time = LocalDateTime.now().minusMonths(11).minusDays(29).minusHours(23);
        assertEquals(fileFilterUtils.isDuringLastYear(time), true);
    }
}
