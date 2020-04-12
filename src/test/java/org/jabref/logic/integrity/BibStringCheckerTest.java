package org.jabref.logic.integrity;

import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class BibStringCheckerTest {

    @Test
    void fieldAcceptsNoHashMarks() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.TITLE, "Not a single hash mark"));
    }

    @Test
    void monthAcceptsEvenNumberOfHashMarks() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.MONTH, "#jan#"));
    }

    @Test
    void authorAcceptsEvenNumberOfHashMarks() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.AUTHOR, "#einstein# and #newton#"));
    }

    @Test
    void monthDoesNotAcceptOddNumberOfHashMarks() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.MONTH, "#jan"));
    }

    @Test
    void authorDoesNotAcceptOddNumberOfHashMarks() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.AUTHOR, "#einstein# #amp; #newton#"));
    }
}
