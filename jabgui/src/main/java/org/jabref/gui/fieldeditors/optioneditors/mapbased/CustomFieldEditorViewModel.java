package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

public class CustomFieldEditorViewModel extends StringMapBasedEditorViewModel {

    public CustomFieldEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider,
                                      FieldCheckers fieldCheckers, UndoManager undoManager, List<String> selectorValues) {
        super(field, suggestionProvider, fieldCheckers, undoManager, getMap(selectorValues));
    }

    private static Map<String, String> getMap(List<String> selectorValues) {
        Map<String, String> map = new HashMap<>();
        for (String value : selectorValues) {
            map.put(value, value);
        }
        return map;
    }
}
