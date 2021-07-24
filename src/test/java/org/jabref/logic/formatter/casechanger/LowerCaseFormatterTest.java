package org.jabref.logic.formatter.casechanger;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class LowerCaseFormatterTest {

    private LowerCaseFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new LowerCaseFormatter();
    }

    @ParameterizedTest
    @MethodSource("provideStringsForFormat")
    public void test(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }

    private static Stream<Arguments> provideStringsForFormat() {
        return Stream.of(
                Arguments.of("lower", "lower"),
                Arguments.of("lower", "LOWER"),
                Arguments.of("lower {UPPER}", "LOWER {UPPER}"),
                Arguments.of("lower {U}pper", "LOWER {U}PPER")
        );
    }

    @Test
    public void formatExample() {
        assertEquals("kde {Amarok}", formatter.format(formatter.getExampleInput()));
    }
}
