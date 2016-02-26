package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class DOIStripTest {

    @Test
    public void testFormat() {
        LayoutFormatter lf = new DOIStrip();

        Assert.assertEquals("", lf.format(""));
        Assert.assertEquals(null, lf.format(null));

        Assert.assertEquals("10.1000/ISBN1-900512-44-0", lf.format("10.1000/ISBN1-900512-44-0"));
        Assert.assertEquals("10.1000/ISBN1-900512-44-0",
                lf.format("http://dx.doi.org/10.1000/ISBN1-900512-44-0"));

        Assert.assertEquals("10.1000/ISBN1-900512-44-0",
                lf.format("http://doi.acm.org/10.1000/ISBN1-900512-44-0"));

    }

}
