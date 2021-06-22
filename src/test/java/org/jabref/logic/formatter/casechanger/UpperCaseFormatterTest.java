package org.jabref.logic.formatter.casechanger;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UpperCaseFormatterTest {

    private UpperCaseFormatter formatter = new UpperCaseFormatter();

    @ParameterizedTest
    @MethodSource("upperCaseTests")
    public void upperCaseTest(String expectedFormat, String inputFormat) {
        assertEquals(expectedFormat, formatter.format(inputFormat));
    }

    private static Stream<Arguments> upperCaseTests() {
        return Stream.of(
                Arguments.of("LOWER", "LOWER"),
                Arguments.of("UPPER", "upper"),
                Arguments.of("UPPER", "UPPER"),
                Arguments.of("UPPER {lower}", "upper {lower}"),
                Arguments.of("UPPER {l}OWER", "upper {l}ower"),
                Arguments.of("1", "1"),
                Arguments.of("!", "!")
        );
    }

    @Test
    public void formatExample() {
        assertEquals("KDE {Amarok}", formatter.format(formatter.getExampleInput()));
    }
}
