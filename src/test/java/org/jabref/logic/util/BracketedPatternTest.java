package org.jabref.logic.util;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.BibtexString;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class BracketedPatternTest {
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
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry));
    }

    @Test
    public void nullDatabaseExpansionTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry, another_database));
    }

    @Test
    public void emptyDatabaseExpansionTest() {
        BibDatabase another_database = new BibDatabase();
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Kitsune_213", pattern.expand(bibentry, another_database));
    }

    @Test
    public void databaseWithStringsExpansionTest() {
        BibDatabase another_database = new BibDatabase();
        BibtexString string = new BibtexString("sgr", "Saulius Gražulis");
        another_database.addString(string);
        bibentry = new BibEntry();
        bibentry.setField("author", "#sgr#");
        bibentry.setField("year", "2017");
        bibentry.setField("pages", "213--216");
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        assertEquals("2017_Gražulis_213", pattern.expand(bibentry,
                another_database));
    }

    @Test
    public void unbalancedBracketExpansionTest() {
        // FIXME: this test throws the ugly 'java.lang.IllegalStateException: Toolkit not initialized'
        // exception for some reason; the exception should not occur in the application! Should figure
        // out how to suppress it.
        BracketedPattern pattern = new BracketedPattern("[year]_[auth_[firstpage]");
        assertNotEquals("", pattern.expand(bibentry));
    }

    @Test
    public void unbalancedLastBracketExpansionTest() {
        // FIXME: this test throws the ugly 'java.lang.IllegalStateException: Toolkit not initialized'
        // exception for some reason; the exception should not occur in the application! Should figure
        // out how to suppress it.
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage");
        assertNotEquals("", pattern.expand(bibentry));
    }

    @Test
    public void entryTypeExpansionTest() {
        BracketedPattern pattern = new BracketedPattern("[entrytype]:[year]_[auth]_[pages]");
        assertEquals("Misc:2017_Kitsune_213--216", pattern.expand(bibentry));
    }

    @Test
    public void entryTypeExpansionLowercaseTest() {
        BracketedPattern pattern = new BracketedPattern("[entrytype:lower]:[year]_[auth]_[firstpage]");
        assertEquals("misc:2017_Kitsune_213", pattern.expand(bibentry));
    }

    @Test
    public void suppliedBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        BibEntry another_bibentry = new BibEntry();
        another_bibentry.setField("author", "Gražulis, Saulius");
        another_bibentry.setField("year", "2017");
        another_bibentry.setField("pages", "213--216");
        assertEquals("2017_Gražulis_213", pattern.expand(another_bibentry, ';', another_database));
    }

    @Test(expected = NullPointerException.class)
    public void nullBibentryBracketExpansionTest() {
        BibDatabase another_database = null;
        BibEntry another_bibentry = null;
        BracketedPattern pattern = new BracketedPattern("[year]_[auth]_[firstpage]");
        pattern.expand(another_bibentry, ';', another_database);
        // The control should not reach this point, exception should be triggered:
        assert(false);
    }

    @Test(expected = NullPointerException.class)
    public void brachetedExpressionDefaultConstructorTest() {
        BibDatabase another_database = null;
        BracketedPattern pattern = new BracketedPattern();
        pattern.expand(bibentry, ';', another_database);
        // The control should not reach this point, exception should be triggered:
        assert (false);
    }

    @Test
    public void testFieldAndFormat() {
        Character separator = ';';
        assertEquals("Eric von Hippel and Georg von Krogh",
                BracketedPattern.expandBrackets("[author]", separator, dbentry, database));

        assertEquals("", BracketedPattern.expandBrackets("[unknownkey]", separator, dbentry, database));

        assertEquals("", BracketedPattern.expandBrackets("[:]", separator, dbentry, database));

        assertEquals("", BracketedPattern.expandBrackets("[:lower]", separator, dbentry, database));

        assertEquals("eric von hippel and georg von krogh",
                BracketedPattern.expandBrackets("[author:lower]", separator, dbentry, database));

        assertEquals("HipKro03", BracketedPattern.expandBrackets("[bibtexkey]", separator, dbentry, database));

        assertEquals("HipKro03", BracketedPattern.expandBrackets("[bibtexkey:]", separator, dbentry, database));
    }

}
