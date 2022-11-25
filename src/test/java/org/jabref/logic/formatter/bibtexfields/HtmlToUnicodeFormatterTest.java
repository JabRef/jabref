package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HtmlToUnicodeFormatterTest {

    private HtmlToUnicodeFormatter formatter;

    private static Stream<Arguments> data() {
        return Stream.of(
                         Arguments.of("abc", "abc"),
                         Arguments.of("åäö", "&aring;&auml;&ouml;"),
                         Arguments.of("í", "i&#x301;"),
                         Arguments.of("Ε", "&Epsilon;"),
                         Arguments.of("ä", "&auml;"),
                         Arguments.of("ä", "&#228;"),
                         Arguments.of("ä", "&#xe4;"),
                         Arguments.of("ñ", "&#241;"),
                         Arguments.of("aaa", "<p>aaa</p>"),
                         Arguments.of("bread & butter", "<b>bread</b> &amp; butter"));
    }

    @BeforeEach
    public void setUp() {
        formatter = new HtmlToUnicodeFormatter();
    }

    @ParameterizedTest
    @MethodSource("data")
    void testFormatterWorksCorrectly(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
