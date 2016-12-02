package net.sf.jabref.logic.bibtex;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


public class EntryTypesTestBibtex {

    @Test
    public void testBibtexMode() {
        // Bibtex mode
        assertEquals(BibtexEntryTypes.ARTICLE, EntryTypes.getType("article", BibDatabaseMode.BIBTEX).get());
        assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", BibDatabaseMode.BIBTEX));
        assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", BibDatabaseMode.BIBTEX));

        assertEquals("Values: " + EntryTypes.getAllValues(BibDatabaseMode.BIBTEX).stream().map(entryType -> entryType.getName()).collect(Collectors.toList()), 19, EntryTypes.getAllValues(BibDatabaseMode.BIBTEX).size());
        assertEquals(19, EntryTypes.getAllTypes(BibDatabaseMode.BIBTEX).size());

        // Edit the "article" entry type
        ArrayList<String> requiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        requiredFields.add("specialfield");

        CustomEntryType newArticle = new CustomEntryType("article", requiredFields,
                BibtexEntryTypes.ARTICLE.getOptionalFields());

        EntryTypes.addOrModifyCustomEntryType(newArticle, BibDatabaseMode.BIBTEX);
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
