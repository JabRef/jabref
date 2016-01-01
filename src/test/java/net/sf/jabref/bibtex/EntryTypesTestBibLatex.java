package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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
        EntryTypes.resetTypeInformation();
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.overwritePreferences(backup);
    }

    @Test
    public void testBibLatexMode() {
        // BibLatex mode
        Assert.assertTrue(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
        assertEquals(BibLatexEntryTypes.ARTICLE, EntryTypes.getType("article"));
        assertNull(EntryTypes.getType("aaaaarticle"));
        assertNull(EntryTypes.getStandardType("aaaaarticle"));
        assertEquals(34, EntryTypes.getAllValues().size());
        assertEquals(34, EntryTypes.getAllTypes().size());

        EntryTypes.removeType("article");
        // Should not be possible to remove a standard type
        assertEquals(BibLatexEntryTypes.ARTICLE, EntryTypes.getType("article"));
    }

    @Test
    public void defaultType() {
        Assert.assertTrue(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
        assertEquals(BibLatexEntryTypes.MISC, EntryTypes.getTypeOrDefault("unknowntype"));
    }

    @Test
    public void testIsThisBibLatex() {
        assertTrue(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
    }

}
