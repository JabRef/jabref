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
    public void givenIdenticalStrings_whenIsSimilarMethod_thenReturnTrue() {
        String a = "Hello";
        String b = "Hello";
        assertTrue(sut.isSimilar(a, b));
    }

    @Test
    public void givenIdenticalStrings_whenEditDistanceIgnoreCaseMethod_thenReturnZero() {
        String a = "A";
        String b = "A";
        assertEquals(0.0, sut.editDistanceIgnoreCase(a, b));
    }

    @Test
    public void givenSlightlyDifferentStrings_whenEditDistanceIgnoreCaseMethod_thenReturnLevenshteinDistance() {
        String a = "Abc";
        String b = "Bbc";
        assertEquals(1.0, sut.editDistanceIgnoreCase(a,b));
    }

    @Test
    public void givenVeryDifferentStrings_whenEditDistanceIgnoreCaseMethod_thenReturnLevenshteinDistance() {
        String a = "Fish";
        String b = "Bears";
        assertEquals(5.0, sut.editDistanceIgnoreCase(a,b));
    }

    @Test
    public void givenDissimilarStrings_whenIsSimilarMethod_thenReturnFalse() {
        String a = "Johnson";
        String b = "Daghtsen";
        System.out.println(sut.editDistanceIgnoreCase(a,b));
        assertFalse(sut.isSimilar(a,b));
    }

}
