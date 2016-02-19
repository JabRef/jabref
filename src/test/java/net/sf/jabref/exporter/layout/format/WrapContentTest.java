package net.sf.jabref.exporter.layout.format;

import net.sf.jabref.exporter.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class WrapContentTest {

    @Test
    public void testSimpleText() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Bob,Ben");
        Assert.assertEquals("BobBob BruceBen", a.format("Bob Bruce"));
    }

    @Test
    public void testEmptyStart() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument(",Ben");
        Assert.assertEquals("Bob BruceBen", a.format("Bob Bruce"));
    }

    @Test
    public void testEmptyEnd() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Bob,");
        Assert.assertEquals("BobBob Bruce", a.format("Bob Bruce"));
    }

    @Test
    public void testFormatNull() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals(null, a.format(null));
    }

    @Test
    public void testFormatEmpty() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("", a.format(""));
    }

    @Test
    public void testNoArgumentSet() {
        ParamLayoutFormatter a = new WrapContent();
        Assert.assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testNoProperArgument() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.");
        Assert.assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }
}
