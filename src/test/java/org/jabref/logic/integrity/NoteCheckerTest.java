package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class NoteCheckerTest {

    private NoteChecker checker;
    private NoteChecker checkerBiblatex;

    @BeforeEach
    void setUp() {
        BibDatabaseContext database = new BibDatabaseContext();
        BibDatabaseContext databaseBiblatex = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBTEX);
        checker = new NoteChecker(database);
        databaseBiblatex.setMode(BibDatabaseMode.BIBLATEX);
        checkerBiblatex = new NoteChecker(databaseBiblatex);

    }

    @Test
    void bibTexAcceptsNoteWithFirstCapitalLetter() {
        assertEquals(Optional.empty(), checker.checkValue("Lorem ipsum"));
    }

    @Test
    void bibTexAcceptsNoteWithFirstCapitalLetterAndDoesNotCareAboutTheRest() {
        assertEquals(Optional.empty(), checker.checkValue("Lorem ipsum? 10"));
    }

    @Test
    void bibTexDoesNotAcceptFirstLowercaseLetter() {
        assertNotEquals(Optional.empty(), checker.checkValue("lorem ipsum"));
    }

    @Test
    void bibLaTexAcceptsNoteWithFirstCapitalLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("Lorem ipsum"));
    }

    @Test
    void bibTexAcceptsUrl() {
        assertEquals(Optional.empty(), checker.checkValue("\\url{someurl}"));
    }

    @Test
    void bibLaTexAcceptsFirstLowercaseLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("lorem ipsum"));
    }

}
