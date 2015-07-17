package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RemoveTildeTest {

    @Test
    public void testFormatString() {

        LayoutFormatter l = new RemoveTilde();

        Assert.assertEquals("", l.format(""));

        Assert.assertEquals("simple", l.format("simple"));

        Assert.assertEquals(" ", l.format("~"));

        Assert.assertEquals("   ", l.format("~~~"));

        Assert.assertEquals(" \\~ ", l.format("~\\~~"));

        Assert.assertEquals("\\\\ ", l.format("\\\\~"));

        Assert.assertEquals("Doe Joe and Jane, M. and Kamp, J. A.", l
                .format("Doe Joe and Jane, M. and Kamp, J.~A."));

        Assert.assertEquals("T\\~olkien, J. R. R.", l
                .format("T\\~olkien, J.~R.~R."));
    }
}
