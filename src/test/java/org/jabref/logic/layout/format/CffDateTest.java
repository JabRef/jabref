package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.util.OS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CffDateTest {

    private LayoutFormatter formatter;
    private String newLine;

    @BeforeEach
    public void setUp() {
        formatter = new CffDate();
        newLine = OS.NEWLINE;
    }

    @Test
    public void dayMonthYear() {
        String expected = "date-released: 2003-11-06";
        assertEquals(expected, formatter.format("2003-11-06"));
    }

    @Test
    public void monthYear() {
        String expected = "month: 7" + newLine + "  " + "year: 2016";
        assertEquals(expected, formatter.format("2016-07"));
    }

    @Test
    public void year() {
        String expected = "year: 2021";
        assertEquals(expected, formatter.format("2021"));
    }

    @Test
    public void poorlyFormatted() {
        String expected = "issue-date: -2023";
        assertEquals(expected, formatter.format("-2023"));
    }
}
