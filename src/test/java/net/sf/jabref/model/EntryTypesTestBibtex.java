package net.sf.jabref.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.IEEETranEntryTypes;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EntryTypesTestBibtex {

    private CustomEntryType newCustomType;
    private CustomEntryType overwrittenStandardType;

    @Before
    public void setUp() {
        newCustomType = new CustomEntryType("customType", "required", "optional");
        List<String> newRequiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        newRequiredFields.add("additional");
        overwrittenStandardType = new CustomEntryType(BibtexEntryTypes.ARTICLE.getName(), newRequiredFields,
                Collections.singletonList("optional"));
    }

    @Test
    public void assertDefaultValues() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BibtexEntryTypes.ALL);
        sortedDefaultType.addAll(IEEETranEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(EntryTypes.getAllValues(BibDatabaseMode.BIBTEX));
        Collections.sort(sortedEntryTypes);

        assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @Test
    public void unknownTypeIsNotFound() {
        assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", BibDatabaseMode.BIBTEX));
        assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", BibDatabaseMode.BIBTEX));
    }

    @Test
    public void unknownTypeIsConvertedToMiscByGetTypeOrDefault() {
        assertEquals(BibtexEntryTypes.MISC, EntryTypes.getTypeOrDefault("unknowntype", BibDatabaseMode.BIBTEX));
    }

    @Test
    public void registerCustomEntryType() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, BibDatabaseMode.BIBTEX);
        assertEquals(Optional.of(newCustomType), EntryTypes.getType("customType", BibDatabaseMode.BIBTEX));
    }

    @Test
    public void registerCustomEntryTypeDoesNotAffectBiblatex() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, BibDatabaseMode.BIBTEX);
        assertFalse(EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX).contains(newCustomType));
    }

    @Test
    public void overwriteCustomEntryTypeFields() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, BibDatabaseMode.BIBTEX);
        CustomEntryType newCustomEntryTypeAuthorRequired = new CustomEntryType("customType", FieldName.AUTHOR, "optional");
        EntryTypes.addOrModifyCustomEntryType(newCustomEntryTypeAuthorRequired, BibDatabaseMode.BIBTEX);
        assertEquals(Optional.of(newCustomEntryTypeAuthorRequired), EntryTypes.getType("customType", BibDatabaseMode.BIBTEX));
    }

    @Test
    public void overwriteStandardTypeRequiredFields() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, BibDatabaseMode.BIBTEX);
        assertEquals(Optional.of(overwrittenStandardType), EntryTypes.getType(overwrittenStandardType.getName(), BibDatabaseMode.BIBTEX));
    }

    @Test
    public void standardTypeIsStillAcessibleIfOverwritten() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, BibDatabaseMode.BIBTEX);
        assertEquals(Optional.of(BibtexEntryTypes.ARTICLE), EntryTypes.getStandardType(BibtexEntryTypes.ARTICLE.getName(), BibDatabaseMode.BIBTEX));
    }

    @Test
    public void standardTypeIsRestoredAfterDeletionOfOverwrittenType() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, BibDatabaseMode.BIBTEX);
        EntryTypes.removeType(overwrittenStandardType.getName(), BibDatabaseMode.BIBTEX);
        assertEquals(Optional.of(BibtexEntryTypes.ARTICLE), EntryTypes.getType(overwrittenStandardType.getName(), BibDatabaseMode.BIBTEX));
    }

    @Test
    public void standardTypeCannotBeRemoved() {
        EntryTypes.removeType(BibtexEntryTypes.BOOK.getName(), BibDatabaseMode.BIBTEX);
        assertEquals(Optional.of(BibtexEntryTypes.BOOK), EntryTypes.getType(BibtexEntryTypes.BOOK.getName(), BibDatabaseMode.BIBTEX));
    }

    @Test
    public void overwriteStandardTypeRequiredFieldsDoesNotAffectBiblatex() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, BibDatabaseMode.BIBTEX);
        assertFalse(EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX).contains(overwrittenStandardType));
    }




}
