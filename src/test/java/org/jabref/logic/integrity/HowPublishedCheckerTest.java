package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class HowPublishedCheckerTest {

    private HowPublishedChecker checker;
    private HowPublishedChecker checkerBiblatex;

    @BeforeEach
    public void setUp() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibDatabaseContext databaseBiblatex = new BibDatabaseContext();
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        checker = new HowPublishedChecker(databaseContext);
        databaseBiblatex.setMode(BibDatabaseMode.BIBLATEX);
        checkerBiblatex = new HowPublishedChecker(databaseBiblatex);
    }

    @Test
    void bibTexAcceptsStringWithCapitalFirstLetter() {
        assertEquals(Optional.empty(), checker.checkValue("Lorem ipsum"));
    }

    @Test
    void bibTexDoesNotCareAboutSpecialCharacters() {
        assertEquals(Optional.empty(), checker.checkValue("Lorem ipsum? 10"));
    }

    @Test
    void bibTexDoesNotAcceptStringWithLowercaseFirstLetter() {
        assertNotEquals(Optional.empty(), checker.checkValue("lorem ipsum"));
    }

    @Test
    void bibTexAcceptsUrl() {
        assertEquals(Optional.empty(), checker.checkValue("\\url{someurl}"));
    }

    @Test
    void bibLaTexAcceptsStringWithCapitalFirstLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("Lorem ipsum"));
    }

    @Test
    void bibLaTexAcceptsStringWithLowercaseFirstLetter() {
        assertEquals(Optional.empty(), checkerBiblatex.checkValue("lorem ipsum"));
    }

}
