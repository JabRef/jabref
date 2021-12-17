package org.jabref.model.entry;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTest {

    @Test
    void parseCorrectlyYearRangeDate() throws Exception {
        Date expectedDataRange = new Date(Year.of(2014), Year.of(2017));
        assertEquals(Optional.of(expectedDataRange), Date.parse("2014/2017"));
    }

    @Test
    void parseCorrectlyDayMonthYearDate() throws Exception {
        Date expected = new Date(LocalDate.of(2014, 6, 19));
        assertEquals(Optional.of(expected), Date.parse("19-06-2014"));
    }

    @Test
    void parseCorrectlyMonthYearDate() throws Exception {
        Date expected = new Date(YearMonth.of(2014, 6));
        assertEquals(Optional.of(expected), Date.parse("06-2014"));
    }

    @Test
    void parseCorrectlyYearMonthDate() throws Exception {
        Date expected = new Date(YearMonth.of(2014, 6));
        assertEquals(Optional.of(expected), Date.parse("2014-06"));
    }

    @Test
    void parseCorrectlyYearDate() throws Exception {
        Date expected = new Date(Year.of(2014));
        assertEquals(Optional.of(expected), Date.parse("2014"));
    }

    @Test
    void parseDateNull() {
        assertThrows(NullPointerException.class, () -> Date.parse(null));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCornerCaseArguments")
    public void nonExistentDates(String invalidDate, String errorMessage) {
        assertEquals(Optional.empty(), Date.parse(invalidDate), errorMessage);

    }

    private static Stream<Arguments> provideInvalidCornerCaseArguments() {
        return Stream.of(
                Arguments.of("", "input value not empty"),
                Arguments.of("32-06-2014", "day of month exists [1]"),
                Arguments.of("00-06-2014", "day of month exists [2]"),
                Arguments.of("30-13-2014", "month exists [1]"),
                Arguments.of("30-00-2014", "month exists [2]")
        );
    }
}
