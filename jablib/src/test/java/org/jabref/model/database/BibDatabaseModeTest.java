package org.jabref.model.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BibDatabaseModeTest {

    @Test
    void getFormattedNameReturnsExpectedValues() {
        assertEquals("BibTeX", BibDatabaseMode.BIBTEX.getFormattedName());
        assertEquals("biblatex", BibDatabaseMode.BIBLATEX.getFormattedName());
    }

    @Test
    void getOppositeModeReturnsExpectedValues() {
        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseMode.BIBTEX.getOppositeMode());
        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX.getOppositeMode());
    }

    @Test
    void parseAcceptsDifferentCasing() {
        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseMode.parse("bibtex"));
        assertEquals(BibDatabaseMode.BIBTEX, BibDatabaseMode.parse("BIBTEX"));
        assertEquals(BibDatabaseMode.BIBLATEX, BibDatabaseMode.parse("BibLaTex"));
    }

    @Test
    void parseThrowsExceptionOnInvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> BibDatabaseMode.parse("unknown"));
        assertThrows(IllegalArgumentException.class, () -> BibDatabaseMode.parse(""));
        assertThrows(IllegalArgumentException.class, () -> BibDatabaseMode.parse("bibtexlatex"));
    }

    @Test
    void getAsStringReturnsLowercaseNames() {
        assertEquals("bibtex", BibDatabaseMode.BIBTEX.getAsString());
        assertEquals("biblatex", BibDatabaseMode.BIBLATEX.getAsString());
    }
}
