package org.jabref.logic.util.strings;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringSimilarityTest {

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
    public void testStringSimilarity(String a, String b, String expectedResult) {
        assertEquals(Boolean.valueOf(expectedResult), similarityChecker.isSimilar(a, b));
    }
}
