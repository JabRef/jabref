package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RemoveBracketsAddCommaTest {
    private LayoutFormatter formatter;

    @Before
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