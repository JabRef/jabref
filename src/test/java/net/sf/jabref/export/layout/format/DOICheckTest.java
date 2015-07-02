package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DOICheckTest {

    @Test
    @Ignore
    public void testFormat() {
        LayoutFormatter lf = new DOICheck();

        Assert.assertEquals("", lf.format(""));
        Assert.assertEquals(null, lf.format(null));

        Assert.assertEquals("http://dx.doi.org/10.1000/ISBN1-900512-44-0", lf
                .format("10.1000/ISBN1-900512-44-0"));
        Assert.assertEquals("http://dx.doi.org/10.1000/ISBN1-900512-44-0", lf
                .format("http://dx.doi.org/10.1000/ISBN1-900512-44-0"));

        Assert.assertEquals("http://doi.acm.org/10.1000/ISBN1-900512-44-0", lf
                .format("http://doi.acm.org/10.1000/ISBN1-900512-44-0"));

        Assert.assertEquals("http://doi.acm.org/10.1145/354401.354407", lf
                .format("http://doi.acm.org/10.1145/354401.354407"));
        Assert.assertEquals("http://dx.doi.org/10.1145/354401.354407", lf.format("10.1145/354401.354407"));

        // Works even when having a / at the front
        Assert.assertEquals("http://dx.doi.org/10.1145/354401.354407", lf.format("/10.1145/354401.354407"));

        // Obviously a wrong doi, but we still accept it.
        Assert.assertEquals("http://dx.doi.org/10", lf.format("10"));

        // Obviously a wrong doi, but we still accept it.
        Assert.assertEquals("1", lf.format("1"));
    }

}
