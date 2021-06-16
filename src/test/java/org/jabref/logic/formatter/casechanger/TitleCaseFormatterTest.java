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
public class TitleCaseFormatterTest {

    private TitleCaseFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new TitleCaseFormatter();
    }

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of("Upper Each First", "upper each first"),
                Arguments.of("Upper Each First", "upper eACH first"),
                Arguments.of("An Upper Each First And", "an upper each first and"),
                Arguments.of("An Upper Each First And", "an upper each first AND"),
                Arguments.of("An Upper Each of the and First And",
                        "an upper each of the and first and"),
                Arguments.of("An Upper Each of the and First And",
                        "an upper each of the AND first and"),
                Arguments.of("An Upper Each of: The and First And",
                        "an upper each of: the and first and"),
                Arguments.of("An Upper First with and without {CURLY} {brackets}",
                        "AN UPPER FIRST WITH AND WITHOUT {CURLY} {brackets}"),
                Arguments.of("An Upper First with {A}nd without {C}urly {b}rackets",
                        "AN UPPER FIRST WITH {A}ND WITHOUT {C}URLY {b}rackets"),
                Arguments.of("{b}rackets {b}rac{K}ets Brack{E}ts",
                        "{b}RaCKeTS {b}RaC{K}eTS bRaCK{E}ts"),
                Arguments.of("Two Experiences Designing for Effective Security",
                        "Two experiences designing for effective security"),
                Arguments.of("Bibliographic Software. A Comparison.",
                        "bibliographic software. a comparison."),
                Arguments.of("Bibliographic Software. {A COMPARISON.}",
                        "bibliographic software. {A COMPARISON.}"),
                Arguments.of("{BPMN} Conformance in Open Source Engines",
                        new TitleCaseFormatter().getExampleInput()));
    }

    @ParameterizedTest
    @MethodSource("testData")
    public void test(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
