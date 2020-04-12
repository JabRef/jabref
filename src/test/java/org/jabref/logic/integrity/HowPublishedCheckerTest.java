package org.jabref.logic.integrity;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

public class HowPublishedCheckerTest {

    @Test
    void bibTexAcceptsStringWithCapitalFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.HOWPUBLISHED, "Lorem ipsum"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotCareAboutSpecialChracters() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.HOWPUBLISHED, "Lorem ipsum? 10"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptStringWithLowercaseFirstLetter() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.HOWPUBLISHED, "lorem ipsum"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexAcceptsUrl() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.HOWPUBLISHED, "\\url{someurl}"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsStringWithCapitalFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.HOWPUBLISHED, "Lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsStringWithLowercaseFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.HOWPUBLISHED, "lorem ipsum"), BibDatabaseMode.BIBLATEX));
    }

}
