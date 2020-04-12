package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class NoteCheckerTest {

    @Test
    void bibTexAcceptsNoteWithFirstCapitalLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.NOTE, "Lorem ipsum"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsNoteWithFirstCapitalLetterAndDoesNotCareAboutTheRest() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.NOTE, "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptFirstLowercaseLetter() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.NOTE, "lorem ipsum"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsNoteWithFirstCapitalLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.NOTE, "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsUrl() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.NOTE, "\\url{someurl}"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsFirstLowercaseLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.NOTE, "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

}
