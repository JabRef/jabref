package net.sf.jabref.bibtex;

import static org.junit.Assert.*;

import java.util.ArrayList;

import net.sf.jabref.model.database.BibDatabaseMode;
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
    }

    @After
    public void tearDown() throws Exception {
        Globals.prefs.overwritePreferences(backup);
    }

    @Test
    public void testBibtexMode() {
        // Bibtex mode
        EntryTypes bibtexentrytypes = new EntryTypes();

        assertEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article", BibDatabaseMode.BIBTEX));
        assertNull(bibtexentrytypes.getType("aaaaarticle", BibDatabaseMode.BIBTEX));
        assertNull(bibtexentrytypes.getStandardType("aaaaarticle", BibDatabaseMode.BIBTEX));
        assertEquals(19, bibtexentrytypes.getAllValues(BibDatabaseMode.BIBTEX).size());
        assertEquals(19, bibtexentrytypes.getAllTypes(BibDatabaseMode.BIBTEX).size());

        // Edit the "article" entry type
        ArrayList<String> requiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        requiredFields.add("specialfield");

        CustomEntryType newArticle = new CustomEntryType("article", requiredFields,
                BibtexEntryTypes.ARTICLE.getOptionalFields());

        bibtexentrytypes.addOrModifyCustomEntryType(newArticle);
        // Should not be the same any more
        assertNotEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article", BibDatabaseMode.BIBTEX));

        // Remove the custom "article" entry type, which should restore the original
        bibtexentrytypes.removeType("article", BibDatabaseMode.BIBTEX);
        // Should not be possible to remove a standard type
        assertEquals(BibtexEntryTypes.ARTICLE, bibtexentrytypes.getType("article", BibDatabaseMode.BIBTEX));
    }

    @Test
    public void defaultType() {
        EntryTypes types = new EntryTypes();
        assertEquals(BibtexEntryTypes.MISC, types.getTypeOrDefault("unknowntype", BibDatabaseMode.BIBTEX));
    }

}
