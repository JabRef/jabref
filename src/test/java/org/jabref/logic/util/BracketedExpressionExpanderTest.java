package org.jabref.logic.util;

import javafx.embed.swing.JFXPanel;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BracketedExpressionExpanderTest {
    // The javafxPanel instace is only created to suppress the ugly
    // "... java.lang.IllegalStateException: Toolkit not initialized" exception.
    // It should be removed when a better solution is found. S.G.
    final JFXPanel javafxPanel = new JFXPanel();
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
        assertEquals("2017_Kitsune_213", bex.expandBrackets("[year]_[auth]_[firstpage]"));
    }

    @Test
    public void nullDatabaseExpansionTest() {
        BibDatabase database = null;
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals("2017_Kitsune_213", bex.expandBrackets("[year]_[auth]_[firstpage]", database));
    }

    @Test
    public void emptyDatabaseExpansionTest() {
        BibDatabase database = new BibDatabase();
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals("2017_Kitsune_213", bex.expandBrackets("[year]_[auth]_[firstpage]", database));
    }

    @Test
    public void databaseWithStringsExpansionTest() {
        BibDatabase database = new BibDatabase();
        BibtexString string = new BibtexString("sgr", "Saulius Gražulis");
        database.addString(string);
        bibentry = new BibEntry();
        bibentry.setField("author", "#sgr#");
        bibentry.setField("year", "2017");
        bibentry.setField("pages", "213--216");
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        assertEquals("2017_Gražulis_213", bex.expandBrackets("[year]_[auth]_[firstpage]", database));
    }

    @Test
    public void unbalancedBracketExpansionTest() {
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        // assertEquals("2017_Kitsune_213", bex.expandBrackets("[year]_[auth_[firstpage]"));
        bex.expandBrackets("[year]_[auth_[firstpage]");
    }

    @Test
    public void unbalancedLastBracketExpansionTest() {
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        // assertEquals("2017_Kitsune_213", bex.expandBrackets("[year]_[auth_[firstpage]"));
        bex.expandBrackets("[year]_[auth]_[firstpage");
    }
}
