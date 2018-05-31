package org.jabref.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.IEEETranEntryTypes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class EntryTypesTest {

    private final BibDatabaseMode mode;
    private final BibDatabaseMode otherMode;
    private final EntryType standardArticleType;
    private final EntryType defaultType;

    private CustomEntryType newCustomType;
    private CustomEntryType overwrittenStandardType;

    public EntryTypesTest(BibDatabaseMode mode) {
        this.mode = mode;
        this.otherMode = (mode == BibDatabaseMode.BIBLATEX) ? BibDatabaseMode.BIBTEX : BibDatabaseMode.BIBLATEX;
        this.standardArticleType = (mode == BibDatabaseMode.BIBLATEX) ? BiblatexEntryTypes.ARTICLE : BibtexEntryTypes.ARTICLE;
        this.defaultType = (mode == BibDatabaseMode.BIBLATEX) ? BiblatexEntryTypes.MISC : BibtexEntryTypes.MISC;
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[] {BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX};
    }

    @Before
    public void setUp() {
        newCustomType = new CustomEntryType("customType", "required", "optional");
        List<String> newRequiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        newRequiredFields.add("additional");
        overwrittenStandardType = new CustomEntryType(BibtexEntryTypes.ARTICLE.getName(), newRequiredFields,
                Collections.singletonList("optional"));
    }

    @After
    public void tearDown() {
        EntryTypes.removeAllCustomEntryTypes();
    }

    @Test
    public void assertDefaultValuesBibtex() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BibtexEntryTypes.ALL);
        sortedDefaultType.addAll(IEEETranEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(EntryTypes.getAllValues(BibDatabaseMode.BIBTEX));
        Collections.sort(sortedEntryTypes);

        assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @Test
    public void assertDefaultValuesBiblatex() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BiblatexEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX));
        Collections.sort(sortedEntryTypes);

        assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @Test
    public void unknownTypeIsNotFound() {
        assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", mode));
        assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", mode));
    }

    @Test
    public void unknownTypeIsConvertedToMiscByGetTypeOrDefault() {
        assertEquals(defaultType, EntryTypes.getTypeOrDefault("unknowntype", mode));
    }

    @Test
    public void registerCustomEntryType() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        assertEquals(Optional.of(newCustomType), EntryTypes.getType("customType", mode));
    }

    @Test
    public void registeredCustomEntryTypeIsContainedInListOfCustomizedEntryTypes() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        assertEquals(Arrays.asList(newCustomType), EntryTypes.getAllCustomTypes(mode));
    }

    @Test
    public void registerCustomEntryTypeDoesNotAffectOtherMode() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        assertFalse(EntryTypes.getAllValues(otherMode).contains(newCustomType));
    }

    @Test
    public void overwriteCustomEntryTypeFields() {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        CustomEntryType newCustomEntryTypeAuthorRequired = new CustomEntryType("customType", FieldName.AUTHOR, "optional");
        EntryTypes.addOrModifyCustomEntryType(newCustomEntryTypeAuthorRequired, mode);
        assertEquals(Optional.of(newCustomEntryTypeAuthorRequired), EntryTypes.getType("customType", mode));
    }

    @Test
    public void overwriteStandardTypeRequiredFields() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertEquals(Optional.of(overwrittenStandardType), EntryTypes.getType(overwrittenStandardType.getName(), mode));
    }

    @Test
    public void registeredCustomizedStandardEntryTypeIsContainedInListOfCustomizedEntryTypes() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertEquals(Arrays.asList(overwrittenStandardType), EntryTypes.getAllModifiedStandardTypes(mode));
    }


    @Test
    public void standardTypeIsStillAcessibleIfOverwritten() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertEquals(Optional.of(standardArticleType), EntryTypes.getStandardType(overwrittenStandardType.getName(), mode));
    }

    @Test
    public void standardTypeIsRestoredAfterDeletionOfOverwrittenType() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        EntryTypes.removeType(overwrittenStandardType.getName(), mode);
        assertEquals(Optional.of(standardArticleType), EntryTypes.getType(overwrittenStandardType.getName(), mode));
    }

    @Test
    public void standardTypeCannotBeRemoved() {
        EntryTypes.removeType(standardArticleType.getName(), mode);
        assertEquals(Optional.of(standardArticleType), EntryTypes.getType(standardArticleType.getName(), mode));
    }

    @Test
    public void overwriteStandardTypeRequiredFieldsDoesNotAffectOtherMode() {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertFalse(EntryTypes.getAllValues(otherMode).contains(overwrittenStandardType));
    }
}
