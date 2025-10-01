package org.jabref.logic.bst.util;

import java.util.stream.Stream;

import org.jabref.model.entry.AuthorList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BstNameFormatterTest {

    private static final String EDOUARD_MASTERLY = "{\\'{E}}douard Masterly";
    private static final String MEYER_AND_DE_LA_VALLEE_POUSSIN = "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin";
    private static final String UNDERWOOD_NET_AND_POT = "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot";
    private static final String VICTOR_AND_CIERVA = "Paul {\\'E}mile Victor and and de la Cierva y Codorn{\\’\\i}u, Juan";

    @Test
    void umlautsFullNames() {
        AuthorList list = AuthorList.parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

        assertEquals("de~laVall{\\'e}e~PoussinCharles Louis Xavier~Joseph",
                BstNameFormatter.formatName(list.getAuthor(0), "{vv}{ll}{jj}{ff}"));
    }

    @Test
    void umlautsAbbreviations() {
        AuthorList list = AuthorList.parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

        assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J.",
                BstNameFormatter.formatName(list.getAuthor(0), "{vv~}{ll}{, jj}{, f.}"));
    }

    @Test
    void umlautsAbbreviationsWithQuestionMark() {
        AuthorList list = AuthorList.parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");

        assertEquals("de~la Vall{\\'e}e~Poussin, C.~L. X.~J?",
                BstNameFormatter.formatName(list.getAuthor(0), "{vv~}{ll}{, jj}{, f}?"));
    }

    @Test
    void formatName() {
        AuthorList list = AuthorList.parse("Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertEquals("dlVP", BstNameFormatter.formatName(list.getAuthor(0), "{v{}}{l{}}"));
    }

    static Stream<Arguments> provideNameFormattingCases() {
        return Stream.of(
                // Format pattern: "{vv~}{ll}{, jj}{, f}?"
                Arguments.of("Meyer, J?", MEYER_AND_DE_LA_VALLEE_POUSSIN, "{vv~}{ll}{, jj}{, f}?"),
                Arguments.of("Masterly, {\\'{E}}?", EDOUARD_MASTERLY, "{vv~}{ll}{, jj}{, f}?"),
                Arguments.of("{\\\"{U}}nderwood, U?", UNDERWOOD_NET_AND_POT, "{vv~}{ll}{, jj}{, f}?"),
                Arguments.of("Victor, P.~{\\'E}?", VICTOR_AND_CIERVA, "{vv~}{ll}{, jj}{, f}?"),

                // Format pattern: "{f.~}{vv~}{ll}{, jj}"
                Arguments.of("J.~Meyer", MEYER_AND_DE_LA_VALLEE_POUSSIN, "{f.~}{vv~}{ll}{, jj}"),
                Arguments.of("{\\'{E}}.~Masterly", EDOUARD_MASTERLY, "{f.~}{vv~}{ll}{, jj}"),
                Arguments.of("U.~{\\\"{U}}nderwood", UNDERWOOD_NET_AND_POT, "{f.~}{vv~}{ll}{, jj}"),
                Arguments.of("P.~{\\'E}. Victor", VICTOR_AND_CIERVA, "{f.~}{vv~}{ll}{, jj}"),

                // Format pattern: "{ff }{vv }{ll}{ jj}"
                Arguments.of("Jonathan Meyer", MEYER_AND_DE_LA_VALLEE_POUSSIN, "{ff }{vv }{ll}{ jj}"),
                Arguments.of(EDOUARD_MASTERLY, EDOUARD_MASTERLY, "{ff }{vv }{ll}{ jj}"),
                Arguments.of("Ulrich {\\\"{U}}nderwood", UNDERWOOD_NET_AND_POT, "{ff }{vv }{ll}{ jj}"),
                Arguments.of("Paul~{\\'E}mile Victor", VICTOR_AND_CIERVA, "{ff }{vv }{ll}{ jj}")
        );
    }

    @ParameterizedTest
    @MethodSource("provideNameFormattingCases")
    void formatNameVariations(String expected, String authorList, String formatString) {
        assertEquals(expected, BstNameFormatter.formatName(authorList, 1, formatString));
    }

    @Test
    void matchingBraceConsumedForCompleteWords() {
        StringBuilder sb = new StringBuilder();
        assertEquals(6, BstNameFormatter.consumeToMatchingBrace(sb, "{HELLO} {WORLD}".toCharArray(), 0));
        assertEquals("{HELLO}", sb.toString());
    }

    @Test
    void matchingBraceConsumedForBracesInWords() {
        StringBuilder sb = new StringBuilder();
        assertEquals(18, BstNameFormatter.consumeToMatchingBrace(sb, "{HE{L{}L}O} {WORLD}".toCharArray(), 12));
        assertEquals("{WORLD}", sb.toString());
    }

    @Test
    void consumeToMatchingBrace() {
        StringBuilder sb = new StringBuilder();
        assertEquals(10, BstNameFormatter.consumeToMatchingBrace(sb, "{HE{L{}L}O} {WORLD}".toCharArray(), 0));
        assertEquals("{HE{L{}L}O}", sb.toString());
    }

    @ParameterizedTest
    @CsvSource({"C, Charles", "V, Vall{\\'e}e", "{\\'e}, {\\'e}", "{\\'e, {\\'e", "E, {E"})
    void getFirstCharOfString(String expected, String s) {
        assertEquals(expected, BstNameFormatter.getFirstCharOfString(s));
    }

    @ParameterizedTest
    @CsvSource({"6, Vall{\\'e}e, -1",
            "2, Vall{\\'e}e, 2",
            "1, Vall{\\'e}e, 1",
            "6, Vall{\\'e}e, 6",
            "6, Vall{\\'e}e, 7",
            "8, Vall{e}e, -1",
            "6, Vall{\\'e this will be skipped}e, -1"
    })
    void numberOfChars(int expected, String token, int inStop) {
        assertEquals(expected, BstNameFormatter.numberOfChars(token, inStop));
    }
}
