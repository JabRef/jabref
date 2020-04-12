package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class PagesCheckerTest {

    @Test
    void bibTexAcceptsRangeOfNumbersWithDoubleDash() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.PAGES, "1--2"));
    }

    @Test
    void bibTexAcceptsOnePageNumber() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.PAGES, "12"));
    }

    @Test
    void bibTexDoesNotAcceptRangeOfNumbersWithSingleDash() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.PAGES, "1-2"));
    }

    @Test
    void bibTexAcceptsMorePageNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.PAGES, "1,2,3"));
    }

    @Test
    void bibTexAcceptsNoSimpleRangeOfNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.PAGES, "43+"));
    }

    @Test
    void bibTexDoesNotAcceptMorePageNumbersWithoutComma() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.PAGES, "1 2"));
    }

    @Test
    void bibTexDoesNotAcceptBrackets() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.createContext(StandardField.PAGES, "{1}-{2}"));
    }

    @Test
    void bibTexAcceptsMorePageNumbersWithRangeOfNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.createContext(StandardField.PAGES, "7+,41--43,73"));
    }

    @Test
    void bibLaTexAcceptsRangeOfNumbersWithDoubleDash() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "1--2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsOnePageNumber() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "12"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsRangeOfNumbersWithSingleDash() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "1-2"), BibDatabaseMode.BIBLATEX)); // only diff to bibtex
    }

    @Test
    void bibLaTexAcceptsMorePageNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "1,2,3"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsNoSimpleRangeOfNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "43+"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexDoesNotAcceptMorePageNumbersWithoutComma() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "1 2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexDoesNotAcceptBrackets() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "{1}-{2}"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsMorePageNumbersWithRangeOfNumbers() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.PAGES, "7+,41--43,73"), BibDatabaseMode.BIBLATEX));
    }

}
