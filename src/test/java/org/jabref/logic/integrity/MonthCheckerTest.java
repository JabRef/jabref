package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class MonthCheckerTest {

    @Test
    void bibTexAcceptsThreeLetterAbbreviationsWithHashMarks() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "#mar#"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptWhateverThreeLetterAbbreviations() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "#bla#"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptThreeLetterAbbreviationsWithNoHashMarks() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "Dec"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptFullInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "December"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptRandomString() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "Lorem"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptInteger() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "10"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsThreeLetterAbbreviationsWithHashMarks() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "#jan#"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexDoesNotAcceptThreeLetterAbbreviationsWithNoHashMarks() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "jan"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexDoesNotAcceptFullInput() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "January"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexDoesNotAcceptRandomString() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "Lorem"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsInteger() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.MONTH, "10"), BibDatabaseMode.BIBLATEX));

    }
}
