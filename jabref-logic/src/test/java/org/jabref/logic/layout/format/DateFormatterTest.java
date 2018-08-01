package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateFormatterTest {

    private ParamLayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new DateFormatter();
    }

    @Test
    public void testDefaultFormat() {
        assertEquals("2016-07-15", formatter.format("2016-07-15"));
    }

    @Test
    public void testRequestedFormat() {
        formatter.setArgument("MM/yyyy");
        assertEquals("07/2016", formatter.format("2016-07-15"));
    }

}
