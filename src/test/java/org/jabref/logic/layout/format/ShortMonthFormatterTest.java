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
    public void testFormat() {
        assertEquals("jan", formatter.format("01"));
        assertEquals("jan", formatter.format("Januar"));
    }
}
