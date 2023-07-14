package org.jabref.gui.libraryproperties.constants;

import java.util.Collection;
import java.util.List;

import javafx.beans.property.ListProperty;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibtexString;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConstantsPropertiesViewModelTest {

    @DisplayName("Check that the list of strings is sorted according to their keys")
    @Test
    void testStringsListPropertySorting() {
        DialogService service = mock(DialogService.class);
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        BibDatabase db = mock(BibDatabase.class);
        BibtexString string1 = new BibtexString("TSE", "Transactions on Software Engineering");
        BibtexString string2 = new BibtexString("ICSE", "International Conference on Software Engineering");
        Collection<BibtexString> stringValues = List.of(string1, string2);
        when(db.getStringValues()).thenReturn(stringValues);
        when(context.getDatabase()).thenReturn(db);

        ConstantsPropertiesViewModel model = new ConstantsPropertiesViewModel(context, service);
        model.setValues();

        ListProperty<ConstantsItemModel> strings = model.stringsListProperty();
        assertAll(
                () -> assertEquals("ICSE", strings.get(0).labelProperty().getValue()),
                () -> assertEquals("TSE", strings.get(1).labelProperty().getValue()));
    }
}
