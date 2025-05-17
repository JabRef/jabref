package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorsTest {

    private ParamLayoutFormatter authorsLayoutFormatter = new Authors();

    @Test
    void standardUsage() {
        assertEquals("B. C. Bruce, C. Manson and J. Jumper",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    void standardUsageOne() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma, Comma");
        assertEquals("Bruce, Bob Croydon, Jumper, Jolly", authorsLayoutFormatter.format("Bob Croydon Bruce and Jolly Jumper"));
    }

    @Test
    void standardUsageTwo() {
        authorsLayoutFormatter.setArgument("initials");
        assertEquals("B. C. Bruce and J. Jumper", authorsLayoutFormatter.format("Bob Croydon Bruce and Jolly Jumper"));
    }

    @Test
    void standardUsageThree() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma");
        assertEquals("Bruce, Bob Croydon, Manson, Charles and Jumper, Jolly",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    void standardUsageFour() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma, 2");
        assertEquals("Bruce, Bob Croydon et al.",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    void standardUsageFive() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma, 3");
        assertEquals("Bruce, Bob Croydon et al.",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    void standardUsageSix() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma, 3, 2");
        assertEquals("Bruce, Bob Croydon, Manson, Charles et al.",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    void specialEtAl() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma, 3, etal= and a few more");
        assertEquals("Bruce, Bob Croydon and a few more",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @Test
    void standardUsageNull() {
        assertEquals("", authorsLayoutFormatter.format(null));
    }

    @Test
    void standardOxford() {
        authorsLayoutFormatter.setArgument("Oxford");
        assertEquals("B. C. Bruce, C. Manson, and J. Jumper",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper"));
    }

    @Test
    void standardOxfordFullName() {
        authorsLayoutFormatter.setArgument("FullName,Oxford");
        assertEquals("Bob Croydon Bruce, Charles Manson, and Jolly Jumper",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    void standardCommaFullName() {
        authorsLayoutFormatter.setArgument("FullName,Comma,Comma");
        assertEquals("Bob Croydon Bruce, Charles Manson, Jolly Jumper",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    void standardAmpFullName() {
        authorsLayoutFormatter.setArgument("FullName,Amp");
        assertEquals("Bob Croydon Bruce, Charles Manson & Jolly Jumper",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Manson and Jolly Jumper"));
    }

    @Test
    void lastName() {
        authorsLayoutFormatter.setArgument("LastName");
        assertEquals("Bruce, von Manson and Jumper",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles von Manson and Jolly Jumper"));
    }

    @Test
    void middleInitial() {
        authorsLayoutFormatter.setArgument("MiddleInitial");
        assertEquals("Bob C. Bruce, Charles K. von Manson and Jolly Jumper",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    void noPeriod() {
        authorsLayoutFormatter.setArgument("NoPeriod");
        assertEquals("B C Bruce, C K von Manson and J Jumper",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    void etAl() {
        authorsLayoutFormatter.setArgument("2,1");
        assertEquals("B. C. Bruce et al.",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Kermit von Manson and Jumper, Jolly"));
    }

    @Test
    void etAlNotEnoughAuthors() {
        authorsLayoutFormatter.setArgument("2,1");
        assertEquals("B. C. Bruce and C. K. von Manson",
                authorsLayoutFormatter.format("Bruce, Bob Croydon and Charles Kermit von Manson"));
    }

    @Test
    void emptyEtAl() {
        authorsLayoutFormatter.setArgument("fullname, LastFirst, Comma, 3, etal=");
        assertEquals("Bruce, Bob Croydon",
                authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles"));
    }

    @ParameterizedTest(name = "arg={0}, formattedStr={1}")
    @CsvSource({
            "FirstFirst, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // FirstFirst
            "LastFirst, 'Bruce, B. C., Manson, C., Jumper, J. and Chuckles, C.'", // LastFirst
            "LastFirstFirstFirst, 'Bruce, B. C., C. Manson, J. Jumper and C. Chuckles'" // LastFirstFirstFirst
    })
    void authorOrder(String arg, String expectedResult) {
        authorsLayoutFormatter.setArgument(arg);
        String formattedStr = authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles");
        assertEquals(expectedResult, formattedStr);
    }

    @ParameterizedTest(name = "arg={0}, formattedStr={1}")
    @CsvSource({
            "FullName, 'Bob Croydon Bruce, Charles Manson, Jolly Jumper and Chuck Chuckles'", // FullName
            "Initials, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // Initials
            "FirstInitial, 'B. Bruce, C. Manson, J. Jumper and C. Chuckles'", // FirstInitial
            "MiddleInitial, 'Bob C. Bruce, Charles Manson, Jolly Jumper and Chuck Chuckles'", // MiddleInitial
            "LastName, 'Bruce, Manson, Jumper and Chuckles'", // LastName
            "InitialsNoSpace, 'B.C. Bruce, C. Manson, J. Jumper and C. Chuckles'" // InitialsNoSpace
    })
    void authorABRV(String arg, String expectedResult) {
        authorsLayoutFormatter.setArgument(arg);
        String formattedStr = authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles");
        assertEquals(expectedResult, formattedStr);
    }

    @ParameterizedTest(name = "arg={0}, formattedStr={1}")
    @CsvSource({
            "FullPunc, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // FullPunc
            "NoPunc, 'B C Bruce, C Manson, J Jumper and C Chuckles'", // NoPunc
            "NoComma, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // NoComma
            "NoPeriod, 'B C Bruce, C Manson, J Jumper and C Chuckles'" // NoPeriod
    })
    void authorPUNC(String arg, String expectedResult) {
        authorsLayoutFormatter.setArgument(arg);
        String formattedStr = authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles");
        assertEquals(expectedResult, formattedStr);
    }

    @ParameterizedTest(name = "arg={0}, formattedStr={1}")
    @CsvSource({
            "Comma, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // Comma
            "And, 'B. C. Bruce and C. Manson and J. Jumper and C. Chuckles'", // And
            "Colon, 'B. C. Bruce: C. Manson: J. Jumper and C. Chuckles'", // Colon
            "Semicolon, 'B. C. Bruce; C. Manson; J. Jumper and C. Chuckles'", // Semicolon
            "Oxford, 'B. C. Bruce, C. Manson, J. Jumper, and C. Chuckles'", // Oxford
            "Amp, 'B. C. Bruce, C. Manson, J. Jumper & C. Chuckles'", // Amp
            "Sep, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // Sep
            "LastSep, 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // LastSep
            "Sep=|, 'B. C. Bruce|C. Manson|J. Jumper and C. Chuckles'", // Custom Sep
            "LastSep=|, 'B. C. Bruce, C. Manson, J. Jumper|C. Chuckles'", // Custom LastSep
            "'Comma, And', 'B. C. Bruce, C. Manson, J. Jumper and C. Chuckles'", // Comma And
            "'Comma, Colon', 'B. C. Bruce, C. Manson, J. Jumper: C. Chuckles'", // Comma Colon
            "'Comma, Semicolon', 'B. C. Bruce, C. Manson, J. Jumper; C. Chuckles'", // Comma Semicolon
    })
    void authorSEPARATORS(String arg, String expectedResult) {
        authorsLayoutFormatter.setArgument(arg);
        String formattedStr = authorsLayoutFormatter.format("Bob Croydon Bruce and Charles Manson and Jolly Jumper and Chuck Chuckles");
        assertEquals(expectedResult, formattedStr);
    }
}
