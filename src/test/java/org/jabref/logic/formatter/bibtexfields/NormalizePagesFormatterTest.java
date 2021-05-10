package org.jabref.logic.formatter.bibtexfields;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizePagesFormatterTest {

    private NormalizePagesFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizePagesFormatter();
    }

    private static Stream<Arguments> tests() {
        return Stream.of(
                // formatSinglePageResultsInNoChange
                Arguments.of("1", "1"),

                // formatPageNumbers
                Arguments.of("1--2", "1-2"),

                // endash
                Arguments.of("1--2", "1\u20132"),

                // emdash
                Arguments.of("1--2", "1\u20142"),

                // formatPageNumbersCommaSeparated
                Arguments.of("1,2,3", "1,2,3"),

                // formatPageNumbersPlusRange
                Arguments.of("43+", "43+"),

                // ignoreWhitespaceInPageNumbers
                Arguments.of("1--2", "   1  - 2 "),

                // removeWhitespaceSinglePage
                Arguments.of("1", "   1  "),

                // removeWhitespacePageRange
                Arguments.of("1--2", "   1 -- 2  "),

                // ignoreWhitespaceInPageNumbersWithDoubleDash
                Arguments.of("43--103", "43 -- 103"),

                // keepCorrectlyFormattedPageNumbers
                Arguments.of("1--2", "1--2"),

                // three dashes get two dashes
                Arguments.of("1--2", "1---2"),

                // formatPageNumbersRemoveUnexpectedLiterals
                Arguments.of("1--2", "{1}-{2}"),

                // formatPageNumbersRegexNotMatching
                Arguments.of("12", "12"),

                // special case, where -- is also put into
                Arguments.of("some--text", "some-text"),
                Arguments.of("pages 1--50", "pages 1-50"),
                Arguments.of("--43", "-43"),

                // keep arbitrary text
                Arguments.of("some-text-with-dashes", "some-text-with-dashes"),
                Arguments.of("{A}", "{A}"),
                Arguments.of("43+", "43+"),
                Arguments.of("Invalid", "Invalid"),

                // doNotRemoveLetters
                Arguments.of("R1--R50", "R1-R50"),

                // replaceLongDashWithDoubleDash
                Arguments.of("1--50", "1 \u2014 50"),

                // removePagePrefix
                Arguments.of("50", "p.50"),

                // removePagesPrefix
                Arguments.of("50", "pp.50"),

                // keep &
                Arguments.of("40&50", "40&50"),

                // formatACMPages
                // This appears in https://doi.org/10.1145/1658373.1658375
                Arguments.of("2:1--2:33", "2:1-2:33"),

                // keepFormattedACMPages
                // This appears in https://doi.org/10.1145/1658373.1658375
                Arguments.of("2:1--2:33", "2:1--2:33"),

                // formatExample
                Arguments.of("1--2", new NormalizePagesFormatter().getExampleInput()),

                // replaceDashWithMinus
                Arguments.of("R404--R405", "R404–R405"));
    }

    @ParameterizedTest
    @MethodSource("tests")
    public void test(String expected, String input) {
        assertEquals(expected, formatter.format(input));
    }
}
