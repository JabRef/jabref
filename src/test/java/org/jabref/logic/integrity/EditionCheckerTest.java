package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EditionCheckerTest {

    public BibDatabaseContext bibDatabaseContextEdition = new BibDatabaseContext();

    private EditionChecker checker;
    private EditionChecker checkerb;
    private BibDatabaseContext bibtex;
    private BibDatabaseContext biblatex;

    @BeforeEach
    void setUp() {
        bibtex = new BibDatabaseContext();
        bibtex.setMode(BibDatabaseMode.BIBTEX);
        biblatex = new BibDatabaseContext();
        biblatex.setMode(BibDatabaseMode.BIBLATEX);
        checker = new EditionChecker(bibtex, true);
        checkerb = new EditionChecker(biblatex, true);
    }

    @Test
    void isFirstCharacterANumber() {
        boolean allowIntegerEdition = false;
        EditionChecker editionChecker = new EditionChecker(bibDatabaseContextEdition, allowIntegerEdition);
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
        assertEquals(Optional.empty(), checker.checkValue("Second"));
    }

    @Test
    void bibTexDoesNotAcceptOrdinalNumberInWordsWithNonCapitalFirstLetter() {
        assertNotEquals(Optional.empty(), checker.checkValue("second"));
    }

    @Test
    void bibTexAcceptsIntegerInputInEdition() {
        assertEquals(Optional.empty(), checker.checkValue("2"));
    }

    @Test
    void bibTexAcceptsOrdinalNumberInNumbers() {
        assertEquals(Optional.empty(), checker.checkValue("2nd"));
    }

    @Test
    void bibLaTexAcceptsEditionWithCapitalFirstLetter() {
        assertEquals(Optional.empty(), checkerb.checkValue("Edition 2000"));
    }

    @Test
    void bibLaTexAcceptsIntegerInputInEdition() {
        assertEquals(Optional.empty(), checkerb.checkValue("2"));
    }

    @Test
    void bibLaTexAcceptsEditionAsLiteralString() {
        assertEquals(Optional.empty(), checkerb.checkValue("Third, revised and expanded edition"));
    }

    @Test
    void bibLaTexDoesNotAcceptOrdinalNumberInNumbers() {
        assertNotEquals(Optional.empty(), checkerb.checkValue("2nd"));
    }

}
