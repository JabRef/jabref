package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class FirstPageTest {

    @Test
    public void testFormatEmpty() {
        LayoutFormatter a = new FirstPage();
        Assert.assertEquals("", a.format(""));
    }

    @Test
    public void testFormatNull() {
        LayoutFormatter a = new FirstPage();
        Assert.assertEquals("", a.format(null));
    }

    @Test
    public void testFormatSinglePage() {
        LayoutFormatter a = new FirstPage();
        Assert.assertEquals("345", a.format("345"));
    }

    @Test
    public void testFormatSingleDash() {
        LayoutFormatter a = new FirstPage();
        Assert.assertEquals("345", a.format("345-350"));
    }

    @Test
    public void testFormatDoubleDash() {
        LayoutFormatter a = new FirstPage();
        Assert.assertEquals("345", a.format("345--350"));
    }
}
