package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class BracketCheckerTest {

    @Test
    void fieldAcceptsNoBrackets() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.TITLE, "x"));
    }

    @Test
    void fieldAcceptsEvenNumberOfBrackets() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.TITLE, "{x}"));
    }

    @Test
    void fieldAcceptsExpectedBracket() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.TITLE, "{x}x{}x{{}}"));
    }

    @Test
    void fieldDoesNotAcceptOddNumberOfBrackets() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.TITLE, "{x}x{}}x{{}}"));
    }

    @Test
    void fieldDoesNotAcceptUnexpectedClosingBracket() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.TITLE, "}"));
    }

    @Test
    void fieldDoesNotAcceptUnexpectedOpeningBracket() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.TITLE, "{"));
    }

}
