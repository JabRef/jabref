package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibtexEntryTypes;


public class EntryTypesTestBibtex {

    Boolean biblatex;

    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        biblatex = Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE);
        Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, false);
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, biblatex);
    }

    @Test
    public void testBibtexMode() {
        // Bibtex mode
        EntryTypes bibtexentrytypes = new EntryTypes();
        assertEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article"));
        assertNull(bibtexentrytypes.getType("aaaaarticle"));
        assertNull(bibtexentrytypes.getStandardType("aaaaarticle"));
        assertEquals(19, bibtexentrytypes.getAllValues().size());
        assertEquals(19, bibtexentrytypes.getAllTypes().size());

        assertEquals(BibtexEntryTypes.MISC, bibtexentrytypes.getBibtexEntryType("aaaaarticle"));

        bibtexentrytypes.removeType("article");
        // Should not be possible to remove a standard type
        assertEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article"));

    }

}
