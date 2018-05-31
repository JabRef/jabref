package org.jabref.model.entry;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateTest {

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
}
