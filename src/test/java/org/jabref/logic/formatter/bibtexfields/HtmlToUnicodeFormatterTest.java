package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HtmlToUnicodeFormatterTest {

    private static Stream<Arguments> data() {
        return Stream.of(
                         Arguments.of("abc", "abc"),
                         Arguments.of("&aring;&auml;&ouml;", "åäö"),
                         Arguments.of("i&#x301;", "i"),
                         Arguments.of("Ε", "&Epsilon;"),
                         Arguments.of("ä", "&auml;"),
                         Arguments.of("ä", "&#228"),
                         Arguments.of("ä", "&#xe4;"),
                         Arguments.of("ñ", "&#241;"),
                         Arguments.of("aaa", "<p>aaa</p>"),
                         Arguments.of("bread & butter", "<b>bread</b> &amp; butter"));
    }

    private HtmlToUnicodeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new HtmlToUnicodeFormatter();
    }

    @Test
    @MethodSource("data")
    void testFormatterWorksCorrectly(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
