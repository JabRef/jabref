package org.jabref.gui.fieldeditors;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class GroupsEditorViewModelTest {

    private GroupsEditorViewModel viewModel;
    private StringProperty mergedValue;

    @BeforeEach
    void setUp() {
        viewModel = new GroupsEditorViewModel(StandardField.GROUPS, mock(SuggestionProvider.class), mock(FieldCheckers.class), ',', mock(UndoManager.class));
        mergedValue = new SimpleStringProperty("Used, Paywalled");
        viewModel.textProperty().bindBidirectional(mergedValue);
    }

    @Test
    void showsTextAsGroups() {
        assertEquals(List.of(new Keyword("Used"), new Keyword("Paywalled")), viewModel.groupListProperty());
    }

    @Test
    void updatesGroupsWhenTextChanges() {
        mergedValue.set("Read");

        assertEquals(List.of(new Keyword("Read")), viewModel.groupListProperty());
    }

    @Test
    void updatesTextWhenGroupIsRemoved() {
        viewModel.groupListProperty().remove(new Keyword("Paywalled"));

        assertEquals("Used", mergedValue.get());
    }
}
