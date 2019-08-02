package org.jabref.gui.fieldeditors;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.JabRefPreferences;

public class MultilineEditor extends SimpleEditor implements FieldEditorFX {

    public MultilineEditor(Field field, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, JabRefPreferences preferences) {
        super(field, suggestionProvider, fieldCheckers, preferences);
    }

    @Override
    public double getWeight() {
        return 4;
    }
}
