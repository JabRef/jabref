package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorNatBibTest {

    LayoutFormatter authorNatBibFormatter = new AuthorNatBib();

    @ParameterizedTest
    @MethodSource("formatTests")
    void paramLayoutFormatTest(String expectedString, String inputString) {
        assertEquals(expectedString, authorNatBibFormatter.format(inputString));
    }

    private static Stream<Arguments> formatTests() {
        return Stream.of(
                // Test method for {@link org.jabref.logic.layout.format.AuthorNatBib#format(java.lang.String)}.
                Arguments.of("von Neumann et al.", "von Neumann,,John and John Smith and Black Brown, Jr, Peter"),

                // Test method for {@link org.jabref.logic.layout.format.AuthorLF_FF#format(java.lang.String)}.
                Arguments.of("von Neumann and Smith", "von Neumann,,John and John Smith"),
                Arguments.of("von Neumann", "von Neumann, John")
        );
    }
}
