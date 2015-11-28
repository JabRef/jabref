package net.sf.jabref.export.layout.format;

import net.sf.jabref.exporter.layout.LayoutFormatter;
import net.sf.jabref.exporter.layout.format.RemoveBracketsAddComma;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RemoveBracketsAddCommaTest {
    private LayoutFormatter formatter;

    @Before
    public void setup() {
        formatter = new RemoveBracketsAddComma();
    }

    @Test
    public void testFormat() throws Exception {
        assertEquals("some text,", formatter.format("{some text}"));
        assertEquals("some text", formatter.format("{some text"));
        assertEquals("some text,", formatter.format("some text}"));
    }
}