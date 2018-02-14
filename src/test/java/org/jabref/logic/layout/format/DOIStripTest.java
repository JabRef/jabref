package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DOIStripTest {

    @Test
    public void testFormat() {
        LayoutFormatter lf = new DOIStrip();

        assertEquals("", lf.format(""));
        assertEquals(null, lf.format(null));

        assertEquals("10.1000/ISBN1-900512-44-0", lf.format("10.1000/ISBN1-900512-44-0"));
        assertEquals("10.1000/ISBN1-900512-44-0",
                lf.format("http://dx.doi.org/10.1000/ISBN1-900512-44-0"));

        assertEquals("10.1000/ISBN1-900512-44-0",
                lf.format("http://doi.acm.org/10.1000/ISBN1-900512-44-0"));

    }

}
