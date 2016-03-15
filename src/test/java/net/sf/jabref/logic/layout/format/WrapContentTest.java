package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class WrapContentTest {

    @Test
    public void testSimpleText() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("<,>");
        Assert.assertEquals("<Bob>", a.format("Bob"));
    }

    @Test
    public void testEmptyStart() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument(",:");
        Assert.assertEquals("Bob:", a.format("Bob"));
    }

    @Test
    public void testEmptyEnd() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Content: ,");
        Assert.assertEquals("Content: Bob", a.format("Bob"));
    }

    @Test
    public void testEscaping() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Name\\,Field\\,,\\,Author");
        Assert.assertEquals("Name,Field,Bob,Author", a.format("Bob"));
    }

    @Test
    public void testFormatNullExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals(null, a.format(null));
    }

    @Test
    public void testFormatEmptyExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.,Ed.");
        Assert.assertEquals("", a.format(""));
    }

    @Test
    public void testNoArgumentSetExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        Assert.assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }

    @Test
    public void testNoProperArgumentExpectNothingAdded() {
        ParamLayoutFormatter a = new WrapContent();
        a.setArgument("Eds.");
        Assert.assertEquals("Bob Bruce and Jolly Jumper", a.format("Bob Bruce and Jolly Jumper"));
    }
}
