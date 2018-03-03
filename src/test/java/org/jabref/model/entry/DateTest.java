package org.jabref.model.entry;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateTest {

    @Test
    public void parseCorrectlyDayMonthYearDate() throws Exception {
        Date expected = new Date(LocalDate.of(2014, 6, 19));
        assertEquals(Optional.of(expected), Date.parse("19-06-2014"));
    }

    public void parseDateNull() {
        assertThrows(NullPointerException.class, () -> assertEquals(Optional.empty(), Date.parse(null)));
    }
}
