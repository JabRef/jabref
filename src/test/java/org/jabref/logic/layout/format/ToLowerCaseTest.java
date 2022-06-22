package org.jabref.logic.layout.format;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ToLowerCaseTest {

    @Test
    public void testNull() {
        assertNull(new ToLowerCase().format(null));
    }

    @ParameterizedTest
    @MethodSource("provideArguments")
    public void toLowerCaseWithDifferentInputs(String expectedString, String originalString) {
        assertEquals(expectedString, new ToLowerCase().format(originalString));
    }

    private static Stream<Arguments> provideArguments() {
        return Stream.of(
                Arguments.of("", ""),
                Arguments.of("abcd efg", "abcd efg"),
                Arguments.of("abcd efg", "ABCD EFG"),
                Arguments.of("abcd efg", "abCD eFg"),
                Arguments.of("abcd123efg", "abCD123eFg"),
                Arguments.of("hello!*#", "Hello!*#"),
                Arguments.of("123*%&456", "123*%&456")
        );
    }
}
