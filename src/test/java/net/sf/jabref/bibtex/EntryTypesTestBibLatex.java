package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibLatexEntryTypes;


public class EntryTypesTestBibLatex {

    Boolean biblatex;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        biblatex = Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE);
        Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, true);
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, biblatex);
    }

    @Test
    public void testBibLatexMode() {
        // BibLatex mode
        EntryTypes biblatexentrytypes = new EntryTypes();
        assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article"));
        assertNull(biblatexentrytypes.getType("aaaaarticle"));
        assertNull(biblatexentrytypes.getStandardType("aaaaarticle"));
        assertEquals(34, biblatexentrytypes.getAllValues().size());
        assertEquals(34, biblatexentrytypes.getAllTypes().size());

        assertEquals(BibLatexEntryTypes.MISC, biblatexentrytypes.getBibtexEntryType("aaaaarticle"));

        biblatexentrytypes.removeType("article");
        // Should not be possible to remove a standard type
        assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article"));

    }

}
