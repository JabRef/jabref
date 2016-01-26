package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import net.sf.jabref.model.database.BibDatabaseType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibLatexEntryTypes;


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
        EntryTypes biblatexentrytypes = new EntryTypes();
        assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article", BibDatabaseType.BIBLATEX));
        assertNull(biblatexentrytypes.getType("aaaaarticle", BibDatabaseType.BIBLATEX));
        assertNull(biblatexentrytypes.getStandardType("aaaaarticle", BibDatabaseType.BIBLATEX));
        assertEquals(34, biblatexentrytypes.getAllValues(BibDatabaseType.BIBLATEX).size());
        assertEquals(34, biblatexentrytypes.getAllTypes(BibDatabaseType.BIBLATEX).size());

        biblatexentrytypes.removeType("article", BibDatabaseType.BIBLATEX);
        // Should not be possible to remove a standard type
        assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article", BibDatabaseType.BIBLATEX));
    }

    @Test
    public void defaultType() {
        EntryTypes types = new EntryTypes();
        assertEquals(BibLatexEntryTypes.MISC, types.getTypeOrDefault("unknowntype", BibDatabaseType.BIBLATEX));
    }


}
