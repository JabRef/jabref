package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorsTest {

    @Test
    public void testStandardUsage() {
        ParamLayoutFormatter a = new Authors();
        assertEquals("B. C. Bruce, C. Manson and J. Jumper",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageOne() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, Comma");
        assertEquals("Bruce, Bob Croydon, Jumper, Jolly", a.format("Bob Croydon Bruce and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageTwo() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("initials");
        assertEquals("B. C. Bruce and J. Jumper", a.format("Bob Croydon Bruce and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageThree() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma");
        assertEquals("Bruce, Bob Croydon, Manson, Charles and Jumper, Jolly",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageFour() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 2");
        assertEquals("Bruce, Bob Croydon et al.",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardUsageFive() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3");
        assertEquals("Bruce, Bob Croydon et al.",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    public void testStandardUsageSix() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3, 2");
        assertEquals("Bruce, Bob Croydon, Manson, Charles et al.",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    /**
     * Test the FirstFirst method in authors order.
     * setArgument() will pass the String into handleArgument() to set flMode.
     * Increase branch coverage from 58% to 61%.
     */
    @Test
    public void testStandardUsageFirstFirst() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FirstFirst, Comma, Comma");
        assertEquals("B. C. Bruce, C. Manson, J. Jumper",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    /**
     * Test the And method in separators of authors.
     * setArgument() will pass the String into handleArgument() to set separator and lastSeparator.
     * Increase branch coverage from 58% to 66%.
     */
    @Test
    public void testStandardUsageAnd() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, And");
        assertEquals("Bruce, Bob Croydon, Jumper, Jolly and Manson, Charles", 
                a.format("Bob Croydon Bruce and Jolly Jumper and Charles Manson"));
        
        a = new Authors();
        a.setArgument("fullname, LastFirst, Add, And");
        assertEquals("Bruce, Bob Croydon and Jumper, Jolly and Manson, Charles", 
                a.format("Bob Croydon Bruce and Jolly Jumper and Charles Manson"));
    }

    /**
     * Test the Colon method in separators of authors.
     * setArgument() will pass the String into handleArgument() to set separator and lastSeparator.
     * Increase branch coverage from 58% to 66%
     */
    @Test
    public void testStandardUsageColon() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Colon, Colon");
        assertEquals("Bruce, Bob Croydon: Jumper, Jolly: Manson, Charles", 
                a.format("Bob Croydon Bruce and Jolly Jumper and Charles Manson"));
    }

    @Test
    public void testSpecialEtAl() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3, etal= and a few more");
        assertEquals("Bruce, Bob Croydon and a few more",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    public void testStandardUsageNull() {
        ParamLayoutFormatter a = new Authors();
        assertEquals("", a.format(null));
    }

    @Test
    public void testStandardOxford() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("Oxford");
        assertEquals("B. C. Bruce, C. Manson, and J. Jumper",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardOxfordFullName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FullName,Oxford");
        assertEquals("Bob Croydon Bruce, Charles Manson, and Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardCommaFullName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FullName,Comma,Comma");
        assertEquals("Bob Croydon Bruce, Charles Manson, Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testStandardAmpFullName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FullName,Amp");
        assertEquals("Bob Croydon Bruce, Charles Manson & Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    public void testLastName() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("LastName");
        assertEquals("Bruce, von Manson and Jumper",
                a.format("Bruce, Bob Croydon and Charles von Manson and Jolly Jumper"));
    }

    @Test
    public void testMiddleInitial() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("MiddleInitial");
        assertEquals("Bob C. Bruce, Charles K. von Manson and Jolly Jumper",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    /**
     * Test the FirstInitial method in abbreviation of authors.
     * setArgument() will pass the String into handleArgument() to set abbreviate.
     * Increase branch coverage from 58% to 61%.
     */
    @Test
    public void testFirstInitial() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("FirstInitial");
        assertEquals("B. Bruce, C. von Manson and J. Jumper",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    public void testNoPeriod() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("NoPeriod");
        assertEquals("B C Bruce, C K von Manson and J Jumper",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    public void testEtAl() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("2,1");
        assertEquals("B. C. Bruce et al.",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    public void testEtAlNotEnoughAuthors() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("2,1");
        assertEquals("B. C. Bruce and C. K. von Manson",
                a.format("Bruce, Bob Croydon and Charles Kermit von Manson"));
    }

    @Test
    public void testEmptyEtAl() {
        ParamLayoutFormatter a = new Authors();
        a.setArgument("fullname, LastFirst, Comma, 3, etal=");
        assertEquals("Bruce, Bob Croydon",
                a.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }
}
