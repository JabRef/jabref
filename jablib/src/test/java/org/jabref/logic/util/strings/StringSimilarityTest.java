package org.jabref.logic.util.strings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void stringSimilarity(String a, String b, String expectedResult) {
        assertEquals(Boolean.valueOf(expectedResult), similarityChecker.isSimilar(a, b));
    }

    @Test
    void similarityReturnsOneForExactStrings() {
        String a = "abcdef";
        String b = "abcdef";
        double expectedResult = 1.0;
        double similarity = similarityChecker.similarity(a, b);

        assertEquals(expectedResult, similarity, EPSILON_SIMILARITY);
    }

    @Test
    void similarityReturnsZeroForNonMatchingStrings() {
        String a = "abcdef";
        String b = "uvwxyz";
        double expectedResult = 0.0;
        double similarity = similarityChecker.similarity(a, b);

        assertEquals(expectedResult, similarity, EPSILON_SIMILARITY);
    }

    @Test
    void similarityReturnsValueBetweenZeroAndOneForSimilarStrings() {
        String a = "abc";
        String b = "abcdefgh";
        double exactMatch = 1.0;
        double similarity = similarityChecker.similarity(a, b);

        assertTrue(similarity >= EPSILON_SIMILARITY && similarity < exactMatch);
    }

    @Test
    void similarityReturnsOneForEmptyStrings() {
        String a = "";
        String b = "";
        double expectedResult = 1.0;
        double similarity = similarityChecker.similarity(a, b);

        assertEquals(expectedResult, similarity, EPSILON_SIMILARITY);
    }

    @Test
    void similarityReturnsZeroWhenOneStringIsEmpty() {
        String a = "abcdef";
        String b = "";
        double expectedResult = 0.0;
        double similarity = similarityChecker.similarity(a, b);

        assertEquals(expectedResult, similarity, EPSILON_SIMILARITY);
    }

    @Test
    void similarityIsCaseInsensitive() {
        String a = "abcdef";
        String b = "ABCDEF";
        double expectedResult = 1.0;
        double similarity = similarityChecker.similarity(a, b);

        assertEquals(expectedResult, similarity, EPSILON_SIMILARITY);
    }
}
