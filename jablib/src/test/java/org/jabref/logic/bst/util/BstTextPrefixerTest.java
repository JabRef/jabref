package org.jabref.logic.bst.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BstTextPrefixerTest {
    @ParameterizedTest
    @CsvSource({
            "i, i",
            "0I~ , 0I~ ",
            "Hi Hi, Hi Hi ",
            "{\\oe}, {\\oe}",
            "Hi {\\oe   }H, Hi {\\oe   }Hi ",
            "Jonat, Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin",
            "{\\'e}, {\\'e}",
            "{\\'{E}}doua, {\\'{E}}douard Masterly",
            "Ulric, Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot",
            "abcd{e}, abcd{efg}hi",
            "ab{cd}e, ab{cd}efghi",
            "ab{cd}e, ab{cd}efghi{}",
            "Hi {{\\o}}, Hi {{\\oe   }}Hi ",
            "Hi {\\{oe   }}H, Hi {\\{oe   }}Hi ",
            "Hi {\\\"oe   }H, Hi {\\\"oe   }Hi ",
            "Hi {\\{\\oe   }}H, Hi {\\{\\oe   }}Hi "
    })
    void assertPrefix(final String expectedResult, final String toPrefixInput) {
        assertEquals(expectedResult, BstTextPrefixer.textPrefix(5, toPrefixInput));
    }
}
