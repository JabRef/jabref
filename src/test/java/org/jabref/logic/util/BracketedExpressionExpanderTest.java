package org.jabref.logic.util;

import javafx.embed.swing.JFXPanel;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
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
    private BibDatabase database;
    private BibEntry dbentry;

    @Before
    public void setUp() throws Exception {
        bibentry = new BibEntry();
        bibentry.setField("author", "O. Kitsune");
        bibentry.setField("year", "2017");
        bibentry.setField("pages", "213--216");

        dbentry = new BibEntry();
        dbentry.setType(BibtexEntryTypes.ARTICLE);
        dbentry.setCiteKey("HipKro03");
        dbentry.setField("author", "Eric von Hippel and Georg von Krogh");
        dbentry.setField("title", "Open Source Software and the \"Private-Collective\" Innovation Model: Issues for Organization Science");
        dbentry.setField("journal", "Organization Science");
        dbentry.setField("year", "2003");
        dbentry.setField("volume", "14");
        dbentry.setField("pages", "209--223");
        dbentry.setField("number", "2");
        dbentry.setField("address", "Institute for Operations Research and the Management Sciences (INFORMS), Linthicum, Maryland, USA");
        dbentry.setField("doi", "http://dx.doi.org/10.1287/orsc.14.2.209.14992");
        dbentry.setField("issn", "1526-5455");
        dbentry.setField("publisher", "INFORMS");

        database = new BibDatabase();
        database.insertEntry(dbentry);

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

    @Test
    public void suppliedBibentryBracketExpansionTest() {
        BibDatabase database = null;
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        BibEntry another_bibentry = new BibEntry();
        another_bibentry.setField("author", "Gražulis, Saulius");
        another_bibentry.setField("year", "2017");
        another_bibentry.setField("pages", "213--216");
        assertEquals("2017_Gražulis_213", bex.expandBrackets("[year]_[auth]_[firstpage]", ';', another_bibentry, database));
    }

    @Test(expected = NullPointerException.class)
    public void nullBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BracketedExpressionExpander bex = new BracketedExpressionExpander(bibentry);
        BibEntry another_bibentry = null;
        bex.expandBrackets("[year]_[auth]_[firstpage]", ';', another_bibentry, another_database);
    }

    @Test
    public void testFieldAndFormat() {
        Character separator = ';';
        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedExpressionExpander.expandBrackets("[author]", separator, dbentry, database));

        // assertEquals("Eric von Hippel and Georg von Krogh",
        //         BracketedExpressionExpander.expandBrackets("author", separator, dbentry, database));

        assertEquals("", BracketedExpressionExpander.expandBrackets("[unknownkey]", separator, dbentry, database));

        assertEquals("", BracketedExpressionExpander.expandBrackets("[:]", separator, dbentry, database));

        assertEquals("", BracketedExpressionExpander.expandBrackets("[:lower]", separator, dbentry, database));

        assertEquals("eric von hippel and georg von krogh",
                BracketedExpressionExpander.expandBrackets("[author:lower]", separator, dbentry, database));

        assertEquals("HipKro03", BracketedExpressionExpander.expandBrackets("[bibtexkey]", separator, dbentry, database));

        assertEquals("HipKro03", BracketedExpressionExpander.expandBrackets("[bibtexkey:]", separator, dbentry, database));
    }

}
