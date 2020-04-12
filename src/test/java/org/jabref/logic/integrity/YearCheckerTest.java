package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class YearCheckerTest {

    @Test
    void yearFieldAccepts21stCenturyDate() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "2014"));
    }

    @Test
    void yearFieldAccepts20thCenturyDate() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "1986"));
    }

    @Test
    void yearFieldAcceptsApproximateDate() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "around 1986"));
    }

    @Test
    void yearFieldAcceptsApproximateDateWithParenthesis() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "(around 1986)"));
    }

    @Test
    void yearFieldRemovesCommaFromYear() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "1986,"));
    }

    @Test
    void yearFieldRemovesBraceAndPercentageFromYear() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "1986}%"));
    }

    @Test
    void yearFieldRemovesSpecialCharactersFromYear() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.YEAR, "1986(){},.;!?<>%&$"));
    }

    @Test
    void yearFieldDoesNotAcceptStringAsInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "abc"));
    }

    @Test
    void yearFieldDoesNotAcceptDoubleDigitNumber() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "86"));
    }

    @Test
    void yearFieldDoesNotAcceptTripleDigitNumber() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "204"));
    }

    @Test
    void yearFieldDoesNotRemoveStringInYear() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "1986a"));
    }

    @Test
    void yearFieldDoesNotRemoveStringInParenthesis() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "(1986a)"));
    }

    @Test
    void yearFieldDoesNotRemoveStringBeforeComma() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "1986a,"));
    }

    @Test
    void yearFieldDoesNotRemoveStringInsideBraceAndPercentage() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "1986}a%"));
    }

    @Test
    void yearFieldDoesNotRemoveStringBeforeSpecialCharacters() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.YEAR, "1986a(){},.;!?<>%&$"));
    }
}
