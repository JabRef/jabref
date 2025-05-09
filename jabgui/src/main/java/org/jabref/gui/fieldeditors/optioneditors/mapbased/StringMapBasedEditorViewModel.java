package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public abstract class StringMapBasedEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap;

    public StringMapBasedEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager, Map<String, String> entries) {
        super(field, suggestionProvider, fieldCheckers, undoManager);

        itemMap = HashBiMap.create(entries);
    }

    @Override
    protected BiMap<String, String> getItemMap() {
        return itemMap;
    }

    @Override
    public String convertToDisplayText(String object) {
        return object;
    }
}
