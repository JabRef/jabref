package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class ISBNCheckerTest {

    @Test
    void isbnAcceptsValidInput() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.ISBN, "0-201-53082-1"));
    }

    @Test
    void isbnAcceptsNumbersAndCharacters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.ISBN, "0-9752298-0-X"));
    }

    @Test
    void isbnDoesNotAcceptRandomInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.ISBN, "Some other stuff"));
    }

    @Test
    void isbnDoesNotAcceptInvalidInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.ISBN, "0-201-53082-2"));
    }

}
