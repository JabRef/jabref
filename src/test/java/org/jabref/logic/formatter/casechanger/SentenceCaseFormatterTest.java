package org.jabref.logic.formatter.casechanger;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class SentenceCaseFormatterTest {

    private SentenceCaseFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new SentenceCaseFormatter();
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of("Upper first", "upper First"),
                Arguments.of("Upper first", "uPPER FIRST"),
                Arguments.of("Upper {NOT} first", "upper {NOT} FIRST"),
                Arguments.of("Upper {N}ot first", "upper {N}OT FIRST"),
                Arguments.of("Whose music? A sociology of musical language",
                        "Whose music? a sociology of musical language"),
                Arguments.of("Bibliographic software. A comparison.",
                        "bibliographic software. a comparison."),
                Arguments.of("England’s monitor; The history of the separation",
                        "England’s Monitor; the History of the Separation"),
                Arguments.of("Dr. schultz: a dentist turned bounty hunter.",
                        "Dr. schultz: a dentist turned bounty hunter."),
                Arguments.of("Example case. {EXCLUDED SENTENCE.}",
                        "Example case. {EXCLUDED SENTENCE.}"),
                Arguments.of("I have {Aa} dream", new SentenceCaseFormatter().getExampleInput()));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void test(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
