package net.sf.jabref.exporter.layout.format;

import net.sf.jabref.exporter.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorsTest {

    @Test
    public void testStandardUsageOne() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, Comma");
        Assert.assertEquals("Bruce, Bob Croydon, Jumper, Jolly", a.format("Bob Croydon Bruce and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageTwo() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("initials");
        Assert.assertEquals("B. C. Bruce and J. Jumper", a.format("Bob Croydon Bruce and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageThree() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma");
        Assert.assertEquals("Bruce, Bob Croydon, Manson, Charles and Jumper, Jolly",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageFour() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 2");
        Assert.assertEquals("Bruce, Bob Croydon et al.",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageNull() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 2");
        Assert.assertEquals("", a.format(null));
    }

}
