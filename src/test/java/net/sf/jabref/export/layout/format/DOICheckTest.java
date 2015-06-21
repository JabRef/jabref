package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DOICheckTest {

    @Test
    @Ignore
    public void testFormat() {
        LayoutFormatter lf = new DOICheck();

        assertEquals("", lf.format(""));
        assertEquals(null, lf.format(null));

        assertEquals("http://dx.doi.org/10.1000/ISBN1-900512-44-0", lf
                .format("10.1000/ISBN1-900512-44-0"));
        assertEquals("http://dx.doi.org/10.1000/ISBN1-900512-44-0", lf
                .format("http://dx.doi.org/10.1000/ISBN1-900512-44-0"));

        assertEquals("http://doi.acm.org/10.1000/ISBN1-900512-44-0", lf
                .format("http://doi.acm.org/10.1000/ISBN1-900512-44-0"));

        assertEquals("http://doi.acm.org/10.1145/354401.354407", lf
                .format("http://doi.acm.org/10.1145/354401.354407"));
        assertEquals("http://dx.doi.org/10.1145/354401.354407", lf.format("10.1145/354401.354407"));

        // Works even when having a / at the front
        assertEquals("http://dx.doi.org/10.1145/354401.354407", lf.format("/10.1145/354401.354407"));

        // Obviously a wrong doi, but we still accept it.
        assertEquals("http://dx.doi.org/10", lf.format("10"));

        // Obviously a wrong doi, but we still accept it.
        assertEquals("1", lf.format("1"));
    }

}
