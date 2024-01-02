package org.jabref.gui.libraryproperties.constants;

import java.util.List;

import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConstantsPropertiesViewModelTest {

    private DialogService service;
    private FilePreferences filePreferences;

    @BeforeEach
    void setUp() {
        service = mock(DialogService.class);
        filePreferences = mock(FilePreferences.class);
    }

    @DisplayName("Check that the list of strings is sorted according to their keys")
    @Test
    void testStringsListPropertySorting() {
        BibtexString string1 = new BibtexString("TSE", "Transactions on Software Engineering");
        BibtexString string2 = new BibtexString("ICSE", "International Conference on Software Engineering");
        BibDatabase db = new BibDatabase();
        db.setStrings(List.of(string1, string2));
        BibDatabaseContext context = new BibDatabaseContext(db);
        List<String> expected = List.of(string2.getName(), string1.getName()); // ICSE before TSE

        ConstantsPropertiesViewModel model = new ConstantsPropertiesViewModel(context, service, filePreferences);
        model.setValues();

        List<String> actual = model.stringsListProperty().stream()
                .map(ConstantsItemModel::labelProperty)
                .map(StringProperty::getValue)
                .toList();

        assertEquals(expected, actual);
    }

    @DisplayName("Check that the list of strings is sorted after resorting it")
    @Test
    void testStringsListPropertyResorting() {
        BibDatabase db = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(db);
        List<String> expected = List.of("ICSE", "TSE");

        ConstantsPropertiesViewModel model = new ConstantsPropertiesViewModel(context, service, filePreferences);
        var stringsList = model.stringsListProperty();
        stringsList.add(new ConstantsItemModel("TSE", "Transactions on Software Engineering"));
        stringsList.add(new ConstantsItemModel("ICSE", "International Conference on Software Engineering"));

        model.resortStrings();

        List<String> actual = model.stringsListProperty().stream()
                .map(ConstantsItemModel::labelProperty)
                .map(StringProperty::getValue)
                .toList();

        assertEquals(expected, actual);
    }
}
