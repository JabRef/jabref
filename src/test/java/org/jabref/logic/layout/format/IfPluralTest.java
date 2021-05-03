package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IfPluralTest {

    ParamLayoutFormatter ifPluralFormatter = new IfPlural();

    @ParameterizedTest
    @MethodSource("formatTests")
    void testIfPluralFormat(String expectedString, String inputString, String formatterArgument) {
        if (!formatterArgument.isEmpty()) {
            ifPluralFormatter.setArgument(formatterArgument);
        }
        assertEquals(expectedString, ifPluralFormatter.format(inputString));
    }

    private static Stream<Arguments> formatTests() {
        return Stream.of(
                Arguments.of("Ed.", "Bob Bruce", "Eds.,Ed."),
                Arguments.of("Eds.", "Bob Bruce and Jolly Jumper", "Eds.,Ed."),
                Arguments.of("", null, "Eds.,Ed."),
                Arguments.of("", "", "Eds.,Ed."),
                Arguments.of("", "Bob Bruce and Jolly Jumper", ""),
                Arguments.of("", "Bob Bruce and Jolly Jumper", "Eds.")
        );
    }
}
