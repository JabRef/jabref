package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoveTildeTest {

    @Test
    public void testFormatString() {

        LayoutFormatter l = new RemoveTilde();

        assertEquals("", l.format(""));

        assertEquals("simple", l.format("simple"));

        assertEquals(" ", l.format("~"));

        assertEquals("   ", l.format("~~~"));

        assertEquals(" \\~ ", l.format("~\\~~"));

        assertEquals("\\\\ ", l.format("\\\\~"));

        assertEquals("Doe Joe and Jane, M. and Kamp, J. A.", l
                .format("Doe Joe and Jane, M. and Kamp, J.~A."));

        assertEquals("T\\~olkien, J. R. R.", l
                .format("T\\~olkien, J.~R.~R."));
    }
}
