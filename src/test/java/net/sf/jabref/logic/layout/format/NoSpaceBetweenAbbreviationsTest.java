package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class NoSpaceBetweenAbbreviationsTest {

    @Test
    public void testFormat() {
        LayoutFormatter f = new NoSpaceBetweenAbbreviations();
        Assert.assertEquals("", f.format(""));
        Assert.assertEquals("John Meier", f.format("John Meier"));
        Assert.assertEquals("J.F. Kennedy", f.format("J. F. Kennedy"));
        Assert.assertEquals("J.R.R. Tolkien", f.format("J. R. R. Tolkien"));
        Assert.assertEquals("J.R.R. Tolkien and J.F. Kennedy", f.format("J. R. R. Tolkien and J. F. Kennedy"));
    }

}
