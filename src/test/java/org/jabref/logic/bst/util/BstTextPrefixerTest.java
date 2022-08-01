package org.jabref.logic.bst.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BstTextPrefixerTest {

    @Test
    public void testPrefix() {
        assertPrefix("i", "i");
        assertPrefix("0I~ ", "0I~ ");
        assertPrefix("Hi Hi", "Hi Hi ");
        assertPrefix("{\\oe}", "{\\oe}");
        assertPrefix("Hi {\\oe   }H", "Hi {\\oe   }Hi ");
        assertPrefix("Jonat", "Jonathan Meyer and Charles Louis Xavier Joseph de la Vall{\\'e}e Poussin");
        assertPrefix("{\\'e}", "{\\'e}");
        assertPrefix("{\\'{E}}doua", "{\\'{E}}douard Masterly");
        assertPrefix("Ulric", "Ulrich {\\\"{U}}nderwood and Ned {\\~N}et and Paul {\\={P}}ot");
    }

    private static void assertPrefix(final String string, final String string2) {
        assertEquals(string, BstTextPrefixer.textPrefix(5, string2));
    }
}
