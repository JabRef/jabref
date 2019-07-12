package org.jabref.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.EntryTypeFactory;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.model.entry.StandardEntryType;
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

    private static final EntryType UNKNOWN_TYPE = EntryTypeFactory.parse("aaaaarticle");
    private static final EntryType CUSTOM_TYPE = EntryTypeFactory.parse("customType");
    private BibEntryType newCustomType;
    private BibEntryType overwrittenStandardType;

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
    }

    @Test
    void allTypesBibtexAreCorrect() {
        List<BibEntryType> defaultTypes = new ArrayList<>(BibtexEntryTypes.ALL);
        defaultTypes.addAll(IEEETranEntryTypes.ALL);

        assertEquals(defaultTypes, BibEntryTypesManager.getAllTypes(BibDatabaseMode.BIBTEX));
    }

    @Test
    void allTypesBiblatexAreCorrect() {
        List<BibEntryType> defaultTypes = new ArrayList<>(BiblatexEntryTypes.ALL);
        assertEquals(defaultTypes, BibEntryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void unknownTypeIsNotFound(BibDatabaseMode mode) {
        assertEquals(Optional.empty(), BibEntryTypesManager.enrich(UNKNOWN_TYPE, mode));
        assertFalse(BibEntryTypesManager.isCustomType(UNKNOWN_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void newCustomEntryTypeFound(BibDatabaseMode mode) {
        BibEntryTypesManager.addCustomizedEntryType(newCustomType, mode);
        assertEquals(Optional.of(newCustomType), BibEntryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredBibEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        BibEntryTypesManager.addCustomizedEntryType(newCustomType, mode);
        assertEquals(Collections.singletonList(newCustomType), BibEntryTypesManager.getAllCustomTypes(mode));
    }

    @Test
    void registerBibEntryTypeDoesNotAffectOtherMode() {
        BibEntryTypesManager.addCustomizedEntryType(newCustomType, BibDatabaseMode.BIBTEX);
        assertFalse(BibEntryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX).contains(newCustomType));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteBibEntryTypeFields(BibDatabaseMode mode) {
        BibEntryTypesManager.addCustomizedEntryType(newCustomType, mode);
        BibEntryType newBibEntryTypeTitle = new BibEntryType(
                CUSTOM_TYPE,
                Collections.singleton(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Collections.emptySet());
        BibEntryTypesManager.addCustomizedEntryType(newBibEntryTypeTitle, mode);
        assertEquals(Optional.of(newBibEntryTypeTitle), BibEntryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void overwriteStandardTypeRequiredFields(BibDatabaseMode mode) {
        BibEntryTypesManager.addCustomizedEntryType(overwrittenStandardType, mode);
        assertEquals(Optional.of(overwrittenStandardType), BibEntryTypesManager.enrich(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void registeredCustomizedStandardEntryTypeIsNotContainedInListOfCustomEntryTypes(BibDatabaseMode mode) {
        BibEntryTypesManager.addCustomizedEntryType(overwrittenStandardType, mode);
        assertEquals(Collections.emptyList(), BibEntryTypesManager.getAllCustomTypes(mode));
    }

    @ParameterizedTest
    @MethodSource("mode")
    void standardTypeIsStillAccessibleIfOverwritten(BibDatabaseMode mode) {
        BibEntryTypesManager.addCustomizedEntryType(overwrittenStandardType, mode);
        assertFalse(BibEntryTypesManager.isCustomType(overwrittenStandardType.getType(), mode));
    }

    @Test
    void overwriteStandardTypeRequiredFieldsDoesNotAffectOtherMode() {
        BibEntryTypesManager.addCustomizedEntryType(overwrittenStandardType, BibDatabaseMode.BIBTEX);
        assertFalse(BibEntryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX).contains(overwrittenStandardType));
    }
}
