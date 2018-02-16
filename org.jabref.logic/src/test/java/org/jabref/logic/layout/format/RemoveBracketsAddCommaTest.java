package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveBracketsAddCommaTest {
    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveBracketsAddComma();
    }

    @Test
    public void testFormat() throws Exception {
        assertEquals("some text,", formatter.format("{some text}"));
        assertEquals("some text", formatter.format("{some text"));
        assertEquals("some text,", formatter.format("some text}"));
    }
}
