package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShortMonthFormatterTest {

    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new ShortMonthFormatter();
    }

    @Test
    public void testFormat() {
        assertEquals("jan", formatter.format("1"));
        assertEquals("feb", formatter.format("2"));
        assertEquals("mar", formatter.format("3"));
        assertEquals("apr", formatter.format("4"));
        assertEquals("may", formatter.format("5"));
        assertEquals("jun", formatter.format("6"));
        assertEquals("jul", formatter.format("7"));
        assertEquals("aug", formatter.format("8"));
        assertEquals("sep", formatter.format("9"));
        assertEquals("oct", formatter.format("10"));
        assertEquals("nov", formatter.format("11"));
        assertEquals("dec", formatter.format("12"));
        assertEquals("jan", formatter.format("Januar"));
    }

    @Test
    public void testInvalidFormat() {
        assertEquals("", formatter.format("-1"));
        assertEquals("", formatter.format("0"));
        assertEquals("", formatter.format("13"));
        assertEquals("", formatter.format("abc"));
    }
}
