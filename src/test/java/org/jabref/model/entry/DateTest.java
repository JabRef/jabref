package org.jabref.model.entry;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateTest {

    @Test
    public void parseCorrectlyDayMonthYearDate() throws Exception {
        Date expected = new Date(LocalDate.of(2014, 6, 19));
        assertEquals(Optional.of(expected), Date.parse("19-06-2014"));
    }

    @Test(expected = NullPointerException.class)
    public void parseDateNull() {
        assertEquals(Optional.empty(), Date.parse(null));
    }
}
