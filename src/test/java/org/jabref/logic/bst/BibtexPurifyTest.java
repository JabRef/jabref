package org.jabref.logic.bst;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class BibtexPurifyTest {

    @ParameterizedTest
    @MethodSource("provideTestStrings")
    public void testPurify(String expected, String toBePurified) {
        assertEquals(expected, BibtexPurify.purify(toBePurified, s -> fail("Should not Warn (" + s + ")! purify should be " + expected + " for " + toBePurified)));
    }

    private static Stream<Arguments> provideTestStrings() {
        return Stream.of(
                Arguments.of("i", "i"),
                Arguments.of("0I  ", "0I~ "),
                Arguments.of("Hi Hi ", "Hi Hi "),
                Arguments.of("oe", "{\\oe}"),
                Arguments.of("Hi oeHi ", "Hi {\\oe   }Hi "),
                Arguments.of("Jonathan Meyer and Charles Louis Xavier Joseph de la Vallee Poussin", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin"),
                Arguments.of("e", "{\\'e}"),
                Arguments.of("Edouard Masterly", "{\\'{E}}douard Masterly"),
                Arguments.of("Ulrich Underwood and Ned Net and Paul Pot", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot")
        );
    }
}
