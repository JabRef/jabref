package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultTest {

    ParamLayoutFormatter paramLayoutFormatter = new Default();

    @ParameterizedTest
    @MethodSource("formatTests")
    void paramLayoutFormatTest(String expectedString, String inputString, String formatterArgument) {
        if (!formatterArgument.isEmpty()) {
            paramLayoutFormatter.setArgument(formatterArgument);
        }
        assertEquals(expectedString, paramLayoutFormatter.format(inputString));
    }

    private static Stream<Arguments> formatTests() {
        return Stream.of(
                Arguments.of("Bob Bruce", "Bob Bruce", "DEFAULT TEXT"),
                Arguments.of("DEFAULT TEXT", null, "DEFAULT TEXT"),
                Arguments.of("DEFAULT TEXT", "", "DEFAULT TEXT"),
                Arguments.of("Bob Bruce and Jolly Jumper", "Bob Bruce and Jolly Jumper", ""),
                Arguments.of("", null, ""),
                Arguments.of("", "", "")
        );
    }
}
