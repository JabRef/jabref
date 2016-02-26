package net.sf.jabref.logic.layout.format;

import static org.junit.Assert.*;

import org.junit.Test;


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
