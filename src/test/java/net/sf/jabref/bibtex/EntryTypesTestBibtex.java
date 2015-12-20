package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;


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

        // Edit the "article" entry type
        ArrayList<String> requiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        requiredFields.add("specialfield");

        CustomEntryType newArticle = new CustomEntryType("article", requiredFields,
                BibtexEntryTypes.ARTICLE.getOptionalFields());

        bibtexentrytypes.addOrModifyCustomEntryType(newArticle);
        // Should not be the same any more
        assertNotEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article"));

        // Remove the custom "article" entry type, which should restore the original
        bibtexentrytypes.removeType("article");
        // Should not be possible to remove a standard type
        assertEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article"));
    }

    @Test
    public void defaultType() {
        EntryTypes types = new EntryTypes();
        assertEquals(BibtexEntryTypes.MISC, types.getTypeOrDefault("unknowntype"));
    }

    @Test
    public void testIsThisBibtex() {
        assertFalse(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
    }

}
