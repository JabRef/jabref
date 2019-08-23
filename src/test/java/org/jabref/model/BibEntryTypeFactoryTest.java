package org.jabref.model;

import java.util.Collections;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.model.entry.StandardEntryType;
import org.jabref.model.entry.UnknownEntryType;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BibEntryTypeFactoryTest {

    private static final EntryType UNKNOWN_TYPE = new UnknownEntryType("aaaaarticle");
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

    @Test
    void allTypesBibtexAreCorrect() {
        TreeSet<BibEntryType> defaultTypes = new TreeSet<>(BibtexEntryTypes.ALL);
        defaultTypes.addAll(IEEETranEntryTypes.ALL);

        assertEquals(defaultTypes, entryTypesManager.getAllTypes(BibDatabaseMode.BIBTEX));
    }

    @Test
    void allTypesBiblatexAreCorrect() {
        TreeSet<BibEntryType> defaultTypes = new TreeSet<>(BiblatexEntryTypes.ALL);
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
        entryTypesManager.addCustomizedEntryType(newCustomType, mode);
        assertEquals(Optional.of(newCustomType), entryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredBibEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        entryTypesManager.addCustomizedEntryType(newCustomType, mode);
        assertEquals(Collections.singletonList(newCustomType), entryTypesManager.getAllCustomTypes(mode));
    }

    @Test
    void registerBibEntryTypeDoesNotAffectOtherMode() {
        entryTypesManager.addCustomizedEntryType(newCustomType, BibDatabaseMode.BIBTEX);
        assertFalse(entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX).contains(newCustomType));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteBibEntryTypeFields(BibDatabaseMode mode) {
        entryTypesManager.addCustomizedEntryType(newCustomType, mode);
        BibEntryType newBibEntryTypeTitle = new BibEntryType(
                CUSTOM_TYPE,
                Collections.singleton(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Collections.emptySet());
        entryTypesManager.addCustomizedEntryType(newBibEntryTypeTitle, mode);
        assertEquals(Optional.of(newBibEntryTypeTitle), entryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteStandardTypeRequiredFields(BibDatabaseMode mode) {
        entryTypesManager.addCustomizedEntryType(overwrittenStandardType, mode);
        assertEquals(Optional.of(overwrittenStandardType), entryTypesManager.enrich(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredCustomizedStandardEntryTypeIsNotContainedInListOfCustomEntryTypes(BibDatabaseMode mode) {
        entryTypesManager.addCustomizedEntryType(overwrittenStandardType, mode);
        assertEquals(Collections.emptyList(), entryTypesManager.getAllCustomTypes(mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void standardTypeIsStillAccessibleIfOverwritten(BibDatabaseMode mode) {
        entryTypesManager.addCustomizedEntryType(overwrittenStandardType, mode);
        assertFalse(entryTypesManager.isCustomType(overwrittenStandardType.getType(), mode));
    }
}
