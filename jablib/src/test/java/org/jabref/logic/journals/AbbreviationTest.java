package org.jabref.logic.journals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AbbreviationTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideAbbreviationTestCases")
    void testAbbreviationProperties(String testName, Abbreviation abbreviation,
                                    String expectedName, String expectedAbbreviation,
                                    String expectedDotless, String expectedShortest) {
        assertEquals(expectedName, abbreviation.getName());
        assertEquals(expectedAbbreviation, abbreviation.getAbbreviation());
        assertEquals(expectedDotless, abbreviation.getDotlessAbbreviation());
        assertEquals(expectedShortest, abbreviation.getShortestUniqueAbbreviation());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideGetNextTestCases")
    void testGetNext(String testName, Abbreviation abbreviation, String input, String expected) {
        assertEquals(expected, abbreviation.getNext(input));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideEqualityTestCases")
    void testEquality(String testName, Object obj1, Object obj2, boolean shouldBeEqual) {
        if (shouldBeEqual) {
            assertEquals(obj1, obj2);
        } else {
            assertNotEquals(obj1, obj2);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideComparisonTestCases")
    void testComparison(String testName, Abbreviation abbrev1, Abbreviation abbrev2, int expectedComparison) {
        assertEquals(expectedComparison, abbrev1.compareTo(abbrev2));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideSameValueTestCases")
    void testSameValues(String testName, Abbreviation abbreviation, String method1Result, String method2Result) {
        assertEquals(method1Result, method2Result);
    }

    private static Stream<Arguments> provideAbbreviationTestCases() {
        return Stream.of(
                // Abbreviations with trailing spaces
                Arguments.of("abbreviationsWithTrailingSpaces",
                        new Abbreviation("Long Name", "L. N."),
                        "Long Name", "L. N.", "L N", "L. N."),

                // Abbreviations with trailing spaces with shortest unique abbreviation
                Arguments.of("abbreviationsWithTrailingSpacesWithShortestUniqueAbbreviation",
                        new Abbreviation("Long Name", "L. N.", "LN"),
                        "Long Name", "L. N.", "L N", "LN"),

                // Abbreviations with semicolons
                Arguments.of("abbreviationsWithSemicolons",
                        new Abbreviation("Long Name", "L. N.;LN;M"),
                        "Long Name", "L. N.;LN;M", "L N ;LN;M", "L. N.;LN;M"),

                // Abbreviations with semicolons with shortest unique abbreviation
                Arguments.of("abbreviationsWithSemicolonsWithShortestUniqueAbbreviation",
                        new Abbreviation("Long Name", "L. N.;LN;M", "LNLNM"),
                        "Long Name", "L. N.;LN;M", "L N ;LN;M", "LNLNM")
        );
    }

    private static Stream<Arguments> provideGetNextTestCases() {
        Abbreviation abbrev1 = new Abbreviation("Long Name", "L. N.");
        Abbreviation abbrev2 = new Abbreviation("Long Name", "L. N.", "LN");

        return Stream.of(
                // getNextElement tests
                Arguments.of("getNextElement - Long Name to L. N.", abbrev1, "Long Name", "L. N."),
                Arguments.of("getNextElement - L. N. to L N", abbrev1, "L. N.", "L N"),
                Arguments.of("getNextElement - L N to Long Name", abbrev1, "L N", "Long Name"),

                // getNextElementWithShortestUniqueAbbreviation tests
                Arguments.of("getNextElementWithShortestUniqueAbbreviation - Long Name to L. N.", abbrev2, "Long Name", "L. N."),
                Arguments.of("getNextElementWithShortestUniqueAbbreviation - L. N. to L N", abbrev2, "L. N.", "L N"),
                Arguments.of("getNextElementWithShortestUniqueAbbreviation - L N to LN", abbrev2, "L N", "LN"),
                Arguments.of("getNextElementWithShortestUniqueAbbreviation - LN to Long Name", abbrev2, "LN", "Long Name"),

                // getNextElementWithTrailingSpaces tests
                Arguments.of("getNextElementWithTrailingSpaces - Long Name to L. N.", abbrev1, "Long Name", "L. N."),
                Arguments.of("getNextElementWithTrailingSpaces - L. N. to L N", abbrev1, "L. N.", "L N"),
                Arguments.of("getNextElementWithTrailingSpaces - L N to Long Name", abbrev1, "L N", "Long Name"),

                // getNextElementWithTrailingSpacesWithShortestUniqueAbbreviation tests
                Arguments.of("getNextElementWithTrailingSpacesWithShortestUniqueAbbreviation - Long Name to L. N.", abbrev2, "Long Name", "L. N."),
                Arguments.of("getNextElementWithTrailingSpacesWithShortestUniqueAbbreviation - L. N. to L N", abbrev2, "L. N.", "L N"),
                Arguments.of("getNextElementWithTrailingSpacesWithShortestUniqueAbbreviation - L N to LN", abbrev2, "L N", "LN"),
                Arguments.of("getNextElementWithTrailingSpacesWithShortestUniqueAbbreviation - LN to Long Name", abbrev2, "LN", "Long Name")
        );
    }

    private static Stream<Arguments> provideEqualityTestCases() {
        Abbreviation abbreviation = new Abbreviation("Long Name", "L N", "LN");
        Abbreviation otherAbbreviation = new Abbreviation("Long Name", "L N", "LN");

        return Stream.of(
                Arguments.of("equals - same abbreviations", abbreviation, otherAbbreviation, true),
                Arguments.of("equals - abbreviation vs string", abbreviation, "String", false)
        );
    }

    private static Stream<Arguments> provideComparisonTestCases() {
        return Stream.of(
                Arguments.of("equalAbbrevationsWithFourComponentsAreAlsoCompareZero",
                        new Abbreviation("Long Name", "L. N.", "LN"),
                        new Abbreviation("Long Name", "L. N.", "LN"),
                        0)
        );
    }

    private static Stream<Arguments> provideSameValueTestCases() {
        Abbreviation abbrev1 = new Abbreviation("Long Name", "L N");
        Abbreviation abbrev2 = new Abbreviation("Long Name", "L N", "LN");
        Abbreviation abbrev3 = new Abbreviation("Long Name", "L N");

        return Stream.of(
                Arguments.of("defaultAndMedlineAbbreviationsAreSame",
                        abbrev1, abbrev1.getAbbreviation(), abbrev1.getDotlessAbbreviation()),
                Arguments.of("defaultAndMedlineAbbreviationsAreSameWithShortestUniqueAbbreviation",
                        abbrev2, abbrev2.getAbbreviation(), abbrev2.getDotlessAbbreviation()),
                Arguments.of("defaultAndShortestUniqueAbbreviationsAreSame",
                        abbrev3, abbrev3.getAbbreviation(), abbrev3.getShortestUniqueAbbreviation())
        );
    }
}
