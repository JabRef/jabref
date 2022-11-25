package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class YearCheckerTest {

    private final YearChecker checker = new YearChecker();

    @Test
    void yearFieldAccepts21stCenturyDate() {
        assertEquals(Optional.empty(), checker.checkValue("2014"));
    }

    @Test
    void yearFieldAccepts20thCenturyDate() {
        assertEquals(Optional.empty(), checker.checkValue("1986"));
    }

    @Test
    void yearFieldAcceptsApproximateDate() {
        assertEquals(Optional.empty(), checker.checkValue("around 1986"));
    }

    @Test
    void yearFieldAcceptsApproximateDateWithParenthesis() {
        assertEquals(Optional.empty(), checker.checkValue("(around 1986)"));
    }

    @Test
    void yearFieldRemovesCommaFromYear() {
        assertEquals(Optional.empty(), checker.checkValue("1986,"));
    }

    @Test
    void yearFieldRemovesBraceAndPercentageFromYear() {
        assertEquals(Optional.empty(), checker.checkValue("1986}%"));
    }

    @Test
    void yearFieldRemovesSpecialCharactersFromYear() {
        assertEquals(Optional.empty(), checker.checkValue("1986(){},.;!?<>%&$"));
    }

    @Test
    void yearFieldDoesNotAcceptStringAsInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("abc"));
    }

    @Test
    void yearFieldDoesNotAcceptDoubleDigitNumber() {
        assertNotEquals(Optional.empty(), checker.checkValue("86"));
    }

    @Test
    void yearFieldDoesNotAcceptTripleDigitNumber() {
        assertNotEquals(Optional.empty(), checker.checkValue("204"));
    }

    @Test
    void yearFieldDoesNotRemoveStringInYear() {
        assertNotEquals(Optional.empty(), checker.checkValue("1986a"));
    }

    @Test
    void yearFieldDoesNotRemoveStringInParenthesis() {
        assertNotEquals(Optional.empty(), checker.checkValue("(1986a)"));
    }

    @Test
    void yearFieldDoesNotRemoveStringBeforeComma() {
        assertNotEquals(Optional.empty(), checker.checkValue("1986a,"));
    }

    @Test
    void yearFieldDoesNotRemoveStringInsideBraceAndPercentage() {
        assertNotEquals(Optional.empty(), checker.checkValue("1986}a%"));
    }

    @Test
    void yearFieldDoesNotRemoveStringBeforeSpecialCharacters() {
        assertNotEquals(Optional.empty(), checker.checkValue("1986a(){},.;!?<>%&$"));
    }

    @Test
    void testEmptyValue() {
        assertEquals(Optional.empty(), checker.checkValue(""));
    }
}
