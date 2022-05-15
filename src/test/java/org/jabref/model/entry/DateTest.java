package org.jabref.model.entry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTest {
    private static Stream<Arguments> validDates() {
        return Stream.of(
                Arguments.of(LocalDateTime.of(2018, Month.OCTOBER, 3, 7, 24), "2018-10-03T07:24"),
                Arguments.of(LocalDateTime.of(2018, Month.OCTOBER, 3, 17, 2), "2018-10-03T17:2"),
                Arguments.of(LocalDateTime.of(2018, Month.OCTOBER, 3, 7, 24), "2018-10-03T7:24"),
                Arguments.of(LocalDateTime.of(2018, Month.OCTOBER, 3, 7, 7), "2018-10-03T7:7"),
                Arguments.of(LocalDateTime.of(2018, Month.OCTOBER, 3, 7, 0), "2018-10-03T07"),
                Arguments.of(LocalDateTime.of(2018, Month.OCTOBER, 3, 7, 0), "2018-10-03T7"),
                Arguments.of(LocalDate.of(2009, Month.JANUARY, 15), "2009-1-15"),
                Arguments.of(YearMonth.of(2009, Month.NOVEMBER), "2009-11"),
                Arguments.of(LocalDate.of(2012, Month.JANUARY, 15), "15-1-2012"),
                Arguments.of(YearMonth.of(2012, Month.JANUARY), "1-2012"),
                Arguments.of(YearMonth.of(2015, Month.SEPTEMBER), "9/2015"),
                Arguments.of(YearMonth.of(2015, Month.SEPTEMBER),  "09/2015"),
                Arguments.of(YearMonth.of(2015, Month.SEPTEMBER), "9/15"),
                Arguments.of(LocalDate.of(2015, Month.SEPTEMBER, 1), "September 1, 2015"),
                Arguments.of(YearMonth.of(2015, Month.SEPTEMBER), "September, 2015"),
                Arguments.of(LocalDate.of(2015, Month.JANUARY, 15), "15.1.2015"),
                Arguments.of(LocalDate.of(2015, Month.JANUARY, 15), "2015.1.15"),
                Arguments.of(Year.of(2015), "2015"),
                Arguments.of(YearMonth.of(2020, Month.JANUARY), "Jan, 2020"));
    }

    @ParameterizedTest
    @MethodSource("validDates")
    void parseByDatePattern(Temporal expected, String provided) {
        assertEquals(Optional.of(new Date(expected)), Date.parse(provided));
    }

    private static Stream<Arguments> invalidCornerCases() {
        return Stream.of(
                Arguments.of("", "input value not empty"),
                Arguments.of("32-06-2014", "day of month exists [1]"),
                Arguments.of("00-06-2014", "day of month exists [2]"),
                Arguments.of("30-13-2014", "month exists [1]"),
                Arguments.of("30-00-2014", "month exists [2]")
        );
    }

    @ParameterizedTest
    @MethodSource("invalidCornerCases")
    void nonExistentDates(String invalidDate, String errorMessage) {
        assertEquals(Optional.empty(), Date.parse(invalidDate), errorMessage);
    }

    @Test
    void parseYearRange() {
        Date expectedDataRange = new Date(Year.of(2014), Year.of(2017));
        assertEquals(Optional.of(expectedDataRange), Date.parse("2014/2017"));
    }

    @Test
    void parseZonedTime() {
        Optional<Date> expected = Optional.of(
                new Date(ZonedDateTime.of(
                        LocalDateTime.of(2018, Month.OCTOBER, 3, 7, 25, 14),
                        ZoneId.from(ZoneOffset.ofHours(3)))
                )
        );

        assertEquals(expected, Date.parse("2018-10-03T07:24:14+03:00"));
        assertNotEquals(expected, Date.parse("2018-10-03T07:24:14+03:00"));
    }

    @Test
    void parseDateNull() {
        assertThrows(NullPointerException.class, () -> Date.parse(null));
    }
}
