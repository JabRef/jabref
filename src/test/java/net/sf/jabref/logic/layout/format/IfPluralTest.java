package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class IfPluralTest {

    @Test
    public void testStandardUsageOneEditor() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("Ed.", a.format("Bob Bruce"));
    }

    @Test
    public void testStandardUsageTwoEditors() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("Eds.", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testFormatNull() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("", a.format(null));
    }

    @Test
    public void testFormatEmpty() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("", a.format(""));
    }

    @Test
    public void testNoArgumentSet() {
        ParamLayoutFormatter a = new IfPlural();
        Assert.assertEquals("", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testNoProperArgument() {
        ParamLayoutFormatter a = new IfPlural();
        a.setArgument("Eds.");
        Assert.assertEquals("", a.format("Bob Bruce and Jolly Jumper"));
    }
}
