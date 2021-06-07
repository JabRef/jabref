package org.jabref.logic.l10n;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalizationKeyTest {

    private static Stream<Arguments> propertiesKeyTestCases() {
        return Stream.of(
                // underscore is preserved
                Arguments.of("test_with_underscore", "test_with_underscore"),

                // Java code: Copy \\cite{citation key}
                // String representation: "Copy \\\\cite{citation key}"
                // In other words: The property is "Copy\ \\cite{citation\\ key}", because the "\" before "cite" is a backslash, not an escape for the c. That property is "Copy\ \\cite{citation\ key}" in Java code, because of the escaping of the backslash.
                Arguments.of("Copy\\ \\\\cite{citation\\ key}", "Copy \\\\cite{citation key}"),

                // Java code: Newline follows\n
                // String representation: "Newline follows\\n"
                Arguments.of("Newline\\ follows\\n", "Newline follows\\n"),

                // Java code: First line\nSecond line
                // String representation: "First line\\nSecond line"
                // In other words: "First line\nSecond line" is the wrong test case, because the source code contains "First line\nSecond line", which is rendered as Java String as "First line\\nSecond line"
                Arguments.of("First\\ line\\nSecond\\ line", "First line\\nSecond line")
        );
    }

    @ParameterizedTest
    @MethodSource("propertiesKeyTestCases")
    public void getPropertiesKeyReturnsCorrectValue(String expected, String input) {
        assertEquals(expected, LocalizationKey.fromEscapedJavaString(input).getEscapedPropertiesKey());
    }
}
