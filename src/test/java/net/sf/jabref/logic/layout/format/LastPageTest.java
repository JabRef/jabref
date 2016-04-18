package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class LastPageTest {

    @Test
    public void testFormatEmpty() {
        LayoutFormatter a = new LastPage();
        Assert.assertEquals("", a.format(""));
    }

    @Test
    public void testFormatNull() {
        LayoutFormatter a = new LastPage();
        Assert.assertEquals("", a.format(null));
    }

    @Test
    public void testFormatSinglePage() {
        LayoutFormatter a = new LastPage();
        Assert.assertEquals("345", a.format("345"));
    }

    @Test
    public void testFormatSingleDash() {
        LayoutFormatter a = new LastPage();
        Assert.assertEquals("350", a.format("345-350"));
    }

    @Test
    public void testFormatDoubleDash() {
        LayoutFormatter a = new LastPage();
        Assert.assertEquals("350", a.format("345--350"));
    }

    @Test
    public void testFinalCoverageCase() {
        LayoutFormatter a = new LastPage();
        Assert.assertEquals("", a.format("--"));
    }
}
