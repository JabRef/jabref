package org.jabref.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BibEntryTypeFactoryTest {

    private CustomEntryType newCustomType;
    private CustomEntryType overwrittenStandardType;

    private static Stream<Object> mode() {
        return Stream.of(BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX);
    }

    private static Stream<Arguments> defaultTypeAndMode() {
        return Stream.of(
                Arguments.of(BiblatexEntryTypes.MISC, BibDatabaseMode.BIBLATEX),
                Arguments.of(BibtexEntryTypes.MISC, BibDatabaseMode.BIBTEX));
    }

    private static Stream<Arguments> modeAndOtherMode() {
        return Stream.of(
                Arguments.of(BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX),
                Arguments.of(BibDatabaseMode.BIBLATEX, BibDatabaseMode.BIBTEX));
    }

    private static Stream<Arguments> standardArticleTypeAndMode() {
        return Stream.of(
                Arguments.of(StandardEntryType.ARTICLE, BibDatabaseMode.BIBLATEX),
                Arguments.of(StandardEntryType.ARTICLE, BibDatabaseMode.BIBTEX));
    }

    @BeforeEach
    void setUp() {
        newCustomType = new CustomEntryType("customType", "required", "optional");
        List<String> newRequiredFields = new ArrayList<>(StandardEntryType.ARTICLE.getRequiredFields());
        newRequiredFields.add("additional");
        overwrittenStandardType = new CustomEntryType(StandardEntryType.ARTICLE.getType(), newRequiredFields,
                Collections.singletonList("optional"));
    }

    @AfterEach
    void tearDown() {
        BibEntryTypesManager.removeAllCustomEntryTypes();
    }

    @Test
    void assertDefaultValuesBibtex() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BibtexEntryTypes.ALL);
        sortedDefaultType.addAll(IEEETranEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(BibEntryTypesManager.getAllValues(BibDatabaseMode.BIBTEX));
        Collections.sort(sortedEntryTypes);

        assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @Test
    void assertDefaultValuesBiblatex() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BiblatexEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(BibEntryTypesManager.getAllValues(BibDatabaseMode.BIBLATEX));
        Collections.sort(sortedEntryTypes);

        assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @ParameterizedTest
    @MethodSource("mode")
    void unknownTypeIsNotFound(BibDatabaseMode mode) {
        assertEquals(Optional.empty(), BibEntryTypesManager.getType("aaaaarticle", mode));
        assertEquals(Optional.empty(), BibEntryTypesManager.getStandardType("aaaaarticle", mode));
    }

    @ParameterizedTest
    @MethodSource("defaultTypeAndMode")
    void unknownTypeIsConvertedToMiscByGetTypeOrDefault(EntryType defaultType, BibDatabaseMode mode) {
        assertEquals(defaultType, BibEntryTypesManager.getTypeOrDefault("unknowntype", mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registerCustomEntryType(BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(newCustomType, mode);
        assertEquals(Optional.of(newCustomType), BibEntryTypesManager.getType("customType", mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredCustomEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(newCustomType, mode);
        assertEquals(Arrays.asList(newCustomType), BibEntryTypesManager.getAllCustomTypes(mode));
    }

    @ParameterizedTest
    @MethodSource("modeAndOtherMode")
    void registerCustomEntryTypeDoesNotAffectOtherMode(BibDatabaseMode mode, BibDatabaseMode otherMode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(newCustomType, mode);
        assertFalse(BibEntryTypesManager.getAllValues(otherMode).contains(newCustomType));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteCustomEntryTypeFields(BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(newCustomType, mode);
        CustomEntryType newCustomEntryTypeAuthorRequired = new CustomEntryType("customType", StandardField.AUTHOR, "optional");
        BibEntryTypesManager.addOrModifyCustomEntryType(newCustomEntryTypeAuthorRequired, mode);
        assertEquals(Optional.of(newCustomEntryTypeAuthorRequired), BibEntryTypesManager.getType("customType", mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteStandardTypeRequiredFields(BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertEquals(Optional.of(overwrittenStandardType), BibEntryTypesManager.getType(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredCustomizedStandardEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertEquals(Arrays.asList(overwrittenStandardType), BibEntryTypesManager.getAllModifiedStandardTypes(mode));
    }

    @ParameterizedTest
    @MethodSource("standardArticleTypeAndMode")
    void standardTypeIsStillAcessibleIfOverwritten(EntryType standardArticleType, BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertEquals(Optional.of(standardArticleType), BibEntryTypesManager.getStandardType(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("standardArticleTypeAndMode")
    void standardTypeIsRestoredAfterDeletionOfOverwrittenType(EntryType standardArticleType, BibDatabaseMode mode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        BibEntryTypesManager.removeType(overwrittenStandardType.getType(), mode);
        assertEquals(Optional.of(standardArticleType), BibEntryTypesManager.getType(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("standardArticleTypeAndMode")
    void standardTypeCannotBeRemoved(EntryType standardArticleType, BibDatabaseMode mode) {
        BibEntryTypesManager.removeType(standardArticleType.getType(), mode);
        assertEquals(Optional.of(standardArticleType), BibEntryTypesManager.getType(standardArticleType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("modeAndOtherMode")
    void overwriteStandardTypeRequiredFieldsDoesNotAffectOtherMode(BibDatabaseMode mode, BibDatabaseMode otherMode) {
        BibEntryTypesManager.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        assertFalse(BibEntryTypesManager.getAllValues(otherMode).contains(overwrittenStandardType));
    }
}
