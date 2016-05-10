package net.sf.jabref.bibtex;

import java.util.ArrayList;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class EntryTypesTestBibtex {

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
    public void testBibtexMode() {
        // Bibtex mode
        assertEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBTEX).get());
        assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", BibDatabaseMode.BIBTEX));
        assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", BibDatabaseMode.BIBTEX));
        assertEquals(19, EntryTypes.getAllValues(BibDatabaseMode.BIBTEX).size());
        assertEquals(19, EntryTypes.getAllTypes(BibDatabaseMode.BIBTEX).size());

        // Edit the "article" entry type
        ArrayList<String> requiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        requiredFields.add("specialfield");

        CustomEntryType newArticle = new CustomEntryType("article", requiredFields,
                BibtexEntryTypes.ARTICLE.getOptionalFields());

        EntryTypes.addOrModifyCustomEntryType(newArticle);
        // Should not be the same any more
        assertNotEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBTEX).get());

        // Remove the custom "article" entry type, which should restore the original
        EntryTypes.removeType("article", BibDatabaseMode.BIBTEX);
        // Should not be possible to remove a standard type
        assertEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBTEX).get());
    }

    @Test
    public void defaultType() {
        assertEquals(BibtexEntryTypes.MISC, EntryTypes.getTypeOrDefault("unknowntype", BibDatabaseMode.BIBTEX));
    }

}
