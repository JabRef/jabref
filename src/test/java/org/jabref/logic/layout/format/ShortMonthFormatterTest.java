package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShortMonthFormatterTest {

    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ShortMonthFormatter();
    }

    @Test
    public void formatShortName() {
        assertEquals("jan", formatter.format("jan"));
    }

    @Test
    public void formatFullName() {
        assertEquals("jan", formatter.format("January"));
    }

    @Test
    public void formatGermanFullName() {
        assertEquals("jan", formatter.format("Januar"));
    }

    @Test
    public void formatMonthNumber() {
        assertEquals("jan", formatter.format("01"));
    }

    @Test
    public void formatRandomInput() {
        assertEquals("", formatter.format("Invented Month"));
    }

    @Test
    public void formatNullInput() {
        assertEquals("", formatter.format(null));
    }

    @Test
    public void formatEmptyInput() {
        assertEquals("", formatter.format(""));
    }
}
