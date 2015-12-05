package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibtexEntryTypes;


public class EntryTypesTestBibtex {

    private JabRefPreferences backup;


    @Before
    public void setUp() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();
        backup = Globals.prefs;
        Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, false);
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.overwritePreferences(backup);
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

    @Test
    public void testIsThisBibtex() {
        assertFalse(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
    }

}
