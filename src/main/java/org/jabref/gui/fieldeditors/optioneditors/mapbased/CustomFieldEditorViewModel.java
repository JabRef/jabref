package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;

public class CustomFieldEditorViewModel extends StringMapBasedEditorViewModel {

    public CustomFieldEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider,
                                      FieldCheckers fieldCheckers, UndoManager undoManager, BibDatabaseContext databaseContext) {
        super(field, suggestionProvider, fieldCheckers, undoManager, getMap(databaseContext, field));
    }

    private static Map<String, String> getMap(BibDatabaseContext databaseContext, Field field) {
        List<String> values = databaseContext.getMetaData().getContentSelectorValuesForField(field);
        Map<String, String> map = new HashMap<>();
        for (String value : values) {
            map.put(value, value);
        }
        return map;
    }
}
