package org.jabref.logic.util.strings;

import org.junit.Before;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringSimilarityTest {

    private StringSimilarity sut;

    @Before
    public void setup() {
        sut = new StringSimilarity();
    }

    @Test
    public void givenIdenticalStrings_whenTestingForSimilarity_thenJudgeThatAreSimilar() {
        String a = "Hello";
        String b = "Hello";
        boolean similarity = sut.isSimilar(a, b);
        assertTrue(similarity);
    }

    @Test
    public void givenIdenticalStrings_whenFindingAmountOfCharChangesNeededToMakeStringsIdentical_thenLevenshteinDistance() {
        String a = "A";
        String b = "A";
        double testVal = 0.0;
        double distance = sut.editDistanceIgnoreCase(a, b);
        assertEquals(testVal, distance);
    }

    @Test
    public void givenSlightlyDifferentStrings_whenFindingAmountOfCharChangesNeededToMakeStringsIdentical_thenReturnCorrectLevenshteinDistance() {
        String a = "Abc";
        String b = "Bbc";
        double testVal = 1.0;
        double distance = sut.editDistanceIgnoreCase(a, b);
        assertEquals(testVal, distance);
    }

    @Test
    public void givenVeryDifferentStrings_whenFindingAmountOfCharChangesNeededToMakeStringsIdentical_thenReturnCorrectLevenshteinDistance() {
        String a = "Fish";
        String b = "Bears";
        double testVal = 5.0;
        double distance = sut.editDistanceIgnoreCase(a, b);
        assertEquals(testVal, distance);
    }

    @Test
    public void givenTwoStringsWithLevenshteinGreaterThanFour_whenTestingForSimilarity_thenJudgeThemToBeDissimilar() {
        String a = "Johnson";
        String b = "Daghtsen";
        double testDistance = 5.0;
        double distance = sut.editDistanceIgnoreCase(a,b);
        boolean similarity = sut.isSimilar(a,b);
        assertEquals(testDistance, distance);
        assertFalse(similarity);
    }

}
