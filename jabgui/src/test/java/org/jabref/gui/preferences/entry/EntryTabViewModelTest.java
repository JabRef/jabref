package org.jabref.gui.preferences.entry;

import java.util.List;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryTabViewModelTest {

    private FieldPreferences fieldPreferences;
    private EntryTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        fieldPreferences = FieldPreferences.getDefault();
        viewModel = new EntryTabViewModel(
                BibEntryPreferences.getDefault(),
                fieldPreferences,
                OwnerPreferences.getDefault(),
                TimestampPreferences.getDefault());
        viewModel.setValues();
    }

    @Test
    void storeSettingsKeepsNonWrappableFieldsEnteredByUser() {
        List<Field> nonWrappableFields = List.of(StandardField.URL, StandardField.DOI, new UnknownField("abc"));
        viewModel.nonWrappableTagsFieldProperty().setAll(nonWrappableFields);
        viewModel.resolvableTagsFieldProperty().setAll(List.of(StandardField.AUTHOR, StandardField.JOURNAL));

        viewModel.storeSettings();

        assertEquals(nonWrappableFields, fieldPreferences.getNonWrappableFields());
    }

    @Test
    void storeSettingsKeepsResolvableFieldsEnteredByUser() {
        List<Field> resolvableFields = List.of(StandardField.AUTHOR, StandardField.JOURNAL);
        viewModel.resolvableTagsFieldProperty().setAll(resolvableFields);
        viewModel.nonWrappableTagsFieldProperty().setAll(List.of(StandardField.URL));

        viewModel.storeSettings();

        assertEquals(resolvableFields, fieldPreferences.getResolvableFields());
    }
}
