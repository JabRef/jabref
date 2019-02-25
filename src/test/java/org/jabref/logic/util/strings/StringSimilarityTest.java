package org.jabref.logic.util.strings;

import org.junit.Test;
import org.junit.Before;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(sut.isSimilar(a, b));
    }

    @Test
    public void givenIdenticalStrings_whenFindingAmountOfCharChangesNeededToMakeStringsIdentical_thenLevenshteinDistance() {
        String a = "A";
        String b = "A";
        assertEquals(0.0, sut.editDistanceIgnoreCase(a, b));
    }

    @Test
    public void givenSlightlyDifferentStrings_whenFindingAmountOfCharChangesNeededToMakeStringsIdentical_thenReturnCorrectLevenshteinDistance() {
        String a = "Abc";
        String b = "Bbc";
        assertEquals(1.0, sut.editDistanceIgnoreCase(a,b));
    }

    @Test
    public void givenVeryDifferentStrings_whenFindingAmountOfCharChangesNeededToMakeStringsIdentical_thenReturnCorrectLevenshteinDistance() {
        String a = "Fish";
        String b = "Bears";
        assertEquals(5.0, sut.editDistanceIgnoreCase(a,b));
    }

    @Test
    public void givenVeryDifferentStrings_whenTestingForSimilarity_thenJudgeThemToBeDissimilar() {
        String a = "Johnson";
        String b = "Daghtsen";
        System.out.println(sut.editDistanceIgnoreCase(a,b));
        assertFalse(sut.isSimilar(a,b));
    }

}
