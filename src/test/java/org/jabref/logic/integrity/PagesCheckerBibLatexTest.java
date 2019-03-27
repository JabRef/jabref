package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PagesCheckerBibLatexTest {

    private PagesChecker checker;

    @BeforeEach
    void setUp() {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(BibDatabaseMode.BIBLATEX);
        checker = new PagesChecker(database);
    }

    @Test
    void acceptsSinglePage() {
        assertEquals(Optional.empty(), checker.checkValue("12"));
    }

    @Test
    void acceptsSinglePageRange() {
        assertEquals(Optional.empty(), checker.checkValue("12-15"));
    }

    @Test
    void acceptsSinglePageRangeWithDoubleDashes() {
        assertEquals(Optional.empty(), checker.checkValue("12--15"));
    }

    @Test
    void acceptsSinglePageRangeWithEnDashes() {
        assertEquals(Optional.empty(), checker.checkValue("12–15"));
    }

    @Test
    void acceptsSinglePageRangeWithPagePrefix() {
        assertEquals(Optional.empty(), checker.checkValue("R795--R804"));
    }

    @Test
    void acceptsMultiplePageRange() {
        assertEquals(Optional.empty(), checker.checkValue("12-15,18-29"));
    }

    @Test
    void acceptsOpenEndPageRange() {
        assertEquals(Optional.empty(), checker.checkValue("-15"));
    }

    @Test
    void acceptsOpenStartPageRange() {
        assertEquals(Optional.empty(), checker.checkValue("12-"));
    }

    @Test
    void complainsAboutPPrefix() {
        assertEquals(Optional.of("should contain a valid page number range"), checker.checkValue("p. 12"));
    }

    @Test
    void complainsAboutPPPrefix() {
        assertEquals(Optional.of("should contain a valid page number range"), checker.checkValue("pp. 12-15"));
    }
}
