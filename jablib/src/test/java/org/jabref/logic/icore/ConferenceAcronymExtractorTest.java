package org.jabref.logic.icore;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.jabref.logic.icore.ConferenceAcronymExtractor.extract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConferenceAcronymExtractorTest {
    @Test
    void extractReturnsInnerContentForValidAcronym() {
        String input = "(SERA)";
        String expectedResult = "SERA";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void extractReturnsAcronymEmbeddedInConferenceTitle() {
        String input = "ACIS Conference on Software Engineering Research, Management and Applications (SERA)";
        String expectedResult = "SERA";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void extractReturnsFirstMatchForInputWithMultipleCandidates() {
        String input = "This (SERA) has multiple (CONF) acronyms";
        String expectedResult = "SERA";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void extractReturnsAcronymWithSpecialCharacters() {
        String input = "This (C++SER@-20_26) has special characters";
        String expectedResult = "C++SER@-20_26";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void extractStripsWhitespaceAroundAcronym() {
        String input = "Input with whitespace (     ACR     )";
        String expectedResult = "ACR";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void extractReturnsEmptyForEmptyParentheses() {
        String input = "Input with empty () parentheses";
        Optional<String> result = extract(input);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractReturnsEmptyForWhitespaceInParentheses() {
        String input = "Input with empty (        ) whitespace in parens";
        Optional<String> result = extract(input);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractReturnsEmptyForEmptyString() {
        String input = "";
        Optional<String> result = extract(input);

        assertTrue(result.isEmpty());
    }

    @Test
    void extractReturnsDeepestAcronymWithinNestedParentheses() {
        String input = "(Nested (parentheses (SERA)))";
        String expectedResult = "SERA";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void extractReturnsAcronymForMissingClosingParentheses() {
        String input = "This open paren ((SERA) is never closed";
        String expectedResult = "SERA";
        Optional<String> result = extract(input);

        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }
}
