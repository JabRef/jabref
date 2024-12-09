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
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexEntryTypeDefinitions;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
