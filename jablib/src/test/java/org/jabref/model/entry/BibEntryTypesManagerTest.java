package org.jabref.model.entry;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jabref.architecture.AllowedToUseLogic;
import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.FieldPriority;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexAPAEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexNonStandardEntryTypeDefinitions;
import org.jabref.model.entry.types.BiblatexSoftwareEntryTypeDefinitions;
import org.jabref.model.entry.types.BibtexEntryTypeDefinitions;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@AllowedToUseLogic("Requires MetaDataSerializer and MetaDataParser for parsing tests")
class BibEntryTypesManagerTest {

    private static final EntryType UNKNOWN_TYPE = new UnknownEntryType("unknownType");
    private static final EntryType CUSTOM_TYPE = new UnknownEntryType("customType");

    private BibEntryType newCustomType;
    private BibEntryType overwrittenStandardType;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        newCustomType = new BibEntryType(
                CUSTOM_TYPE,
                List.of(new BibField(StandardField.AUTHOR, FieldPriority.IMPORTANT)),
                Set.of());
        overwrittenStandardType = new BibEntryType(
                StandardEntryType.Article,
                List.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Set.of());
        entryTypesManager = new BibEntryTypesManager();
    }

    private BibEntryType getStandardArticleType(BibDatabaseMode mode) {
        return entryTypesManager.getEntryTypes(mode).standardTypes.stream().filter(t -> StandardEntryType.Article == t.getType()).findAny().get();
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
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
        defaultTypes.addAll(BiblatexSoftwareEntryTypeDefinitions.ALL);
        defaultTypes.addAll(BiblatexAPAEntryTypeDefinitions.ALL);
        defaultTypes.addAll(BiblatexNonStandardEntryTypeDefinitions.ALL);

        assertEquals(defaultTypes, entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX));
    }

    @Test
    void nonStandardTypesNotInBibtexMode() {
        Set<EntryType> nonStandardTypes = BiblatexNonStandardEntryTypeDefinitions.ALL.stream()
                                                                                     .map(BibEntryType::getType)
                                                                                     .collect(Collectors.toSet());

        Set<EntryType> bibtexTypes = entryTypesManager.getAllTypes(BibDatabaseMode.BIBTEX).stream()
                                                      .map(BibEntryType::getType)
                                                      .collect(Collectors.toSet());

        for (EntryType nonStandardType : nonStandardTypes) {
            assertFalse(bibtexTypes.contains(nonStandardType),
                    "Non-standard type " + nonStandardType.getName() + " should not be in BibTeX mode");
        }
    }

    @Test
    void nonStandardTypesCanBeEnrichedInBiblatexMode() {
        for (BibEntryType nonStandardType : BiblatexNonStandardEntryTypeDefinitions.ALL) {
            Optional<BibEntryType> enriched = entryTypesManager.enrich(nonStandardType.getType(), BibDatabaseMode.BIBLATEX);

            assertTrue(enriched.isPresent(),
                    "Type " + nonStandardType.getType().getName() + " should be enrichable in BibLaTeX mode");
            assertEquals(nonStandardType.getType(), enriched.get().getType());
        }
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void unknownTypeIsNotFound(BibDatabaseMode mode) {
        assertEquals(Optional.empty(), entryTypesManager.enrich(UNKNOWN_TYPE, mode));
        assertFalse(entryTypesManager.isCustomType(UNKNOWN_TYPE, mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void newCustomEntryTypeFound(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(newCustomType, mode);
        assertEquals(Optional.of(newCustomType), entryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void registeredBibEntryTypeIsContainedInListOfCustomizedEntryTypes(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(newCustomType, mode);
        assertEquals(List.of(newCustomType), entryTypesManager.getAllCustomTypes(mode));
    }

    @Test
    void registerBibEntryTypeDoesNotAffectOtherMode() {
        entryTypesManager.addCustomOrModifiedType(newCustomType, BibDatabaseMode.BIBTEX);
        assertFalse(entryTypesManager.getAllTypes(BibDatabaseMode.BIBLATEX).contains(newCustomType));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void overwriteBibEntryTypeFields(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(newCustomType, mode);
        BibEntryType newBibEntryTypeTitle = new BibEntryType(
                CUSTOM_TYPE,
                Set.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT)),
                Set.of());
        entryTypesManager.addCustomOrModifiedType(newBibEntryTypeTitle, mode);
        assertEquals(Optional.of(newBibEntryTypeTitle), entryTypesManager.enrich(CUSTOM_TYPE, mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void overwriteStandardTypeRequiredFields(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertEquals(Optional.of(overwrittenStandardType), entryTypesManager.enrich(overwrittenStandardType.getType(), mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void registeredCustomizedStandardEntryTypeIsNotContainedInListOfCustomEntryTypes(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertEquals(List.of(), entryTypesManager.getAllCustomTypes(mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void standardTypeIsStillAccessibleIfOverwritten(BibDatabaseMode mode) {
        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertFalse(entryTypesManager.isCustomType(overwrittenStandardType, mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void modifyingArticleWithUpdate(BibDatabaseMode mode) {
        entryTypesManager.update(overwrittenStandardType, mode);
        BibEntryType enriched = entryTypesManager.enrich(StandardEntryType.Article, mode).get();
        assertEquals(overwrittenStandardType, enriched);
        assertNotEquals(getStandardArticleType(mode), enriched);
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void isDifferentCustomOrModifiedType(BibDatabaseMode mode) {
        entryTypesManager.update(overwrittenStandardType, mode);
        assertTrue(entryTypesManager.isCustomOrModifiedType(overwrittenStandardType, mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void resettingArticleWithUpdate(BibDatabaseMode mode) {
        entryTypesManager.update(overwrittenStandardType, mode);
        // Change back to standard article
        entryTypesManager.update(getStandardArticleType(mode), mode);
        assertFalse(entryTypesManager.isCustomOrModifiedType(overwrittenStandardType, mode));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void modifyingArticle(BibDatabaseMode mode) {
        overwrittenStandardType = new BibEntryType(
                StandardEntryType.Article,
                List.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.NUMBER, FieldPriority.IMPORTANT),
                        new BibField(StandardField.LANGUAGEID, FieldPriority.IMPORTANT),
                        new BibField(StandardField.COMMENT, FieldPriority.IMPORTANT)),
                Set.of());

        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        assertEquals(List.of(overwrittenStandardType), entryTypesManager.getAllTypes(mode).stream().filter(t -> "article".equals(t.getType().getName())).collect(Collectors.toList()));
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void modifyingArticleWithParsing(BibDatabaseMode mode) {
        overwrittenStandardType = new BibEntryType(
                StandardEntryType.Article,
                List.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.NUMBER, FieldPriority.IMPORTANT),
                        new BibField(StandardField.LANGUAGEID, FieldPriority.IMPORTANT),
                        new BibField(StandardField.COMMENT, FieldPriority.IMPORTANT)),
                Set.of());

        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        String serialized = MetaDataSerializer.serializeCustomEntryTypes(overwrittenStandardType);
        Optional<BibEntryType> type = MetaDataParser.parseCustomEntryType(serialized);

        assertEquals(Optional.of(overwrittenStandardType), type);
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void modifyingArticleWithParsingKeepsListOrder(BibDatabaseMode mode) {
        overwrittenStandardType = new BibEntryType(
                StandardEntryType.Article,
                List.of(new BibField(StandardField.TITLE, FieldPriority.IMPORTANT),
                        new BibField(StandardField.NUMBER, FieldPriority.IMPORTANT),
                        new BibField(StandardField.LANGUAGEID, FieldPriority.IMPORTANT),
                        new BibField(StandardField.COMMENT, FieldPriority.IMPORTANT)),
                Set.of());

        entryTypesManager.addCustomOrModifiedType(overwrittenStandardType, mode);
        String serialized = MetaDataSerializer.serializeCustomEntryTypes(overwrittenStandardType);
        Optional<BibEntryType> type = MetaDataParser.parseCustomEntryType(serialized);

        assertEquals(overwrittenStandardType.getOptionalFields(), type.get().getOptionalFields());
    }

    @Test
    void translatorDetailOptionalAtArticle() {
        BibEntryType entryType = entryTypesManager.enrich(StandardEntryType.Article, BibDatabaseMode.BIBLATEX).get();
        assertTrue(entryType.getDetailOptionalFields().contains(StandardField.TRANSLATOR));
    }
}
