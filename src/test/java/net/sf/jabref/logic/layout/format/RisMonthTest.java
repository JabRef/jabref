package net.sf.jabref.logic.layout.format;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class RisMonthTest {

    @Test
    public void testEmpty() {
        assertEquals("", new RisMonth().format(""));
    }

    @Test
    public void testNull() {
        assertEquals("", new RisMonth().format(null));
    }

    @Test
    public void testMonth() {
        assertEquals("12", new RisMonth().format("dec"));
    }

    @Test
    public void testInvalidMonth() {
        assertEquals("abcd", new RisMonth().format("abcd"));
    }
}
