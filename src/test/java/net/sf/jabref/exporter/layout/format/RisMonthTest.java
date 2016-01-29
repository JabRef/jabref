package net.sf.jabref.exporter.layout.format;

import static org.junit.Assert.*;

import net.sf.jabref.Globals;
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

    @Test
    public void testMultipleKeywords() {
        assertEquals("KW  - abcd" + Globals.NEWLINE +"KW  - efg" + Globals.NEWLINE + "KW  - hij" + Globals.NEWLINE + "KW  - klm",
                new RisKeywords().format("abcd, efg, hij, klm"));
    }
}
