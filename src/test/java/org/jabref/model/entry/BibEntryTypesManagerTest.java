package org.jabref.model.entry;

import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibEntryTypesManagerTest {

    private static final EntryType UNKNOWN_TYPE = new UnknownEntryType("unknownType");
    private static final EntryType CUSTOM_TYPE = new UnknownEntryType("customType");
    private BibEntryType newCustomType;
    private BibEntryType overwrittenStandardType;
    private BibEntryTypesManager entryTypesManager;

    private static Stream<BibDatabaseMode> mode() {
        return Stream.of(BibDatabaseMode.BIBTEX, BibDatabaseMode.BIBLATEX);
    }

    @BeforeEach
    void setUp() {
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                Collections.singleton(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT)),
                Collections.emptySet());
        overwrittenStandardType = new BibEntryType(
                StandardEntryType.Article,
                Collections.singleton(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Collections.emptySet());
        entryTypesManager = new BibEntryTypesManager();
    }

    @ParameterizedTest
    @MethodSource("mode")
    void isCustomOrModifiedTypeReturnsTrueForModifiedStandardEntryType(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertTrue(entryTypesManager.isCustomOrModifiedType(overwrittenStandardType, mode));
    }

    @Test
    void allTypesBibtexAreCorrect() {
        TreeSet<BibEntryType> defaultTypes = new TreeSet<>(BibtexEntryTypeDefinitions.ALL);
        defaultTypes.addAll(IEEETranEntryTypeDefinitions.ALL);

        assertEquals(defaultTypes, entryTypesManager.getAllTypes(BibDatabaseMode.BIBTEX));
    }

    @Test
    void allTypesBiblatexAreCorrect() {
        TreeSet<BibEntryType> defaultTypes = new TreeSet<>(BiblatexEntryTypeDefinitions.ALL);
        assertEquals(defaultTypes, entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void unknownTypeIsNotFound(BibDatabaseMode mode) {
        assertEquals(Optional.empty(), entryTypesManager.enrich(UNKNOWN_TYPE, mode));
        assertFalse(entryTypesManager.isCustomType(UNKNOWN_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void newCustomEntryTypeFound(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(newCustomType, mode);
        assertEquals(Optional.of(newCustomType), entryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredBibEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(newCustomType, mode);
        assertEquals(Collections.singletonList(newCustomType), entryTypesManager.getAllCustomTypes(mode));
    }

    @Test
    void registerBibEntryTypeDoesNotAffectOtherMode() {
        entryTypesManager.addCustomOrModifiedType(newCustomType, BibDatabaseMode.BIBTEX);
        assertFalse(entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX).contains(newCustomType));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteBibEntryTypeFields(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(newCustomType, mode);
        BibEntryType newBibEntryTypeTitle = new BibEntryType(
                CUSTOM_TYPE,
                Collections.singleton(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Collections.emptySet());
        entryTypesManager.addCustomOrModifiedType(newBibEntryTypeTitle, mode);
        assertEquals(Optional.of(newBibEntryTypeTitle), entryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteStandardTypeRequiredFields(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertEquals(Optional.of(overwrittenStandardType), entryTypesManager.enrich(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredCustomizedStandardEntryTypeIsNotContainedInListOfCustomEntryTypes(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertEquals(Collections.emptyList(), entryTypesManager.getAllCustomTypes(mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void standardTypeIsStillAccessibleIfOverwritten(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertFalse(entryTypesManager.isCustomType(overwrittenStandardType.getType(), mode));
    }
}
