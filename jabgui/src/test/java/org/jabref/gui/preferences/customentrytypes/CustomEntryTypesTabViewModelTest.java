package org.jabref.gui.preferences.customentrytypes;

import java.util.List;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomEntryTypesTabViewModelTest {

    private BibEntryType online;

    private BibEntryTypesManager entryTypesManager;
    private FieldPreferences fieldPreferences;
    private CliPreferences preferences;

    @BeforeEach
    void setup() {
        preferences = mock(CliPreferences.class);
        fieldPreferences = mock(FieldPreferences.class);
        when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.observableArrayList());
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);
        entryTypesManager = new BibEntryTypesManager();
        online = BiblatexEntryTypeDefinitions.ALL.stream().filter(type -> type.getType().equals(StandardEntryType.Online)).findAny().get();
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void storeSettingsKeepsStandardTypes(BibDatabaseMode mode) {
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(mode, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();
        model.storeSettings();
        assertEquals(new TreeSet<>(), entryTypesManager.getAllCustomizedTypes(mode));
    }

    @Test
    void storeSettingsKeepsTypeWhenOrFieldsDiffersOnly() {
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();

        // This is similar ot the standard online type, but has no OR fields
        BibEntryType onlineWithoutOrFields = new BibEntryTypeBuilder()
                .withType(StandardEntryType.Online)
                .withRequiredFields(StandardField.AUTHOR, StandardField.EDITOR, StandardField.TITLE, StandardField.DATE, StandardField.URL)
                .withImportantFields(
                        StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.URLDATE)
                .withDetailFields(StandardField.LANGUAGE, StandardField.VERSION,
                        StandardField.ADDENDUM, StandardField.PUBSTATE)
                .build();
        model.entryTypes().setAll(List.of(new CustomEntryTypeViewModel(onlineWithoutOrFields, x -> false)));

        model.storeSettings();

        assertEquals(new TreeSet<>(), entryTypesManager.getAllCustomizedTypes(BibDatabaseMode.BIBLATEX));
    }

    @Test
    void storeSettingsUpdatesType() {
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();

        // No important optional fields anymore (they are required now)
        BibEntryType modified = new BibEntryTypeBuilder()
                .withType(StandardEntryType.Online)
                .withRequiredFields(StandardField.AUTHOR, StandardField.EDITOR, StandardField.TITLE, StandardField.DATE, StandardField.URL, StandardField.SUBTITLE, StandardField.TITLEADDON, StandardField.NOTE, StandardField.ORGANIZATION, StandardField.URLDATE)
                .withDetailFields(StandardField.LANGUAGE, StandardField.VERSION,
                        StandardField.ADDENDUM, StandardField.PUBSTATE)
                .build();
        model.entryTypes().setAll(List.of(new CustomEntryTypeViewModel(modified, x -> false)));

        model.storeSettings();

        TreeSet<BibEntryType> expected = new TreeSet<>(List.of(modified));
        assertEquals(expected, entryTypesManager.getAllCustomizedTypes(BibDatabaseMode.BIBLATEX));
    }

    @Test
    void unchangedSaveHasNoRestartWarning() {
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();

        model.storeSettings();

        assertEquals(List.of(), model.getRestartWarnings());
    }

    @Test
    void unchangedSaveWithFieldOutsideEntryTypesHasNoRestartWarning() {
        when(preferences.getFieldPreferences()).thenReturn(new FieldPreferences(true, List.of(), List.of(StandardField.PDF, StandardField.PS, StandardField.URL)));
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();

        model.storeSettings();

        assertEquals(List.of(), model.getRestartWarnings());
    }

    @Test
    void changedTypeHasRestartWarningUntilNextUnchangedSave() {
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();
        BibEntryType modified = new BibEntryTypeBuilder()
                .withType(StandardEntryType.Online)
                .withRequiredFields(StandardField.TITLE)
                .build();
        model.entryTypes().setAll(List.of(new CustomEntryTypeViewModel(modified, x -> false)));

        model.storeSettings();
        assertEquals(List.of("Entry types changed."), model.getRestartWarnings());

        model.storeSettings();
        assertEquals(List.of(), model.getRestartWarnings());
    }

    @Test
    void changedFieldPropertyHasRestartWarning() {
        UnknownField field = new UnknownField("custom");
        entryTypesManager.update(new BibEntryTypeBuilder().withType(new UnknownEntryType("mytype")).withImportantFields(field).build(), BibDatabaseMode.BIBLATEX);
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();

        model.entryTypes().stream()
             .filter(type -> "mytype".equals(type.entryType().getValue().getType().getName()))
             .findFirst().orElseThrow()
             .fields().getFirst()
             .getProperties().add(FieldProperty.DATE);
        model.storeSettings();

        assertEquals(List.of("Entry types changed."), model.getRestartWarnings());
    }

    @Test
    void resetBeforeSaveHasRestartWarning() {
        entryTypesManager.update(new BibEntryTypeBuilder().withType(new UnknownEntryType("mytype")).withRequiredFields(StandardField.TITLE).build(), BibDatabaseMode.BIBLATEX);
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.setValues();

        model.resetAllCustomEntryTypes();
        model.setValues();
        model.storeSettings();

        assertEquals(List.of("Entry types changed."), model.getRestartWarnings());
    }

    @Test
    void resetMultilinePropertyOfStandardFieldsToDefault() {
        CustomEntryTypesTabViewModel model = new CustomEntryTypesTabViewModel(BibDatabaseMode.BIBLATEX, entryTypesManager, mock(DialogService.class), preferences);
        model.resetMultilineFieldsToDefault();

        StandardField fieldTitle = StandardField.TITLE;
        assertFalse(fieldTitle.getProperties().contains(FieldProperty.MULTILINE_TEXT));
        fieldTitle.getProperties().add(FieldProperty.MULTILINE_TEXT);
        assertTrue(fieldTitle.getProperties().contains(FieldProperty.MULTILINE_TEXT));
        model.resetMultilineFieldsToDefault();
        assertFalse(fieldTitle.getProperties().contains(FieldProperty.MULTILINE_TEXT));

        StandardField fieldAbstract = StandardField.ABSTRACT;
        assertTrue(fieldAbstract.getProperties().contains(FieldProperty.MULTILINE_TEXT));
        fieldAbstract.getProperties().remove(FieldProperty.MULTILINE_TEXT);
        assertFalse(fieldAbstract.getProperties().contains(FieldProperty.MULTILINE_TEXT));
        model.resetMultilineFieldsToDefault();
        assertTrue(fieldAbstract.getProperties().contains(FieldProperty.MULTILINE_TEXT));
    }
}
