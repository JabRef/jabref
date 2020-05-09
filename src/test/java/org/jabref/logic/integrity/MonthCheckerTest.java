package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MonthCheckerTest {

    private MonthChecker checker;
    private MonthChecker checkerBiblatex;

    @BeforeEach
    public void setUp() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibDatabaseContext databaseBiblatex = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new MonthChecker(databaseContext);
        databaseBiblatex.setMode(BibDatabaseMode.BIBLATEX);
        checkerBiblatex = new MonthChecker(databaseBiblatex);
    }

    @Test
    void bibTexAcceptsThreeLetterAbbreviationsWithHashMarks() {
        assertEquals(Optional.empty(), checker.checkValue("#mar#"));
    }

    @Test
    void bibTexDoesNotAcceptWhateverThreeLetterAbbreviations() {
        assertNotEquals(Optional.empty(), checker.checkValue("#bla#"));
    }

    @Test
    void bibTexDoesNotAcceptThreeLetterAbbreviationsWithNoHashMarks() {
        assertNotEquals(Optional.empty(), checker.checkValue("Dec"));
    }

    @Test
    void bibTexDoesNotAcceptFullInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("December"));
    }

    @Test
    void bibTexDoesNotAcceptRandomString() {
        assertNotEquals(Optional.empty(), checker.checkValue("Lorem"));
    }

    @Test
    void bibTexDoesNotAcceptInteger() {
        assertNotEquals(Optional.empty(), checker.checkValue("10"));
    }

    @Test
    void bibLaTexAcceptsThreeLetterAbbreviationsWithHashMarks() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("#jan#"));
    }

    @Test
    void bibLaTexDoesNotAcceptThreeLetterAbbreviationsWithNoHashMarks() {
        assertNotEquals(Optional.empty(), checkerBiblatex.checkValue("jan"));
    }

    @Test
    void bibLaTexDoesNotAcceptFullInput() {
        assertNotEquals(Optional.empty(), checkerBiblatex.checkValue("January"));
    }

    @Test
    void bibLaTexDoesNotAcceptRandomString() {
        assertNotEquals(Optional.empty(), checkerBiblatex.checkValue("Lorem"));
    }

    @Test
    void bibLaTexAcceptsInteger() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("10"));
    }
}
