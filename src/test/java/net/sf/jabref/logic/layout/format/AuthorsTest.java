package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorsTest {

    @Test
    public void testStandardUsage() {
        ParamLayoutFormatter a = new Authors();
        Assert.assertEquals("B. C. Bruce, C. Manson and J. Jumper",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

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
    public void testStandardUsageFive() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3");
        Assert.assertEquals("Bruce, Bob Croydon et al.",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    public void testStandardUsageSix() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3, 2");
        Assert.assertEquals("Bruce, Bob Croydon, Manson, Charles et al.",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    public void testSpecialEtAl() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3, etal= and a few more");
        Assert.assertEquals("Bruce, Bob Croydon and a few more",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    public void testStandardUsageNull() {
        ParamLayoutFormatter a = new Authors();
        Assert.assertEquals("", a.format(null));
    }

    @Test
    public void testStandardOxford() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("Oxford");
        Assert.assertEquals("B. C. Bruce, C. Manson, and J. Jumper",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardOxfordFullName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FullName,Oxford");
        Assert.assertEquals("Bob Croydon Bruce, Charles Manson, and Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardCommaFullName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FullName,Comma,Comma");
        Assert.assertEquals("Bob Croydon Bruce, Charles Manson, Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardAmpFullName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FullName,Amp");
        Assert.assertEquals("Bob Croydon Bruce, Charles Manson & Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testLastName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("LastName");
        Assert.assertEquals("Bruce, von Manson and Jumper",
                a.format("Bruce, Bob Croydon and Charles von Manson and Jolly Jumper"));
    }

    @Test
    public void testMiddleInitial() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("MiddleInitial");
        Assert.assertEquals("Bob C. Bruce, Charles K. von Manson and Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    public void testNoPeriod() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("NoPeriod");
        Assert.assertEquals("B C Bruce, C K von Manson and J Jumper",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }
}
