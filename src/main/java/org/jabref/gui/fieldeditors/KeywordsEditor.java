package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class KeywordsEditor extends SimpleEditor implements FieldEditorFX {

    public KeywordsEditor(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        super(field, suggestionProvider, fieldCheckers, preferences);
    }

    @Override
    public double getWeight() {
        return 2;
    }
}
