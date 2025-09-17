package org.jabref.logic.util.strings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringSimilarityTest {
    private final double EPSILON_SIMILARITY = 1e-6;

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

    @ParameterizedTest(name = "\"{0}\" should match \"{1}\" with LCS similarity rating of \"{2}\".")
    @CsvSource({
            "'', '', 1.0",                      // Both empty strings
            "'', test, 0.0",                    // First empty
            "test, '', 0.0",                    // Second empty
            "a, a, 1.0",                        // Single character identical strings
            "test, test, 1.0",                  // Identical strings
            "hellotheregeneralkenobi, hellotheregeneralkenobi, 1.0",    // Longer identical strings
            "abc, xyz, 0.0",                    // No common characters
            "a, ab, 1.0",                       // Single char match, shorter string length used
            "ab, a, 1.0",                       // Single char match, reverse argument order
            "axc, ayc, 0.3333333",              // Single char difference
            "hello, help, 0.75",                // Common 'hel' (3 chars) / min(5,4) = 0.75
            "abcd, dcba, 0.25",                 // Only single chars match at a time, longest common substring is of size 1
            "prefix123, prefixabc, 0.6666666",  // substring is 'prefix' (6 chars) / min(9,9) = 0.666...
            "123suffix, abcsuffix, 0.6666666",  // substring is 'suffix' (6 chars) / min(9,9) = 0.666...
            "efgh, abcdefgh, 1.0",              // Exact match at end of string
            "cde, abcdefgh, 1.0",               // Exact match in the middle of string
            "123abc456, xyzabcpqr, 0.3333333",  // common substring in the middle
            "ABC, abc, 1.0",                    // Matching ignores case
    })
    void LCSSimilarity(String a, String b, String expectedResult) {
        assertEquals(Double.parseDouble(expectedResult), StringSimilarity.LCSSimilarity(a, b), EPSILON_SIMILARITY);
    }
}
