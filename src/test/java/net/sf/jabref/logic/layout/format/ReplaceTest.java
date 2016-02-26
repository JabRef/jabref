package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class ReplaceTest {

    @Test
    public void testSimpleText() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Bob,Ben");
        Assert.assertEquals("Ben Bruce", a.format("Bob Bruce"));
    }

    @Test
    public void testSimpleTextNoHit() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Bob,Ben");
        Assert.assertEquals("Jolly Jumper", a.format("Jolly Jumper"));
    }

    @Test
    public void testFormatNull() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals(null, a.format(null));
    }

    @Test
    public void testFormatEmpty() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("", a.format(""));
    }

    @Test
    public void testNoArgumentSet() {
        ParamLayoutFormatter a = new Replace();
        Assert.assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testNoProperArgument() {
        ParamLayoutFormatter a = new Replace();
        a.setArgument("Eds.");
        Assert.assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }
}
