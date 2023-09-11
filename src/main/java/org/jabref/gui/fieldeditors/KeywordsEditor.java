package org.jabref.gui.fieldeditors;

import javax.swing.undo.UndoManager;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

public class KeywordsEditor extends SimpleEditor implements FieldEditorFX {

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers,
                          PreferencesService preferences,
                          UndoManager undoManager) {
        super(field, suggestionProvider, fieldCheckers, preferences, undoManager);
    }

    @Override
    public double getWeight() {
        return 2;
    }
}
