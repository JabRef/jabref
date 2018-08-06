package org.jabref.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.IEEETranEntryTypes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class EntryTypesTest {

    private CustomEntryType newCustomType;
    private CustomEntryType overwrittenStandardType;

    private static Stream<Object> data4mode() {
        return Stream.of(BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX);
    }

    private static Stream<Arguments> data4defaultTypeAndmode() {
        return Stream.of(
                Arguments.of(BiblatexEntryTypes.MISC, BibDatabaseMode.BIBLATEX),
                Arguments.of(BibtexEntryTypes.MISC, BibDatabaseMode.BIBTEX));
    }

    private static Stream<Arguments> data4modeAndotherMode() {
        return Stream.of(
                Arguments.of(BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX),
                Arguments.of(BibDatabaseMode.BIBLATEX, BibDatabaseMode.BIBTEX));
    }

    private static Stream<Arguments> data4standardArticleTypeandmode() {
        return Stream.of(
                Arguments.of(BiblatexEntryTypes.ARTICLE, BibDatabaseMode.BIBLATEX),
                Arguments.of(BibtexEntryTypes.ARTICLE, BibDatabaseMode.BIBTEX));
    }

    @BeforeEach
    void setUp() {
        newCustomType = new CustomEntryType("customType", "required", "optional");
        List<String> newRequiredFields = new ArrayList<>(BibtexEntryTypes.ARTICLE.getRequiredFields());
        newRequiredFields.add("additional");
        overwrittenStandardType = new CustomEntryType(BibtexEntryTypes.ARTICLE.getName(), newRequiredFields,
                Collections.singletonList("optional"));
    }

    @AfterEach
    void tearDown() {
        EntryTypes.removeAllCustomEntryTypes();
    }

    @Test
    void assertDefaultValuesBibtex() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BibtexEntryTypes.ALL);
        sortedDefaultType.addAll(IEEETranEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(EntryTypes.getAllValues(BibDatabaseMode.BIBTEX));
        Collections.sort(sortedEntryTypes);

        Assertions.assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @Test
    void assertDefaultValuesBiblatex() {
        List<EntryType> sortedDefaultType = new ArrayList<>(BiblatexEntryTypes.ALL);
        Collections.sort(sortedDefaultType);

        List<EntryType> sortedEntryTypes = new ArrayList<>(EntryTypes.getAllValues(BibDatabaseMode.BIBLATEX));
        Collections.sort(sortedEntryTypes);

        Assertions.assertEquals(sortedDefaultType, sortedEntryTypes);
    }

    @ParameterizedTest
    @MethodSource("data4mode")
    void unknownTypeIsNotFound(BibDatabaseMode mode) {
        Assertions.assertEquals(Optional.empty(), EntryTypes.getType("aaaaarticle", mode));
        Assertions.assertEquals(Optional.empty(), EntryTypes.getStandardType("aaaaarticle", mode));
    }

    @ParameterizedTest
    @MethodSource("data4defaultTypeAndmode")
    void unknownTypeIsConvertedToMiscByGetTypeOrDefault(EntryType defaultType, BibDatabaseMode mode) {
        Assertions.assertEquals(defaultType, EntryTypes.getTypeOrDefault("unknowntype", mode));
    }

    @ParameterizedTest
    @MethodSource("data4mode")
    void registerCustomEntryType(BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        Assertions.assertEquals(Optional.of(newCustomType), EntryTypes.getType("customType", mode));
    }

    @ParameterizedTest
    @MethodSource("data4mode")
    void registeredCustomEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        Assertions.assertEquals(Arrays.asList(newCustomType), EntryTypes.getAllCustomTypes(mode));
    }

    @ParameterizedTest
    @MethodSource("data4modeAndotherMode")
    void registerCustomEntryTypeDoesNotAffectOtherMode(BibDatabaseMode mode, BibDatabaseMode otherMode) {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        Assertions.assertFalse(EntryTypes.getAllValues(otherMode).contains(newCustomType));
    }

    @ParameterizedTest
    @MethodSource("data4mode")
    void overwriteCustomEntryTypeFields(BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(newCustomType, mode);
        CustomEntryType newCustomEntryTypeAuthorRequired = new CustomEntryType("customType", FieldName.AUTHOR, "optional");
        EntryTypes.addOrModifyCustomEntryType(newCustomEntryTypeAuthorRequired, mode);
        Assertions.assertEquals(Optional.of(newCustomEntryTypeAuthorRequired), EntryTypes.getType("customType", mode));
    }

    @ParameterizedTest
    @MethodSource("data4mode")
    void overwriteStandardTypeRequiredFields(BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        Assertions.assertEquals(Optional.of(overwrittenStandardType), EntryTypes.getType(overwrittenStandardType.getName(), mode));
    }

    @ParameterizedTest
    @MethodSource("data4mode")
    void registeredCustomizedStandardEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        Assertions.assertEquals(Arrays.asList(overwrittenStandardType), EntryTypes.getAllModifiedStandardTypes(mode));
    }


    @ParameterizedTest
    @MethodSource("data4standardArticleTypeandmode")
    void standardTypeIsStillAcessibleIfOverwritten(EntryType standardArticleType, BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        Assertions.assertEquals(Optional.of(standardArticleType), EntryTypes.getStandardType(overwrittenStandardType.getName(), mode));
    }

    @ParameterizedTest
    @MethodSource("data4standardArticleTypeandmode")
    void standardTypeIsRestoredAfterDeletionOfOverwrittenType(EntryType standardArticleType, BibDatabaseMode mode) {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        EntryTypes.removeType(overwrittenStandardType.getName(), mode);
        Assertions.assertEquals(Optional.of(standardArticleType), EntryTypes.getType(overwrittenStandardType.getName(), mode));
    }

    @ParameterizedTest
    @MethodSource("data4standardArticleTypeandmode")
    void standardTypeCannotBeRemoved(EntryType standardArticleType, BibDatabaseMode mode) {
        EntryTypes.removeType(standardArticleType.getName(), mode);
        Assertions.assertEquals(Optional.of(standardArticleType), EntryTypes.getType(standardArticleType.getName(), mode));
    }


    @ParameterizedTest
    @MethodSource("data4modeAndotherMode")
    void overwriteStandardTypeRequiredFieldsDoesNotAffectOtherMode(BibDatabaseMode mode, BibDatabaseMode otherMode) {
        EntryTypes.addOrModifyCustomEntryType(overwrittenStandardType, mode);
        Assertions.assertFalse(EntryTypes.getAllValues(otherMode).contains(overwrittenStandardType));
    }

}
