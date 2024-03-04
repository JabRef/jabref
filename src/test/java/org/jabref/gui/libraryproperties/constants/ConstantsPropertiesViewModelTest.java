package org.jabref.gui.libraryproperties.constants;

import java.util.List;
import java.util.stream.Stream;

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
    void stringsListPropertySorting() {
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
    void stringsListPropertyResorting() {
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

   @Test
   @DisplayName("Check that the storeSettings method store settings on the model")
   void storeSettingsTest() {
       // Setup
       // create a bibdatabse
       BibDatabase db = new BibDatabase();
       BibDatabaseContext context = new BibDatabaseContext(db);
       List<String> expected = List.of("KTH", "Royal Institute of Technology");
       // initialize a constantsPropertiesViewModel
       ConstantsPropertiesViewModel model = new ConstantsPropertiesViewModel(context, service, filePreferences);

       // construct value to store in model
       var stringsList = model.stringsListProperty();
       stringsList.add(new ConstantsItemModel("KTH", "Royal Institute of Technology"));

       // Act
       model.storeSettings();

       // Assert
       // get the names stored
       List<String> names = context.getDatabase().getStringValues().stream()
                                    .map(BibtexString::getName).toList();
       // get the content stored
       List<String> content = context.getDatabase().getStringValues().stream()
                                          .map(BibtexString::getContent).toList();

       List<String> actual = Stream.concat(names.stream(), content.stream()).toList();

       assertEquals(expected, actual);
   }

    @Test
    @DisplayName("Check that the storeSettings method can identify string constants")
    void storeSettingsWithStringConstantTest() {
        // Setup
        // create a bibdatabse
        BibDatabase db = new BibDatabase();
        BibDatabaseContext context = new BibDatabaseContext(db);
        List<String> expected = List.of("@String{KTH = Royal Institute of Technology}");
        // initialize a constantsPropertiesViewModel
        ConstantsPropertiesViewModel model = new ConstantsPropertiesViewModel(context, service, filePreferences);

        // construct value to store in model
        var stringsList = model.stringsListProperty();
        stringsList.add(new ConstantsItemModel("KTH", "Royal Institute of Technology"));

        // Act
        model.storeSettings();

        // Assert
        // get string the constants through parsedSerialization() method
        List<String> actual = context.getDatabase().getStringValues().stream()
                                     .map(BibtexString::getParsedSerialization).toList();

        // get the first value and clean strings
        String actual_value = actual.getFirst().replaceAll("\\s+", " ").trim();
        String expected_value = expected.getFirst().replaceAll("\\s+", " ").trim();

        assertEquals(expected_value, actual_value);
    }
}
