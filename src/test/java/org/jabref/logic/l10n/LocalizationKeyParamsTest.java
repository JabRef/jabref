package org.jabref.logic.l10n;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LocalizationKeyParamsTest {

    @ParameterizedTest
    @MethodSource("provideTestData")
    public void testReplacePlaceholders(String expected, LocalizationKeyParams input) {
        assertEquals(expected, input.replacePlaceholders());
    }

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of("biblatex mode", new LocalizationKeyParams("biblatex mode")),
                Arguments.of("biblatex mode", new LocalizationKeyParams("%0 mode", "biblatex")),
                Arguments.of("C:\\bla mode", new LocalizationKeyParams("%0 mode", "C:\\bla")),
                Arguments.of("What \n : %e %c a b", new LocalizationKeyParams("What \n : %e %c %0 %1", "a", "b")),
                Arguments.of("What \n : %e %c_a b", new LocalizationKeyParams("What \n : %e %c_%0 %1", "a", "b"))
        );
    }

    @Test
    public void testTooManyParams() {
        assertThrows(IllegalStateException.class, () -> new LocalizationKeyParams("", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"));
    }
}
