package org.jabref.logic.util.strings;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringLengthComparatorTest {

    private StringLengthComparator slc;

    @BeforeEach
    public void setUp() {
        slc = new StringLengthComparator();
    }

    @ParameterizedTest
    @MethodSource("tests")
    void compareStringLength(int comparisonResult, String firstString, String secondString) {
        assertEquals(comparisonResult, slc.compare(firstString, secondString));
    }

    private static Stream<Arguments> tests() {
        return Stream.of(
                Arguments.of(-1, "AAA", "AA"),
                Arguments.of(0, "AA", "AA"),
                Arguments.of(1, "AA", "AAA"),

                // empty strings
                Arguments.of(-1, "A", ""),
                Arguments.of(0, "", ""),
                Arguments.of(1, "", "A"),

                // backslash
                Arguments.of(-1, "\\\\", "A"),
                Arguments.of(0, "\\", "A"),
                Arguments.of(0, "\\", "\\"),
                Arguments.of(0, "A", "\\"),
                Arguments.of(1, "A", "\\\\"),

                // empty string + backslash
                Arguments.of(-1, "\\", ""),
                Arguments.of(1, "", "\\"));
    }
}
