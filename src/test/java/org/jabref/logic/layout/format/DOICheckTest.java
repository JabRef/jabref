package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DOICheckTest {

    @Test
    public void testFormat() {
        LayoutFormatter lf = new DOICheck();

        assertEquals("", lf.format(""));
        assertEquals(null, lf.format(null));

        assertEquals("https://doi.org/10.1000/ISBN1-900512-44-0", lf.format("10.1000/ISBN1-900512-44-0"));
        assertEquals("https://doi.org/10.1000/ISBN1-900512-44-0",
                lf.format("http://dx.doi.org/10.1000/ISBN1-900512-44-0"));

        assertEquals("https://doi.org/10.1000/ISBN1-900512-44-0",
                lf.format("http://doi.acm.org/10.1000/ISBN1-900512-44-0"));

        assertEquals("https://doi.org/10.1145/354401.354407",
                lf.format("http://doi.acm.org/10.1145/354401.354407"));
        assertEquals("https://doi.org/10.1145/354401.354407", lf.format("10.1145/354401.354407"));

        // Works even when having a / at the front
        assertEquals("https://doi.org/10.1145/354401.354407", lf.format("/10.1145/354401.354407"));

        // Obviously a wrong doi, will not change anything.
        assertEquals("10", lf.format("10"));

        // Obviously a wrong doi, will not change anything.
        assertEquals("1", lf.format("1"));
    }

}
