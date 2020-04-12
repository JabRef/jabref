package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditionCheckerTest {

    public BibDatabaseContext bibDatabaseContextEdition = new BibDatabaseContext();

    @Test
    void isFirstCharacterANumber() {
        boolean allowIntegerEdition = false;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertTrue(editionChecker.isFirstCharDigit("0HelloWorld"));
    }

    @Test
    void isFirstCharacterANumberFalseForEmptyString() {
        boolean allowIntegerEdition = false;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertFalse(editionChecker.isFirstCharDigit(""));
    }

    @Test
    void isFirstCharacterNotANumber() {
        boolean allowIntegerEdition = false;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertFalse(editionChecker.isFirstCharDigit("HelloWorld"));
    }

    @Test
    void editionCheckerDoesNotComplainIfAllowIntegerEditionIsEnabled() {
        boolean allowIntegerEdition = true;
        var editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
        assertEquals(Optional.empty(), editionChecker.checkValue("2"));
    }

    @Test
    void bibTexAcceptsOrdinalNumberInWordsWithCapitalFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "Second"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptOrdinalNumberInWordsWithNonCapitalFirstLetter() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "second"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexDoesNotAcceptIntegerInputInEdition() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "2"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibTexNowAcceptsIntegerInputInEdition() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "2"), BibDatabaseMode.BIBTEX), true);
    }

    @Test
    void bibTexDoesNotAcceptOrdinalNumberInNumbers() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "2nd"), BibDatabaseMode.BIBTEX));
    }

    @Test
    void bibLaTexAcceptsEditionWithCapitalFirstLetter() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "Edition 2000"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsIntegerInputInEdition() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "2"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexAcceptsEditionAsLiteralString() {
        IntegrityCheckTest.assertCorrect(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "Third, revised and expanded edition"), BibDatabaseMode.BIBLATEX));
    }

    @Test
    void bibLaTexDoesNotAcceptOrdinalNumberInNumbers() {
        IntegrityCheckTest.assertWrong(IntegrityCheckTest.withMode(IntegrityCheckTest.createContext(StandardField.EDITION, "2nd"), BibDatabaseMode.BIBLATEX));
    }

}
