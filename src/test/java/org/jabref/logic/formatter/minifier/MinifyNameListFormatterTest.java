package org.jabref.logic.formatter.minifier;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class MinifyNameListFormatterTest {

    private MinifyNameListFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new MinifyNameListFormatter();
    }

    @ParameterizedTest
    @MethodSource("provideAuthorNames")
    void minifyAuthorNames(String expectedAuthorNames, String originalAuthorNames) {
        assertEquals(expectedAuthorNames, formatter.format(originalAuthorNames));
    }

    private static Stream<Arguments> provideAuthorNames() {
        return Stream.of(
                Arguments.of("Simon Harrer", "Simon Harrer"),
                Arguments.of("Simon Harrer and others", "Simon Harrer and others"),
                Arguments.of("Simon Harrer and Jörg Lenhard", "Simon Harrer and Jörg Lenhard"),
                Arguments.of("Simon Harrer and others", "Simon Harrer and Jörg Lenhard and Guido Wirtz"),
                Arguments.of("Simon Harrer and others", "Simon Harrer and Jörg Lenhard and Guido Wirtz and others"),
                Arguments.of("Stefan Kolb and others", new MinifyNameListFormatter().getExampleInput())
                );
    }
}
