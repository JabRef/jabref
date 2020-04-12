package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class ISSNCheckerTest {

    @Test
    void issnAcceptsValidInput() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.ISSN, "0020-7217"));
    }

    @Test
    void issnAcceptsNumbersAndCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.ISSN, "2434-561x"));
    }

    @Test
    void issnDoesNotAcceptRandomInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.ISSN, "Some other stuff"));
    }

    @Test
    void issnDoesNotAcceptInvalidInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.ISSN, "0020-7218"));
    }

}
