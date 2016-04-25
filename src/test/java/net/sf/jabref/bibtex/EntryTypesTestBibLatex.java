package net.sf.jabref.bibtex;

import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibLatexEntryTypes;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntryTypesTestBibLatex {

    private JabRefPreferences backup;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        backup = Globals.prefs;
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.overwritePreferences(backup);
    }

    @Test
    @Ignore
    public void testBibLatexMode() {
        // BibLatex mode
        assertEquals(BibLatexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBLATEX).get());
        assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", BibDatabaseMode.BIBLATEX));
        assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", BibDatabaseMode.BIBLATEX));
        assertEquals(34, EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX).size());
        assertEquals(34, EntryTypes.getAllTypes(BibDatabaseMode.BIBLATEX).size());

        EntryTypes.removeType("article", BibDatabaseMode.BIBLATEX);
        // Should not be possible to remove a standard type
        assertEquals(BibLatexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBLATEX).get());
    }

    @Test
    public void defaultType() {
        assertEquals(BibLatexEntryTypes.MISC, EntryTypes.getTypeOrDefault("unknowntype", BibDatabaseMode.BIBLATEX));
    }


}
