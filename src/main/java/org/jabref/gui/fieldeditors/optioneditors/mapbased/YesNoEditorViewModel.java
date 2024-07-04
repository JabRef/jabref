package org.jabref.gui.fieldeditors.optioneditors.mapbased;

import java.util.Map;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class YesNoEditorViewModel extends StringMapBasedEditorViewModel {

    private BiMap<String, String> itemMap = HashBiMap.create(2);

    public YesNoEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager, Map.of(
                "yes", "Yes",
                "no", "No"
        ));
    }
}
