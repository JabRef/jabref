package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import net.sf.jabref.model.entry.BibtexEntryTypes;
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
        Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, true);
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
        assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article"));
        assertNull(biblatexentrytypes.getType("aaaaarticle"));
        assertNull(biblatexentrytypes.getStandardType("aaaaarticle"));
        assertEquals(34, biblatexentrytypes.getAllValues().size());
        assertEquals(34, biblatexentrytypes.getAllTypes().size());

        biblatexentrytypes.removeType("article");
        // Should not be possible to remove a standard type
        assertEquals(BibLatexEntryTypes.ARTICLE, biblatexentrytypes.getType("article"));
    }

    @Test
    public void defaultType() {
        EntryTypes types = new EntryTypes();
        assertEquals(BibLatexEntryTypes.MISC, types.getTypeOrDefault("unknowntype"));
    }

    @Test
    public void testIsThisBibLatex() {
        assertTrue(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
    }

}
