package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NoSpaceBetweenAbbreviationsTest {

    @Test
    public void testFormat() {
        LayoutFormatter f = new NoSpaceBetweenAbbreviations();
        assertEquals("", f.format(""));
        assertEquals("John Meier", f.format("John Meier"));
        assertEquals("J.F. Kennedy", f.format("J. F. Kennedy"));
        assertEquals("J.R.R. Tolkien", f.format("J. R. R. Tolkien"));
        assertEquals("J.R.R. Tolkien and J.F. Kennedy", f.format("J. R. R. Tolkien and J. F. Kennedy"));
    }

}
