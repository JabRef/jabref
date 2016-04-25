package net.sf.jabref.logic.layout.format;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OrdinalTest {

    @Test
    public void testEmpty() {
        assertEquals("", new Ordinal().format(""));
    }

    @Test
    public void testNull() {
        assertNull(new Ordinal().format(null));
    }

    @Test
    public void testSingleDigit() {
        assertEquals("1st", new Ordinal().format("1"));
        assertEquals("2nd", new Ordinal().format("2"));
        assertEquals("3rd", new Ordinal().format("3"));
        assertEquals("4th", new Ordinal().format("4"));
    }

    @Test
    public void testMultiDigits() {
        assertEquals("11th", new Ordinal().format("11"));
        assertEquals("111th", new Ordinal().format("111"));
        assertEquals("21st", new Ordinal().format("21"));
    }

    @Test
    public void testAlreadyOrdinals() {
        assertEquals("1st", new Ordinal().format("1st"));
        assertEquals("111th", new Ordinal().format("111th"));
        assertEquals("22nd", new Ordinal().format("22nd"));
    }

    @Test
    public void testFullSentence() {
        assertEquals("1st edn.", new Ordinal().format("1 edn."));
        assertEquals("1st edition", new Ordinal().format("1st edition"));
        assertEquals("The 2nd conference on 3rd.14th", new Ordinal().format("The 2 conference on 3.14"));
    }

    @Test
    public void testLetters() {
        assertEquals("abCD eFg", new Ordinal().format("abCD eFg"));
    }
}
