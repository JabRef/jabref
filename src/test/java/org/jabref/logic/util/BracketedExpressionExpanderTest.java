package org.jabref.logic.util;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BracketedExpressionExpanderTest {

    private BibEntry bibentry;

    @Before
    public void setUp() throws Exception {
        bibentry = new BibEntry();
        bibentry.setField("author", "O. Kitsune");
        bibentry.setField("year", "2017");
        bibentry.setField("pages", "213--216");
    }

    @Test
    public void bibentryExpansionTest() {
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals(bex.expandBrackets("[year]_[auth]_[firstpage]"), "2017_Kitsune_213");
    }

    @Test
    public void nullDatabaseExpansionTest() {
        BibDatabase database = null;
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals(bex.expandBrackets("[year]_[auth]_[firstpage]", database), "2017_Kitsune_213");
    }

    @Test
    public void emptyDatabaseExpansionTest() {
        BibDatabase database = new BibDatabase();
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals(bex.expandBrackets("[year]_[auth]_[firstpage]", database), "2017_Kitsune_213");
    }

    @Test
    public void databaseWithStringsExpansionTest() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("#sgr", "Saulius Gražulis");
        database.addString(string);
        bibentry = new BibEntry();
        bibentry.setField("author", "#sgr");
        bibentry.setField("year", "2017");
        bibentry.setField("pages", "213--216");
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals(bex.expandBrackets("[year]_[auth]_[firstpage]", database), "2017_Gražulis_213");
    }
}
