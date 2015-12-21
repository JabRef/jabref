package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import java.util.ArrayList;
import org.junit.After;
import org.junit.Assert;
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
        EntryTypes.resetTypeInformation();
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.overwritePreferences(backup);
    }

    @Test
    public void testBibtexMode() {
        // Bibtex mode
        Assert.assertFalse(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));

        assertEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article"));
        assertNull(EntryTypes.getType("aaaaarticle"));
        assertNull(EntryTypes.getStandardType("aaaaarticle"));
        assertEquals(19, EntryTypes.getAllValues().size());
        assertEquals(19, EntryTypes.getAllTypes().size());

        // Edit the "article" entry type
        ArrayList<String> requiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        requiredFields.add("specialfield");

        CustomEntryType newArticle = new CustomEntryType("article", requiredFields,
                BibtexEntryTypes.ARTICLE.getOptionalFields());

        EntryTypes.addOrModifyCustomEntryType(newArticle);
        // Should not be the same any more
        assertNotEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article"));

        // Remove the custom "article" entry type, which should restore the original
        EntryTypes.removeType("article");
        // Should not be possible to remove a standard type
        assertEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article"));
    }

    @Test
    public void defaultType() {
        assertEquals(BibtexEntryTypes.MISC, EntryTypes.getTypeOrDefault("unknowntype"));
    }

    @Test
    public void testIsThisBibtex() {
        assertFalse(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE));
    }

}
