package org.jabref.logic.util.strings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringSimilarityTest {
    private final double EPSILON_SIMILARITY = 0.00001;

    private StringSimilarity similarityChecker = new StringSimilarity();

    @ParameterizedTest(name = "a={0}, b={1}, result={2}")
    @CsvSource({
            "'', '', true", // same empty strings
            "a, a, true", // same one-letter strings
            "a, '', true", // one string is empty and similarity < threshold (4)
            "'', a, true", // one string is empty and similarity < threshold (4)
            "abcd, '', true", // one string is empty and similarity == threshold (4)
            "'', abcd, true", // one string is empty and similarity == threshold (4)
            "abcde, '', false", // one string is empty and similarity > threshold (4)
            "'', abcde, false", // one string is empty and similarity > threshold (4)
            "abcdef, abcdef, true", // same multi-letter strings
            "abcdef, abc, true", // no empty strings and similarity < threshold (4)
            "abcdef, ab, true", // no empty strings and similarity == threshold (4)
            "abcdef, a, false" // no empty string sand similarity > threshold (4)
    })
    void isSimilar(String a, String b, String expectedResult) {
        assertEquals(Boolean.valueOf(expectedResult), similarityChecker.isSimilar(a, b));
    }

    @ParameterizedTest(name = "\"{0}\" should match \"{1}\" with similarity rating of \"{2}\".")
    @CsvSource({
            "abcdef, abcdef, 1.0",  // same strings should match perfectly
            "abcdef, uvwxyz, 0.0",  // different strings should not match at all
            "'', '', 1.0",          // empty string should match perfectly
            "abcdef, '', 0.0",      // should not match at all with one empty string
            "abcdef, ABCDEF, 1.0"   // same string should match perfectly regardless of case
    })
    void stringSimilarity(String a, String b, String expectedResult) {
        assertEquals(Double.parseDouble(expectedResult), similarityChecker.similarity(a, b), EPSILON_SIMILARITY);
    }
}
