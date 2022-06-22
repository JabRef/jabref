package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ToUpperCaseTest {

    ToUpperCase upperCase = new ToUpperCase();

    @ParameterizedTest
    @MethodSource("toUpperCaseTests")
    void toUpperCaseTests(String expectedString, String inputString) {
        assertEquals(expectedString, upperCase.format(inputString));
    }

    private static Stream<Arguments> toUpperCaseTests() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of(null, null),
                Arguments.of("ABCD EFG", "abcd efg"),
                Arguments.of("ABCD EFG", "ABCD EFG"),
                Arguments.of("ABCD EFG", "abCD eFg")
        );
    }
}
