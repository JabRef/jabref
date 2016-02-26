package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RemoveTildeTest {
    private LayoutFormatter formatter;

    @Before
    public void setUp() {
        formatter = new RemoveTilde();
    }

    @Test
    public void testFormatString() {
        Assert.assertEquals("", formatter.format(""));
        Assert.assertEquals("simple", formatter.format("simple"));
        Assert.assertEquals(" ", formatter.format("~"));
        Assert.assertEquals("   ", formatter.format("~~~"));
        Assert.assertEquals(" \\~ ", formatter.format("~\\~~"));
        Assert.assertEquals("\\\\ ", formatter.format("\\\\~"));
        Assert.assertEquals("Doe Joe and Jane, M. and Kamp, J. A.", formatter.format("Doe Joe and Jane, M. and Kamp, J.~A."));
        Assert.assertEquals("T\\~olkien, J. R. R.", formatter.format("T\\~olkien, J.~R.~R."));
    }
}
